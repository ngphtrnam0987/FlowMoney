package com.flowmoney.app.domain.screenshot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.flowmoney.app.MainActivity
import com.flowmoney.app.R
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.domain.engine.ReceiptOcrParser
import com.flowmoney.app.receiver.QuickSaveExpenseReceiver
import com.flowmoney.app.util.CurrencyUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotDetectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrParser: ReceiptOcrParser,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val TAG = "ScreenshotDetector"
        const val CHANNEL_ID = "flowmoney_screenshot_channel"
        const val ACTION_VIEW_SCREENSHOT = "com.flowmoney.app.ACTION_VIEW_SCREENSHOT_EXPENSE"
        private const val MAX_PROCESSED_CACHE = 50
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val processedIds = Collections.synchronizedSet(LinkedHashSet<Long>())
    private var isListening = false

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            scope.launch {
                checkForNewScreenshot()
            }
        }
    }

    fun startListening() {
        if (isListening) return
        createNotificationChannel()

        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
            isListening = true
            Log.d(TAG, "Screenshot detector started listening")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register content observer: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            context.contentResolver.unregisterContentObserver(contentObserver)
            isListening = false
            Log.d(TAG, "Screenshot detector stopped listening")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister content observer: ${e.message}")
        }
    }

    private suspend fun checkForNewScreenshot() {
        val isEnabled = settingsRepository.isScreenshotDetectionEnabledFlow.first()
        if (!isEnabled) return

        if (!hasStoragePermission()) {
            Log.d(TAG, "Storage permission not granted, skipping screenshot scan")
            return
        }

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val dateAdded = cursor.getLong(dateCol)
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""

                    // Check if it's recent (within last 120 seconds)
                    val nowSec = System.currentTimeMillis() / 1000
                    if (nowSec - dateAdded > 120) return

                    // Check if already processed
                    if (processedIds.contains(id)) return

                    // Check if it looks like a screenshot
                    val isScreenshot = name.contains("screenshot", ignoreCase = true) ||
                            path.contains("screenshot", ignoreCase = true) ||
                            path.contains("Screenshots", ignoreCase = true) ||
                            name.startsWith("Screenshot", ignoreCase = true)

                    if (isScreenshot) {
                        recordProcessedId(id)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        processScreenshotImage(contentUri, id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for screenshots: ${e.message}", e)
        }
    }

    private suspend fun processScreenshotImage(uri: Uri, imageId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Decode bitmap with downsampling if large
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Good resolution for OCR while saving memory
                }
                val stream = context.contentResolver.openInputStream(uri) ?: return@withContext
                val bitmap = BitmapFactory.decodeStream(stream, null, options)
                stream.close()

                if (bitmap == null) return@withContext

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsed = ocrParser.parseOcrText(visionText.text)
                        if (parsed.isBankTransfer && parsed.totalMinor > 0) {
                            showBankTransferDetectedNotification(uri, imageId, parsed)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "OCR failed on screenshot: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read screenshot image: ${e.message}", e)
            }
        }
    }

    private fun showBankTransferDetectedNotification(
        imageUri: Uri,
        imageId: Long,
        parsed: com.flowmoney.app.domain.engine.ParsedReceiptData
    ) {
        if (!hasNotificationPermission()) return

        val notifId = (10000 + (imageId % 80000)).toInt()

        // Intent to open MainActivity and show transaction in ScanScreen
        val viewIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_VIEW_SCREENSHOT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("image_uri", imageUri.toString())
            putExtra("amount_minor", parsed.totalMinor)
            putExtra("merchant_name", parsed.merchantName)
            putExtra("note", parsed.transactionNote)
            putExtra("bank_name", parsed.bankName)
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick Save direct action button
        val saveIntent = Intent(context, QuickSaveExpenseReceiver::class.java).apply {
            action = QuickSaveExpenseReceiver.ACTION_QUICK_SAVE_EXPENSE
            putExtra("amount_minor", parsed.totalMinor)
            putExtra("merchant_name", parsed.merchantName)
            putExtra("note", parsed.transactionNote)
            putExtra("bank_name", parsed.bankName)
            putExtra("notification_id", notifId)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = CurrencyUtils.formatVnd(parsed.totalMinor)
        val recipient = parsed.merchantName ?: "Người nhận"
        val noteText = parsed.transactionNote?.let { " • $it" } ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💸 Phát hiện chuyển tiền: $formattedAmt")
            .setContentText("Tới $recipient$noteText. Chạm để ghi nhận chi tiêu!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Đã phát hiện biên lai chuyển tiền $formattedAmt tới $recipient$noteText.\nChạm để xem chi tiết hoặc bấm 'Ghi nhận ngay'.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(viewPendingIntent)
            .addAction(0, "Ghi nhận ngay", savePendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Biên lai chuyển tiền"
            val descriptionText = "Thông báo khi phát hiện ảnh chụp màn hình chuyển khoản ngân hàng"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun recordProcessedId(id: Long) {
        if (processedIds.size >= MAX_PROCESSED_CACHE) {
            val first = processedIds.iterator().next()
            processedIds.remove(first)
        }
        processedIds.add(id)
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
