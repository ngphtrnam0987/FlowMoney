package com.flowmoney.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flowmoney.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE isActive = 1")
    fun getActiveBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND isActive = 1 LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND isActive = 1 LIMIT 1")
    suspend fun getOverallBudget(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}
