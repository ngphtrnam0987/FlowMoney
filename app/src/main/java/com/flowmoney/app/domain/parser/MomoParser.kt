package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.util.CurrencyUtils
import java.util.regex.Pattern

class MomoParser : BankNotificationParser {
    override val bankId: String = "MOMO"
    override val bankName: String = "Ví MoMo"
    override val supportedPackages: List<String> = listOf("com.mservice.momotransfer")

    override fun canParse(packageName: String, title: String, text: String): Boolean {
        if (isOtpNotification(text)) return false
        val isPackage = supportedPackages.contains(packageName)
        val hasKeyword = title.contains("MoMo", ignoreCase = true) ||
                         text.contains("MoMo", ignoreCase = true) ||
                         text.contains("Túi Thần Tài", ignoreCase = true)
        return isPackage || hasKeyword
    }

    override fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification? {
        val fullContent = "$title $text"
        if (isOtpNotification(fullContent)) return null

        // 1. Detect amount:
        // Supports: "Số tiền 3.000 đ", "3.000 ₫", "Nhận 50.000đ", "Thanh toán 120.000 đ", etc.
        val amountPattern = Pattern.compile(
            "(?:số tiền|so tien|nhận|nhan|thanh toán|thanh toan|chuyển|chuyen|nạp|nap|rút|rut)?\\s*:?\\s*([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)",
            Pattern.CASE_INSENSITIVE
        )
        val amountMatcher = amountPattern.matcher(fullContent)
        val rawAmount = if (amountMatcher.find()) {
            amountMatcher.group(1)?.trim()
        } else {
            // Fallback for standalone signed or unsigned amount with currency
            val fallbackPattern = Pattern.compile("([+-]?\\s*[0-9]{1,3}(?:[.,][0-9]{3})*)\\s*(?:VND|đ|₫|d)", Pattern.CASE_INSENSITIVE)
            val fbMatcher = fallbackPattern.matcher(fullContent)
            if (fbMatcher.find()) fbMatcher.group(1)?.trim() else null
        }
        val amountMinor = CurrencyUtils.parseAmount(rawAmount) ?: return null

        // 2. Detect direction
        val isExplicitIncome = rawAmount?.startsWith("+") == true ||
                               fullContent.contains("về Túi Thần Tài", ignoreCase = true) ||
                               fullContent.contains("ve tui than tai", ignoreCase = true) ||
                               fullContent.contains("nhận được", ignoreCase = true) ||
                               fullContent.contains("nhan duoc", ignoreCase = true) ||
                               fullContent.contains("bạn vừa nhận", ignoreCase = true) ||
                               fullContent.contains("hoàn tiền", ignoreCase = true) ||
                               fullContent.contains("nạp thành công", ignoreCase = true) ||
                               fullContent.contains("tăng", ignoreCase = true) ||
                               fullContent.contains("cộng", ignoreCase = true)

        val isExplicitExpense = rawAmount?.startsWith("-") == true ||
                                fullContent.contains("thanh toán thành công", ignoreCase = true) ||
                                fullContent.contains("thanh toan", ignoreCase = true) ||
                                fullContent.contains("chuyển thành công", ignoreCase = true) ||
                                fullContent.contains("đã chuyển", ignoreCase = true) ||
                                fullContent.contains("rút tiền", ignoreCase = true) ||
                                fullContent.contains("rút", ignoreCase = true)

        val direction = when {
            isExplicitIncome -> TransactionDirection.INCOME
            isExplicitExpense -> TransactionDirection.EXPENSE
            fullContent.contains("+") -> TransactionDirection.INCOME
            else -> TransactionDirection.EXPENSE
        }

        // 3. Extract Reference (e.g. FT26318343885839 or Mã GD: 123456)
        val refPattern = Pattern.compile("(?:FT[0-9]{8,20}|(?:mã gd|ma gd|so gd|số gd)\\s*:?\\s*([A-Za-z0-9]+))", Pattern.CASE_INSENSITIVE)
        val refMatcher = refPattern.matcher(fullContent)
        val externalRef = if (refMatcher.find()) {
            refMatcher.group(1) ?: refMatcher.group(0)
        } else null

        // 4. Extract Description / Note (e.g. Lời nhắn: "..." or merchant name)
        val notePattern = Pattern.compile("""(?:lời nhắn|loi nhan|nd|nội dung)\s*:?\s*["“]?([^"”\n\r]+)["”]?""", Pattern.CASE_INSENSITIVE)
        val noteMatcher = notePattern.matcher(fullContent)
        val description = if (noteMatcher.find()) {
            noteMatcher.group(1)?.trim() ?: text
        } else if (fullContent.contains("Túi Thần Tài", ignoreCase = true)) {
            "Túi Thần Tài"
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
            externalReference = externalRef,
            confidenceScore = 0.95f,
            rawNotification = text
        )

        return if (validate(parsed)) parsed else null
    }
}
