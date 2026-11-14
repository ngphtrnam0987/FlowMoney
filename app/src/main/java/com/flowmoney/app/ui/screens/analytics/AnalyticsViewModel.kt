package com.flowmoney.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.local.dao.CategoryExpenseTotal
import com.flowmoney.app.data.local.dao.ItemCategoryExpense
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.ReceiptRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.ui.components.BarData
import com.flowmoney.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class MerchantTotal(
    val merchantName: String,
    val totalExpenseMinor: Long,
    val transactionCount: Int
)

data class AnalyticsUiState(
    val totalIncomeMinor: Long = 0L,
    val totalExpenseMinor: Long = 0L,
    val netSavingsMinor: Long = 0L,
    val savingsRatePercentage: Float = 0f,
    val categoryExpenses: List<CategoryExpenseTotal> = emptyList(),
    val itemExpenses: List<ItemCategoryExpense> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val topMerchants: List<MerchantTotal> = emptyList(),
    val barChartData: List<BarData> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        categoryRepository.getAllCategoriesFlow(),
        _refreshTrigger
    ) { categories, _ ->
        val catMap = categories.associateBy { it.id }

        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        val transactions = transactionRepository.getTransactionsBetween(startOfMonth, endOfMonth)

        val totalIncome = transactions.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amountMinor }
        val totalExpense = transactions.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amountMinor }
        val netSavings = (totalIncome - totalExpense).coerceAtLeast(0L)
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense).toFloat() / totalIncome * 100f).coerceIn(0f, 100f) else 0f

        // Group by category
        val categoryExpenses = transactions
            .filter { it.direction == TransactionDirection.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txs) -> CategoryExpenseTotal(catId, txs.sumOf { it.amountMinor }) }
            .sortedByDescending { it.totalAmount }

        // Top Merchants
        val topMerchants = transactions
            .filter { it.direction == TransactionDirection.EXPENSE && !it.merchantName.isNullOrBlank() }
            .groupBy { it.merchantName!!.trim() }
            .map { (merchant, txs) ->
                MerchantTotal(
                    merchantName = merchant,
                    totalExpenseMinor = txs.sumOf { it.amountMinor },
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalExpenseMinor }
            .take(5)

        // Generate 7-day bar chart data
        val barData = mutableListOf<BarData>()
        val calendar = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startDay = DateUtils.getStartOfDay(dayCal.timeInMillis)
            val endDay = DateUtils.getEndOfDay(dayCal.timeInMillis)
            val dayTxs = transactions.filter { it.transactionTime in startDay..endDay }

            val dayIncome = dayTxs.filter { it.direction == TransactionDirection.INCOME }.sumOf { it.amountMinor }
            val dayExpense = dayTxs.filter { it.direction == TransactionDirection.EXPENSE }.sumOf { it.amountMinor }

            val label = when (i) {
                0 -> "Hôm nay"
                1 -> "Hôm qua"
                else -> "${dayCal.get(Calendar.DAY_OF_MONTH)}/${dayCal.get(Calendar.MONTH) + 1}"
            }
            barData.add(BarData(label, dayIncome, dayExpense))
        }

        AnalyticsUiState(
            totalIncomeMinor = totalIncome,
            totalExpenseMinor = totalExpense,
            netSavingsMinor = netSavings,
            savingsRatePercentage = savingsRate,
            categoryExpenses = categoryExpenses,
            categories = catMap,
            topMerchants = topMerchants,
            barChartData = barData,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsUiState(isLoading = true)
    )
}
