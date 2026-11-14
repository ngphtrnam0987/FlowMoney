package com.flowmoney.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flowmoney.app.data.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balanceMinor: Long = 0L,
    val openingBalanceMinor: Long = 0L,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val isDefault: Boolean = false,
    val iconName: String = "wallet",
    val colorHex: String = "#8B5CF6",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
