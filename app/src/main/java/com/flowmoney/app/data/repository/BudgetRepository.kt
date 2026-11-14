package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.dao.BudgetDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.BudgetEntity
import com.flowmoney.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetStatus(
    val budget: BudgetEntity,
    val spentMinor: Long,
    val remainingMinor: Long,
    val utilizationPercentage: Float,
    val isNearLimit: Boolean, // >= 80%
    val isOverLimit: Boolean  // >= 100%
)

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    fun getActiveBudgetsFlow(): Flow<List<BudgetEntity>> = budgetDao.getActiveBudgetsFlow()

    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgetsFlow()

    suspend fun saveBudget(budget: BudgetEntity): Long = budgetDao.insertBudget(budget)

    suspend fun updateBudget(budget: BudgetEntity) = budgetDao.updateBudget(budget)

    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)

    suspend fun calculateBudgetStatus(budget: BudgetEntity): BudgetStatus {
        val start = DateUtils.getStartOfMonth()
        val end = DateUtils.getEndOfMonth()

        val transactions = transactionDao.getTransactionsBetween(start, end)
        val spent = if (budget.categoryId != null) {
            transactions
                .filter { it.categoryId == budget.categoryId && !it.isExcludedFromBudget && it.direction == com.flowmoney.app.data.model.TransactionDirection.EXPENSE }
                .sumOf { it.amountMinor }
        } else {
            transactions
                .filter { !it.isExcludedFromBudget && it.direction == com.flowmoney.app.data.model.TransactionDirection.EXPENSE }
                .sumOf { it.amountMinor }
        }

        val remaining = (budget.amountMinor - spent).coerceAtLeast(0L)
        val ratio = if (budget.amountMinor > 0) spent.toFloat() / budget.amountMinor else 0f
        val percentage = (ratio * 100f).coerceAtLeast(0f)

        return BudgetStatus(
            budget = budget,
            spentMinor = spent,
            remainingMinor = remaining,
            utilizationPercentage = percentage,
            isNearLimit = percentage >= budget.alertThresholdPercent,
            isOverLimit = percentage >= 100f
        )
    }
}
