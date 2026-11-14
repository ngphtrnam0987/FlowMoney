package com.flowmoney.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.local.dao.CategoryExpenseTotal
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.BudgetRepository
import com.flowmoney.app.data.repository.BudgetStatus
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalEstimatedBalanceMinor: Long = 0L,
    val hasAccounts: Boolean = false,
    val monthlyIncomeMinor: Long = 0L,
    val monthlyExpenseMinor: Long = 0L,
    val savingsMinor: Long = 0L,
    val savingsRatePercentage: Float = 0f,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val pendingCount: Int = 0,
    val categoryExpenses: List<CategoryExpenseTotal> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val budgetStatus: BudgetStatus? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val dailyReviewRepository: DailyReviewRepository
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        accountRepository.getAllAccountsFlow(),
        transactionRepository.getRecentTransactionsFlow(10),
        transactionRepository.getPendingCountFlow(),
        categoryRepository.getAllCategoriesFlow(),
        transactionRepository.getTransactionsBetweenFlow(
            DateUtils.getStartOfMonth(),
            DateUtils.getEndOfMonth()
        )
    ) { accounts, recentTx, pendingCount, categories, monthlyTxs ->
        val catMap = categories.associateBy { it.id }
        val hasAcc = accounts.isNotEmpty()
        val totalBal = accounts.sumOf { it.balanceMinor }

        val income = monthlyTxs
            .filter { it.direction == com.flowmoney.app.data.model.TransactionDirection.INCOME }
            .sumOf { it.amountMinor }
        val expense = monthlyTxs
            .filter { it.direction == com.flowmoney.app.data.model.TransactionDirection.EXPENSE }
            .sumOf { it.amountMinor }
        val savings = (income - expense).coerceAtLeast(0L)
        val savingsRate = if (income > 0) ((income - expense).toFloat() / income * 100f).coerceIn(0f, 100f) else 0f

        // Group expenses by category
        val catExpenses = monthlyTxs
            .filter { it.direction == com.flowmoney.app.data.model.TransactionDirection.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txList) -> CategoryExpenseTotal(catId, txList.sumOf { it.amountMinor }) }
            .sortedByDescending { it.totalAmount }

        HomeUiState(
            accounts = accounts,
            totalEstimatedBalanceMinor = totalBal,
            hasAccounts = hasAcc,
            monthlyIncomeMinor = income,
            monthlyExpenseMinor = expense,
            savingsMinor = savings,
            savingsRatePercentage = savingsRate,
            recentTransactions = recentTx,
            pendingCount = pendingCount,
            categoryExpenses = catExpenses,
            categories = catMap,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(isLoading = true)
    )

    fun refresh() {
        _refreshTrigger.value += 1
    }

    fun quickConfirmTransaction(id: Long, categoryId: Long) {
        viewModelScope.launch {
            transactionRepository.updateClassification(id, categoryId)
            dailyReviewRepository.generateOrUpdateTodayReview()
            refresh()
        }
    }
}
