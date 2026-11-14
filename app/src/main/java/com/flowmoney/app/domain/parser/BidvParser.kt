package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class BidvParser : BankNotificationParser {
    override val bankId: String = "BIDV"
    override val bankName: String = "BIDV"
    override val supportedPackages: List<String> = listOf("com.vnpay.bidv", "com.bidv.smartbanking")

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val isPackage = supportedPackages.contains(packageName)
        val hasKeyword = title.contains("BIDV", ignoreCase = true) ||
                         text.contains("BIDV", ignoreCase = true)
        return isPackage || hasKeyword
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        // 1. Extract balance first
        val balancePattern = Pattern.compile("(?:so du|số dư|sd)(?:\\s+(?:kha dung|khả dụng|cuối|cuoi|hien tai|hiện tại))?\\s*:?\\s*(?:vnd|đ|₫)?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balancePattern.matcher(fullContent)
        var balanceAfter: Long? = null
        var contentWithoutBalance = fullContent
        if (balanceMatcher.find()) {
            balanceAfter = CurrencyUtils.parseAmount(balanceMatcher.group(1))
            val matched = balanceMatcher.group(0)
            if (matched != null) {
                contentWithoutBalance = fullContent.replace(matched, " ")
            }
        }

        // 2. Extract transaction amount
        val amountPattern = Pattern.compile("([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)", Pattern.CASE_INSENSITIVE)
        val amountMatcher = amountPattern.matcher(contentWithoutBalance)
        val rawAmount = if (amountMatcher.find()) amountMatcher.group(1)?.trim() else null
        val amountMinor = CurrencyUtils.parseAmount(rawAmount) ?: return null

        val direction = when {
            rawAmount?.startsWith("+") == true -> TransactionDirection.INCOME
            rawAmount?.startsWith("-") == true -> TransactionDirection.EXPENSE
            fullContent.contains("+") ||
            fullContent.contains("nhan tien", ignoreCase = true) ||
            fullContent.contains("nhận tiền", ignoreCase = true) ||
            fullContent.contains("tăng", ignoreCase = true) ||
            fullContent.contains("cộng", ignoreCase = true) -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }

        val descPattern = Pattern.compile("(?:nd|noi dung|nội dung)\\s*:?\\s*(.+?)(?:\\.|Ref|$)", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim() ?: text
        } else {
            text.take(80)
        }

        val parsed = ParsedBankNotification(
            bankIdentifier = bankId,
            bankName = bankName,
            direction = direction,
            amountMinor = amountMinor,
            timestamp = timestamp,
            transactionDescription = description,
            balanceAfterMinor = balanceAfter,
            confidenceScore = 0.93f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
