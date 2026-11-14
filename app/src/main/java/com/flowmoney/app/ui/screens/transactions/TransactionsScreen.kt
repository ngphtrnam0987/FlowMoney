package com.flowmoney.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.ui.components.EmptyStateView
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.components.TransactionItem
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.DateUtils

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = FlowPrimary,
                contentColor = FlowTextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Title
            Text(
                text = "Sổ giao dịch",
                style = MaterialTheme.typography.headlineLarge,
                color = FlowTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Tìm theo tên, cửa hàng, danh mục...", color = FlowTextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FlowTextSecondary) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = FlowTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlowPrimary,
                    unfocusedBorderColor = FlowDarkBorder,
                    focusedContainerColor = FlowDarkSurfaceVariant,
                    unfocusedContainerColor = FlowDarkSurfaceVariant,
                    focusedTextColor = FlowTextPrimary,
                    unfocusedTextColor = FlowTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Pair(TransactionFilter.ALL, "Tất cả"),
                    Pair(TransactionFilter.EXPENSE, "Chi tiêu"),
                    Pair(TransactionFilter.INCOME, "Thu nhập"),
                    Pair(TransactionFilter.TRANSFER, "Chuyển tiền"),
                    Pair(TransactionFilter.PENDING, "Chờ duyệt")
                )

                items(filters) { (filter, label) ->
                    val isSelected = state.selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) FlowPrimary else FlowDarkSurfaceVariant)
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transactions List
            if (state.filteredTransactions.isEmpty()) {
                EmptyStateView(
                    title = "Không tìm thấy giao dịch",
                    message = "Thử thay đổi bộ lọc hoặc thêm giao dịch mới."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredTransactions, key = { it.id }) { tx ->
                        val cat = state.categories[tx.categoryId]
                        val acc = state.accounts[tx.accountId]
                        FlowCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = FlowDarkSurface,
                            cornerRadius = 12.dp,
                            onClick = { onNavigateToTransactionDetail(tx.id) }
                        ) {
                            TransactionItem(
                                transaction = tx,
                                categoryName = cat?.name,
                                categoryColorHex = cat?.colorHex,
                                accountName = acc?.name,
                                onClick = { onNavigateToTransactionDetail(tx.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}
