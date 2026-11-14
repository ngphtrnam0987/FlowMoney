package com.flowmoney.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowmoney.app.MainActivity
import com.flowmoney.app.R
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyReviewWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val dailyReviewRepository: DailyReviewRepository,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "flowmoney_daily_review_work"
        const val CHANNEL_ID = "flowmoney_daily_review_channel"
        const val NOTIFICATION_ID = 1001

        fun schedule(context: Context, hour: Int = 22, minute: Int = 0) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<DailyReviewWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val isEnabled = settingsRepository.isDailyReminderEnabledFlow.first()
        if (!isEnabled) return Result.success()

        val startOfDay = DateUtils.getStartOfDay()
        val endOfDay = DateUtils.getEndOfDay()

        val dayTransactions = transactionRepository.getTransactionsBetween(startOfDay, endOfDay)
        val pendingCount = dayTransactions.count { it.classificationStatus == ClassificationStatus.PENDING }
        val totalCount = dayTransactions.size

        // Only notify if there are transactions or pending reviews
        val message = if (pendingCount > 0) {
            "Hôm nay bạn có $totalCount giao dịch, trong đó $pendingCount giao dịch cần phân loại. Kiểm tra chi tiêu trong 1 phút nhé!"
        } else {
            "Hôm nay bạn có $totalCount giao dịch. Nhấn để tổng kết tài chính cuối ngày nhé!"
        }

        sendReviewNotification(message)
        dailyReviewRepository.generateOrUpdateTodayReview()

        return Result.success()
    }

    private fun sendReviewNotification(message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "daily_review")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FlowMoney Tổng kết cuối ngày")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
