package com.flowmoney.app

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.parser.BankNotificationParserRegistry
import com.flowmoney.app.domain.parser.BidvParser
import com.flowmoney.app.domain.parser.MbBankParser
import com.flowmoney.app.domain.parser.TechcombankParser
import com.flowmoney.app.domain.parser.VietcombankParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BankNotificationParserTest {

    private lateinit var registry: BankNotificationParserRegistry

    @Before
    fun setUp() {
        registry = BankNotificationParserRegistry()
    }

    @Test
    fun parseVietcombankNotification() {
        val parser = VietcombankParser()
        val title = "Vietcombank"
        val text = "VCB: 001100123456 -120,000 VND luc 14-11-2026 10:15:30. So du 5,230,000 VND. Ref MBVCB.98765. ND: Thanh toan ca phe Highlands"

        assertTrue(parser.canParse("com.VCB", title, text))
        val parsed = parser.parse(title, text, System.currentTimeMillis())

        assertNotNull(parsed)
        assertEquals("VCB", parsed!!.bankIdentifier)
        assertEquals(120000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(5230000L, parsed.balanceAfterMinor)
        assertEquals("MBVCB.98765", parsed.externalReference)
        assertTrue(parsed.transactionDescription.contains("ca phe", ignoreCase = true))
    }

    @Test
    fun parseMbBankNotification() {
        val parser = MbBankParser()
        val title = "MB Bank"
        val text = "TK 0987654321 GD: -50,000VND 14/11/26 12:30. SD: 1,500,000VND. ND: Chuyen tien an trua bun bo"

        assertTrue(parser.canParse("com.mbmobile", title, text))
        val parsed = parser.parse(title, text, System.currentTimeMillis())

        assertNotNull(parsed)
        assertEquals("MB", parsed!!.bankIdentifier)
        assertEquals(50000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(1500000L, parsed.balanceAfterMinor)
        assertTrue(parsed.transactionDescription.contains("an trua", ignoreCase = true))
    }

    @Test
    fun parseTechcombankNotification() {
        val parser = TechcombankParser()
        val title = "Techcombank"
        val text = "TCB: GD -250,000 VND tai Highlands Coffee. So du: 3,450,000 VND."

        assertTrue(parser.canParse("vn.com.techcombank.bb.app", title, text))
        val parsed = parser.parse(title, text, System.currentTimeMillis())

        assertNotNull(parsed)
        assertEquals("TCB", parsed!!.bankIdentifier)
        assertEquals(250000L, parsed.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
        assertEquals(3450000L, parsed.balanceAfterMinor)
        assertEquals("Highlands Coffee", parsed.counterparty)
    }

    @Test
    fun parseBidvIncomeNotification() {
        val parser = BidvParser()
        val title = "BIDV SmartBanking"
        val text = "BIDV: TK 123456789 GD: +5,000,000 VND 14/11/2026. So du: 15,000,000 VND. ND: Nhan luong thang 11"

        assertTrue(parser.canParse("com.vnpay.bidv", title, text))
        val parsed = parser.parse(title, text, System.currentTimeMillis())

        assertNotNull(parsed)
        assertEquals("BIDV", parsed!!.bankIdentifier)
        assertEquals(5000000L, parsed.amountMinor)
        assertEquals(TransactionDirection.INCOME, parsed.direction)
        assertEquals(15000000L, parsed.balanceAfterMinor)
    }

    @Test
    fun parseGenericBankViaRegistry() {
        // TPBank notification parsed through Registry
        val title = "TPBank"
        val text = "TPBank: TK 0123456789 -80,000 VND. So du: 2,500,000 VND. ND: Thanh toan Grab"

        val parsed = registry.parseNotification("com.tpb.mb.gprsandroid", title, text)
        assertNotNull(parsed)
        assertEquals(80000L, parsed!!.amountMinor)
        assertEquals(TransactionDirection.EXPENSE, parsed.direction)
    }

    @Test
    fun rejectOtpNotificationsForPrivacyAndSecurity() {
        // OTP notification MUST be rejected by all parsers!
        val otpTitle = "Vietcombank"
        val otpText = "Ma OTP xac thuc giao dich cua ban la 123456. Khong chia se ma nay voi bat ky ai."

        val vcbParser = VietcombankParser()
        assertTrue(vcbParser.isOtpNotification(otpText))
        assertNull(vcbParser.parse(otpTitle, otpText, System.currentTimeMillis()))

        val mbParser = MbBankParser()
        assertTrue(mbParser.isOtpNotification("Mã xác thực Smart OTP là 987654. Quý khách tuyệt đối không chia sẻ."))
        assertNull(mbParser.parse("MB Bank", "Mã xác thực Smart OTP là 987654. Quý khách tuyệt đối không chia sẻ.", System.currentTimeMillis()))
    }
}
