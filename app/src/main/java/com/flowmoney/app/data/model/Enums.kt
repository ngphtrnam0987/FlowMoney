package com.flowmoney.app.data.model

enum class TransactionDirection {
    INCOME,
    EXPENSE,
    TRANSFER
}

enum class TransactionSource {
    MANUAL,
    BANK_NOTIFICATION,
    RECEIPT_SCAN
}

enum class ClassificationStatus {
    PENDING,
    SUGGESTED,
    CONFIRMED
}

enum class AccountType {
    BANK,
    WALLET,
    CASH
}

enum class BudgetPeriod {
    MONTHLY,
    WEEKLY
}

enum class MatchingStatus {
    UNMATCHED,
    SUGGESTED,
    CONFIRMED,
    REJECTED
}
