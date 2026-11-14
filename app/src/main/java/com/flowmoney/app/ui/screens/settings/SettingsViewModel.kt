package com.flowmoney.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.BudgetRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.ReceiptRepository
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.service.DailyReviewWorker
import com.flowmoney.app.util.BackupUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BankSetting(
    val name: String,
    val packageName: String,
    val isEnabled: Boolean
)

data class SettingsUiState(
    val isBiometricEnabled: Boolean = false,
    val isDailyReminderEnabled: Boolean = true,
    val reminderHour: Int = 22,
    val reminderMinute: Int = 0,
    val banks: List<BankSetting> = emptyList(),
    val exportedFile: File? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val supportedBanks = listOf(
        Pair("MB Bank", "com.mbmobile"),
        Pair("Vietcombank", "com.VCB"),
        Pair("Techcombank", "vn.com.techcombank.bb.app"),
        Pair("BIDV", "com.vnpay.bidv"),
        Pair("VietinBank", "com.vietinbank.ipay"),
        Pair("ACB", "mobile.acb.com.vn"),
        Pair("TPBank", "com.tpb.mb.gprsandroid"),
        Pair("VPBank", "com.vpbank.neo"),
        Pair("Sacombank", "com.vnpay.sacombank"),
        Pair("VIB", "com.vib.myvib2")
    )

    private val _message = MutableStateFlow<String?>(null)
    private val _exportedFile = MutableStateFlow<File?>(null)

    private val _settingsConfig = combine(
        settingsRepository.isBiometricEnabledFlow,
        settingsRepository.isDailyReminderEnabledFlow,
        settingsRepository.dailyReminderTimeFlow,
        settingsRepository.enabledBanksFlow
    ) { biometric, reminderEnabled, reminderTime, enabledBanks ->
        val bankSettings = supportedBanks.map { (name, pkg) ->
            BankSetting(
                name = name,
                packageName = pkg,
                isEnabled = enabledBanks.contains(pkg)
            )
        }
        Triple(biometric, Pair(reminderEnabled, reminderTime), bankSettings)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        _settingsConfig,
        _message,
        _exportedFile
    ) { config, msg, expFile ->
        val (biometric, reminderPair, bankSettings) = config
        val (reminderEnabled, reminderTime) = reminderPair
        SettingsUiState(
            isBiometricEnabled = biometric,
            isDailyReminderEnabled = reminderEnabled,
            reminderHour = reminderTime.first,
            reminderMinute = reminderTime.second,
            banks = bankSettings,
            exportedFile = expFile,
            message = msg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBiometricEnabled(enabled)
        }
    }

    fun toggleDailyReminder(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderEnabled(enabled)
            if (enabled) {
                DailyReviewWorker.schedule(context, uiState.value.reminderHour, uiState.value.reminderMinute)
            }
        }
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyReminderTime(hour, minute)
            DailyReviewWorker.schedule(context, hour, minute)
        }
    }

    fun toggleBank(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBankEnabled(packageName, enabled)
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            try {
                val txs = transactionRepository.getTransactionsBetween(0L, System.currentTimeMillis() + 86400000L)
                val cats = categoryRepository.getAllCategories().associate { it.id to it.name }
                val accs = accountRepository.getAllAccounts().associate { it.id to it.name }
                val file = BackupUtils.exportTransactionsToCsv(context, txs, cats, accs)
                _exportedFile.value = file
                _message.value = "Xuất file CSV thành công: ${file.name}"
            } catch (e: Exception) {
                _message.value = "Lỗi xuất CSV: ${e.localizedMessage}"
            }
        }
    }

    fun exportJsonBackup(context: Context) {
        viewModelScope.launch {
            try {
                val accs = accountRepository.getAllAccounts()
                val cats = categoryRepository.getAllCategories()
                val txs = transactionRepository.getTransactionsBetween(0L, System.currentTimeMillis() + 86400000L)
                val file = BackupUtils.createJsonBackup(
                    context = context,
                    accounts = accs,
                    categories = cats,
                    transactions = txs,
                    budgets = emptyList(),
                    receipts = emptyList()
                )
                _exportedFile.value = file
                _message.value = "Sao lưu dữ liệu thành công: ${file.name}"
            } catch (e: Exception) {
                _message.value = "Lỗi sao lưu: ${e.localizedMessage}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
