package com.flowmoney.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.flowmoney.app.domain.engine.AuditStatus
import com.flowmoney.app.domain.engine.NotificationAuditLog
import com.flowmoney.app.domain.engine.NotificationAuditManager
import com.flowmoney.app.domain.engine.SmartTransactionEngine
import com.flowmoney.app.domain.engine.TransactionProcessResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "BankNotificationListener"
    }

    @Inject
    lateinit var transactionEngine: SmartTransactionEngine

    @Inject
    lateinit var auditManager: NotificationAuditManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListenerService connected")
        auditManager.setListenerConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotificationListenerService disconnected")
        auditManager.setListenerConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        // Extract title (check both standard title and big title)
        val titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
        val titleStd = extras.getString(Notification.EXTRA_TITLE)
        val title = listOfNotNull(titleBig, titleStd)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() } ?: ""

        // Extract and combine ALL notification body parts without dropping anything
        val stdText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.mapNotNull { it?.toString() }
        val ticker = sbn.notification?.tickerText?.toString()

        val bodyElements = mutableListOf<String>()
        // If titleStd is different from title and contains transaction info (e.g. amount), retain it
        if (!titleStd.isNullOrBlank() && titleStd.trim() != title.trim()) {
            bodyElements.add(titleStd.trim())
        }
        if (!stdText.isNullOrBlank()) {
            bodyElements.add(stdText.trim())
        }
        if (!bigText.isNullOrBlank() && bigText.trim() != stdText?.trim()) {
            bodyElements.add(bigText.trim())
        }
        if (!subText.isNullOrBlank() && !bodyElements.contains(subText.trim())) {
            bodyElements.add(subText.trim())
        }
        if (!infoText.isNullOrBlank() && !bodyElements.contains(infoText.trim())) {
            bodyElements.add(infoText.trim())
        }
        if (!summaryText.isNullOrBlank() && !bodyElements.contains(summaryText.trim())) {
            bodyElements.add(summaryText.trim())
        }
        lines?.forEach { line ->
            if (line.isNotBlank() && !bodyElements.contains(line.trim())) {
                bodyElements.add(line.trim())
            }
        }
        if (!ticker.isNullOrBlank() && !bodyElements.contains(ticker.trim())) {
            bodyElements.add(ticker.trim())
        }

        val text = bodyElements.joinToString("\n")

        if (title.isBlank() && text.isBlank()) return

        serviceScope.launch {
            try {
                val result = transactionEngine.processNotification(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime
                )

                val appName = auditManager.getAppNameFromPackage(applicationContext, packageName)
                val sanitized = auditManager.sanitizeContent(text)

                when (result) {
                    is TransactionProcessResult.Success -> {
                        auditManager.logEvent(
                            NotificationAuditLog(
                                packageName = packageName,
                                appName = appName,
                                title = title,
                                sanitizedSnippet = sanitized,
                                matchedParser = result.parsed.bankName,
                                parsedAmountMinor = result.parsed.amountMinor,
                                direction = result.parsed.direction,
                                dbResult = "Đã lưu vào cơ sở dữ liệu (ID: ${result.transactionId})",
                                status = AuditStatus.PERSISTED
                            )
                        )
                    }
                    is TransactionProcessResult.Duplicate -> {
                        auditManager.logEvent(
                            NotificationAuditLog(
                                packageName = packageName,
                                appName = appName,
                                title = title,
                                sanitizedSnippet = sanitized,
                                duplicateReason = result.reason,
                                dbResult = "Bị loại bỏ do trùng lặp",
                                status = AuditStatus.REJECTED_DUPLICATE
                            )
                        )
                    }
                    is TransactionProcessResult.Ignored -> {
                        auditManager.logEvent(
                            NotificationAuditLog(
                                packageName = packageName,
                                appName = appName,
                                title = title,
                                sanitizedSnippet = sanitized,
                                dbResult = result.reason,
                                status = AuditStatus.REJECTED_PARSER
                            )
                        )
                    }
                    is TransactionProcessResult.Failed -> {
                        auditManager.logEvent(
                            NotificationAuditLog(
                                packageName = packageName,
                                appName = appName,
                                title = title,
                                sanitizedSnippet = sanitized,
                                dbResult = "Lỗi: ${result.error}",
                                status = AuditStatus.ERROR
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing bank notification: ${e.message}", e)
                auditManager.logEvent(
                    NotificationAuditLog(
                        packageName = packageName,
                        title = title,
                        sanitizedSnippet = auditManager.sanitizeContent(text),
                        dbResult = "Exception: ${e.localizedMessage}",
                        status = AuditStatus.ERROR
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        auditManager.setListenerConnected(false)
        serviceScope.cancel()
    }
}
