package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.dao.ItemCategoryExpense
import com.flowmoney.app.data.local.dao.ReceiptDao
import com.flowmoney.app.data.local.dao.ReceiptWithItems
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.ReceiptItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepository @Inject constructor(
    private val receiptDao: ReceiptDao
) {
    fun getAllReceiptsFlow(): Flow<List<ReceiptWithItems>> =
        receiptDao.getAllReceiptsWithItemsFlow()

    fun getUnlinkedReceiptsFlow(): Flow<List<ReceiptEntity>> =
        receiptDao.getUnlinkedReceiptsFlow()

    suspend fun getReceiptById(id: Long): ReceiptWithItems? =
        receiptDao.getReceiptWithItemsById(id)

    suspend fun insertReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>
    ): Long {
        val receiptId = receiptDao.insertReceipt(receipt)
        val itemsWithId = items.map { it.copy(receiptId = receiptId) }
        if (itemsWithId.isNotEmpty()) {
            receiptDao.insertReceiptItems(itemsWithId)
        }
        return receiptId
    }

    suspend fun updateReceiptWithItems(
        receipt: ReceiptEntity,
        items: List<ReceiptItemEntity>
    ) {
        receiptDao.updateReceipt(receipt)
        receiptDao.deleteItemsForReceipt(receipt.id)
        val itemsWithId = items.map { it.copy(receiptId = receipt.id) }
        if (itemsWithId.isNotEmpty()) {
            receiptDao.insertReceiptItems(itemsWithId)
        }
    }

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        receiptDao.deleteReceipt(receipt)
    }

    fun getItemExpensesBetweenFlow(startTime: Long, endTime: Long): Flow<List<ItemCategoryExpense>> =
        receiptDao.getItemExpensesBetweenFlow(startTime, endTime)
}
