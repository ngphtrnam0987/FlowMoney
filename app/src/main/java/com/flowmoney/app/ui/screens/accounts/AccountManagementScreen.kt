package com.flowmoney.app.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.ui.components.EmptyStateView
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.CurrencyUtils
import kotlinx.coroutines.launch

@Composable
fun AccountManagementScreen(
    accountRepository: AccountRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val accounts by accountRepository.getAllAccountsFlow().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = FlowPrimary,
                contentColor = FlowTextPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm tài khoản")
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
                        text = "Tài khoản & Nguồn tiền",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (accounts.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Chưa có tài khoản",
                        message = "Tạo tài khoản ngân hàng, ví điện tử hoặc tiền mặt để bắt đầu ghi nhận số dư.",
                        actionButton = {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
                            ) {
                                Text("Thêm tài khoản đầu tiên")
                            }
                        }
                    )
                }
            } else {
                items(accounts, key = { it.id }) { acc ->
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (acc.type) {
                                AccountType.BANK -> Icons.Default.AccountBalance
                                AccountType.WALLET -> Icons.Default.Wallet
                                AccountType.CASH -> Icons.Default.Payments
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = FlowPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    color = FlowTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Số dư ban đầu: ${CurrencyUtils.formatVnd(acc.openingBalanceMinor)}",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyUtils.formatVnd(acc.balanceMinor),
                                    color = FlowIncome,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (acc.isDefault) {
                                    Text(
                                        text = "Mặc định",
                                        color = FlowPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddAccountDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, openingBalance, bankName, accNum ->
                    coroutineScope.launch {
                        accountRepository.createAccount(
                            name = name,
                            type = type,
                            openingBalanceMinor = openingBalance,
                            bankName = bankName,
                            accountNumber = accNum,
                            isDefault = accounts.isEmpty()
                        )
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, openingBalance: Long, bankName: String?, accNum: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var rawBalance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var bankName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FlowDarkSurface,
        title = { Text("Thêm tài khoản mới", color = FlowTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên tài khoản (vd: VCB Chính)", color = FlowTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )

                OutlinedTextField(
                    value = rawBalance,
                    onValueChange = { rawBalance = it },
                    label = { Text("Số dư ban đầu (VND)", color = FlowTextSecondary) },
                    placeholder = { Text("vd: 5000000 hoặc 5tr", color = FlowTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPrimary,
                        unfocusedBorderColor = FlowDarkBorder,
                        focusedTextColor = FlowTextPrimary,
                        unfocusedTextColor = FlowTextPrimary
                    )
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Tên ngân hàng (vd: Vietcombank, MB)", color = FlowTextSecondary) },
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
                    if (name.isNotBlank()) {
                        val balance = CurrencyUtils.parseAmount(rawBalance) ?: 0L
                        onConfirm(name, selectedType, balance, bankName.ifBlank { null }, null)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary)
            ) {
                Text("Lưu tài khoản")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = FlowTextSecondary)
            }
        }
    )
}
