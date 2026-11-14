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
    val rawText: String,
    val isBankTransfer: Boolean = false,
    val bankName: String? = null,
    val recipientAccount: String? = null,
    val transactionNote: String? = null,
    val transactionCode: String? = null
)

@Singleton
class ReceiptOcrParser @Inject constructor() {

    companion object {
        private val BANK_TRANSFER_KEYWORDS = listOf(
            "chuyển tiền thành công", "chuyen tien thanh cong",
            "chuyển khoản thành công", "chuyen khoan thanh cong",
            "giao dịch thành công", "giao dich thanh cong",
            "thanh toán thành công", "thanh toan thanh cong",
            "chuyển thành công", "chuyen thanh cong",
            "người thụ hưởng", "nguoi thu huong",
            "tài khoản thụ hưởng", "tai khoan thu huong",
            "tài khoản nguồn", "tai khoan nguon",
            "người nhận", "nguoi nhan",
            "ngân hàng nhận", "ngan hang nhan",
            "vietqr", "napas247", "napas 247"
        )

        private val KNOWN_BANKS = listOf(
            "Techcombank", "Vietcombank", "MB Bank", "MB", "MoMo", "BIDV",
            "VPBank", "ACB", "TPBank", "VietinBank", "Sacombank", "VIB",
            "Agribank", "ZaloPay", "ShopeePay", "HDBank", "OCB", "MSB", "SHB", "SeABank", "LPBank"
        )
    }

    fun parseOcrText(rawText: String, imagePath: String? = null): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Date and Time Extraction
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

        // 1. Check if this is a Bank Transfer Receipt
        val isBankTransfer = BANK_TRANSFER_KEYWORDS.any { rawText.contains(it, ignoreCase = true) }
        if (isBankTransfer) {
            val bankTransferResult = parseBankTransfer(rawText, lines, receiptDate, receiptTime)
            if (bankTransferResult.totalMinor > 0) {
                return bankTransferResult
            }
        }

