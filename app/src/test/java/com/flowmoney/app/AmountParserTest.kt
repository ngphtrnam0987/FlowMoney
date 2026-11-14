package com.flowmoney.app

import com.flowmoney.app.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    @Test
    fun parseStandardDotSeparatedAmounts() {
        assertEquals(120000L, CurrencyUtils.parseAmount("120.000 VND"))
        assertEquals(1500000L, CurrencyUtils.parseAmount("1.500.000 đ"))
        assertEquals(50000L, CurrencyUtils.parseAmount("50.000d"))
        assertEquals(10000000L, CurrencyUtils.parseAmount("10.000.000 VND"))
    }

    @Test
    fun parseCommaSeparatedAmounts() {
        assertEquals(120000L, CurrencyUtils.parseAmount("120,000 VND"))
        assertEquals(2500000L, CurrencyUtils.parseAmount("2,500,000 VND"))
    }

    @Test
    fun parseAbbreviatedAmounts() {
        assertEquals(150000L, CurrencyUtils.parseAmount("150k"))
        assertEquals(50000L, CurrencyUtils.parseAmount("50k"))
        assertEquals(1500000L, CurrencyUtils.parseAmount("1.5tr"))
        assertEquals(2000000L, CurrencyUtils.parseAmount("2tr"))
        assertEquals(3000000L, CurrencyUtils.parseAmount("3m"))
    }

    @Test
    fun parseSignedAmounts() {
        assertEquals(50000L, CurrencyUtils.parseAmount("-50.000 VND"))
        assertEquals(1200000L, CurrencyUtils.parseAmount("+1.200.000 VND"))
    }

    @Test
    fun formatVndOutput() {
        assertEquals("120.000 ₫", CurrencyUtils.formatVnd(120000L))
        assertEquals("1.500.000 ₫", CurrencyUtils.formatVnd(1500000L))
        assertEquals("+ 50.000 ₫", CurrencyUtils.formatSignedVnd(50000L, isIncome = true))
        assertEquals("- 50.000 ₫", CurrencyUtils.formatSignedVnd(50000L, isIncome = false))
    }

    @Test
    fun handleInvalidAndCorruptedInput() {
        assertNull(CurrencyUtils.parseAmount(null))
        assertNull(CurrencyUtils.parseAmount(""))
        assertNull(CurrencyUtils.parseAmount("abc xyz"))
        assertNull(CurrencyUtils.parseAmount("!@#$%^"))
    }
}
