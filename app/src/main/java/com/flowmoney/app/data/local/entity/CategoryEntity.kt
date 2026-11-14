package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["parentId"])]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val isIncome: Boolean = false,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
