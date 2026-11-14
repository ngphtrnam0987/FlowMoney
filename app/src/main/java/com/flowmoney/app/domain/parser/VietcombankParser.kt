package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class VietcombankParser : BankNotificationParser {
    override val bankId: String = "VCB"
    override val bankName: String = "Vietcombank"
    override val supportedPackages: List<String> = listOf("com.VCB", "com.vcb.digibank")

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val isPackage = supportedPackages.contains(packageName)
        val hasKeyword = title.contains("Vietcombank", ignoreCase = true) ||
                         title.contains("VCB", ignoreCase = true) ||
                         text.contains("VCB", ignoreCase = true) ||
                         text.contains("Vietcombank", ignoreCase = true)
        return isPackage || hasKeyword
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        // 1. Extract balance first so the amount matcher never confuses balance for transaction amount
        val balancePattern = Pattern.compile("(?:so du|sodu|số dư|sd)(?:\\s+(?:kha dung|khả dụng|cuối|cuoi|hien tai|hiện tại))?\\s*:?\\s*(?:vnd|đ|₫)?\\s*([0-9]{1,3}(?:[.,][0-9]{3})*)", Pattern.CASE_INSENSITIVE)
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

        // 3. Detect direction
        val direction = when {
            rawAmount?.startsWith("+") == true -> TransactionDirection.INCOME
            rawAmount?.startsWith("-") == true -> TransactionDirection.EXPENSE
            fullContent.contains("+") ||
            fullContent.contains("nhan tien", ignoreCase = true) ||
            fullContent.contains("nhận tiền", ignoreCase = true) ||
            fullContent.contains("tang", ignoreCase = true) ||
            fullContent.contains("tăng", ignoreCase = true) ||
            fullContent.contains("cộng", ignoreCase = true) -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }

        // Extract reference number: Ref MBVCB.12345 or Ref: 12345
        val refPattern = Pattern.compile("(?:ref|ma gd|mã gd|so gd|số gd)\\s*:?\\s*([A-Za-z0-9.]+)", Pattern.CASE_INSENSITIVE)
        val refMatcher = refPattern.matcher(fullContent)
        val externalRef = if (refMatcher.find()) refMatcher.group(1)?.trimEnd('.') else null

        // Extract description: ND: Thanh toan...
        val descPattern = Pattern.compile("(?:nd|nội dung|noi dung)\\s*:?\\s*(.+)$", Pattern.CASE_INSENSITIVE)
        val descMatcher = descPattern.matcher(fullContent)
        val description = if (descMatcher.find()) {
            descMatcher.group(1)?.trim()?.trimEnd('.') ?: text
        } else {
            text.take(80)
        }

        // Extract account: e.g. VCB: 001100... or TK 0011...
        val accountPattern = Pattern.compile("(?:tk|tai khoan|tài khoản|vcb)\\s*:?\\s*([0-9xX*]+)", Pattern.CASE_INSENSITIVE)
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
            externalReference = externalRef,
            confidenceScore = 0.95f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
