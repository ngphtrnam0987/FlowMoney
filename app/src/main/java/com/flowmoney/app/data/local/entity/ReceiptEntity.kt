package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String? = null,
    val merchantName: String? = null,
    val merchantAddress: String? = null,
    val receiptDate: Long? = null,
    val receiptTime: String? = null,
    val receiptNumber: String? = null,
    val subtotalMinor: Long? = null,
    val discountMinor: Long? = null,
    val taxMinor: Long? = null,
    val serviceFeeMinor: Long? = null,
    val totalMinor: Long = 0L,
    val paymentMethod: String? = null,
    val rawOcrText: String? = null,
    val confidenceScore: Float = 1.0f,
    val status: String = "CONFIRMED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
