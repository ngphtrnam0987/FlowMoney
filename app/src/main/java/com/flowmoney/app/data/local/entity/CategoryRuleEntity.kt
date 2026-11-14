package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val keyword: String,
    val merchantNamePattern: String? = null,
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val isRegex: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
