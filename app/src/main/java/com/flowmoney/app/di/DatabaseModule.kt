package com.flowmoney.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowmoney.app.data.local.FlowMoneyDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        categoryDaoProvider: Provider<CategoryDao>
    ): FlowMoneyDatabase {
        return Room.databaseBuilder(
            context,
            FlowMoneyDatabase::class.java,
            FlowMoneyDatabase.DATABASE_NAME
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val categoryDao = categoryDaoProvider.get()
                    categoryDao.insertCategories(FlowMoneyDatabase.DEFAULT_CATEGORIES)
                }
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideAccountDao(database: FlowMoneyDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: FlowMoneyDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideCategoryRuleDao(database: FlowMoneyDatabase): CategoryRuleDao = database.categoryRuleDao()

    @Provides
    fun provideTransactionDao(database: FlowMoneyDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideReceiptDao(database: FlowMoneyDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideTransactionReceiptLinkDao(database: FlowMoneyDatabase): TransactionReceiptLinkDao =
        database.transactionReceiptLinkDao()

    @Provides
    fun provideBudgetDao(database: FlowMoneyDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideDailyReviewDao(database: FlowMoneyDatabase): DailyReviewDao = database.dailyReviewDao()

    @Provides
    fun provideNotificationFingerprintDao(database: FlowMoneyDatabase): NotificationFingerprintDao =
        database.notificationFingerprintDao()

    @Provides
    fun provideAppSettingDao(database: FlowMoneyDatabase): AppSettingDao = database.appSettingDao()
}
