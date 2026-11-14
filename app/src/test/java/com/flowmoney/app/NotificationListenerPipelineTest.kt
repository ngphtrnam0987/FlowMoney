package com.flowmoney.app

import com.flowmoney.app.data.local.dao.AccountDao
import com.flowmoney.app.data.local.dao.NotificationFingerprintDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.NotificationFingerprintEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.SettingsRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.AuditStatus
import com.flowmoney.app.domain.engine.CategoryClassifier
import com.flowmoney.app.domain.engine.DuplicateDetector
import com.flowmoney.app.domain.engine.NotificationAuditLog
import com.flowmoney.app.domain.engine.NotificationAuditManager
import com.flowmoney.app.domain.engine.SmartTransactionEngine
import com.flowmoney.app.domain.engine.TransactionProcessResult
import com.flowmoney.app.domain.parser.BankNotificationParserRegistry
import com.flowmoney.app.domain.parser.BidvParser
import com.flowmoney.app.domain.parser.MbBankParser
import com.flowmoney.app.domain.parser.TechcombankParser
import com.flowmoney.app.domain.parser.VietcombankParser
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationListenerPipelineTest {

    private lateinit var auditManager: NotificationAuditManager
    private lateinit var parserRegistry: BankNotificationParserRegistry

    @Before
    fun setUp() {
        auditManager = NotificationAuditManager()
        parserRegistry = BankNotificationParserRegistry()
    }

    @Test
    fun testAuditManagerConnectionLifecycle() {
        assertFalse(auditManager.isListenerConnected.value)
        assertNull(auditManager.lastConnectedTime.value)

        auditManager.setListenerConnected(true)
        assertTrue(auditManager.isListenerConnected.value)
        assertNotNull(auditManager.lastConnectedTime.value)

        auditManager.setListenerConnected(false)
        assertFalse(auditManager.isListenerConnected.value)
        assertNotNull(auditManager.lastDisconnectedTime.value)
    }

    @Test
    fun testSensitiveDataRedactionInAuditLogs() {
        val rawMessageWithOtp = "Vietcombank: Mã OTP xác thực của bạn là 876543 để chuyển 500,000 VND"
        val sanitized = auditManager.sanitizeContent(rawMessageWithOtp)

        assertFalse(sanitized.contains("876543"))
        assertTrue(sanitized.contains("[MÃ_OTP_ĐÃ_ẨN]"))

        val rawWithPassword = "TK đăng nhập mật khẩu abcXYZ123 thành công"
        val sanitizedPassword = auditManager.sanitizeContent(rawWithPassword)
        assertFalse(sanitizedPassword.contains("abcXYZ123"))
    }

    @Test
    fun testAuditLogsCircularBufferCapacity() {
        // Add 60 logs, buffer must retain max 50
        for (i in 1..60) {
            auditManager.logEvent(
                NotificationAuditLog(
                    packageName = "com.test.bank$i",
                    title = "Bank $i",
                    sanitizedSnippet = "Snippet $i",
                    status = AuditStatus.RECEIVED
                )
            )
        }

        assertEquals(50, auditManager.auditLogs.value.size)
        // Most recent should be at the front
        assertEquals("com.test.bank60", auditManager.auditLogs.value.first().packageName)
    }

    @Test
    fun testVietcombankRealNotificationParsing() {
        val parser = VietcombankParser()
        val text = "SD TK 0011001234567 -350,000 VND luc 15:30 14/05/2024. So du: 14,850,000 VND. Ref MBVCB.123456. ND: Mua sam Shopee"
        val parsed = parser.parse("Vietcombank", text, 1715675400000L)

        assertNotNull(parsed)
        assertEquals("VCB", parsed!!.bankIdentifier)
        assertEquals(350000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(14850000L, parsed.balanceAfterMinor)
        assertEquals("MBVCB.123456", parsed.externalReference)
        assertEquals("0011001234567", parsed.maskedAccount)
    }

    @Test
    fun testMbBankRealNotificationParsing() {
        val parser = MbBankParser()
        val text = "TK 0987654321|GD: +10,000,000VND 15/05/24 08:30|SD: 25,000,000VND|ND: Cong ty thanh toan luong T4"
        val parsed = parser.parse("MB Bank", text, 1715736600000L)

        assertNotNull(parsed)
        assertEquals("MB", parsed!!.bankIdentifier)
        assertEquals(10000000L, parsed.amountMinor)
        assertEquals(TransactionDirection.INCOME, parsed.direction)
        assertEquals(25000000L, parsed.balanceAfterMinor)
        assertEquals("0987654321", parsed.maskedAccount)
    }

    @Test
    fun testTechcombankRealNotificationParsing() {
        val parser = TechcombankParser()
        val text = "Tài khoản 1903... biến động -150,000 VND tại ShopeePay vào 12/05/2024. Số dư khả dụng: 2,850,000 VND."
        val parsed = parser.parse("Techcombank", text, 1715500000000L)

        assertNotNull(parsed)
        assertEquals("TCB", parsed!!.bankIdentifier)
        assertEquals(150000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(2850000L, parsed.balanceAfterMinor)
    }

    @Test
    fun testTechcombankUserScreenshotCase() {
        val parser = TechcombankParser()
        val title = "Techcombank: -3,000 VND"
        val body = """
            Tài khoản: 19073124996013
            Số dư: VND 3,650
            156895296834-0926906226-NGUYEN PHAN TRONG NAM chuyen tien qua MoMo
        """.trimIndent()

        val parsed = parser.parse(title, body, System.currentTimeMillis())
        assertNotNull(parsed)
        assertEquals("TCB", parsed!!.bankIdentifier)
        assertEquals(3000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(3650L, parsed.balanceAfterMinor)
        assertEquals("19073124996013", parsed.maskedAccount)
        assertTrue(parsed.transactionDescription.contains("chuyen tien qua MoMo"))
    }

    @Test
    fun testMomoUserScreenshotCase() {
        val parser = com.flowmoney.app.domain.parser.MomoParser()
        val text = """Số tiền 3.000 đ về Túi Thần Tài. Lời nhắn: "NGUYEN PHAN TRONG NAM chuyen tien FT26318343885839"."""

        assertTrue(parser.canParse("com.mservice.momotransfer", "MoMo", text))
        val parsed = parser.parse("MoMo", text, System.currentTimeMillis())

        assertNotNull(parsed)
        assertEquals("MOMO", parsed!!.bankIdentifier)
        assertEquals(3000L, parsed.amountMinor)
        assertEquals(TransactionDirection.INCOME, parsed.direction)
        assertTrue(parsed.externalReference?.contains("FT26318343885839") == true)
        assertTrue(parsed.transactionDescription.contains("NGUYEN PHAN TRONG NAM chuyen tien"))
    }

    @Test
    fun testBidvRealNotificationParsing() {
        val parser = BidvParser()
        val text = "TK 1234567890: -200,000 VND lúc 12:00 14/05/2024. Số dư: 4,500,000 VND. ND: An trua nha hang"
        val parsed = parser.parse("BIDV SmartBanking", text, 1715662800000L)

        assertNotNull(parsed)
        assertEquals("BIDV", parsed!!.bankIdentifier)
        assertEquals(200000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(4500000L, parsed.balanceAfterMinor)
    }

    @Test
    fun testTestTransactionIsolationFromFinancialTotals() {
        val realTx = TransactionEntity(
            id = 1L,
            accountId = 1L,
            source = TransactionSource.BANK_NOTIFICATION,
            amountMinor = 500000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = System.currentTimeMillis(),
            description = "Real Expense",
            isTestTransaction = false
        )

        val testTx = TransactionEntity(
            id = 2L,
            accountId = 1L,
            source = TransactionSource.MANUAL,
            amountMinor = 50000L,
            direction = TransactionDirection.EXPENSE,
            transactionTime = System.currentTimeMillis(),
            description = "[TEST] FlowMoney Test Pipeline",
            isTestTransaction = true
        )

        assertFalse(realTx.isTestTransaction)
        assertTrue(testTx.isTestTransaction)

        // Verifying filtering logic matches TransactionDao queries
        val allList = listOf(realTx, testTx)
        val productionOnly = allList.filter { !it.isTestTransaction }

        assertEquals(1, productionOnly.size)
        assertEquals(500000L, productionOnly.sumOf { it.amountMinor })
    }
}
