package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class TechcombankParser : BankNotificationParser {
    override val bankId: String = "TCB"
    override val bankName: String = "Techcombank"
    override val supportedPackages: List<String> = listOf("vn.com.techcombank.bb.app")

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val isPackage = supportedPackages.contains(packageName)
        val hasKeyword = title.contains("Techcombank", ignoreCase = true) ||
                         title.contains("TCB", ignoreCase = true) ||
                         text.contains("Techcombank", ignoreCase = true) ||
                         text.contains("TCB", ignoreCase = true)
        return isPackage || hasKeyword
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        // Amount: GD -250,000 VND or -250,000 VND
        val amountPattern = Pattern.compile("([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|d)", Pattern.CASE_INSENSITIVE)
        val amountMatcher = amountPattern.matcher(fullContent)
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

        // Balance
        val balancePattern = Pattern.compile("(?:so du|số dư|sd)(?:\\s+(?:kha dung|khả dụng|cuối|cuoi|hien tai|hiện tại))?\\s*:?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)", Pattern.CASE_INSENSITIVE)
        val balanceMatcher = balancePattern.matcher(fullContent)
        val balanceAfter = if (balanceMatcher.find()) CurrencyUtils.parseAmount(balanceMatcher.group(1)) else null

        // Merchant or counterparty: tai Highlands Coffee / tu NGUYEN VAN A
        val merchantPattern = Pattern.compile("(?:tại|tai|cho|đến|den|từ|tu)\\s+([A-Za-z0-9\\s]{3,30}?)(?:\\.|So du|Số dư|$)", Pattern.CASE_INSENSITIVE)
        val merchantMatcher = merchantPattern.matcher(fullContent)
        val merchant = if (merchantMatcher.find()) merchantMatcher.group(1)?.trim() else null

        // Description
        val descPattern = Pattern.compile("(?:nd|noi dung|nội dung)\\s*:?\\s*(.+?)(?:\\.|Ref|$)", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim() ?: text
        } else {
            merchant ?: text.take(80)
        }

        val parsed = ParsedBankNotification(
            bankIdentifier = bankId,
            bankName = bankName,
            direction = direction,
            amountMinor = amountMinor,
            timestamp = timestamp,
            transactionDescription = description,
            counterparty = merchant,
            balanceAfterMinor = balanceAfter,
            confidenceScore = 0.95f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
