package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowmoney.app.data.model.TransactionDirection

@Entity(
    tableName = "notification_fingerprints",
    indices = [
        Index(value = ["fingerprintHash"], unique = true),
        Index(value = ["receivedAt"])
    ]
)
data class NotificationFingerprintEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fingerprintHash: String,
    val bankIdentifier: String,
    val amountMinor: Long,
    val direction: TransactionDirection,
    val receivedAt: Long,
    val transactionId: Long? = null
)
