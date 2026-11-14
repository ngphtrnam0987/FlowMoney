package com.flowmoney.app.domain.parser

import com.flowmoney.app.data.model.TransactionDirection

data class ParsedBankNotification(
    val bankIdentifier: String,
    val bankName: String,
    val direction: TransactionDirection,
    val amountMinor: Long,
    val currency: String = "VND",
    val timestamp: Long,
    val transactionDescription: String,
    val counterparty: String? = null,
    val maskedAccount: String? = null,
    val balanceAfterMinor: Long? = null,
    val externalReference: String? = null,
    val confidenceScore: Float = 0.9f,
    val rawNotification: String = ""
)
