package com.flowmoney.app

import com.flowmoney.app.data.local.dao.CategoryDao
import com.flowmoney.app.data.local.dao.CategoryRuleDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.CategoryRuleEntity
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.engine.CategoryClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryClassificationTest {

    private class FakeCategoryDao : CategoryDao {
        val categories = mutableListOf(
            CategoryEntity(id = 1, name = "Ăn uống", icon = "restaurant", colorHex = "#F59E0B"),
            CategoryEntity(id = 2, name = "Mua sắm", icon = "shopping_bag", colorHex = "#EC4899"),
            CategoryEntity(id = 3, name = "Di chuyển", icon = "directions_car", colorHex = "#3B82F6"),
            CategoryEntity(id = 4, name = "Giải trí", icon = "movie", colorHex = "#8B5CF6"),
            CategoryEntity(id = 6, name = "Hóa đơn", icon = "receipt_long", colorHex = "#EF4444"),
            CategoryEntity(id = 10, name = "Khác", icon = "more_horiz", colorHex = "#94A3B8"),
            CategoryEntity(id = 11, name = "Tiền lương", icon = "payments", colorHex = "#22C55E", isIncome = true)
        )

        override fun getAllCategoriesFlow() = throw NotImplementedError()
        override suspend fun getAllCategories(): List<CategoryEntity> = categories
        override fun getCategoriesByTypeFlow(isIncome: Boolean) = throw NotImplementedError()
        override suspend fun getParentCategories() = categories
        override suspend fun getSubcategories(parentId: Long) = emptyList<CategoryEntity>()
        override suspend fun getCategoryById(id: Long) = categories.firstOrNull { it.id == id }
        override suspend fun findByName(name: String) = categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
        override suspend fun insertCategory(category: CategoryEntity) = 1L
        override suspend fun insertCategories(categories: List<CategoryEntity>) {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun count() = categories.size
    }

    private class FakeCategoryRuleDao : CategoryRuleDao {
        val rules = mutableListOf<CategoryRuleEntity>()
        override fun getAllRulesFlow() = throw NotImplementedError()
        override suspend fun getAllRules(): List<CategoryRuleEntity> = rules
        override suspend fun getRulesByCategoryId(categoryId: Long) = rules.filter { it.categoryId == categoryId }
        override suspend fun insertRule(rule: CategoryRuleEntity): Long {
            rules.add(rule)
            return rules.size.toLong()
        }
        override suspend fun deleteRule(rule: CategoryRuleEntity) {}
    }

    private class StubTransactionDao : TransactionDao {
        override fun getAllTransactionsFlow() = throw NotImplementedError()
        override fun getRecentTransactionsFlow(limit: Int) = throw NotImplementedError()
        override fun getTransactionsByStatusFlow(status: com.flowmoney.app.data.model.ClassificationStatus) = throw NotImplementedError()
        override suspend fun getTransactionById(id: Long) = null
        override fun getTransactionsBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun getTransactionsBetween(startTime: Long, endTime: Long) = emptyList<com.flowmoney.app.data.local.entity.TransactionEntity>()
        override fun getFinancialSummaryBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override fun getCategoryExpensesBetweenFlow(startTime: Long, endTime: Long) = throw NotImplementedError()
        override suspend fun findSimilarTransactions(amountMinor: Long, direction: TransactionDirection, minTime: Long, maxTime: Long) = emptyList<com.flowmoney.app.data.local.entity.TransactionEntity>()
        override suspend fun findByExternalReference(externalReference: String) = null
        override suspend fun insertTransaction(transaction: com.flowmoney.app.data.local.entity.TransactionEntity) = 1L
        override suspend fun updateTransaction(transaction: com.flowmoney.app.data.local.entity.TransactionEntity) {}
        override suspend fun updateClassification(id: Long, categoryId: Long, status: com.flowmoney.app.data.model.ClassificationStatus, updatedAt: Long) {}
        override suspend fun linkReceipt(transactionId: Long, receiptId: Long, updatedAt: Long) {}
        override suspend fun softDeleteTransaction(id: Long, updatedAt: Long) {}
        override suspend fun deleteTransaction(transaction: com.flowmoney.app.data.local.entity.TransactionEntity) {}
        override fun getPendingClassificationCountFlow() = throw NotImplementedError()
    }

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var categoryRuleDao: FakeCategoryRuleDao
    private lateinit var classifier: CategoryClassifier

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        categoryRuleDao = FakeCategoryRuleDao()
        classifier = CategoryClassifier(categoryDao, categoryRuleDao, StubTransactionDao())
    }

    @Test
    fun classifyLevel1UserDefinedRule() = runBlocking {
        categoryRuleDao.rules.add(
            CategoryRuleEntity(
                id = 1L,
                categoryId = 4L, // Giải trí
                keyword = "netflix",
                priority = 10
            )
        )

        val result = classifier.classify(
            description = "Thanh toan NETFLIX monthly",
            merchantName = "NETFLIX",
            amountMinor = 260000L,
            direction = TransactionDirection.EXPENSE
        )

        assertEquals(1, result.level)
        assertEquals(4L, result.categoryId)
        assertEquals("Giải trí", result.categoryName)
    }

    @Test
    fun classifyLevel2MerchantMapping() = runBlocking {
        // Shopee mapped to Mua sắm
        val result = classifier.classify(
            description = "Thanh toan don hang ShopeePay",
            merchantName = "Shopee",
            amountMinor = 150000L,
            direction = TransactionDirection.EXPENSE
        )

        assertEquals(2, result.level)
        assertEquals(2L, result.categoryId)
        assertEquals("Mua sắm", result.categoryName)
    }

    @Test
    fun classifyLevel3KeywordMatching() = runBlocking {
        // "xang" mapped to Di chuyển
        val result = classifier.classify(
            description = "Do xang xe may",
            merchantName = null,
            amountMinor = 70000L,
            direction = TransactionDirection.EXPENSE
        )

        assertEquals(3, result.level)
        assertEquals(3L, result.categoryId)
        assertEquals("Di chuyển", result.categoryName)
    }

    @Test
    fun classifyIncomeAsSalary() = runBlocking {
        val result = classifier.classify(
            description = "Nhan tien chuyen khoan",
            merchantName = null,
            amountMinor = 15000000L,
            direction = TransactionDirection.INCOME
        )

        assertEquals(11L, result.categoryId)
        assertEquals("Tiền lương", result.categoryName)
    }
}
