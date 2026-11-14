package com.flowmoney.app.domain.engine

import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.ReceiptItemEntity
import com.flowmoney.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedReceiptData(
    val merchantName: String?,
    val merchantAddress: String?,
    val date: Long?,
    val time: String?,
    val subtotalMinor: Long?,
    val discountMinor: Long?,
    val taxMinor: Long?,
    val totalMinor: Long,
    val paymentMethod: String?,
    val lineItems: List<ReceiptItemEntity>,
    val confidenceScore: Float,
    val rawText: String
)

@Singleton
class ReceiptOcrParser @Inject constructor() {

    fun parseOcrText(rawText: String, imagePath: String? = null): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Merchant Name heuristics: top non-empty lines that don't look like dates or receipts
        var merchantName: String? = null
        for (line in lines.take(5)) {
            val lower = line.lowercase()
            if (!lower.contains("hoa don") &&
                !lower.contains("phiếu") &&
                !lower.contains("phieu") &&
                !lower.contains("receipt") &&
                !lower.contains("vat") &&
                !lower.contains("ngày") &&
                !lower.contains("ngay") &&
                line.length in 3..40) {
                merchantName = line
                break
            }
        }

        // 2. Date and Time Extraction
        var receiptDate: Long? = null
        var receiptTime: String? = null
        val datePattern = Pattern.compile("([0-3]?[0-9][/-][0-1]?[0-9][/-][12][09][0-9]{2})")
        val timePattern = Pattern.compile("([0-2]?[0-9]:[0-5][0-9](?::[0-5][0-9])?)")

        for (line in lines) {
            if (receiptDate == null) {
                val dm = datePattern.matcher(line)
                if (dm.find()) {
                    val dateStr = dm.group(1)?.replace("-", "/") ?: ""
                    try {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        receiptDate = sdf.parse(dateStr)?.time
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            if (receiptTime == null) {
                val tm = timePattern.matcher(line)
                if (tm.find()) {
                    receiptTime = tm.group(1)
                }
            }
        }
        if (receiptDate == null) {
            receiptDate = System.currentTimeMillis()
        }

        // 3. Total Amount, Subtotal, Tax, Discount
        var totalAmount: Long? = null
        var subtotal: Long? = null
        var discount: Long? = null
        var tax: Long? = null

        val totalKeywords = listOf("tổng cộng", "tong cong", "tổng tiền", "tong tien", "thành tiền", "thanh tien", "tổng thanh toán", "tong thanh toan", "total", "amount", "phải trả", "phai tra")
        val subtotalKeywords = listOf("tiền hàng", "tien hang", "tạm tính", "tam tinh", "subtotal", "cộng tiền hàng", "cong tien hang")
        val discountKeywords = listOf("giảm giá", "giam gia", "chiết khấu", "chiet khau", "discount", "voucher")
        val taxKeywords = listOf("thuế vat", "thue vat", "vat", "thuế", "thue", "tax")

        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase()

            // Check total
            if (totalAmount == null && totalKeywords.any { lower.contains(it) }) {
                val parsedAmt = CurrencyUtils.parseAmount(line)
                if (parsedAmt != null) {
                    totalAmount = parsedAmt
                } else if (i + 1 < lines.size) {
                    // Try next line if amount is on the next line
                    totalAmount = CurrencyUtils.parseAmount(lines[i + 1])
                }
            }

            // Check subtotal
            if (subtotal == null && subtotalKeywords.any { lower.contains(it) }) {
                subtotal = CurrencyUtils.parseAmount(line)
            }

            // Check discount
            if (discount == null && discountKeywords.any { lower.contains(it) }) {
                discount = CurrencyUtils.parseAmount(line)
            }

            // Check tax
            if (tax == null && taxKeywords.any { lower.contains(it) }) {
                tax = CurrencyUtils.parseAmount(line)
            }
        }

        // 4. Line Items Extraction
        val lineItems = mutableListOf<ReceiptItemEntity>()
        // Items typically look like: "Item Name [quantity] [price]"
        val itemPattern = Pattern.compile("^(.*?)\\s+([0-9]+(?:[.,][0-9]+)?)\\s+([0-9]{1,3}(?:[.,][0-9]{3})+)$")

        for (line in lines) {
            val lower = line.lowercase()
            // Skip lines that are summary lines or payment lines
            if (totalKeywords.any { lower.contains(it) } ||
                subtotalKeywords.any { lower.contains(it) } ||
                taxKeywords.any { lower.contains(it) } ||
                lower.contains("ngày") ||
                lower.contains("ngay") ||
                lower.contains("hóa đơn") ||
                lower.contains("hoa don") ||
                lower.contains("cảm ơn") ||
                lower.contains("cam on") ||
                lower.contains("wifi") ||
                lower.contains("tiền mặt") ||
                lower.contains("tien mat") ||
                lower.contains("tiền thối") ||
                lower.contains("tien thoi") ||
                lower.contains("thối lại") ||
                lower.contains("thoi lai") ||
                lower.contains("trả lại") ||
                lower.contains("tra lai") ||
                lower.contains("khách đưa") ||
                lower.contains("khach dua") ||
                lower.contains("cash") ||
                lower.contains("change")) {
                continue
            }

            val matcher = itemPattern.matcher(line)
            if (matcher.find()) {
                val itemName = matcher.group(1)?.trim() ?: ""
                val qtyStr = matcher.group(2)?.replace(",", ".") ?: "1"
                val priceStr = matcher.group(3) ?: ""

                val qty = qtyStr.toDoubleOrNull() ?: 1.0
                val price = CurrencyUtils.parseAmount(priceStr) ?: 0L

                if (itemName.length > 1 && price > 0L) {
                    lineItems.add(
                        ReceiptItemEntity(
                            receiptId = 0L,
                            name = itemName,
                            quantity = qty,
                            totalPriceMinor = price,
                            unitPriceMinor = if (qty > 0) (price / qty).toLong() else price
                        )
                    )
                }
            } else {
                // Heuristic: line contains text followed by amount at end
                val simplePricePattern = Pattern.compile("^(.*?)\\s+([0-9]{1,3}(?:[.,][0-9]{3})+)$")
                val spMatcher = simplePricePattern.matcher(line)
                if (spMatcher.find()) {
                    val itemName = spMatcher.group(1)?.trim() ?: ""
                    val priceStr = spMatcher.group(2) ?: ""
                    val price = CurrencyUtils.parseAmount(priceStr) ?: 0L
                    if (itemName.length > 2 && price > 0L && price != totalAmount) {
                        lineItems.add(
                            ReceiptItemEntity(
                                receiptId = 0L,
                                name = itemName,
                                quantity = 1.0,
                                totalPriceMinor = price,
                                unitPriceMinor = price
                            )
                        )
                    }
                }
            }
        }

        // If totalAmount was not found but line items exist, sum line items
        val finalTotal = totalAmount ?: if (lineItems.isNotEmpty()) {
            lineItems.sumOf { it.totalPriceMinor }
        } else {
            0L
        }

        val confidence = if (totalAmount != null && lineItems.isNotEmpty()) 0.9f else if (totalAmount != null) 0.75f else 0.5f

        return ParsedReceiptData(
            merchantName = merchantName,
            merchantAddress = null,
            date = receiptDate,
            time = receiptTime,
            subtotalMinor = subtotal,
            discountMinor = discount,
            taxMinor = tax,
            totalMinor = finalTotal,
            paymentMethod = null,
            lineItems = lineItems,
            confidenceScore = confidence,
            rawText = rawText
        )
    }
}
