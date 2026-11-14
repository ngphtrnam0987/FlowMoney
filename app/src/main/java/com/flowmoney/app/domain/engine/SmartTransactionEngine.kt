package com.flowmoney.app.domain.engine

import com.flowmoney.app.data.local.dao.AccountDao
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.parser.BankNotificationParserRegistry
import com.flowmoney.app.domain.parser.ParsedBankNotification
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed class TransactionProcessResult {
    data class Success(val transactionId: Long, val parsed: ParsedBankNotification) : TransactionProcessResult()
    data class Duplicate(val reason: String) : TransactionProcessResult()
    data class Ignored(val reason: String) : TransactionProcessResult()
    data class Failed(val error: String) : TransactionProcessResult()
}

@Singleton
class SmartTransactionEngine @Inject constructor(
    private val parserRegistry: BankNotificationParserRegistry,
    private val duplicateDetector: DuplicateDetector,
    private val categoryClassifier: CategoryClassifier,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val accountDao: AccountDao,
    private val settingsRepository: SettingsRepository,
    private val dailyReviewRepository: DailyReviewRepository
) {

    suspend fun processNotification(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long = System.currentTimeMillis(),
        isTestMode: Boolean = false
    ): TransactionProcessResult {
        // 1. Source & Parser Discovery
        val parser = parserRegistry.findParser(packageName, title, text)
            ?: return TransactionProcessResult.Ignored("Không nhận diện được định dạng giao dịch ngân hàng")

        // 2. Check enabled banks filter (skipped in test mode)
        if (!isTestMode) {
            val enabledBanks = settingsRepository.enabledBanksFlow.first()
            if (enabledBanks.isNotEmpty()) {
                val isExplicitlyAllowed = enabledBanks.contains(packageName) ||
                        parser.supportedPackages.any { enabledBanks.contains(it) } ||
                        enabledBanks.any { it.contains(parser.bankId, ignoreCase = true) }
                if (!isExplicitlyAllowed && !parser.canParse(packageName, title, text)) {
                    return TransactionProcessResult.Ignored("Ngân hàng ${parser.bankName} chưa được kích hoạt")
                }
            }
        }

        // 3. Parse notification
        val parsed = parser.parse(title, text, timestamp)
            ?: return TransactionProcessResult.Ignored("Không thể trích xuất số tiền hoặc thông tin giao dịch hợp lệ")

        if (parsed.amountMinor <= 0L) {
            return TransactionProcessResult.Ignored("Số tiền giao dịch không hợp lệ (amount = 0)")
        }

        // 4. Duplicate Detection (skipped in test mode)
        if (!isTestMode && duplicateDetector.isDuplicate(parsed)) {
            return TransactionProcessResult.Duplicate("Giao dịch đã tồn tại hoặc bị trùng lặp")
        }

        // 5. Resolve or create account
        val accountId = resolveAccount(parsed)

        // 6. Category Classification
        val classification = categoryClassifier.classify(
            description = parsed.transactionDescription,
            merchantName = parsed.counterparty,
            amountMinor = parsed.amountMinor,
            direction = parsed.direction
        )

        val status = if (isTestMode) {
            ClassificationStatus.CONFIRMED
        } else if (classification.confidenceScore >= 0.8f && parsed.confidenceScore >= 0.9f) {
            ClassificationStatus.CONFIRMED
        } else {
            ClassificationStatus.PENDING
        }

        // 7. Create Transaction Entity
        val transaction = TransactionEntity(
            accountId = accountId,
            source = if (isTestMode) TransactionSource.MANUAL else TransactionSource.BANK_NOTIFICATION,
            externalReference = if (isTestMode) "TEST_${System.currentTimeMillis()}" else parsed.externalReference,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            direction = parsed.direction,
            transactionTime = parsed.timestamp,
            merchantName = parsed.counterparty,
            description = if (isTestMode) "[TEST] ${parsed.transactionDescription}" else parsed.transactionDescription,
            categoryId = classification.categoryId,
            classificationStatus = status,
            confidenceScore = classification.confidenceScore * parsed.confidenceScore,
            balanceAfterMinor = if (isTestMode) null else parsed.balanceAfterMinor,
            isTestTransaction = isTestMode
        )

        // 8. Persist Transaction
        val transactionId = transactionRepository.insertTransaction(transaction)

        // 9. Record duplicate fingerprint & update review (only for production transactions)
        if (!isTestMode) {
            duplicateDetector.recordFingerprint(parsed, transactionId)
            try {
                dailyReviewRepository.generateOrUpdateTodayReview()
            } catch (e: Exception) {
                // Non-critical, ignore
            }
        }

        return TransactionProcessResult.Success(transactionId, parsed)
    }

    private suspend fun resolveAccount(parsed: ParsedBankNotification): Long {
        val allAccounts = accountDao.getAllAccounts()

        // 1. Check if an account with matching bank name, identifier, or account number exists
        val matchedByBank = allAccounts.firstOrNull { acc ->
            val bankNameMatch = !acc.bankName.isNullOrBlank() && (
                acc.bankName.contains(parsed.bankIdentifier, ignoreCase = true) ||
                acc.bankName.contains(parsed.bankName, ignoreCase = true) ||
                parsed.bankName.contains(acc.bankName, ignoreCase = true)
            )
            val nameMatch = acc.name.contains(parsed.bankIdentifier, ignoreCase = true) ||
                    acc.name.contains(parsed.bankName, ignoreCase = true) ||
                    parsed.bankName.contains(acc.name, ignoreCase = true)
            val accNumMatch = parsed.maskedAccount != null &&
                    acc.accountNumber?.endsWith(parsed.maskedAccount.takeLast(4)) == true
            bankNameMatch || nameMatch || accNumMatch
        }
        if (matchedByBank != null) return matchedByBank.id

        // 2. Fall back to default account if present
        val defaultAccount = allAccounts.firstOrNull { it.isDefault }
        if (defaultAccount != null) return defaultAccount.id

        // 3. If any account exists, return first
        if (allAccounts.isNotEmpty()) return allAccounts.first().id

        // 4. Otherwise auto-create account for this bank
        return accountRepository.createAccount(
            name = parsed.bankName,
            type = AccountType.BANK,
            openingBalanceMinor = parsed.balanceAfterMinor ?: 0L,
            bankName = parsed.bankName,
            accountNumber = parsed.maskedAccount,
            isDefault = true
        )
    }
}
