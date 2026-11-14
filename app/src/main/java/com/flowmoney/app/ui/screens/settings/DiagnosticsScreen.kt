package com.flowmoney.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.domain.engine.AuditStatus
import com.flowmoney.app.domain.engine.NotificationAuditLog
import com.flowmoney.app.domain.engine.PipelineStep
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chẩn đoán thông báo ngân hàng",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlowTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = FlowTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = FlowPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FlowDarkBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection & Permission Status Card
            item {
                StatusOverviewSection(
                    state = state,
                    timeFormat = timeFormat,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRequestRebind = { viewModel.requestRebind() }
                )
            }

            // 2. End-to-End Test Section
            item {
                EndToEndTestSection(
                    state = state,
                    onRunTest = { viewModel.runEndToEndTest() },
                    onClearTest = { viewModel.clearTestTransactions() }
                )
            }

            // 3. Parser Simulator Section
            item {
                ParserSimulatorSection(
                    state = state,
                    onInputChange = { viewModel.setSimulatorInput(it) },
                    onRunSimulation = { viewModel.runSimulatorTest() }
                )
            }

            // 4. Last Received Notification Card
            item {
                Text(
                    text = "Thông báo nhận gần nhất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FlowTextPrimary
                )
            }

            item {
                if (state.lastAuditLog != null) {
                    AuditLogItemCard(log = state.lastAuditLog!!, timeFormat = timeFormat, isLatest = true)
                } else {
                    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurface) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Chưa nhận được thông báo nào từ khi mở app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowTextSecondary
                            )
                        }
                    }
                }
            }

            // 5. Installed Bank Apps Management
            item {
                Text(
                    text = "Ứng dụng ngân hàng phát hiện trên máy (${state.installedBankApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FlowTextPrimary
                )
            }

            if (state.installedBankApps.isEmpty()) {
                item {
                    Text(
                        text = "Không tìm thấy app ngân hàng nào hoặc chưa cấp quyền truy cập danh sách ứng dụng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlowTextSecondary
                    )
                }
            } else {
                items(state.installedBankApps, key = { it.packageName }) { appInfo ->
                    InstalledAppItemCard(
                        appInfo = appInfo,
                        onToggle = { enable -> viewModel.toggleBankApp(appInfo.packageName, enable) }
                    )
                }
            }

            // 6. Audit Logs History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhật ký nhận diện (${state.auditLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FlowTextPrimary
                    )
                    Text(
                        text = "Tự động che OTP & Mật khẩu",
                        fontSize = 11.sp,
                        color = FlowTextSecondary
                    )
                }
            }

            if (state.auditLogs.isEmpty()) {
                item {
                    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurface) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Nhật ký hiện đang trống",
                                color = FlowTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(state.auditLogs, key = { it.id }) { log ->
                    AuditLogItemCard(log = log, timeFormat = timeFormat)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusOverviewSection(
    state: DiagnosticsUiState,
    timeFormat: SimpleDateFormat,
    onOpenSettings: () -> Unit,
    onRequestRebind: () -> Unit
) {
    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurfaceVariant) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Trạng thái Dịch vụ Lắng nghe",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FlowTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Notification Access Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quyền truy cập thông báo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (state.isNotificationAccessGranted) FlowIncome else FlowExpense)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isNotificationAccessGranted) "Đã cấp quyền" else "Chưa cấp quyền",
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isNotificationAccessGranted) FlowIncome else FlowExpense,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Listener Connection Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trạng thái Listener Service:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FlowTextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (state.isListenerConnected) FlowIncome else FlowWarning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isListenerConnected) "Đang kết nối (Connected)" else "Chưa kết nối (Disconnected)",
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isListenerConnected) FlowIncome else FlowWarning,
                        fontSize = 13.sp
                    )
                }
            }

            if (state.lastConnectedTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lần kết nối gần nhất: ${timeFormat.format(Date(state.lastConnectedTime))}",
                    fontSize = 11.sp,
                    color = FlowTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cài đặt quyền", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRequestRebind,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Kết nối lại", fontSize = 12.sp, color = FlowTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun EndToEndTestSection(
    state: DiagnosticsUiState,
    onRunTest: () -> Unit,
    onClearTest: () -> Unit
) {
    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kiểm tra toàn diện Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FlowTextPrimary
                )
                if (state.testTransactions.isNotEmpty()) {
                    IconButton(
                        onClick = onClearTest,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Xóa test",
                            tint = FlowExpense
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sinh giao dịch thử nghiệm gắn nhãn TEST chạy qua cùng pipeline (Notification → Parser → DB → Dashboard). Không ảnh hưởng số dư tài chính thật.",
                style = MaterialTheme.typography.bodySmall,
                color = FlowTextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRunTest,
                enabled = !state.isRunningTest,
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRunningTest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang thực thi pipeline...", fontSize = 13.sp)
                } else {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kiểm tra ghi nhận giao dịch", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            state.testPipelineResult?.let { result ->
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (result.isOverallSuccess) FlowIncome.copy(alpha = 0.15f) else FlowExpense.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (result.isOverallSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.isOverallSuccess) FlowIncome else FlowExpense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isOverallSuccess) "Pipeline hoạt động hoàn hảo!" else "Có bước gặp sự cố!",
                        fontWeight = FontWeight.Bold,
                        color = if (result.isOverallSuccess) FlowIncome else FlowExpense,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                result.steps.forEach { step ->
                    PipelineStepRow(step = step)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun PipelineStepRow(step: PipelineStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(FlowDarkSurfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (step.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (step.isSuccess) FlowIncome else FlowExpense,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = step.stepName,
                fontWeight = FontWeight.SemiBold,
                color = FlowTextPrimary,
                fontSize = 12.sp
            )
            Text(
                text = step.details,
                color = FlowTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ParserSimulatorSection(
    state: DiagnosticsUiState,
    onInputChange: (String) -> Unit,
    onRunSimulation: () -> Unit
) {
    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kiểm tra Parser trực tiếp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = FlowTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Dán đoạn tin nhắn/thông báo ngân hàng để kiểm tra parser có nhận diện đúng số tiền và ngân hàng hay không.",
                style = MaterialTheme.typography.bodySmall,
                color = FlowTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.simulatorInput,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        "Ví dụ: TK 001100123 -120,000 VND lúc 14:00. So du 5,000,000 VND. ND: Cafe",
                        fontSize = 12.sp,
                        color = FlowTextSecondary.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlowPrimary,
                    unfocusedBorderColor = FlowDarkBorder,
                    focusedTextColor = FlowTextPrimary,
                    unfocusedTextColor = FlowTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onRunSimulation,
                colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Phân tích nội dung", fontSize = 12.sp)
            }

            state.simulatorResult?.let { parsed ->
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FlowIncome.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text("Kết quả nhận diện:", fontWeight = FontWeight.Bold, color = FlowIncome, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ngân hàng: ${parsed.bankName} (${parsed.bankIdentifier})", color = FlowTextPrimary, fontSize = 12.sp)
                    Text("Số tiền: ${CurrencyUtils.formatVnd(parsed.amountMinor)}", color = FlowTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Chiều biến động: ${if (parsed.direction == TransactionDirection.INCOME) "Tiền vào (+)" else "Tiền ra (-)"}", color = if (parsed.direction == TransactionDirection.INCOME) FlowIncome else FlowExpense, fontSize = 12.sp)
                    if (parsed.balanceAfterMinor != null) {
                        Text("Số dư sau giao dịch: ${CurrencyUtils.formatVnd(parsed.balanceAfterMinor)}", color = FlowTextPrimary, fontSize = 12.sp)
                    }
                    Text("Nội dung trích xuất: ${parsed.transactionDescription}", color = FlowTextSecondary, fontSize = 11.sp)
                }
            }

            state.simulatorError?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FlowExpense.copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = FlowExpense, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = err, color = FlowExpense, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun InstalledAppItemCard(
    appInfo: InstalledBankAppInfo,
    onToggle: (Boolean) -> Unit
) {
    FlowCard(modifier = Modifier.fillMaxWidth(), backgroundColor = FlowDarkSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowTextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = appInfo.packageName,
                    fontFamily = FontFamily.Monospace,
                    color = FlowTextSecondary,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = appInfo.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FlowTextPrimary,
                    checkedTrackColor = FlowPrimary,
                    uncheckedTrackColor = FlowDarkBorder
                )
            )
        }
    }
}

@Composable
private fun AuditLogItemCard(
    log: NotificationAuditLog,
    timeFormat: SimpleDateFormat,
    isLatest: Boolean = false
) {
    val statusColor = when (log.status) {
        AuditStatus.PERSISTED -> FlowIncome
        AuditStatus.REJECTED_DUPLICATE -> FlowWarning
        AuditStatus.REJECTED_PARSER -> FlowExpense
        AuditStatus.REJECTED_OTP -> FlowWarning
        AuditStatus.ERROR -> FlowExpense
        else -> FlowTextSecondary
    }

    val statusText = when (log.status) {
        AuditStatus.PERSISTED -> "Đã lưu DB"
        AuditStatus.REJECTED_DUPLICATE -> "Trùng lặp"
        AuditStatus.REJECTED_PARSER -> "Bỏ qua / Chưa hỗ trợ"
        AuditStatus.REJECTED_OTP -> "Mã OTP (Đã chặn)"
        AuditStatus.ERROR -> "Lỗi xử lý"
        else -> log.status.name
    }

    FlowCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isLatest) FlowDarkSurfaceVariant else FlowDarkSurface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.appName ?: log.packageName,
                    fontWeight = FontWeight.Bold,
                    color = FlowTextPrimary,
                    fontSize = 13.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.packageName,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = FlowTextSecondary
                )
                Text(
                    text = timeFormat.format(Date(log.timestamp)),
                    fontSize = 10.sp,
                    color = FlowTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sanitized text snippet
            Text(
                text = log.sanitizedSnippet,
                fontSize = 12.sp,
                color = FlowTextPrimary.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(FlowDarkBackground)
                    .padding(8.dp)
            )

            // Result details
            if (log.parsedAmountMinor != null || log.matchedParser != null || log.dbResult != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    log.matchedParser?.let { parser ->
                        Text("Parser: $parser", fontSize = 11.sp, color = FlowPrimary)
                    }
                    log.parsedAmountMinor?.let { amount ->
                        val sign = if (log.direction == TransactionDirection.INCOME) "+" else "-"
                        val color = if (log.direction == TransactionDirection.INCOME) FlowIncome else FlowExpense
                        Text("$sign${CurrencyUtils.formatVnd(amount)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }

            if (log.dbResult != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kết quả: ${log.dbResult}",
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }
    }
}
