package com.flowmoney.app

import com.flowmoney.app.data.local.dao.NotificationFingerprintDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.NotificationFingerprintEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.engine.DuplicateDetector
import com.flowmoney.app.domain.parser.ParsedBankNotification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DuplicateDetectionTest {

    private class FakeFingerprintDao : NotificationFingerprintDao {
        private val hashes = mutableSetOf<String>()
        override suspend fun exists(hash: String): Boolean = hashes.contains(hash)
        override suspend fun insert(fingerprint: NotificationFingerprintEntity): Long {
            hashes.add(fingerprint.fingerprintHash)
            return 1L
        }
        override suspend fun deleteOlderThan(olderThan: Long) {}
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()

        override suspend fun findByExternalReference(externalReference: String): TransactionEntity? {
            return transactions.firstOrNull { it.externalReference == externalReference }
        }

        override suspend fun findSimilarTransactions(
            amountMinor: Long,
            direction: TransactionDirection,
            minTime: Long,
            maxTime: Long
        ): List<TransactionEntity> {
            return transactions.filter {
                it.amountMinor == amountMinor &&
                it.direction == direction &&
                it.transactionTime in minTime..maxTime
            }
        }

        override fun getAllTransactionsFlow() = throw NotImplementedError()
        override fun getRecentTransactionsFlow(limit: Int) = throw NotImplementedError()
        override fun getTransactionsByStatusFlow(status: com.flowmoney.app.data.model.ClassificationStatus) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long) = null
        override fun getTransactionsBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun getTransactionsBetween(startTime: Long, endTime: Long) = emptyList<TransactionEntity>()
        override fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun insertTransaction(transaction: TransactionEntity) = 1L
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun updateClassification(id: Long, categoryId: Long, status: com.flowmoney.app.data.model.ClassificationStatus, updatedAt: Long) {}
        override suspend fun linkReceipt(transactionId: Long, receiptId: Long, updatedAt: Long) {}
        override suspend fun softDeleteTransaction(id: Long, updatedAt: Long) {}
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getPendingClassificationCountFlow() = throw NotImplementedError()
    }

    private lateinit var fingerprintDao: FakeFingerprintDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var duplicateDetector: DuplicateDetector

    @Before
    fun setUp() {
        fingerprintDao = FakeFingerprintDao()
        transactionDao = FakeTransactionDao()
        duplicateDetector = DuplicateDetector(fingerprintDao, transactionDao)
    }

    @Test
    fun detectDuplicateByRecordedFingerprint() = runBlocking {
        val parsed = ParsedBankNotification(
            bankIdentifier = "VCB",
            bankName = "Vietcombank",
            direction = TransactionDirection.EXPENSE,
            amountMinor = 120000L,
            timestamp = 1700000000000L,
            transactionDescription = "Thanh toan Highlands Coffee",
            externalReference = "REF123"
        )

        assertFalse(duplicateDetector.isDuplicate(parsed))
        duplicateDetector.recordFingerprint(parsed, 1L)
        assertTrue(duplicateDetector.isDuplicate(parsed))
    }

    @Test
    fun detectDuplicateByExternalReference() = runBlocking {
        val existingTx = TransactionEntity(
            id = 1L,
            accountId = 1L,
            source = com.flowmoney.app.data.model.TransactionSource.BANK_NOTIFICATION,
            externalReference = "MBVCB.99999",
            amountMinor = 250000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = 1700000000000L,
            description = "Mua hang Tiki"
        )
        transactionDao.transactions.add(existingTx)

        val newParsed = ParsedBankNotification(
            bankIdentifier = "VCB",
            bankName = "Vietcombank",
            direction = TransactionDirection.EXPENSE,
            amountMinor = 250000L,
            timestamp = 1700000000000L + 60000L,
            transactionDescription = "Mua hang Tiki",
            externalReference = "MBVCB.99999"
        )

        assertTrue(duplicateDetector.isDuplicate(newParsed))
    }

    @Test
    fun detectDuplicateWithinTimeWindow() = runBlocking {
        val now = System.currentTimeMillis()
        val existingTx = TransactionEntity(
            id = 2L,
            accountId = 1L,
            source = com.flowmoney.app.data.model.TransactionSource.BANK_NOTIFICATION,
            amountMinor = 50000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = now - 60000L, // 1 minute ago
            description = "Chuyen tien an trua"
        )
        transactionDao.transactions.add(existingTx)

        val incoming = ParsedBankNotification(
            bankIdentifier = "MB",
            bankName = "MB Bank",
            direction = TransactionDirection.EXPENSE,
            amountMinor = 50000L,
            timestamp = now,
            transactionDescription = "Chuyen tien an trua"
        )

        assertTrue(duplicateDetector.isDuplicate(incoming))
    }

    @Test
    fun allowDifferentTransactionsAtDifferentTimes() = runBlocking {
        val now = System.currentTimeMillis()
        val existingTx = TransactionEntity(
            id = 3L,
            accountId = 1L,
            source = com.flowmoney.app.data.model.TransactionSource.BANK_NOTIFICATION,
            amountMinor = 50000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = now - (60 * 60 * 1000L), // 1 hour ago
            description = "Ca phe sang"
        )
        transactionDao.transactions.add(existingTx)

        val incoming = ParsedBankNotification(
            bankIdentifier = "MB",
            bankName = "MB Bank",
            direction = TransactionDirection.EXPENSE,
            amountMinor = 50000L,
            timestamp = now,
            transactionDescription = "Ca phe chieu"
        )

        assertFalse(duplicateDetector.isDuplicate(incoming))
    }
}
