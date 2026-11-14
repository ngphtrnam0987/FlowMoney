package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flowmoney.app.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {

    @Query("SELECT * FROM category_rules ORDER BY priority DESC, id DESC")
    fun getAllRulesFlow(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM category_rules ORDER BY priority DESC, id DESC")
    suspend fun getAllRules(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules WHERE categoryId = :categoryId")
    suspend fun getRulesByCategoryId(categoryId: Long): List<CategoryRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRuleEntity): Long

    @Delete
    suspend fun deleteRule(rule: CategoryRuleEntity)
}
