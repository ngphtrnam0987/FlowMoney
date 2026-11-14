package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class GenericBankParser(
    override val bankId: String = "GENERIC",
    override val bankName: String = "Ngân hàng",
    override val supportedPackages: List<String> = emptyList()
) : BankNotificationParser {

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val fullContent = "$title $text"
        val hasMoney = fullContent.contains("VND", ignoreCase = true) ||
                       fullContent.contains("đ", ignoreCase = true) ||
                       fullContent.contains("dong", ignoreCase = true)
        val hasTransactionSignal = fullContent.contains("GD", ignoreCase = true) ||
                                  fullContent.contains("biến động", ignoreCase = true) ||
                                  fullContent.contains("bien dong", ignoreCase = true) ||
                                  fullContent.contains("số dư", ignoreCase = true) ||
                                  fullContent.contains("so du", ignoreCase = true) ||
                                  fullContent.contains("thanh toan", ignoreCase = true) ||
                                  fullContent.contains("thanh toán", ignoreCase = true) ||
                                  fullContent.contains("chuyen khoan", ignoreCase = true) ||
                                  fullContent.contains("chuyển khoản", ignoreCase = true) ||
                                  fullContent.contains("nhan tien", ignoreCase = true) ||
                                  fullContent.contains("nhận tiền", ignoreCase = true)
        return hasMoney && hasTransactionSignal
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        val isIncome = fullContent.contains("+") ||
                       fullContent.contains("nhan tien", ignoreCase = true) ||
                       fullContent.contains("nhận tiền", ignoreCase = true) ||
                       fullContent.contains("cong tien", ignoreCase = true) ||
                       fullContent.contains("cộng", ignoreCase = true)
        val direction = if (isIncome) TransactionDirection.INCOME else TransactionDirection.EXPENSE

        // Extract amount: e.g. -120,000 VND, 120.000đ, etc.
        val amountPattern = Pattern.compile("([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|d)", Pattern.CASE_INSENSITIVE)
        val amountMatcher = amountPattern.matcher(fullContent)
        val rawAmount = if (amountMatcher.find()) amountMatcher.group(1) else null
        val amountMinor = CurrencyUtils.parseAmount(rawAmount) ?: return null

        // Extract balance
        val balancePattern = Pattern.compile("(?:so du|số dư|sd)\\s*:?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balancePattern.matcher(fullContent)
        val balanceAfter = if (balanceMatcher.find()) CurrencyUtils.parseAmount(balanceMatcher.group(1)) else null

        // Description
        val descPattern = Pattern.compile("(?:nd|noi dung|nội dung)\\s*:?\\s*(.+?)(?:\\.|Ref|So du|$)", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim() ?: text
        } else {
            text.take(80)
        }

        // Detect Bank Name from Title
        val detectedBankName = when {
            fullContent.contains("ACB", ignoreCase = true) -> "ACB"
            fullContent.contains("TPBank", ignoreCase = true) -> "TPBank"
            fullContent.contains("VPBank", ignoreCase = true) -> "VPBank"
            fullContent.contains("VietinBank", ignoreCase = true) -> "VietinBank"
            fullContent.contains("Sacombank", ignoreCase = true) -> "Sacombank"
            fullContent.contains("VIB", ignoreCase = true) -> "VIB"
            title.isNotBlank() -> title.take(20)
            else -> bankName
        }

        val parsed = ParsedBankNotification(
            bankIdentifier = detectedBankName.uppercase().replace(" ", "_"),
            bankName = detectedBankName,
            direction = direction,
            amountMinor = amountMinor,
            timestamp = timestamp,
            transactionDescription = description,
            balanceAfterMinor = balanceAfter,
            confidenceScore = 0.85f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
