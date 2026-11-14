package com.flowmoney.app.ui.screens.transactions

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.local.FlowMoneyDatabase
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
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
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.ui.theme.FlowWarning
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
    val accounts by accountRepository.getAllAccountsFlow().collectAsState(initial = emptyList())
    val categoriesFromDb by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    val categories = if (categoriesFromDb.isNotEmpty()) categoriesFromDb else FlowMoneyDatabase.DEFAULT_CATEGORIES

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun reloadTransaction() {
        coroutineScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
            transaction = tx
            if (tx != null) {
                account = accountRepository.getAccountById(tx.accountId)
            }
        }
    }

    LaunchedEffect(transactionId) {
        reloadTransaction()
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
        containerColor = FlowDarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
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
                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = FlowPrimary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = FlowExpense)
                        }
                    }
                }
            }

            // Amount Header Card (Clickable to Edit)
            item {
                FlowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditDialog = true },
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge(status = currentTx.classificationStatus)
                            Text(
                                text = "Bấm để chỉnh sửa",
                                fontSize = 11.sp,
                                color = FlowPrimary
                            )
                        }
                    }
                }
            }

            // Details Section (Clickable to Edit)
            item {
                FlowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditDialog = true },
                    backgroundColor = FlowDarkSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailRow("Thời gian", DateUtils.formatDateTime(currentTx.transactionTime))
                        DetailRow("Tài khoản", account?.name ?: "Tài khoản ${currentTx.accountId}")
                        DetailRow("Loại giao dịch", when (currentTx.direction) {
                            TransactionDirection.INCOME -> "Tiền vào / Thu nhập (+)"
                            TransactionDirection.EXPENSE -> "Tiền ra / Chi tiêu (-)"
                            TransactionDirection.TRANSFER -> "Chuyển tiền nội bộ"
                        })
                        DetailRow("Nguồn ghi nhận", when (currentTx.source) {
                            TransactionSource.BANK_NOTIFICATION -> "Thông báo ngân hàng"
                            TransactionSource.RECEIPT_SCAN -> "Quét hóa đơn"
                            TransactionSource.MANUAL -> "Nhập thủ công"
                        })
                        if (currentTx.merchantName != null) {
                            DetailRow("Đối tác / Cửa hàng", currentTx.merchantName)
                        }
                        if (currentTx.description.isNotBlank()) {
                            DetailRow("Nội dung / Lời nhắn", currentTx.description)
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

            // Prominent Edit Button
            item {
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Chỉnh sửa giao dịch này", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Category Change section
            item {
                Text(
                    text = "Thay đổi danh mục nhanh",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FlowTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = currentTx.categoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) FlowPrimary.copy(alpha = 0.25f) else FlowDarkSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) FlowPrimary else FlowDarkBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        transactionRepository.updateClassification(currentTx.id, cat.id)
                                        categoryClassifier.learnFromUserEdit(
                                            currentTx.description,
                                            currentTx.merchantName,
                                            cat.id
                                        )
                                        reloadTransaction()
                                        snackbarHostState.showSnackbar("Đã đổi danh mục sang: ${cat.name}")
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIconBadge(categoryName = cat.name, colorHex = cat.colorHex, size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Full Edit Transaction Dialog
        if (showEditDialog) {
            EditTransactionDialog(
                initialTransaction = currentTx,
                accounts = accounts,
                categories = categories,
                onDismiss = { showEditDialog = false },
                onSave = { updatedTx ->
                    coroutineScope.launch {
                        transactionRepository.updateTransaction(updatedTx)
                        if (updatedTx.categoryId != null) {
                            categoryClassifier.learnFromUserEdit(
                                updatedTx.description,
                                updatedTx.merchantName,
                                updatedTx.categoryId
                            )
                        }
                        reloadTransaction()
                        showEditDialog = false
                        snackbarHostState.showSnackbar("Đã cập nhật giao dịch thành công!")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = FlowDarkSurface,
                title = { Text("Xác nhận xóa giao dịch?", color = FlowTextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Giao dịch sẽ bị xóa và số tiền sẽ được hoàn trả lại tài khoản tương ứng.", color = FlowTextSecondary) },
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
                        Text("Xóa giao dịch")
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
fun EditTransactionDialog(
    initialTransaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var amountInput by remember { mutableStateOf(initialTransaction.amountMinor.toString()) }
    var selectedDirection by remember { mutableStateOf(initialTransaction.direction) }
    var selectedAccountId by remember { mutableStateOf(initialTransaction.accountId) }
    var selectedCategoryId by remember { mutableStateOf(initialTransaction.categoryId) }
    var merchantInput by remember { mutableStateOf(initialTransaction.merchantName ?: "") }
    var descriptionInput by remember { mutableStateOf(initialTransaction.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FlowDarkSurface,
        title = {
            Text(
                text = "Chỉnh sửa giao dịch",
                color = FlowTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Direction Toggle
                Text(text = "Loại giao dịch", color = FlowTextSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Expense Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedDirection == TransactionDirection.EXPENSE) FlowExpense.copy(alpha = 0.2f) else FlowDarkSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (selectedDirection == TransactionDirection.EXPENSE) FlowExpense else FlowDarkBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDirection = TransactionDirection.EXPENSE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chi tiêu (-)",
                            color = if (selectedDirection == TransactionDirection.EXPENSE) FlowExpense else FlowTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Income Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedDirection == TransactionDirection.INCOME) FlowIncome.copy(alpha = 0.2f) else FlowDarkSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (selectedDirection == TransactionDirection.INCOME) FlowIncome else FlowDarkBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDirection = TransactionDirection.INCOME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Thu nhập (+)",
                            color = if (selectedDirection == TransactionDirection.INCOME) FlowIncome else FlowTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Amount TextField
                Text(text = "Số tiền (VND)", color = FlowTextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        amountInput = digits
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0 đ", color = FlowTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )

                // Account Selection
                if (accounts.isNotEmpty()) {
                    Text(text = "Tài khoản", color = FlowTextSecondary, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(accounts) { acc ->
                            val isSelected = selectedAccountId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) FlowPrimary.copy(alpha = 0.2f) else FlowDarkSurfaceVariant)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) FlowPrimary else FlowDarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedAccountId = acc.id }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = acc.name,
                                    color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Category Selection
                Text(text = "Danh mục", color = FlowTextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) FlowPrimary.copy(alpha = 0.2f) else FlowDarkSurfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) FlowPrimary else FlowDarkBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCategoryId = cat.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Description
                Text(text = "Nội dung / Ghi chú", color = FlowTextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )

                // Merchant / Counterparty
                Text(text = "Đối tác / Người chuyển", color = FlowTextSecondary, fontSize = 12.sp)
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
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
                    val newAmount = amountInput.toLongOrNull() ?: initialTransaction.amountMinor
                    val updated = initialTransaction.copy(
                        amountMinor = if (newAmount > 0) newAmount else initialTransaction.amountMinor,
                        direction = selectedDirection,
                        accountId = selectedAccountId,
                        categoryId = selectedCategoryId,
                        merchantName = merchantInput.ifBlank { null },
                        description = descriptionInput.ifBlank { initialTransaction.description },
                        classificationStatus = ClassificationStatus.CONFIRMED,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = FlowTextSecondary)
            }
        }
    )
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
