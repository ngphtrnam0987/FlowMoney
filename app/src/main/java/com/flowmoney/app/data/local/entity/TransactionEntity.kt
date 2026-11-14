package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["transactionTime"]),
        Index(value = ["externalReference"]),
        Index(value = ["classificationStatus"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val source: TransactionSource,
    val externalReference: String? = null,
    val amountMinor: Long,
    val currency: String = "VND",
    val direction: TransactionDirection,
    val transactionTime: Long,
    val merchantName: String? = null,
    val description: String,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val classificationStatus: ClassificationStatus = ClassificationStatus.CONFIRMED,
    val confidenceScore: Float = 1.0f,
    val linkedReceiptId: Long? = null,
    val counterparty: String? = null,
    val balanceAfterMinor: Long? = null,
    val transferAccountId: Long? = null,
    val isExcludedFromBudget: Boolean = false,
    val isTestTransaction: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
