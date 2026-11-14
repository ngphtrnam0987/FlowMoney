package com.flowmoney.app.ui.screens.transactions

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.CategoryClassifier
import com.flowmoney.app.ui.components.CategoryIconBadge
import com.flowmoney.app.ui.components.FlowAmountText
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.components.StatusBadge
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowExpense
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.CurrencyUtils
import com.flowmoney.app.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    categoryClassifier: CategoryClassifier,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var transaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var account by remember { mutableStateOf<AccountEntity?>(null) }
    val categories by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        val tx = transactionRepository.getTransactionById(transactionId)
        transaction = tx
        if (tx != null) {
            account = accountRepository.getAccountById(tx.accountId)
        }
    }

    val currentTx = transaction
    if (currentTx == null) {
        Scaffold(containerColor = FlowDarkBackground) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Đang tải chi tiết giao dịch...", color = FlowTextSecondary)
            }
        }
        return
    }

    val currentCategory = categories.firstOrNull { it.id == currentTx.categoryId }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = FlowTextPrimary)
                    }
                    Text(
                        text = "Chi tiết giao dịch",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = FlowExpense)
                    }
                }
            }

            // Amount Header Card
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryIconBadge(
                            categoryName = currentCategory?.name ?: "Khác",
                            colorHex = currentCategory?.colorHex,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowAmountText(
                            amountMinor = currentTx.amountMinor,
                            direction = currentTx.direction,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentTx.merchantName ?: currentTx.description,
                            color = FlowTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusBadge(status = currentTx.classificationStatus)
                    }
                }
            }

            // Details Section
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Thời gian", DateUtils.formatDateTime(currentTx.transactionTime))
                        DetailRow("Tài khoản", account?.name ?: "Tài khoản ${currentTx.accountId}")
                        DetailRow("Nguồn ghi nhận", when (currentTx.source) {
                            com.flowmoney.app.data.model.TransactionSource.BANK_NOTIFICATION -> "Thông báo ngân hàng"
                            com.flowmoney.app.data.model.TransactionSource.RECEIPT_SCAN -> "Quét hóa đơn"
                            com.flowmoney.app.data.model.TransactionSource.MANUAL -> "Nhập thủ công"
                        })
                        if (currentTx.merchantName != null) {
                            DetailRow("Đối tác / Cửa hàng", currentTx.merchantName)
                        }
                        if (currentTx.externalReference != null) {
                            DetailRow("Mã giao dịch (Ref)", currentTx.externalReference)
                        }
                        if (currentTx.balanceAfterMinor != null) {
                            DetailRow("Số dư sau GD", CurrencyUtils.formatVnd(currentTx.balanceAfterMinor))
                        }
                    }
                }
            }

            // Category Re-classification section
            item {
                Text(
                    text = "Thay đổi danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = currentTx.categoryId == cat.id
                        FlowCard(
                            backgroundColor = if (isSelected) FlowPrimary.copy(alpha = 0.2f) else FlowDarkSurfaceVariant,
                            borderColor = if (isSelected) FlowPrimary else FlowDarkBorder,
                            onClick = {
                                coroutineScope.launch {
                                    transactionRepository.updateClassification(currentTx.id, cat.id)
                                    categoryClassifier.learnFromUserEdit(
                                        currentTx.description,
                                        currentTx.merchantName,
                                        cat.id
                                    )
                                    val updated = transactionRepository.getTransactionById(currentTx.id)
                                    transaction = updated
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(categoryName = cat.name, colorHex = cat.colorHex, size = 26.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = FlowDarkSurface,
                title = { Text("Xác nhận xóa giao dịch?", color = FlowTextPrimary) },
                text = { Text("Số tiền sẽ được hoàn trả lại số dư tài khoản của bạn.", color = FlowTextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                transactionRepository.deleteTransaction(currentTx.id)
                                showDeleteDialog = false
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlowExpense)
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Hủy", color = FlowTextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = FlowTextSecondary, fontSize = 13.sp)
        Text(text = value, color = FlowTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
