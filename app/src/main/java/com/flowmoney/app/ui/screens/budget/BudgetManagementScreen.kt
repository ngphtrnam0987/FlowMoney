package com.flowmoney.app.ui.screens.budget

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.local.entity.BudgetEntity
import com.flowmoney.app.data.model.BudgetPeriod
import com.flowmoney.app.data.repository.BudgetRepository
import com.flowmoney.app.data.repository.BudgetStatus
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.ui.components.EmptyStateView
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowExpense
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.ui.theme.FlowWarning
import com.flowmoney.app.util.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun BudgetManagementScreen(
    budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val budgets by budgetRepository.getActiveBudgetsFlow().collectAsState(initial = emptyList())
    val categories by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    var budgetStatuses by remember { mutableStateOf<List<BudgetStatus>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(budgets) {
        val statuses = budgets.map { budgetRepository.calculateBudgetStatus(it) }
        budgetStatuses = statuses
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FlowPrimary,
                contentColor = FlowTextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ngân sách")
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
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = FlowTextPrimary)
                    }
                    Text(
                        text = "Quản lý ngân sách chi tiêu",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (budgetStatuses.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Chưa thiết lập ngân sách",
                        message = "Thiết lập hạn mức chi tiêu hàng tháng cho từng danh mục để kiểm soát tài chính.",
                        actionButton = {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                            ) {
                                Text("Tạo ngân sách đầu tiên")
                            }
                        }
                    )
                }
            } else {
                items(budgetStatuses, key = { it.budget.id }) { status ->
                    val cat = categories.firstOrNull { it.id == status.budget.categoryId }
                    val label = cat?.name ?: "Tổng chi tiêu toàn hệ thống"
                    val progress = (status.utilizationPercentage / 100f).coerceIn(0f, 1f)
                    val progressColor = when {
                        status.isOverLimit -> FlowExpense
                        status.isNearLimit -> FlowWarning
                        else -> FlowIncome
                    }

                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = FlowTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "%.1f%%".format(status.utilizationPercentage),
                                    color = progressColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = progressColor,
                                trackColor = FlowDarkBorder
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Đã chi: ${CurrencyUtils.formatVnd(status.spentMinor)}",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Hạn mức: ${CurrencyUtils.formatVnd(status.budget.amountMinor)}",
                                    color = FlowTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBudgetDialog(
                categories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { catId, amount ->
                    coroutineScope.launch {
                        val budget = BudgetEntity(
                            categoryId = catId,
                            period = BudgetPeriod.MONTHLY,
                            amountMinor = amount
                        )
                        budgetRepository.saveBudget(budget)
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun AddBudgetDialog(
    categories: List<com.flowmoney.app.data.local.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, amountMinor: Long) -> Unit
) {
    var rawAmount by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FlowDarkSurface,
        title = { Text("Tạo hạn mức ngân sách", color = FlowTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    label = { Text("Hạn mức hàng tháng (VND)", color = FlowTextSecondary) },
                    placeholder = { Text("vd: 3000000 hoặc 3tr", color = FlowTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = CurrencyUtils.parseAmount(rawAmount) ?: 0L
                    if (amt > 0L) {
                        onConfirm(selectedCatId, amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Lưu ngân sách")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = FlowTextSecondary)
            }
        }
    )
}
