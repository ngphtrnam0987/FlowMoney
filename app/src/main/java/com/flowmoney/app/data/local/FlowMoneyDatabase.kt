package com.flowmoney.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flowmoney.app.data.local.dao.AccountDao
import com.flowmoney.app.data.local.dao.AppSettingDao
import com.flowmoney.app.data.local.dao.BudgetDao
import com.flowmoney.app.data.local.dao.CategoryDao
import com.flowmoney.app.data.local.dao.CategoryRuleDao
import com.flowmoney.app.data.local.dao.DailyReviewDao
import com.flowmoney.app.data.local.dao.NotificationFingerprintDao
import com.flowmoney.app.data.local.dao.ReceiptDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.dao.TransactionReceiptLinkDao
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.AppSettingEntity
import com.flowmoney.app.data.local.entity.BudgetEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.CategoryRuleEntity
import com.flowmoney.app.data.local.entity.DailyReviewEntity
import com.flowmoney.app.data.local.entity.NotificationFingerprintEntity
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.ReceiptItemEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.local.entity.TransactionReceiptLinkEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        CategoryRuleEntity::class,
        TransactionEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
        TransactionReceiptLinkEntity::class,
        BudgetEntity::class,
        DailyReviewEntity::class,
        NotificationFingerprintEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FlowMoneyDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun transactionDao(): TransactionDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun transactionReceiptLinkDao(): TransactionReceiptLinkDao
    abstract fun budgetDao(): BudgetDao
    abstract fun dailyReviewDao(): DailyReviewDao
    abstract fun notificationFingerprintDao(): NotificationFingerprintDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val DATABASE_NAME = "flowmoney.db"

        val DEFAULT_CATEGORIES = listOf(
            // Expense Categories
            CategoryEntity(id = 1, name = "Ăn uống", icon = "restaurant", colorHex = "#F59E0B", isIncome = false, sortOrder = 1, isDefault = true),
            CategoryEntity(id = 2, name = "Mua sắm", icon = "shopping_bag", colorHex = "#EC4899", isIncome = false, sortOrder = 2, isDefault = true),
            CategoryEntity(id = 3, name = "Di chuyển", icon = "directions_car", colorHex = "#3B82F6", isIncome = false, sortOrder = 3, isDefault = true),
            CategoryEntity(id = 4, name = "Giải trí", icon = "movie", colorHex = "#8B5CF6", isIncome = false, sortOrder = 4, isDefault = true),
            CategoryEntity(id = 5, name = "Nhà ở", icon = "home", colorHex = "#10B981", isIncome = false, sortOrder = 5, isDefault = true),
            CategoryEntity(id = 6, name = "Hóa đơn", icon = "receipt_long", colorHex = "#EF4444", isIncome = false, sortOrder = 6, isDefault = true),
            CategoryEntity(id = 7, name = "Sức khỏe", icon = "medical_services", colorHex = "#06B6D4", isIncome = false, sortOrder = 7, isDefault = true),
            CategoryEntity(id = 8, name = "Giáo dục", icon = "school", colorHex = "#6366F1", isIncome = false, sortOrder = 8, isDefault = true),
            CategoryEntity(id = 9, name = "Công việc", icon = "work", colorHex = "#64748B", isIncome = false, sortOrder = 9, isDefault = true),
            CategoryEntity(id = 10, name = "Khác", icon = "more_horiz", colorHex = "#94A3B8", isIncome = false, sortOrder = 10, isDefault = true),

            // Income Categories
            CategoryEntity(id = 11, name = "Tiền lương", icon = "payments", colorHex = "#22C55E", isIncome = true, sortOrder = 11, isDefault = true),
            CategoryEntity(id = 12, name = "Thưởng", icon = "redeem", colorHex = "#10B981", isIncome = true, sortOrder = 12, isDefault = true),
            CategoryEntity(id = 13, name = "Thu nhập phụ", icon = "trending_up", colorHex = "#34D399", isIncome = true, sortOrder = 13, isDefault = true),
            CategoryEntity(id = 14, name = "Đầu tư", icon = "savings", colorHex = "#059669", isIncome = true, sortOrder = 14, isDefault = true),

            // Internal Transfer
            CategoryEntity(id = 15, name = "Chuyển tiền nội bộ", icon = "swap_horiz", colorHex = "#A78BFA", isIncome = false, sortOrder = 15, isDefault = true)
        )
    }
}
