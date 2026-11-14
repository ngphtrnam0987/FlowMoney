package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowmoney.app.data.local.entity.TransactionReceiptLinkEntity
import com.flowmoney.app.data.model.MatchingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionReceiptLinkDao {

    @Query("SELECT * FROM transaction_receipt_links WHERE transactionId = :transactionId")
    suspend fun getLinksForTransaction(transactionId: Long): List<TransactionReceiptLinkEntity>

    @Query("SELECT * FROM transaction_receipt_links WHERE receiptId = :receiptId")
    suspend fun getLinksForReceipt(receiptId: Long): List<TransactionReceiptLinkEntity>

    @Query("SELECT * FROM transaction_receipt_links WHERE status = 'SUGGESTED' ORDER BY matchConfidence DESC")
    fun getSuggestedLinksFlow(): Flow<List<TransactionReceiptLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: TransactionReceiptLinkEntity): Long

    @Update
    suspend fun updateLink(link: TransactionReceiptLinkEntity)

    @Query("UPDATE transaction_receipt_links SET status = :status WHERE id = :linkId")
    suspend fun updateStatus(linkId: Long, status: MatchingStatus)

    @Delete
    suspend fun deleteLink(link: TransactionReceiptLinkEntity)
}
