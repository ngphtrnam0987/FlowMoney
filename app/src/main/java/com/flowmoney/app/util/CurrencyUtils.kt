package com.flowmoney.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern

object CurrencyUtils {

    private val vndFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,###", symbols)
    }

    /**
     * Format minor amount (Long) into VND display string
     * e.g. 150000 -> "150.000 ₫"
     */
    fun formatVnd(amountMinor: Long, includeSymbol: Boolean = true): String {
        val formatted = vndFormat.format(amountMinor)
        return if (includeSymbol) "$formatted ₫" else formatted
    }

    /**
     * Format signed amount
     * e.g. + 150.000 ₫ or - 50.000 ₫
     */
    fun formatSignedVnd(amountMinor: Long, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign ${formatVnd(amountMinor)}"
    }

    /**
     * Parses various raw string representations of money into Long (in VND)
     * Handles:
     * - "120.000 VND", "120,000 VND", "120.000đ", "120000"
     * - "+50.000VND", "-100.000d"
     * - "150k" -> 150000
     * - "1.5tr" / "1,5tr" / "1.5m" -> 1500000
     */
    fun parseAmount(rawText: String?): Long? {
        if (rawText.isNullOrBlank()) return null

        val clean = rawText.trim().lowercase(Locale.ROOT)
            .replace("vnd", "")
            .replace("đ", "")
            .replace("dong", "")
            .replace("đồng", "")
            .replace(" ", "")

        // Check for 'tr' or 'm' (triệu / million)
        val trPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*(?:tr|trieu|triệu|m)")
        val trMatcher = trPattern.matcher(clean)
        if (trMatcher.find()) {
            val numStr = trMatcher.group(1)?.replace(",", ".") ?: return null
            val value = numStr.toDoubleOrNull() ?: return null
            return (value * 1_000_000).toLong()
        }

        // Check for 'k' (nghìn / thousand)
        val kPattern = Pattern.compile("([0-9]+(?:[.,][0-9]+)?)\\s*k")
        val kMatcher = kPattern.matcher(clean)
        if (kMatcher.find()) {
            val numStr = kMatcher.group(1)?.replace(",", ".") ?: return null
            val value = numStr.toDoubleOrNull() ?: return null
            return (value * 1_000).toLong()
        }

        // Standard numeric string with dots or commas as thousand separators
        // Extract digits and separators
        val digitsOnly = clean.replace(Regex("[^0-9.,]"), "")
        if (digitsOnly.isEmpty()) return null

        // If there's both dot and comma (e.g. 1,000.00 or 1.000,00)
        return try {
            if (digitsOnly.contains(".") && digitsOnly.contains(",")) {
                val lastDot = digitsOnly.lastIndexOf('.')
                val lastComma = digitsOnly.lastIndexOf(',')
                if (lastDot > lastComma) {
                    // comma is thousands separator, dot is decimal: 1,000.00
                    val whole = digitsOnly.substring(0, lastDot).replace(",", "")
                    whole.toLongOrNull()
                } else {
                    // dot is thousands separator, comma is decimal: 1.000,00
                    val whole = digitsOnly.substring(0, lastComma).replace(".", "")
                    whole.toLongOrNull()
                }
            } else if (digitsOnly.contains(".")) {
                // If ending with .00 (cents), remove it, otherwise dots are thousands separators in VN
                if (digitsOnly.endsWith(".00")) {
                    digitsOnly.substringBeforeLast(".00").replace(".", "").toLongOrNull()
                } else {
                    digitsOnly.replace(".", "").toLongOrNull()
                }
            } else if (digitsOnly.contains(",")) {
                // In VN, comma is sometimes used as thousands separator in banking or decimal
                if (digitsOnly.endsWith(",00")) {
                    digitsOnly.substringBeforeLast(",00").replace(",", "").toLongOrNull()
                } else {
                    digitsOnly.replace(",", "").toLongOrNull()
                }
            } else {
                digitsOnly.toLongOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}
