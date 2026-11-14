package com.flowmoney.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionFilter {
    ALL,
    EXPENSE,
    INCOME,
    TRANSFER,
    PENDING
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val accounts: Map<Long, AccountEntity> = emptyMap(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val selectedFilter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactionsFlow(),
        accountRepository.getAllAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        _selectedFilter,
        _searchQuery
    ) { txs, accounts, categories, filter, query ->
        val accMap = accounts.associateBy { it.id }
        val catMap = categories.associateBy { it.id }

        val filtered = txs.filter { tx ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.EXPENSE -> tx.direction == TransactionDirection.EXPENSE
                TransactionFilter.INCOME -> tx.direction == TransactionDirection.INCOME
                TransactionFilter.TRANSFER -> tx.direction == TransactionDirection.TRANSFER
                TransactionFilter.PENDING -> tx.classificationStatus == ClassificationStatus.PENDING
            }

            val matchesQuery = if (query.isBlank()) true else {
                tx.description.contains(query, ignoreCase = true) ||
                (tx.merchantName?.contains(query, ignoreCase = true) == true) ||
                (catMap[tx.categoryId]?.name?.contains(query, ignoreCase = true) == true)
            }

            matchesFilter && matchesQuery
        }

        TransactionsUiState(
            transactions = txs,
            filteredTransactions = filtered,
            accounts = accMap,
            categories = catMap,
            selectedFilter = filter,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TransactionsUiState(isLoading = true)
    )

    fun setFilter(filter: TransactionFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(id)
        }
    }
}
