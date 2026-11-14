package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class MbBankParser : BankNotificationParser {
    override val bankId: String = "MB"
    override val bankName: String = "MB Bank"
    override val supportedPackages: List<String> = listOf("com.mbmobile")

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val isPackage = supportedPackages.contains(packageName)
        val hasKeyword = title.contains("MB Bank", ignoreCase = true) ||
                         title.contains("MBBank", ignoreCase = true) ||
                         text.contains("MB Bank", ignoreCase = true) ||
                         text.contains("MBBank", ignoreCase = true)
        return isPackage || hasKeyword
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        // 1. Extract balance first so the amount matcher never confuses balance for transaction amount
        val balancePattern = Pattern.compile("(?:SD|So du|Số dư)(?:\\s+(?:kha dung|khả dụng|cuối|cuoi|hien tai|hiện tại))?\\s*:?\\s*(?:vnd|đ|₫)?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)", Pattern.CASE_INSENSITIVE)
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

        // 2. Extract amount: GD: -50,000VND or GD: +2,000,000VND
        val amountPattern = Pattern.compile("(?:GD|Giao dich|giao dịch)\\s*:?\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)|([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)", Pattern.CASE_INSENSITIVE)
        val amountMatcher = amountPattern.matcher(contentWithoutBalance)
        val rawAmount = if (amountMatcher.find()) {
            (amountMatcher.group(1) ?: amountMatcher.group(2))?.trim()
        } else null
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

        // ND: ...
        val descPattern = Pattern.compile("(?:ND|Noi dung|Nội dung)\\s*:?\\s*(.+)$", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim()?.trimEnd('.') ?: text
        } else {
            text.take(80)
        }

        // TK 0987...
        val accountPattern = Pattern.compile("(?:TK|tai khoan|tài khoản)\\s*:?\\s*([0-9xX*]+)", Pattern.CASE_INSENSITIVE)
        val accountMatcher = accountPattern.matcher(fullContent)
        val maskedAccount = if (accountMatcher.find()) accountMatcher.group(1) else null

        val parsed = ParsedBankNotification(
            bankIdentifier = bankId,
            bankName = bankName,
            direction = direction,
            amountMinor = amountMinor,
            timestamp = timestamp,
            transactionDescription = description,
            maskedAccount = maskedAccount,
            balanceAfterMinor = balanceAfter,
            confidenceScore = 0.95f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
