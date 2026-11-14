package com.flowmoney.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.CategoryClassifier
import com.flowmoney.app.util.CurrencyUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class QuickSaveExpenseReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_QUICK_SAVE_EXPENSE = "com.flowmoney.app.ACTION_QUICK_SAVE_EXPENSE"
    }

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var categoryClassifier: CategoryClassifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_QUICK_SAVE_EXPENSE) return

        val amountMinor = intent.getLongExtra("amount_minor", 0L)
        if (amountMinor <= 0L) return

        val merchantName = intent.getStringExtra("merchant_name") ?: "Chuyển tiền ngân hàng"
        val note = intent.getStringExtra("note")
        val bankName = intent.getStringExtra("bank_name")
        val notifId = intent.getIntExtra("notification_id", -1)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Find or match account
                val accounts = accountRepository.getAllAccounts()
                val targetAccount = if (bankName != null) {
                    accounts.firstOrNull { it.name.contains(bankName, ignoreCase = true) }
                } else null

                val accountId = targetAccount?.id
                    ?: accounts.firstOrNull { it.isDefault }?.id
                    ?: accounts.firstOrNull()?.id
                    ?: accountRepository.createAccount(
                        name = bankName ?: "Tài khoản chính",
                        type = AccountType.BANK,
                        openingBalanceMinor = 0L,
                        isDefault = true
                    )

                // Predict category
                val textToClassify = "${note ?: ""} $merchantName"
                val classification = categoryClassifier.classify(
                    description = textToClassify,
                    merchantName = merchantName,
                    amountMinor = amountMinor,
                    direction = TransactionDirection.EXPENSE
                )
                val categoryId = classification.categoryId
                    ?: categoryRepository.findByName("Ăn uống")?.id
                    ?: categoryRepository.getAllCategories().firstOrNull()?.id

                val description = note?.takeIf { it.isNotBlank() } ?: "Chuyển tiền: $merchantName"

                val transaction = TransactionEntity(
                    accountId = accountId,
                    source = TransactionSource.RECEIPT_SCAN,
                    amountMinor = amountMinor,
                    direction = TransactionDirection.EXPENSE,
                    transactionTime = System.currentTimeMillis(),
                    merchantName = merchantName,
                    description = description,
                    categoryId = categoryId,
                    classificationStatus = ClassificationStatus.CONFIRMED
                )

                transactionRepository.insertTransaction(transaction)

                // Dismiss notification
                if (notifId != -1) {
                    try {
                        NotificationManagerCompat.from(context).cancel(notifId)
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "✅ Đã ghi nhận chi tiêu: ${CurrencyUtils.formatVnd(amountMinor)}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Lỗi ghi nhận chi tiêu: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
