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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.ui.components.CategoryIconBadge
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
fun AddTransactionScreen(
    transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var amountString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var merchantName by remember { mutableStateOf("") }
    var selectedDirection by remember { mutableStateOf(TransactionDirection.EXPENSE) }

    val accounts by accountRepository.getAllAccountsFlow().collectAsState(initial = emptyList())
    val categories by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())

    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull()?.id ?: 0L)
    }
    var selectedCategoryId by remember(categories, selectedDirection) {
        val filtered = categories.filter { it.isIncome == (selectedDirection == TransactionDirection.INCOME) }
        mutableStateOf(filtered.firstOrNull()?.id)
    }

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
                        text = "Thêm giao dịch mới",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Direction Switcher (Expense / Income / Transfer)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FlowDarkSurfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val directions = listOf(
                        Triple(TransactionDirection.EXPENSE, "Chi tiêu", FlowExpense),
                        Triple(TransactionDirection.INCOME, "Thu nhập", FlowIncome),
                        Triple(TransactionDirection.TRANSFER, "Chuyển khoản", FlowPrimary)
                    )

                    directions.forEach { (dir, label, color) ->
                        val isSelected = selectedDirection == dir
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) color else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { selectedDirection = dir }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Amount Input
            item {
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Số tiền (VND)", color = FlowTextSecondary) },
                    placeholder = { Text("vd: 50000 hoặc 50k", color = FlowTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )
                val parsed = CurrencyUtils.parseAmount(amountString)
                if (parsed != null) {
                    Text(
                        text = "= ${CurrencyUtils.formatVnd(parsed)}",
                        color = if (selectedDirection == TransactionDirection.INCOME) FlowIncome else FlowExpense,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // Description & Merchant
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Ghi chú / Nội dung", color = FlowTextSecondary) },
                    placeholder = { Text("vd: Ăn trưa bún bò", color = FlowTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Cửa hàng / Đối tác (tùy chọn)", color = FlowTextSecondary) },
                    placeholder = { Text("vd: Highlands Coffee", color = FlowTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )
            }

            // Account Selector
            item {
                Text(
                    text = "Tài khoản thanh toán",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts) { acc ->
                        val isSelected = selectedAccountId == acc.id
                        FlowCard(
                            backgroundColor = if (isSelected) FlowPrimary.copy(alpha = 0.2f) else FlowDarkSurfaceVariant,
                            borderColor = if (isSelected) FlowPrimary else FlowDarkBorder,
                            onClick = { selectedAccountId = acc.id }
                        ) {
                            Text(
                                text = acc.name,
                                color = if (isSelected) FlowTextPrimary else FlowTextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Category Selector
            item {
                Text(
                    text = "Danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                val relevantCategories = categories.filter { it.isIncome == (selectedDirection == TransactionDirection.INCOME) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(relevantCategories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        FlowCard(
                            backgroundColor = if (isSelected) FlowPrimary.copy(alpha = 0.2f) else FlowDarkSurfaceVariant,
                            borderColor = if (isSelected) FlowPrimary else FlowDarkBorder,
                            onClick = { selectedCategoryId = cat.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CategoryIconBadge(categoryName = cat.name, colorHex = cat.colorHex, size = 28.dp)
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

            // Save Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val parsedAmt = CurrencyUtils.parseAmount(amountString) ?: 0L
                        if (parsedAmt > 0L) {
                            coroutineScope.launch {
                                val tx = TransactionEntity(
                                    accountId = selectedAccountId,
                                    source = TransactionSource.MANUAL,
                                    amountMinor = parsedAmt,
                                    direction = selectedDirection,
                                    transactionTime = System.currentTimeMillis(),
                                    merchantName = merchantName.ifBlank { null },
                                    description = description.ifBlank { "Giao dịch chi tiêu" },
                                    categoryId = selectedCategoryId,
                                    classificationStatus = ClassificationStatus.CONFIRMED
                                )
                                transactionRepository.insertTransaction(tx)
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu giao dịch", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
