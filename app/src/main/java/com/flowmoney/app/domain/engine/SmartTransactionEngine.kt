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
        timestamp: Long = System.currentTimeMillis()
    ): TransactionProcessResult {
        // 1. Source Validation: Check if bank notifications are enabled for this package
        val enabledBanks = settingsRepository.enabledBanksFlow.first()
        val isPackageAllowed = enabledBanks.contains(packageName) ||
                               parserRegistry.getAllParsers().any { p ->
                                   p.supportedPackages.contains(packageName) && enabledBanks.contains(packageName)
                               }

        // 2. Parse notification
        val parsed = parserRegistry.parseNotification(packageName, title, text, timestamp)
            ?: return TransactionProcessResult.Ignored("Không thể phân tích định dạng thông báo")

        // 3. Duplicate Detection
        if (duplicateDetector.isDuplicate(parsed)) {
            return TransactionProcessResult.Duplicate("Giao dịch đã tồn tại hoặc bị trùng lặp")
        }

        // 4. Resolve or create account
        val accountId = resolveAccount(parsed)

        // 5. Category Classification
        val classification = categoryClassifier.classify(
            description = parsed.transactionDescription,
            merchantName = parsed.counterparty,
            amountMinor = parsed.amountMinor,
            direction = parsed.direction
        )

        val status = if (classification.confidenceScore >= 0.8f && parsed.confidenceScore >= 0.9f) {
            ClassificationStatus.CONFIRMED
        } else {
            ClassificationStatus.PENDING
        }

        // 6. Create Transaction Entity
        val transaction = TransactionEntity(
            accountId = accountId,
            source = TransactionSource.BANK_NOTIFICATION,
            externalReference = parsed.externalReference,
            amountMinor = parsed.amountMinor,
            currency = parsed.currency,
            direction = parsed.direction,
            transactionTime = parsed.timestamp,
            merchantName = parsed.counterparty,
            description = parsed.transactionDescription,
            categoryId = classification.categoryId,
            classificationStatus = status,
            confidenceScore = classification.confidenceScore * parsed.confidenceScore,
            balanceAfterMinor = parsed.balanceAfterMinor
        )

        // 7. Persist Transaction
        val transactionId = transactionRepository.insertTransaction(transaction)

        // 8. Record duplicate fingerprint
        duplicateDetector.recordFingerprint(parsed, transactionId)

        // 9. Update today's daily review
        try {
            dailyReviewRepository.generateOrUpdateTodayReview()
        } catch (e: Exception) {
            // Non-critical, ignore
        }

        return TransactionProcessResult.Success(transactionId, parsed)
    }

    private suspend fun resolveAccount(parsed: ParsedBankNotification): Long {
        val allAccounts = accountDao.getAllAccounts()

        // 1. Check if an account with matching bank name exists
        val matchedByBank = allAccounts.firstOrNull {
            it.bankName?.contains(parsed.bankIdentifier, ignoreCase = true) == true ||
            it.name.contains(parsed.bankIdentifier, ignoreCase = true) ||
            (parsed.maskedAccount != null && it.accountNumber?.endsWith(parsed.maskedAccount.takeLast(4)) == true)
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
