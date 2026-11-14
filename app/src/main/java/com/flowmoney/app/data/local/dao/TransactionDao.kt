package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import kotlinx.coroutines.flow.Flow

data class CategoryExpenseTotal(
    val categoryId: Long?,
    val totalAmount: Long
)

data class FinancialSummary(
    val totalIncome: Long,
    val totalExpense: Long
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isTestTransaction = 0 ORDER BY transactionTime DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isTestTransaction = 0 ORDER BY transactionTime DESC LIMIT :limit")
    fun getRecentTransactionsFlow(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND isTestTransaction = 0 AND classificationStatus = :status ORDER BY transactionTime DESC")
    fun getTransactionsByStatusFlow(status: ClassificationStatus = ClassificationStatus.PENDING): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id AND isDeleted = 0")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
          AND isTestTransaction = 0
          AND transactionTime >= :startTime 
          AND transactionTime <= :endTime 
        ORDER BY transactionTime DESC
    """)
    fun getTransactionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
          AND isTestTransaction = 0
          AND transactionTime >= :startTime 
          AND transactionTime <= :endTime 
        ORDER BY transactionTime DESC
    """)
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN direction = 'INCOME' THEN amountMinor ELSE 0 END), 0) AS totalIncome,
            COALESCE(SUM(CASE WHEN direction = 'EXPENSE' THEN amountMinor ELSE 0 END), 0) AS totalExpense
        FROM transactions 
        WHERE isDeleted = 0 
          AND isTestTransaction = 0
          AND transactionTime >= :startTime 
          AND transactionTime <= :endTime
    """)
    fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long): Flow<FinancialSummary>

    @Query("""
        SELECT 
            categoryId,
            SUM(amountMinor) AS totalAmount
        FROM transactions 
        WHERE isDeleted = 0 
          AND isTestTransaction = 0
          AND direction = 'EXPENSE'
          AND transactionTime >= :startTime 
          AND transactionTime <= :endTime
        GROUP BY categoryId
        ORDER BY totalAmount DESC
    """)
    fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<CategoryExpenseTotal>>

    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
          AND isTestTransaction = 0
          AND amountMinor = :amountMinor 
          AND direction = :direction
          AND transactionTime >= :minTime 
          AND transactionTime <= :maxTime
    """)
    suspend fun findSimilarTransactions(
        amountMinor: Long,
        direction: TransactionDirection,
        minTime: Long,
        maxTime: Long
    ): List<TransactionEntity>

    @Query("""
        SELECT * FROM transactions 
        WHERE isDeleted = 0 
          AND externalReference = :externalReference 
          AND externalReference IS NOT NULL
        LIMIT 1
    """)
    suspend fun findByExternalReference(externalReference: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isTestTransaction = 1 ORDER BY transactionTime DESC LIMIT 20")
    fun getTestTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE isTestTransaction = 1")
    suspend fun clearTestTransactions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET classificationStatus = :status, categoryId = :categoryId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateClassification(id: Long, categoryId: Long, status: ClassificationStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET linkedReceiptId = :receiptId, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun linkReceipt(transactionId: Long, receiptId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT COUNT(*) FROM transactions WHERE isDeleted = 0 AND isTestTransaction = 0 AND classificationStatus = 'PENDING'")
    fun getPendingClassificationCountFlow(): Flow<Int>
}
