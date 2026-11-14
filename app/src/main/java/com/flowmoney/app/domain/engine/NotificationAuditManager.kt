package com.flowmoney.app.domain.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.parser.ParsedBankNotification
import com.flowmoney.app.service.BankNotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject
import javax.inject.Singleton

enum class AuditStatus {
    RECEIVED,
    PARSED,
    REJECTED_OTP,
    REJECTED_PARSER,
    REJECTED_DUPLICATE,
    PERSISTED,
    ERROR
}

data class NotificationAuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String? = null,
    val title: String,
    val sanitizedSnippet: String,
    val matchedParser: String? = null,
    val parsedAmountMinor: Long? = null,
    val direction: TransactionDirection? = null,
    val duplicateReason: String? = null,
    val dbResult: String? = null,
    val status: AuditStatus
)

data class PipelineStep(
    val stepName: String,
    val isSuccess: Boolean,
    val details: String
)

data class TestPipelineResult(
    val timestamp: Long = System.currentTimeMillis(),
    val isOverallSuccess: Boolean,
    val steps: List<PipelineStep>,
    val testTransactionId: Long? = null
)

@Singleton
class NotificationAuditManager @Inject constructor() {

    private val _isListenerConnected = MutableStateFlow(false)
    val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

    private val _lastConnectedTime = MutableStateFlow<Long?>(null)
    val lastConnectedTime: StateFlow<Long?> = _lastConnectedTime.asStateFlow()

    private val _lastDisconnectedTime = MutableStateFlow<Long?>(null)
    val lastDisconnectedTime: StateFlow<Long?> = _lastDisconnectedTime.asStateFlow()

    private val _lastAuditLog = MutableStateFlow<NotificationAuditLog?>(null)
    val lastAuditLog: StateFlow<NotificationAuditLog?> = _lastAuditLog.asStateFlow()

    private val logHistory = ConcurrentLinkedDeque<NotificationAuditLog>()
    private val _auditLogs = MutableStateFlow<List<NotificationAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<NotificationAuditLog>> = _auditLogs.asStateFlow()

    private val _testPipelineResult = MutableStateFlow<TestPipelineResult?>(null)
    val testPipelineResult: StateFlow<TestPipelineResult?> = _testPipelineResult.asStateFlow()

    fun setListenerConnected(connected: Boolean) {
        _isListenerConnected.value = connected
        if (connected) {
            _lastConnectedTime.value = System.currentTimeMillis()
        } else {
            _lastDisconnectedTime.value = System.currentTimeMillis()
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun requestRebind(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, BankNotificationListenerService::class.java)
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun logEvent(log: NotificationAuditLog) {
        _lastAuditLog.value = log
        logHistory.addFirst(log)
        while (logHistory.size > 50) {
            logHistory.removeLast()
        }
        _auditLogs.value = logHistory.toList()
    }

    fun setTestPipelineResult(result: TestPipelineResult) {
        _testPipelineResult.value = result
    }

    fun clearTestResult() {
        _testPipelineResult.value = null
    }

    fun sanitizeContent(text: String): String {
        // Redact any OTP digits, passwords or credentials
        return text.replace(Regex("(?i)(?:mã otp|otp|xác thực)[^0-9]*[0-9]{4,8}"), "[MÃ_OTP_ĐÃ_ẨN]")
            .replace(Regex("(?i)(?:mật khẩu|password)\\s*[:=]?\\s*\\S+"), "[MẬT_KHẨU_ĐÃ_ẨN]")
    }

    fun getAppNameFromPackage(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
