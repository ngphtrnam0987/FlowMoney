package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.dao.DailyReviewDao
import com.flowmoney.app.data.local.dao.ReceiptDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.DailyReviewEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReviewRepository @Inject constructor(
    private val dailyReviewDao: DailyReviewDao,
    private val transactionDao: TransactionDao,
    private val receiptDao: ReceiptDao
) {
    fun getTodayReviewFlow(): Flow<DailyReviewEntity?> {
        val todayStr = DateUtils.getTodayDateString()
        return dailyReviewDao.getReviewByDateFlow(todayStr)
    }

    suspend fun generateOrUpdateTodayReview(): DailyReviewEntity {
        val todayStr = DateUtils.getTodayDateString()
        val startOfDay = DateUtils.getStartOfDay()
        val endOfDay = DateUtils.getEndOfDay()

        val dayTransactions = transactionDao.getTransactionsBetween(startOfDay, endOfDay)
        val income = dayTransactions.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amountMinor }
        val expense = dayTransactions.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amountMinor }
        val pendingCount = dayTransactions.count { it.classificationStatus == ClassificationStatus.PENDING }
        val unlinkedReceipts = receiptDao.getUnlinkedReceiptsFlow().first().size

        val existing = dailyReviewDao.getReviewByDate(todayStr)
        val entity = existing?.copy(
            totalIncomeMinor = income,
            totalExpenseMinor = expense,
            transactionCount = dayTransactions.size,
            pendingCategorizationCount = pendingCount,
            unlinkedReceiptCount = unlinkedReceipts
        ) ?: DailyReviewEntity(
            dateString = todayStr,
            totalIncomeMinor = income,
            totalExpenseMinor = expense,
            transactionCount = dayTransactions.size,
            pendingCategorizationCount = pendingCount,
            unlinkedReceiptCount = unlinkedReceipts
        )

        dailyReviewDao.insertOrUpdate(entity)
        return entity
    }

    suspend fun completeReview(dateString: String = DateUtils.getTodayDateString()) {
        val review = dailyReviewDao.getReviewByDate(dateString)
        if (review != null) {
            dailyReviewDao.update(
                review.copy(
                    isCompleted = true,
                    reviewedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
