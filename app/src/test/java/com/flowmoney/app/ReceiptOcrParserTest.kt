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
}
