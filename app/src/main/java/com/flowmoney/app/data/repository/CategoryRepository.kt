package com.flowmoney.app.data.repository

import com.flowmoney.app.data.local.FlowMoneyDatabase
import com.flowmoney.app.data.local.dao.CategoryDao
import com.flowmoney.app.data.local.dao.CategoryRuleDao
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao
) {
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow().map { list ->
        if (list.isEmpty()) {
            categoryDao.insertCategories(FlowMoneyDatabase.DEFAULT_CATEGORIES)
            FlowMoneyDatabase.DEFAULT_CATEGORIES
        } else {
            list
        }
    }

    suspend fun ensureDefaultCategories() {
        val existing = categoryDao.getAllCategories()
        if (existing.isEmpty()) {
            categoryDao.insertCategories(FlowMoneyDatabase.DEFAULT_CATEGORIES)
        }
    }

    suspend fun getAllCategories(): List<CategoryEntity> {
        val existing = categoryDao.getAllCategories()
        return if (existing.isEmpty()) {
            categoryDao.insertCategories(FlowMoneyDatabase.DEFAULT_CATEGORIES)
            FlowMoneyDatabase.DEFAULT_CATEGORIES
        } else {
            existing
        }
    }

    fun getExpenseCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByTypeFlow(false)

    fun getIncomeCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getCategoriesByTypeFlow(true)

    suspend fun getCategoryById(id: Long): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun findByName(name: String): CategoryEntity? = categoryDao.findByName(name)

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Category Rules
    fun getAllRulesFlow(): Flow<List<CategoryRuleEntity>> = categoryRuleDao.getAllRulesFlow()

    suspend fun getAllRules(): List<CategoryRuleEntity> = categoryRuleDao.getAllRules()

    suspend fun addRule(rule: CategoryRuleEntity): Long = categoryRuleDao.insertRule(rule)

    suspend fun deleteRule(rule: CategoryRuleEntity) = categoryRuleDao.deleteRule(rule)
}
