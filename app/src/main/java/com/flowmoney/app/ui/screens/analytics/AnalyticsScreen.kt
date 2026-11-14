package com.flowmoney.app.ui.screens.analytics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.flowmoney.app.ui.components.ChartSlice
import com.flowmoney.app.ui.components.DonutChart
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.components.IncomeExpenseBarChart
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowExpense
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.CurrencyUtils

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Báo cáo tài chính",
                    style = MaterialTheme.typography.headlineLarge,
                    color = FlowTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Summary Overview Card
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant,
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Tổng quan tháng này",
                            color = FlowTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Thu nhập", color = FlowTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = CurrencyUtils.formatVnd(state.totalIncomeMinor),
                                    color = FlowIncome,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Chi tiêu", color = FlowTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = CurrencyUtils.formatVnd(state.totalExpenseMinor),
                                    color = FlowExpense,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Tỷ lệ tiết kiệm", color = FlowTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "%.1f%%".format(state.savingsRatePercentage),
                                    color = FlowPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 7-Day Income vs Expense Bar Chart
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Xu hướng 7 ngày gần nhất",
                            style = MaterialTheme.typography.titleMedium,
                            color = FlowTextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IncomeExpenseBarChart(data = state.barChartData)
                    }
                }
            }

            // Category Breakdown Donut Chart
            if (state.categoryExpenses.isNotEmpty()) {
                item {
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Tỷ trọng chi tiêu theo danh mục",
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
                                totalAmountMinor = state.totalExpenseMinor
                            )
                        }
                    }
                }
            }

            // Top Merchants Ranking
            if (state.topMerchants.isNotEmpty()) {
                item {
                    Text(
                        text = "Top điểm chi tiêu nhiều nhất",
                        style = MaterialTheme.typography.titleMedium,
                        color = FlowTextPrimary
                    )
                }

                itemsIndexed(state.topMerchants) { index, merchant ->
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(FlowPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    color = FlowPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = merchant.merchantName,
                                    color = FlowTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${merchant.transactionCount} lần giao dịch",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatVnd(merchant.totalExpenseMinor),
                                color = FlowExpense,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
