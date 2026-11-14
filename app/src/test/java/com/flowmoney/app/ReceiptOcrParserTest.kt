package com.flowmoney.app

import com.flowmoney.app.domain.engine.ReceiptOcrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReceiptOcrParserTest {

    private lateinit var parser: ReceiptOcrParser

    @Before
    fun setUp() {
        parser = ReceiptOcrParser()
    }

    @Test
    fun parseHighlandsCoffeeReceipt() {
        val ocrSample = """
            HIGHLANDS COFFEE
            Chi nhánh Hàm Nghi, Q.1, TP.HCM
            Ngày: 14/11/2026 14:30:15
            Số HĐ: HD12345
            --------------------------------
            Phin Sữa Đá         1   45.000
            Trà Sen Vàng        1   50.000
            --------------------------------
            Tổng cộng: 95.000 VND
            Tiền mặt: 100.000
            Tiền thối: 5.000
            Cảm ơn quý khách!
        """.trimIndent()

        val result = parser.parseOcrText(ocrSample)

        assertEquals("HIGHLANDS COFFEE", result.merchantName)
        assertEquals(95000L, result.totalMinor)
        assertEquals(2, result.lineItems.size)

        val item1 = result.lineItems[0]
        assertTrue(item1.name.contains("Phin Sữa Đá", ignoreCase = true))
        assertEquals(45000L, item1.totalPriceMinor)

        val item2 = result.lineItems[1]
        assertTrue(item2.name.contains("Trà Sen Vàng", ignoreCase = true))
        assertEquals(50000L, item2.totalPriceMinor)

        // Line items total validation
        val itemsSum = result.lineItems.sumOf { it.totalPriceMinor }
        assertEquals(result.totalMinor, itemsSum)
    }

    @Test
    fun parseSupermarketReceiptWithLineItems() {
        val ocrSample = """
            WINMART +
            HÓA ĐƠN BÁN HÀNG
            Ngày: 12-11-2026
            Sữa tươi Vinamilk     2   64.000
            Bánh mì sandwich      1   22.000
            Nước suối Aquafina    1   10.000
            --------------------------------
            Tạm tính: 96.000
            VAT: 9.600
            Tổng thanh toán: 105.600 VND
        """.trimIndent()

        val result = parser.parseOcrText(ocrSample)

        assertTrue(result.merchantName?.contains("WINMART", ignoreCase = true) == true)
        assertEquals(105600L, result.totalMinor)
        assertEquals(3, result.lineItems.size)
    }

    @Test
    fun parseTechcombankTransferReceipt() {
        val ocrSample = """
            Chuyển tiền thành công
            50.000 VND
            Người nhận: NGUYEN PHAN TRONG NAM
            Tài khoản nhận: 19073124996013
            Ngân hàng nhận: Techcombank
            Lời nhắn: tien com trua
            Mã giao dịch: FT241114987213
            Thời gian: 14/11/2026 20:30
        """.trimIndent()

        val result = parser.parseOcrText(ocrSample)

        assertTrue(result.isBankTransfer)
        assertEquals(50000L, result.totalMinor)
        assertTrue(result.merchantName?.contains("NGUYEN PHAN TRONG NAM", ignoreCase = true) == true)
        assertTrue(result.merchantName?.contains("Techcombank", ignoreCase = true) == true)
        assertEquals("tien com trua", result.transactionNote)
        assertEquals("FT241114987213", result.transactionCode)
    }

    @Test
    fun parseVietcombankTransferReceipt() {
        val ocrSample = """
            GIAO DỊCH THÀNH CÔNG
            Quý khách đã chuyển thành công số tiền
            500.000 VND
            đến người thụ hưởng
            TRAN VAN B
            Tài khoản thụ hưởng: 0071000123456
            Tại ngân hàng: Vietcombank
            Nội dung: Thanh toan tien phong
            Mã tham chiếu: 987654321
            Ngày thực hiện: 14/11/2026 14:22:10
        """.trimIndent()

        val result = parser.parseOcrText(ocrSample)

        assertTrue(result.isBankTransfer)
        assertEquals(500000L, result.totalMinor)
        assertTrue(result.merchantName?.contains("TRAN VAN B", ignoreCase = true) == true)
        assertTrue(result.merchantName?.contains("Vietcombank", ignoreCase = true) == true)
        assertEquals("Thanh toan tien phong", result.transactionNote)
    }

    @Test
    fun parseMomoTransferReceipt() {
        val ocrSample = """
            Chuyển tiền thành công
            Số tiền: 30.000đ
            Người nhận: NGUYEN PHAN TRONG NAM
            Lời nhắn: tra tien nuoc
            Mã giao dịch: 26318343885839
            Thời gian: 14/11/2026 20:15
        """.trimIndent()

        val result = parser.parseOcrText(ocrSample)

        assertTrue(result.isBankTransfer)
        assertEquals(30000L, result.totalMinor)
        assertTrue(result.merchantName?.contains("NGUYEN PHAN TRONG NAM", ignoreCase = true) == true)
        assertEquals("tra tien nuoc", result.transactionNote)
    }
}
