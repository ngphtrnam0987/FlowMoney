package com.flowmoney.app.ui.screens.scan

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.flowmoney.app.util.DateUtils

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onScanSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processImageUri(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.processBitmap(bitmap)
        }
    }

    if (state.isSuccess) {
        onScanSuccess()
        viewModel.resetState()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground
    ) { innerPadding ->
        if (state.isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = FlowPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Đang quét hóa đơn bằng AI...",
                        color = FlowTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Trích xuất cửa hàng, số tiền và từng mặt hàng",
                        color = FlowTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (state.rawOcrText != null) {
            // Edit & Review Extracted Receipt Screen
            ReceiptEditReviewView(
                state = state,
                onUpdateMerchant = { viewModel.updateMerchantName(it) },
                onUpdateTotal = { viewModel.updateTotalAmount(it) },
                onSelectAccount = { viewModel.selectAccount(it) },
                onSelectCategory = { viewModel.selectCategory(it) },
                onRemoveItem = { viewModel.removeLineItem(it) },
                onSelectMatch = { viewModel.selectMatchTransaction(it) },
                onSave = { viewModel.saveReceipt() },
                onCancel = { viewModel.resetState() },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            // Capture / Pick Choice Screen
            ScanCaptureHeroView(
                onTakePhoto = { cameraLauncher.launch(null) },
                onPickGallery = { galleryLauncher.launch("image/*") },
                errorMessage = state.errorMessage,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ScanCaptureHeroView(
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(FlowPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = FlowPrimary,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Quét hóa đơn bằng AI",
            style = MaterialTheme.typography.headlineLarge,
            color = FlowTextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Tự động trích xuất tên cửa hàng, thời gian, tổng tiền và từng món hàng từ hóa đơn mua sắm của bạn.",
            style = MaterialTheme.typography.bodyMedium,
            color = FlowTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = FlowExpense,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chụp ảnh hóa đơn", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowTextPrimary)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = FlowTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chọn ảnh từ thư viện", fontSize = 15.sp)
        }
    }
}

@Composable
fun ReceiptEditReviewView(
    state: ScanUiState,
    onUpdateMerchant: (String) -> Unit,
    onUpdateTotal: (Long) -> Unit,
    onSelectAccount: (Long) -> Unit,
    onSelectCategory: (Long) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSelectMatch: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rawAmountInput by remember(state.totalAmount) {
        mutableStateOf(state.totalAmount.toString())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (state.isBankTransfer) "Biên lai chuyển khoản" else "Xác nhận kết quả quét",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isBankTransfer) {
                        Text(
                            text = "Tự động trích xuất thông tin chuyển khoản bằng AI",
                            color = FlowPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Hủy", tint = FlowTextSecondary)
                }
            }
        }

        // Merchant / Recipient Name field
        item {
            OutlinedTextField(
                value = state.merchantName,
                onValueChange = onUpdateMerchant,
                label = {
                    Text(
                        if (state.isBankTransfer) "Người nhận / Đơn vị thụ hưởng" else "Tên cửa hàng",
                        color = FlowTextSecondary
                    )
                },
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

        // Total Amount field
        item {
            OutlinedTextField(
                value = rawAmountInput,
                onValueChange = {
                    rawAmountInput = it
                    val parsed = it.toLongOrNull()
                    if (parsed != null) onUpdateTotal(parsed)
                },
                label = {
                    Text(
                        if (state.isBankTransfer) "Số tiền chuyển (VND)" else "Tổng tiền (VND)",
                        color = FlowTextSecondary
                    )
                },
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

        // Account Selection
        if (state.accounts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Tài khoản thanh toán:",
                        color = FlowTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.accounts.size) { idx ->
                            val acc = state.accounts[idx]
                            val isSelected = state.selectedAccountId == acc.id
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
                                    .clickable { onSelectAccount(acc.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = acc.name,
                                    color = if (isSelected) FlowPrimary else FlowTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Selection
        if (state.categories.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Danh mục chi tiêu (AI tự động chọn):",
                        color = FlowTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.categories.size) { idx ->
                            val cat = state.categories[idx]
                            val isSelected = state.selectedCategoryId == cat.id
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
                                    .clickable { onSelectCategory(cat.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    color = if (isSelected) FlowPrimary else FlowTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Intelligent Bank Transaction Match Section
        if (state.matchCandidates.isNotEmpty()) {
            item {
                Text(
                    text = "Gợi ý ghép với giao dịch ngân hàng",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn giao dịch đã trừ tiền để không bị tính trùng chi tiêu:",
                    color = FlowTextSecondary,
                    fontSize = 12.sp
                )
            }

            itemsIndexed(state.matchCandidates) { _, candidate ->
                val isSelected = state.selectedMatchTransactionId == candidate.transaction.id
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (isSelected) FlowPrimary.copy(alpha = 0.15f) else FlowDarkSurfaceVariant,
                    borderColor = if (isSelected) FlowPrimary else FlowDarkBorder,
                    onClick = {
                        if (isSelected) onSelectMatch(null) else onSelectMatch(candidate.transaction.id)
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) onSelectMatch(null) else onSelectMatch(candidate.transaction.id)
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = FlowPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = candidate.transaction.description,
                                color = FlowTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${DateUtils.formatDateTime(candidate.transaction.transactionTime)} • Khớp ${(candidate.score * 100).toInt()}%",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatVnd(candidate.transaction.amountMinor),
                            color = FlowExpense,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Extracted Line Items Section
        if (state.lineItems.isNotEmpty()) {
            item {
                Text(
                    text = "Chi tiết từng món hàng (${state.lineItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            itemsIndexed(state.lineItems) { idx, item ->
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = FlowTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Số lượng: ${item.quantity.toInt()} • Đơn giá: ${CurrencyUtils.formatVnd(item.unitPriceMinor ?: item.totalPriceMinor)}",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyUtils.formatVnd(item.totalPriceMinor),
                                color = FlowTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { onRemoveItem(idx) }) {
                                Icon(Icons.Default.Close, contentDescription = "Xóa món", tint = FlowTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Save & Confirm Buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                val saveText = when {
                    state.selectedMatchTransactionId != null -> "Lưu & Ghép giao dịch ngân hàng"
                    state.isBankTransfer -> "Ghi nhận chi tiêu: ${CurrencyUtils.formatVnd(state.totalAmount)}"
                    else -> "Lưu hóa đơn & Tạo chi tiêu"
                }
                Text(saveText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
