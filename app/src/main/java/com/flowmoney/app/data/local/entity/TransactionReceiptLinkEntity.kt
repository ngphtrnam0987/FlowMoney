package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowmoney.app.data.model.MatchingStatus

@Entity(
    tableName = "transaction_receipt_links",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["receiptId"])
    ]
)
data class TransactionReceiptLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val receiptId: Long,
    val matchConfidence: Float,
    val status: MatchingStatus = MatchingStatus.SUGGESTED,
    val linkedAt: Long = System.currentTimeMillis()
)
