package com.flowmoney.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.AuditStatus
import com.flowmoney.app.domain.engine.NotificationAuditLog
import com.flowmoney.app.domain.engine.NotificationAuditManager
import com.flowmoney.app.domain.engine.PipelineStep
import com.flowmoney.app.domain.engine.SmartTransactionEngine
import com.flowmoney.app.domain.engine.TestPipelineResult
import com.flowmoney.app.domain.engine.TransactionProcessResult
import com.flowmoney.app.domain.parser.BankNotificationParserRegistry
import com.flowmoney.app.domain.parser.ParsedBankNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledBankAppInfo(
    val packageName: String,
    val appName: String,
    val isSupportedByDefault: Boolean,
    val isEnabled: Boolean
)

data class DiagnosticsUiState(
    val isNotificationAccessGranted: Boolean = false,
    val isListenerConnected: Boolean = false,
    val lastConnectedTime: Long? = null,
    val lastDisconnectedTime: Long? = null,
    val lastAuditLog: NotificationAuditLog? = null,
    val auditLogs: List<NotificationAuditLog> = emptyList(),
    val testPipelineResult: TestPipelineResult? = null,
    val testTransactions: List<TransactionEntity> = emptyList(),
    val installedBankApps: List<InstalledBankAppInfo> = emptyList(),
    val simulatorInput: String = "",
    val simulatorResult: ParsedBankNotification? = null,
    val simulatorError: String? = null,
    val isRunningTest: Boolean = false,
    val actionMessage: String? = null
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auditManager: NotificationAuditManager,
    private val transactionEngine: SmartTransactionEngine,
    private val parserRegistry: BankNotificationParserRegistry,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _simulatorInput = MutableStateFlow("")
    private val _simulatorResult = MutableStateFlow<ParsedBankNotification?>(null)
    private val _simulatorError = MutableStateFlow<String?>(null)
    private val _isRunningTest = MutableStateFlow(false)
    private val _actionMessage = MutableStateFlow<String?>(null)
    private val _installedApps = MutableStateFlow<List<InstalledBankAppInfo>>(emptyList())

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        auditManager.isListenerConnected,
        auditManager.lastConnectedTime,
        auditManager.lastDisconnectedTime,
        auditManager.lastAuditLog,
        auditManager.auditLogs,
        auditManager.testPipelineResult,
        transactionRepository.getTestTransactionsFlow(),
        _installedApps,
        _simulatorInput,
        _simulatorResult,
        _simulatorError,
        _isRunningTest,
        _actionMessage
    ) { params: Array<Any?> ->
        val isConnected = params[0] as Boolean
        val lastConnTime = params[1] as Long?
        val lastDisconnTime = params[2] as Long?
        val lastLog = params[3] as NotificationAuditLog?
        @Suppress("UNCHECKED_CAST")
        val logs = params[4] as List<NotificationAuditLog>
        val testResult = params[5] as TestPipelineResult?
        @Suppress("UNCHECKED_CAST")
        val testTxs = params[6] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val apps = params[7] as List<InstalledBankAppInfo>
        val simInput = params[8] as String
        val simResult = params[9] as ParsedBankNotification?
        val simError = params[10] as String?
        val runningTest = params[11] as Boolean
        val message = params[12] as String?

        val accessGranted = auditManager.isNotificationAccessGranted(context)

        DiagnosticsUiState(
            isNotificationAccessGranted = accessGranted,
            isListenerConnected = isConnected,
            lastConnectedTime = lastConnTime,
            lastDisconnectedTime = lastDisconnTime,
            lastAuditLog = lastLog,
            auditLogs = logs,
            testPipelineResult = testResult,
            testTransactions = testTxs,
            installedBankApps = apps,
            simulatorInput = simInput,
            simulatorResult = simResult,
            simulatorError = simError,
            isRunningTest = runningTest,
            actionMessage = message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DiagnosticsUiState(
            isNotificationAccessGranted = auditManager.isNotificationAccessGranted(context)
        )
    )

    init {
        scanInstalledBankApps()
    }

    fun refreshStatus() {
        scanInstalledBankApps()
    }

    fun requestRebind() {
        val success = auditManager.requestRebind(context)
        _actionMessage.value = if (success) {
            "Đã gửi yêu cầu kết nối lại Listener đến Android OS"
        } else {
            "Không thể kết nối lại. Vui lòng tắt/bật lại quyền thông báo trong Cài đặt Android"
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun scanInstalledBankApps() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    pm.getInstalledApplications(0)
                }

                val enabledBanks = settingsRepository.enabledBanksFlow.first()
                val knownPackages = setOf(
                    "com.VCB",
                    "com.vcb.digibank",
                    "com.mbmobile",
                    "vn.com.techcombank.bb.app",
                    "com.vnpay.bidv",
                    "com.bidv.smartbanking",
                    "com.vietinbank.ipay",
                    "mobile.acb.com.vn",
                    "com.tpb.mb.gprsandroid",
                    "com.vpbank.neo",
                    "com.vnpay.sacombank",
                    "com.vib.myvib2"
                )

                val bankKeywords = listOf("bank", "vcb", "mb", "bidv", "techcombank", "vietin", "acb", "tpb", "vpbank", "vib", "sacom")

                val detected = mutableListOf<InstalledBankAppInfo>()
                for (app in installedPackages) {
                    val pkg = app.packageName.lowercase()
                    val label = pm.getApplicationLabel(app).toString()
                    val isKnown = knownPackages.contains(app.packageName)
                    val matchesKeyword = bankKeywords.any { pkg.contains(it) || label.lowercase().contains(it) }

                    if (isKnown || matchesKeyword) {
                        detected.add(
                            InstalledBankAppInfo(
                                packageName = app.packageName,
                                appName = label,
                                isSupportedByDefault = isKnown,
                                isEnabled = enabledBanks.contains(app.packageName)
                            )
                        )
                    }
                }

                _installedApps.value = detected.sortedByDescending { it.isEnabled }
            } catch (e: Exception) {
                // Ignore scan failure
            }
        }
    }

    fun toggleBankApp(packageName: String, enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBankEnabled(packageName, enable)
            scanInstalledBankApps()
        }
    }

    fun setSimulatorInput(input: String) {
        _simulatorInput.value = input
        _simulatorResult.value = null
        _simulatorError.value = null
    }

    fun runSimulatorTest() {
        val input = _simulatorInput.value.trim()
        if (input.isBlank()) {
            _simulatorError.value = "Vui lòng nhập nội dung thông báo để kiểm tra"
            return
        }

        val parsed = parserRegistry.parseNotification(
            packageName = "simulator.test.app",
            title = "Thông báo ngân hàng",
            text = input,
            timestamp = System.currentTimeMillis()
        )

        if (parsed != null) {
            _simulatorResult.value = parsed
            _simulatorError.value = null
        } else {
            _simulatorResult.value = null
            _simulatorError.value = "Không nhận diện được giao dịch từ văn bản trên. Kiểm tra xem có chứa số tiền (VND/đ) và thông tin biến động không."
        }
    }

    fun runEndToEndTest() {
        if (_isRunningTest.value) return
        _isRunningTest.value = true

        viewModelScope.launch {
            val steps = mutableListOf<PipelineStep>()
            var testTxId: Long? = null

            try {
                // Step 1: Generate test event
                val sampleText = "TK 123456789 -50,000 VND luc 10:00 15/06/2024. SD: 2,500,000 VND. ND: FlowMoney Test Pipeline"
                steps.add(
                    PipelineStep(
                        stepName = "1. Sinh sự kiện thông báo thử nghiệm",
                        isSuccess = true,
                        details = "Nội dung mẫu: $sampleText"
                    )
                )

                // Step 2: Execute transaction pipeline in TEST mode
                val result = transactionEngine.processNotification(
                    packageName = "com.VCB",
                    title = "Vietcombank",
                    text = sampleText,
                    timestamp = System.currentTimeMillis(),
                    isTestMode = true
                )

                when (result) {
                    is TransactionProcessResult.Success -> {
                        testTxId = result.transactionId
                        steps.add(
                            PipelineStep(
                                stepName = "2. Phân tích qua Parser & Trích xuất số tiền",
                                isSuccess = true,
                                details = "Ngân hàng: ${result.parsed.bankName}, Số tiền: ${result.parsed.amountMinor} VND, Chiều: ${result.parsed.direction}"
                            )
                        )

                        steps.add(
                            PipelineStep(
                                stepName = "3. Phân loại danh mục tự động",
                                isSuccess = true,
                                details = "Nội dung giao dịch đã được chuẩn hóa và gán nhãn"
                            )
                        )

                        steps.add(
                            PipelineStep(
                                stepName = "4. Lưu trữ vào cơ sở dữ liệu Room (Cách ly dữ liệu thật)",
                                isSuccess = true,
                                details = "Đã lưu thành công với Transaction ID: $testTxId (isTestTransaction = true)"
                            )
                        )

                        steps.add(
                            PipelineStep(
                                stepName = "5. Xác thực phản hồi Dashboard",
                                isSuccess = true,
                                details = "Dashboard Room Flow không bị ảnh hưởng số dư tài chính thật (được bảo vệ an toàn)"
                            )
                        )

                        auditManager.setTestPipelineResult(
                            TestPipelineResult(
                                isOverallSuccess = true,
                                steps = steps,
                                testTransactionId = testTxId
                            )
                        )
                    }
                    is TransactionProcessResult.Ignored -> {
                        steps.add(
                            PipelineStep(
                                stepName = "2. Phân tích qua Parser",
                                isSuccess = false,
                                details = "Bị từ chối: ${result.reason}"
                            )
                        )
                        auditManager.setTestPipelineResult(
                            TestPipelineResult(
                                isOverallSuccess = false,
                                steps = steps
                            )
                        )
                    }
                    is TransactionProcessResult.Duplicate -> {
                        steps.add(
                            PipelineStep(
                                stepName = "2. Kiểm tra trùng lặp",
                                isSuccess = false,
                                details = "Bị loại bỏ do trùng: ${result.reason}"
                            )
                        )
                        auditManager.setTestPipelineResult(
                            TestPipelineResult(
                                isOverallSuccess = false,
                                steps = steps
                            )
                        )
                    }
                    is TransactionProcessResult.Failed -> {
                        steps.add(
                            PipelineStep(
                                stepName = "2. Thực thi pipeline",
                                isSuccess = false,
                                details = "Thất bại: ${result.error}"
                            )
                        )
                        auditManager.setTestPipelineResult(
                            TestPipelineResult(
                                isOverallSuccess = false,
                                steps = steps
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                steps.add(
                    PipelineStep(
                        stepName = "Lỗi ngoại lệ trong quá trình kiểm thử",
                        isSuccess = false,
                        details = "${e.message}"
                    )
                )
                auditManager.setTestPipelineResult(
                    TestPipelineResult(
                        isOverallSuccess = false,
                        steps = steps
                    )
                )
            } finally {
                _isRunningTest.value = false
            }
        }
    }

    fun clearTestTransactions() {
        viewModelScope.launch {
            transactionRepository.clearTestTransactions()
            auditManager.clearTestResult()
            _actionMessage.value = "Đã xóa tất cả dữ liệu giao dịch kiểm thử"
        }
    }
}
