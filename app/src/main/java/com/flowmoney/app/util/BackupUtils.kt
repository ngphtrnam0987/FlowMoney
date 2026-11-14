package com.flowmoney.app.util

import android.content.Context
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.BudgetEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter

data class FlowMoneyBackup(
    val exportDate: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val receipts: List<ReceiptEntity> = emptyList()
)

object BackupUtils {

    private val gson: Gson by lazy {
        GsonBuilder().setPrettyPrinting().create()
    }

    fun exportTransactionsToCsv(
        context: Context,
        transactions: List<TransactionEntity>,
        categoryMap: Map<Long, String>,
        accountMap: Map<Long, String>
    ): File {
        val file = File(context.cacheDir, "flowmoney_transactions_${System.currentTimeMillis()}.csv")
        FileWriter(file).use { writer ->
            writer.append("ID,Ngày,Thời gian,Tài khoản,Loại,Số tiền (VND),Danh mục,Nội dung,Cửa hàng,Mã tham chiếu\n")
            for (t in transactions) {
                val dateStr = DateUtils.formatDate(t.transactionTime)
                val timeStr = DateUtils.formatTime(t.transactionTime)
                val accName = accountMap[t.accountId] ?: "Tài khoản ${t.accountId}"
                val catName = categoryMap[t.categoryId] ?: "Khác"
                val dirStr = when (t.direction) {
                    com.flowmoney.app.data.model.TransactionDirection.INCOME -> "Thu nhập"
                    com.flowmoney.app.data.model.TransactionDirection.EXPENSE -> "Chi tiêu"
                    com.flowmoney.app.data.model.TransactionDirection.TRANSFER -> "Chuyển khoản"
                }
                val descEscaped = "\"${t.description.replace("\"", "\"\"")}\""
                val merchantEscaped = "\"${(t.merchantName ?: "").replace("\"", "\"\"")}\""
                writer.append("${t.id},$dateStr,$timeStr,\"$accName\",$dirStr,${t.amountMinor},\"$catName\",$descEscaped,$merchantEscaped,${t.externalReference ?: ""}\n")
            }
        }
        return file
    }

    fun createJsonBackup(
        context: Context,
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        receipts: List<ReceiptEntity>
    ): File {
        val backup = FlowMoneyBackup(
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            receipts = receipts
        )
        val json = gson.toJson(backup)
        val file = File(context.cacheDir, "flowmoney_backup_${System.currentTimeMillis()}.json")
        file.writeText(json)
        return file
    }

    fun parseJsonBackup(jsonString: String): FlowMoneyBackup? {
        return try {
            gson.fromJson(jsonString, FlowMoneyBackup::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
