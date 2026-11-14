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
        val title = titleBig ?: titleStd ?: ""

        // Extract body text with full multi-line fallback (BigTextStyle, lines, text, ticker)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val stdText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
        val ticker = sbn.notification?.tickerText?.toString()

        val text = when {
            !bigText.isNullOrBlank() -> bigText
            !lines.isNullOrBlank() -> lines
            !stdText.isNullOrBlank() -> stdText
            !subText.isNullOrBlank() -> subText
            !ticker.isNullOrBlank() -> ticker
            else -> ""
        }

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
