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

        // 1. Extract balance first so the amount matcher never confuses balance for transaction amount
        val balancePattern = Pattern.compile(
            "(?:so du|số dư|sd)(?:\\s+(?:kha dung|khả dụng|cuối|cuoi|hien tai|hiện tại))?\\s*:?\\s*(?:vnd|đ|₫)?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)",
            Pattern.CASE_INSENSITIVE
        )
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

        // 2. Extract transaction amount from content without balance
        val amountPattern = Pattern.compile(
            "([+-]\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)?|(?:gd|giao dịch|số tiền|biến động)\\s*:?\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)?|([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)|(?:VND|đ|₫)\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)",
            Pattern.CASE_INSENSITIVE
        )
        val amountMatcher = amountPattern.matcher(contentWithoutBalance)
        val rawAmount = if (amountMatcher.find()) {
            amountMatcher.group(1) ?: amountMatcher.group(2) ?: amountMatcher.group(3) ?: amountMatcher.group(4)
        } else null
        val amountMinor = CurrencyUtils.parseAmount(rawAmount?.trim()) ?: return null

        // 3. Direction
        val direction = when {
            rawAmount?.trim()?.startsWith("+") == true -> TransactionDirection.INCOME
            rawAmount?.trim()?.startsWith("-") == true -> TransactionDirection.EXPENSE
            fullContent.contains("+") ||
            fullContent.contains("nhan tien", ignoreCase = true) ||
            fullContent.contains("nhận tiền", ignoreCase = true) ||
            fullContent.contains("tăng", ignoreCase = true) ||
            fullContent.contains("cộng", ignoreCase = true) -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }

        // 4. Extract Account
        val accountPattern = Pattern.compile("(?:tài khoản|tai khoan|tk)\\s*:?\\s*([0-9xX*]+)", Pattern.CASE_INSENSITIVE)
        val accountMatcher = accountPattern.matcher(fullContent)
        val maskedAccount = if (accountMatcher.find()) accountMatcher.group(1) else null

        // 5. Merchant or counterparty: tai Highlands Coffee / tu NGUYEN VAN A / qua MoMo
        val merchantPattern = Pattern.compile("(?:tại|tai|cho|đến|den|từ|tu|qua)\\s+([A-Za-z0-9\\s]{3,30}?)(?:\\.|So du|Số dư|$)", Pattern.CASE_INSENSITIVE)
        val merchantMatcher = merchantPattern.matcher(fullContent)
        val merchant = if (merchantMatcher.find()) merchantMatcher.group(1)?.trim() else null

        // 6. Description
        val descPattern = Pattern.compile("(?:nd|noi dung|nội dung)\\s*:?\\s*(.+?)(?:\\.|Ref|$)", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim() ?: text
        } else {
            // Check for format: 156895296834-0926906226-NGUYEN PHAN TRONG NAM chuyen tien qua MoMo
            val customLine = text.lines().firstOrNull { it.contains("chuyen tien", ignoreCase = true) || it.contains("thanh toan", ignoreCase = true) }
            customLine?.trim() ?: (merchant ?: text.take(80))
        }

        val parsed = ParsedBankNotification(
            bankIdentifier = bankId,
            bankName = bankName,
            direction = direction,
            amountMinor = amountMinor,
            timestamp = timestamp,
            transactionDescription = description,
            maskedAccount = maskedAccount,
            counterparty = merchant,
            balanceAfterMinor = balanceAfter,
            confidenceScore = 0.95f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
