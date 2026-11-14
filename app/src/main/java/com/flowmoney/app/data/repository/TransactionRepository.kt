package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.dao.AccountDao
import com.flowmoney.app.data.local.dao.CategoryExpenseTotal
import com.flowmoney.app.data.local.dao.FinancialSummary
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow()

    fun getRecentTransactionsFlow(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactionsFlow(limit)

    fun getPendingTransactionsFlow(): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByStatusFlow(ClassificationStatus.PENDING)

    fun getPendingCountFlow(): Flow<Int> =
        transactionDao.getPendingClassificationCountFlow()

    fun getTransactionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetweenFlow(startTime, endTime)

    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity> =
        transactionDao.getTransactionsBetween(startTime, endTime)

    fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long): Flow<FinancialSummary> =
        transactionDao.getFinancialSummaryBetweenFlow(startTime, endTime)

    fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<CategoryExpenseTotal>> =
        transactionDao.getCategoryExpensesBetweenFlow(startTime, endTime)

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)

        // Update account balance
        when (transaction.direction) {
            TransactionDirection.INCOME -> {
                val acc = accountDao.getAccountById(transaction.accountId)
                if (acc != null) {
                    accountDao.updateBalance(acc.id, acc.balanceMinor + transaction.amountMinor)
                }
            }
            TransactionDirection.EXPENSE -> {
                val acc = accountDao.getAccountById(transaction.accountId)
                if (acc != null) {
                    accountDao.updateBalance(acc.id, acc.balanceMinor - transaction.amountMinor)
                }
            }
            TransactionDirection.TRANSFER -> {
                val srcAcc = accountDao.getAccountById(transaction.accountId)
                if (srcAcc != null) {
                    accountDao.updateBalance(srcAcc.id, srcAcc.balanceMinor - transaction.amountMinor)
                }
                if (transaction.transferAccountId != null) {
                    val destAcc = accountDao.getAccountById(transaction.transferAccountId)
                    if (destAcc != null) {
                        accountDao.updateBalance(destAcc.id, destAcc.balanceMinor + transaction.amountMinor)
                    }
                }
            }
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun updateClassification(id: Long, categoryId: Long) {
        transactionDao.updateClassification(id, categoryId, ClassificationStatus.CONFIRMED)
    }

    suspend fun linkReceipt(transactionId: Long, receiptId: Long) {
        transactionDao.linkReceipt(transactionId, receiptId)
    }

    suspend fun deleteTransaction(id: Long) {
        val tx = transactionDao.getTransactionById(id) ?: return
        transactionDao.softDeleteTransaction(id)

        // Reverse balance update
        when (tx.direction) {
            TransactionDirection.INCOME -> {
                val acc = accountDao.getAccountById(tx.accountId)
                if (acc != null) {
                    accountDao.updateBalance(acc.id, acc.balanceMinor - tx.amountMinor)
                }
            }
            TransactionDirection.EXPENSE -> {
                val acc = accountDao.getAccountById(tx.accountId)
                if (acc != null) {
                    accountDao.updateBalance(acc.id, acc.balanceMinor + tx.amountMinor)
                }
            }
            TransactionDirection.TRANSFER -> {
                val srcAcc = accountDao.getAccountById(tx.accountId)
                if (srcAcc != null) {
                    accountDao.updateBalance(srcAcc.id, srcAcc.balanceMinor + tx.amountMinor)
                }
                if (tx.transferAccountId != null) {
                    val destAcc = accountDao.getAccountById(tx.transferAccountId)
                    if (destAcc != null) {
                        accountDao.updateBalance(destAcc.id, destAcc.balanceMinor - tx.amountMinor)
                    }
                }
            }
        }
    }
}
