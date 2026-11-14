package com.flowmoney.app

import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.dao.TransactionReceiptLinkDao
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.local.entity.TransactionReceiptLinkEntity
import com.flowmoney.app.data.model.MatchingStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.engine.ReceiptMatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReceiptMatchingTest {

    private class FakeTransactionDao : TransactionDao {
        val list = mutableListOf<TransactionEntity>()
        override suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity> {
            return list.filter { it.transactionTime in startTime..endTime }
        }
        override suspend fun linkReceipt(transactionId: Long, receiptId: Long, updatedAt: Long) {
            val idx = list.indexOfFirst { it.id == transactionId }
            if (idx != -1) {
                list[idx] = list[idx].copy(linkedReceiptId = receiptId)
            }
        }
        override fun getAllTransactionsFlow() = throw NotImplementedError()
        override fun getRecentTransactionsFlow(limit: Int) = throw NotImplementedError()
        override fun getTransactionsByStatusFlow(status: com.flowmoney.app.data.model.ClassificationStatus) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long) = list.firstOrNull { it.id == id }
        override fun getTransactionsBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun findSimilarTransactions(amountMinor: Long, direction: TransactionDirection, minTime: Long, maxTime: Long) = emptyList<TransactionEntity>()
        override suspend fun findByExternalReference(externalReference: String) = null
        override suspend fun insertTransaction(transaction: TransactionEntity) = 1L
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun updateClassification(id: Long, categoryId: Long, status: com.flowmoney.app.data.model.ClassificationStatus, updatedAt: Long) {}
        override suspend fun softDeleteTransaction(id: Long, updatedAt: Long) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getPendingClassificationCountFlow() = throw NotImplementedError()
        override fun getTestTransactionsFlow() = kotlinx.coroutines.flow.flowOf(emptyList<TransactionEntity>())
        override suspend fun clearTestTransactions() {}
    }

    private class FakeLinkDao : TransactionReceiptLinkDao {
        val links = mutableListOf<TransactionReceiptLinkEntity>()
        override suspend fun insertLink(link: TransactionReceiptLinkEntity): Long {
            links.add(link)
            return links.size.toLong()
        }
        override suspend fun getLinksForTransaction(transactionId: Long) = links.filter { it.transactionId == transactionId }
        override suspend fun getLinksForReceipt(receiptId: Long) = links.filter { it.receiptId == receiptId }
        override fun getSuggestedLinksFlow() = throw NotImplementedError()
        override suspend fun updateLink(link: TransactionReceiptLinkEntity) {}
        override suspend fun updateStatus(linkId: Long, status: MatchingStatus) {}
        override suspend fun deleteLink(link: TransactionReceiptLinkEntity) {}
    }

    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var linkDao: FakeLinkDao
    private lateinit var matcher: ReceiptMatcher

    @Before
    fun setUp() {
        transactionDao = FakeTransactionDao()
        linkDao = FakeLinkDao()
        matcher = ReceiptMatcher(transactionDao, linkDao)
    }

    @Test
    fun matchReceiptWithExactBankTransaction() = runBlocking {
        val now = System.currentTimeMillis()
        val tx = TransactionEntity(
            id = 10L,
            accountId = 1L,
            source = com.flowmoney.app.data.model.TransactionSource.BANK_NOTIFICATION,
            amountMinor = 95000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = now - 30 * 60 * 1000L, // 30 mins ago
            merchantName = "Highlands Coffee",
            description = "Thanh toan Highlands Coffee"
        )
        transactionDao.list.add(tx)

        val receipt = ReceiptEntity(
            id = 5L,
            merchantName = "Highlands Coffee",
            totalMinor = 95000L,
            receiptDate = now
        )

        val candidates = matcher.findMatchingCandidates(receipt)

        assertEquals(1, candidates.size)
        val best = candidates.first()
        assertEquals(10L, best.transaction.id)
        assertTrue(best.score >= 0.85f) // High confidence

        // Confirm match
        matcher.confirmMatch(best.transaction.id, receipt.id)
        assertEquals(5L, transactionDao.list.first().linkedReceiptId)
        assertEquals(1, linkDao.links.size)
        assertEquals(MatchingStatus.CONFIRMED, linkDao.links.first().status)
    }
}