        // 2. Commercial / Restaurant / Supermarket Receipt Parser
        return parseCommercialReceipt(rawText, lines, receiptDate, receiptTime)
    }

    private fun parseBankTransfer(
        rawText: String,
        lines: List<String>,
        receiptDate: Long,
        receiptTime: String?
    ): ParsedReceiptData {
        // 1. Amount
        var amount: Long? = null

        // Try keyword amount pattern: Số tiền: 50.000 VND / Số tiền chuyển 50.000 đ
        val keywordAmtPattern = Pattern.compile(
            "(?:số tiền chuyển|so tien chuyen|số tiền|so tien|giá trị gd|gia tri gd|thanh toán|thanh toan|amount)\\s*:?\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:vnd|đ|₫|d)?",
            Pattern.CASE_INSENSITIVE
        )
        val km = keywordAmtPattern.matcher(rawText)
        if (km.find()) {
            amount = CurrencyUtils.parseAmount(km.group(1))
        }

        // If not found, look through lines for prominent currency amounts
        if (amount == null || amount <= 0L) {
            val standaloneAmtPattern = Pattern.compile(
                "^([0-9]{1,3}(?:[.,][0-9]{3})+)\\s*(?:vnd|đ|₫|d)?$",
                Pattern.CASE_INSENSITIVE
            )
            for (line in lines) {
                val sm = standaloneAmtPattern.matcher(line)
                if (sm.find()) {
                    val parsed = CurrencyUtils.parseAmount(sm.group(1))
                    if (parsed != null && parsed > (amount ?: 0L)) {
                        amount = parsed
                    }
                }
            }
        }

        // Fallback: look for any number ending with VND or đ
        if (amount == null || amount <= 0L) {
            val generalAmtPattern = Pattern.compile(
                "([0-9]{1,3}(?:[.,][0-9]{3})+)\\s*(?:vnd|đ|₫|d)",
                Pattern.CASE_INSENSITIVE
            )
            val gm = generalAmtPattern.matcher(rawText)
            while (gm.find()) {
                val parsed = CurrencyUtils.parseAmount(gm.group(1))
                if (parsed != null && parsed > (amount ?: 0L)) {
                    amount = parsed
                }
            }
        }

        val finalAmount = amount ?: 0L

        // 2. Bank Name
        val bankName = KNOWN_BANKS.firstOrNull { rawText.contains(it, ignoreCase = true) }

        // 3. Recipient Name
        var recipientName: String? = null
        val recipientPattern = Pattern.compile(
            "(?:người thụ hưởng|nguoi thu huong|tên người thụ hưởng|người nhận|nguoi nhan|tên người nhận|đến|den|tới|toi)\\s*:?\\s*([A-Za-zÀ-ỹ\\s]{2,40})",
            Pattern.CASE_INSENSITIVE
        )
        val rm = recipientPattern.matcher(rawText)
        if (rm.find()) {
            val extracted = rm.group(1)?.trim()
            if (!extracted.isNullOrBlank() && !extracted.equals("tài khoản", ignoreCase = true)) {
                recipientName = extracted
            }
        }

        // If not found by regex, search line by line
        if (recipientName == null) {
            for (i in lines.indices) {
                val lower = lines[i].lowercase()
                if (lower.contains("người nhận") || lower.contains("người thụ hưởng") || lower == "đến" || lower == "tới") {
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        if (nextLine.length in 3..40 && !nextLine.contains(":") && !nextLine.contains("/")) {
                            recipientName = nextLine
                            break
                        }
                    }
                }
            }
        }

        // 4. Note / Description
        var transferNote: String? = null
        val notePattern = Pattern.compile(
            "(?:lời nhắn|loi nhan|nội dung|noi dung|lời chúc|loi chuc|nd chuyển tiền|nd|nội dung gd)\\s*:?\\s*(.+)$",
            Pattern.CASE_INSENSITIVE
        )
        for (line in lines) {
            val nm = notePattern.matcher(line)
            if (nm.find()) {
                val extracted = nm.group(1)?.trim()
                if (!extracted.isNullOrBlank()) {
                    transferNote = extracted
                    break
                }
            }
        }

        // 5. Transaction Code
        var txCode: String? = null
        val codePattern = Pattern.compile(
            "(?:mã giao dịch|ma giao dich|mã gd|ma gd|mã tham chiếu|ma tham chieu|mã tra cứu|ref no|transaction id)\\s*:?\\s*([A-Za-z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
        )
        for (line in lines) {
            val cm = codePattern.matcher(line)
            if (cm.find()) {
                txCode = cm.group(1)?.trim()
                break
            }
        }

        // Format Merchant / Counterparty
        val displayMerchant = when {
            recipientName != null && bankName != null -> "$recipientName ($bankName)"
            recipientName != null -> recipientName
            bankName != null -> "Chuyển tiền $bankName"
            else -> "Chuyển tiền ngân hàng"
        }

        val itemDescription = transferNote ?: "Chuyển tiền: $displayMerchant"
        val lineItems = listOf(
            ReceiptItemEntity(
                receiptId = 0L,
                name = itemDescription,
                quantity = 1.0,
                totalPriceMinor = finalAmount,
                unitPriceMinor = finalAmount
            )
        )

        return ParsedReceiptData(
            merchantName = displayMerchant,
            merchantAddress = null,
            date = receiptDate,
            time = receiptTime,
            subtotalMinor = finalAmount,
            discountMinor = null,
            taxMinor = null,
            totalMinor = finalAmount,
            paymentMethod = bankName ?: "Chuyển khoản",
            lineItems = lineItems,
            confidenceScore = 0.95f,
            rawText = rawText,
            isBankTransfer = true,
            bankName = bankName,
            recipientAccount = null,
            transactionNote = transferNote,
            transactionCode = txCode
        )
    }

    private fun parseCommercialReceipt(
        rawText: String,
        lines: List<String>,
        receiptDate: Long,
        receiptTime: String?
    ): ParsedReceiptData {
        // Merchant Name heuristics: top non-empty lines that don't look like dates or receipts
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

        // Line Items Extraction
        val lineItems = mutableListOf<ReceiptItemEntity>()
        val itemPattern = Pattern.compile("^(.*?)\\s+([0-9]+(?:[.,][0-9]+)?)\\s+([0-9]{1,3}(?:[.,][0-9]{3})+)$")

        for (line in lines) {
            val lower = line.lowercase()
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
            rawText = rawText,
            isBankTransfer = false
        )
    }
}
