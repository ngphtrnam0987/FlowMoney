package com.flowmoney.app.data.local

import androidx.room.TypeConverter
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.BudgetPeriod
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.MatchingStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource

class Converters {

    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { enumValueOf<AccountType>(it) }

    @TypeConverter
    fun fromTransactionDirection(value: TransactionDirection?): String? = value?.name

    @TypeConverter
    fun toTransactionDirection(value: String?): TransactionDirection? =
        value?.let { enumValueOf<TransactionDirection>(it) }

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toTransactionSource(value: String?): TransactionSource? =
        value?.let { enumValueOf<TransactionSource>(it) }

    @TypeConverter
    fun fromClassificationStatus(value: ClassificationStatus?): String? = value?.name

    @TypeConverter
    fun toClassificationStatus(value: String?): ClassificationStatus? =
        value?.let { enumValueOf<ClassificationStatus>(it) }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod?): String? = value?.name

    @TypeConverter
    fun toBudgetPeriod(value: String?): BudgetPeriod? =
        value?.let { enumValueOf<BudgetPeriod>(it) }

    @TypeConverter
    fun fromMatchingStatus(value: MatchingStatus?): String? = value?.name

    @TypeConverter
    fun toMatchingStatus(value: String?): MatchingStatus? =
        value?.let { enumValueOf<MatchingStatus>(it) }
}
