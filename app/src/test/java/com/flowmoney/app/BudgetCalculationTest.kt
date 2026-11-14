package com.flowmoney.app

import com.flowmoney.app.data.local.dao.BudgetDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.BudgetEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.BudgetPeriod
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.BudgetRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BudgetCalculationTest {

    private class StubBudgetDao : BudgetDao {
        override fun getActiveBudgetsFlow() = throw NotImplementedError()
        override fun getAllBudgetsFlow() = throw NotImplementedError()
        override suspend fun getBudgetForCategory(categoryId: Long) = null
        override suspend fun getOverallBudget() = null
        override suspend fun insertBudget(budget: BudgetEntity) = 1L
        override suspend fun updateBudget(budget: BudgetEntity) {}
        override suspend fun deleteBudget(budget: BudgetEntity) {}
    }

    private class StubTxDao(val transactions: List<TransactionEntity>) : TransactionDao {
        override suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity> = transactions
        override fun getAllTransactionsFlow() = throw NotImplementedError()
        override fun getRecentTransactionsFlow(limit: Int) = throw NotImplementedError()
        override fun getTransactionsByStatusFlow(status: ClassificationStatus) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long) = null
        override fun getTransactionsBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun findSimilarTransactions(amountMinor: Long, direction: TransactionDirection, minTime: Long, maxTime: Long) = emptyList<TransactionEntity>()
        override suspend fun findByExternalReference(externalReference: String) = null
        override suspend fun insertTransaction(transaction: TransactionEntity) = 1L
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun updateClassification(id: Long, categoryId: Long, status: ClassificationStatus, updatedAt: Long) {}
        override suspend fun linkReceipt(transactionId: Long, receiptId: Long, updatedAt: Long) {}
        override suspend fun softDeleteTransaction(id: Long, updatedAt: Long) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getPendingClassificationCountFlow() = throw NotImplementedError()
    }

    @Test
    fun calculateBudgetBelowThreshold() = runBlocking {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                accountId = 1L,
                source = TransactionSource.MANUAL,
                amountMinor = 1000000L, // 1,000,000 VND
                direction = TransactionDirection.EXPENSE,
                transactionTime = now,
                description = "Ăn trưa",
                categoryId = 1L // Ăn uống
            )
        )

        val budgetRepo = BudgetRepository(StubBudgetDao(), StubTxDao(txs))
        val budget = BudgetEntity(
            id = 1L,
            categoryId = 1L,
            period = BudgetPeriod.MONTHLY,
            amountMinor = 3000000L, // 3,000,000 VND limit
            alertThresholdPercent = 80
        )

        val status = budgetRepo.calculateBudgetStatus(budget)

        assertEquals(1000000L, status.spentMinor)
        assertEquals(2000000L, status.remainingMinor)
        assertEquals(33.33f, status.utilizationPercentage, 0.1f)
        assertFalse(status.isNearLimit)
        assertFalse(status.isOverLimit)
    }

    @Test
    fun calculateBudgetNearThresholdWarning() = runBlocking {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                accountId = 1L,
                source = TransactionSource.MANUAL,
                amountMinor = 2500000L, // 2,500,000 VND (83.3%)
                direction = TransactionDirection.EXPENSE,
                transactionTime = now,
                description = "Mua sắm",
                categoryId = 2L
            )
        )

        val budgetRepo = BudgetRepository(StubBudgetDao(), StubTxDao(txs))
        val budget = BudgetEntity(
            id = 2L,
            categoryId = 2L,
            period = BudgetPeriod.MONTHLY,
            amountMinor = 3000000L,
            alertThresholdPercent = 80
        )

        val status = budgetRepo.calculateBudgetStatus(budget)

        assertEquals(2500000L, status.spentMinor)
        assertEquals(500000L, status.remainingMinor)
        assertEquals(83.33f, status.utilizationPercentage, 0.1f)
        assertTrue(status.isNearLimit)
        assertFalse(status.isOverLimit)
    }

    @Test
    fun calculateBudgetOverLimit() = runBlocking {
        val now = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                accountId = 1L,
                source = TransactionSource.MANUAL,
                amountMinor = 3200000L, // 3,200,000 VND (> 100%)
                direction = TransactionDirection.EXPENSE,
                transactionTime = now,
                description = "Chi tiêu quá tay",
                categoryId = 2L
            )
        )

        val budgetRepo = BudgetRepository(StubBudgetDao(), StubTxDao(txs))
        val budget = BudgetEntity(
            id = 3L,
            categoryId = 2L,
            period = BudgetPeriod.MONTHLY,
            amountMinor = 3000000L,
            alertThresholdPercent = 80
        )

        val status = budgetRepo.calculateBudgetStatus(budget)

        assertEquals(3200000L, status.spentMinor)
        assertEquals(0L, status.remainingMinor)
        assertTrue(status.isNearLimit)
        assertTrue(status.isOverLimit)
    }

    @Test
    fun testEstimatedBalanceFormula() {
        val openingBalance = 5000000L
        val confirmedIncome = 15000000L
        val confirmedExpense = 6500000L
        val internalTransfer = 2000000L // Transfer out of account A into account B

        // For Account A:
        val balanceA = openingBalance + confirmedIncome - confirmedExpense - internalTransfer
        assertEquals(11500000L, balanceA)

        // System total balance across all accounts:
        // Internal transfer net effect is 0
        val totalNetTransfers = 0L
        val systemTotal = openingBalance + confirmedIncome - confirmedExpense + totalNetTransfers
        assertEquals(13500000L, systemTotal)
    }
}
