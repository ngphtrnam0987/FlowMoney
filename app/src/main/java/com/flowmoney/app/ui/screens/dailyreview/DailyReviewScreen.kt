package com.flowmoney.app.ui.screens.dailyreview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.local.FlowMoneyDatabase
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.DailyReviewRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.CategoryClassifier
import com.flowmoney.app.ui.components.EmptyStateView
import com.flowmoney.app.ui.components.FlowAmountText
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
import com.flowmoney.app.util.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun DailyReviewScreen(
    dailyReviewRepository: DailyReviewRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    categoryClassifier: CategoryClassifier,
    onNavigateBack: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val todayReview by dailyReviewRepository.getTodayReviewFlow().collectAsState(initial = null)
    val pendingTransactions by transactionRepository.getPendingTransactionsFlow().collectAsState(initial = emptyList())
    val categoriesFromDb by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    val categories = if (categoriesFromDb.isNotEmpty()) categoriesFromDb else FlowMoneyDatabase.DEFAULT_CATEGORIES

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
                        text = "Tổng kết tài chính trong ngày",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Today Summary Card
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Hôm nay (${todayReview?.dateString ?: "Hôm nay"})",
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
                                    text = CurrencyUtils.formatVnd(todayReview?.totalIncomeMinor ?: 0L),
                                    color = FlowIncome,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Chi tiêu", color = FlowTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = CurrencyUtils.formatVnd(todayReview?.totalExpenseMinor ?: 0L),
                                    color = FlowExpense,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Số giao dịch", color = FlowTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "${todayReview?.transactionCount ?: 0}",
                                    color = FlowTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Pending Transactions
            item {
                Text(
                    text = "Giao dịch cần xác nhận (${pendingTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            if (pendingTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Tuyệt vời!",
                        message = "Tất cả các giao dịch hôm nay đều đã được phân loại chuẩn xác."
                    )
                }
            } else {
                items(pendingTransactions, key = { it.id }) { tx ->
                    FlowCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTransactionDetail(tx.id) },
                        backgroundColor = FlowDarkSurface
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tx.merchantName ?: tx.description,
                                        color = FlowTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = tx.description,
                                        color = FlowTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 2
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FlowAmountText(
                                        amountMinor = tx.amountMinor,
                                        direction = tx.direction,
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = { onNavigateToTransactionDetail(tx.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Chỉnh sửa",
                                            tint = FlowPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chọn nhanh danh mục:",
                                    color = FlowTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Chạm để sửa >",
                                    color = FlowPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Sort categories so matching direction comes first
                            val relevantCategories = remember(tx.direction, categories) {
                                when (tx.direction) {
                                    TransactionDirection.INCOME -> categories.filter { it.isIncome } + categories.filter { !it.isIncome }
                                    TransactionDirection.EXPENSE -> categories.filter { !it.isIncome } + categories.filter { it.isIncome }
                                    TransactionDirection.TRANSFER -> categories
                                }
                            }

                            // Quick Category Action Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(relevantCategories) { cat ->
                                    val isSelected = tx.categoryId == cat.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) FlowPrimary.copy(alpha = 0.25f)
                                                else FlowDarkSurfaceVariant
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) FlowPrimary else FlowDarkBorder,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                coroutineScope.launch {
                                                    transactionRepository.updateClassification(tx.id, cat.id)
                                                    // Learn rule from user classification
                                                    categoryClassifier.learnFromUserEdit(
                                                        tx.description,
                                                        tx.merchantName,
                                                        cat.id
                                                    )
                                                    dailyReviewRepository.generateOrUpdateTodayReview()
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat.name,
                                            color = if (isSelected) FlowPrimary else FlowTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Finish Daily Review Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            dailyReviewRepository.completeReview()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hoàn thành tổng kết ngày", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
