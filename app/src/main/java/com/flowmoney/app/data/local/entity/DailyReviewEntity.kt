package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_reviews",
    indices = [Index(value = ["dateString"], unique = true)]
)
data class DailyReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String,
    val totalIncomeMinor: Long = 0L,
    val totalExpenseMinor: Long = 0L,
    val transactionCount: Int = 0,
    val pendingCategorizationCount: Int = 0,
    val unlinkedReceiptCount: Int = 0,
    val isCompleted: Boolean = false,
    val reviewedAt: Long? = null
)
