package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["receiptId"])]
)
data class ReceiptItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val quantity: Double = 1.0,
    val unitPriceMinor: Long? = null,
    val totalPriceMinor: Long = 0L,
    val categoryId: Long? = null,
    val confidenceScore: Float = 1.0f
)
