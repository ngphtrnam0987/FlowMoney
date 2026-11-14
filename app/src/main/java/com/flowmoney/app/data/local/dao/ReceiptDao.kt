package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow

data class ReceiptWithItems(
    @androidx.room.Embedded val receipt: ReceiptEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "receiptId"
    )
    val items: List<ReceiptItemEntity>
)

data class ItemCategoryExpense(
    val categoryId: Long?,
    val totalAmount: Long
)

@Dao
interface ReceiptDao {

    @Transaction
    @Query("SELECT * FROM receipts ORDER BY receiptDate DESC, createdAt DESC")
    fun getAllReceiptsWithItemsFlow(): Flow<List<ReceiptWithItems>>

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptWithItemsById(id: Long): ReceiptWithItems?

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Long): ReceiptEntity?

    @Query("""
        SELECT r.* FROM receipts r 
        LEFT JOIN transaction_receipt_links l ON r.id = l.receiptId AND l.status = 'CONFIRMED'
        WHERE l.id IS NULL
        ORDER BY r.createdAt DESC
    """)
    fun getUnlinkedReceiptsFlow(): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceiptItems(items: List<ReceiptItemEntity>)

    @Query("DELETE FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun deleteItemsForReceipt(receiptId: Long)

    @Query("SELECT * FROM receipt_items WHERE receiptId = :receiptId")
    suspend fun getItemsForReceipt(receiptId: Long): List<ReceiptItemEntity>

    @Query("""
        SELECT 
            ri.categoryId,
            SUM(ri.totalPriceMinor) AS totalAmount
        FROM receipt_items ri
        INNER JOIN receipts r ON ri.receiptId = r.id
        WHERE r.receiptDate >= :startTime AND r.receiptDate <= :endTime
        GROUP BY ri.categoryId
    """)
    fun getItemExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<ItemCategoryExpense>>
}
