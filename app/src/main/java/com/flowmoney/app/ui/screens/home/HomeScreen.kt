package com.flowmoney.app.ui.screens.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.ui.components.ChartSlice
import com.flowmoney.app.ui.components.DonutChart
import com.flowmoney.app.ui.components.EmptyStateView
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.components.TransactionItem
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowExpense
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.ui.theme.FlowWarning
import com.flowmoney.app.util.CurrencyUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToDailyReview: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    onNavigateToAllTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isBalanceVisible by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = FlowPrimary,
                contentColor = FlowTextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FlowMoney",
                            style = MaterialTheme.typography.headlineLarge,
                            color = FlowPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Quản lý dòng tiền thông minh",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlowTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToDailyReview,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FlowDarkSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Tổng kết",
                            tint = if (state.pendingCount > 0) FlowWarning else FlowTextPrimary
                        )
                    }
                }
            }

            // Estimated Balance Card
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant,
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tổng số dư ước tính",
                                color = FlowTextSecondary,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { isBalanceVisible = !isBalanceVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ẩn/hiện số dư",
                                    tint = FlowTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!state.hasAccounts) {
                            Text(
                                text = "Chưa thiết lập số dư",
                                color = FlowWarning,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onNavigateToAccounts,
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Thiết lập tài khoản ban đầu", fontSize = 13.sp)
                            }
                        } else {
                            Text(
                                text = if (isBalanceVisible) CurrencyUtils.formatVnd(state.totalEstimatedBalanceMinor) else "•••••••• ₫",
                                color = FlowTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Savings Rate row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiết kiệm tháng: ${if (isBalanceVisible) CurrencyUtils.formatVnd(state.savingsMinor) else "••••"}",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Tỷ lệ: %.1f%%".format(state.savingsRatePercentage),
                                color = FlowIncome,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Income / Expense Quick Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Income Card
                    FlowCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(FlowIncome.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = FlowIncome,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thu nhập",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBalanceVisible) CurrencyUtils.formatVnd(state.monthlyIncomeMinor) else "••••",
                                color = FlowIncome,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Expense Card
                    FlowCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(FlowExpense.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = FlowExpense,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chi tiêu",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isBalanceVisible) CurrencyUtils.formatVnd(state.monthlyExpenseMinor) else "••••",
                                color = FlowExpense,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Pending Review Banner (if any)
            if (state.pendingCount > 0) {
                item {
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowWarning.copy(alpha = 0.12f),
                        borderColor = FlowWarning.copy(alpha = 0.4f),
                        onClick = onNavigateToDailyReview
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = FlowWarning,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${state.pendingCount} giao dịch cần phân loại",
                                        color = FlowTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Xác nhận để hoàn thiện báo cáo tài chính",
                                        color = FlowTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = FlowWarning,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Category Distribution Chart
            if (state.categoryExpenses.isNotEmpty()) {
                item {
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Phân loại chi tiêu tháng này",
                                style = MaterialTheme.typography.titleMedium,
                                color = FlowTextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val slices = state.categoryExpenses.map { ce ->
                                val cat = state.categories[ce.categoryId]
                                val color = try {
                                    if (cat?.colorHex != null) Color(android.graphics.Color.parseColor(cat.colorHex)) else FlowPrimary
                                } catch (e: Exception) {
                                    FlowPrimary
                                }
                                ChartSlice(
                                    label = cat?.name ?: "Khác",
                                    value = ce.totalAmount,
                                    color = color
                                )
                            }

                            DonutChart(
                                slices = slices,
                                totalAmountMinor = state.monthlyExpenseMinor
                            )
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giao dịch gần đây",
                        style = MaterialTheme.typography.titleMedium,
                        color = FlowTextPrimary
                    )
                    Text(
                        text = "Xem tất cả",
                        color = FlowPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToAllTransactions() }
                    )
                }
            }

            // Recent Transactions List
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Chưa có giao dịch",
                        message = "Các giao dịch tự động nhận diện từ thông báo hoặc nhập thủ công sẽ hiển thị ở đây."
                    )
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { tx ->
                    val cat = state.categories[tx.categoryId]
                    val acc = state.accounts.firstOrNull { it.id == tx.accountId }
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
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
