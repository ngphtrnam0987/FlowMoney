package com.flowmoney.app.domain.parser

interface BankNotificationParser {
    val bankId: String
    val bankName: String
    val supportedPackages: List<String>

    fun canParse(packageName: String, title: String, text: String): Boolean

    fun parse(title: String, text: String, timestamp: Long): ParsedBankNotification?

    fun validate(parsed: ParsedBankNotification): Boolean {
        // Amount must be positive and non-zero
        if (parsed.amountMinor <= 0L) return false
        // Description or bank info must not be blank
        if (parsed.bankIdentifier.isBlank()) return false
        return true
    }

    /**
     * Helper to detect and reject any OTP or authentication code notifications
     */
    fun isOtpNotification(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("mã otp") ||
               lower.contains("ma otp") ||
               lower.contains("mã xác thực") ||
               lower.contains("ma xac thuc") ||
               lower.contains("mã xác minh") ||
               lower.contains("không chia sẻ") ||
               lower.contains("khong chia se") ||
               lower.contains("otp của bạn") ||
               lower.contains("smart otp")
    }
}
