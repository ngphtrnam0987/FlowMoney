package com.flowmoney.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

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
            item {
                Text(
                    text = "Cài đặt & Tiện ích",
                    style = MaterialTheme.typography.headlineLarge,
                    color = FlowTextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            // Notification Access Section
            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tự động nhận diện giao dịch",
                                style = MaterialTheme.typography.titleMedium,
                                color = FlowTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "FlowMoney lắng nghe thông báo ngân hàng cục bộ ngay trên thiết bị để phát hiện số tiền chi tiêu. Không gửi dữ liệu lên mạng, không yêu cầu OTP hay mật khẩu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlowTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cấp quyền truy cập thông báo", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onNavigateToDiagnostics,
                            colors = ButtonDefaults.buttonColors(containerColor = FlowDarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = FlowPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chẩn đoán & Kiểm tra (Diagnostics)",
                                        fontSize = 13.sp,
                                        color = FlowTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = FlowTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Automatic Screenshot Detection for Bank Transfers
            item {
                Text(
                    text = "Tự động nhận diện biên lai chuyển khoản",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Phát hiện ảnh chụp màn hình",
                                color = FlowTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tự động gợi ý ghi nhận chi tiêu khi bạn vừa chụp màn hình chuyển khoản ngân hàng thành công.",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = state.isScreenshotDetectionEnabled,
                            onCheckedChange = { viewModel.toggleScreenshotDetection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlowTextPrimary,
                                checkedTrackColor = FlowPrimary,
                                uncheckedTrackColor = FlowDarkBorder
                            )
                        )
                    }
                }
            }

            // Supported Banks Toggles
            item {
                Text(
                    text = "Danh sách ngân hàng hỗ trợ",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            items(state.banks, key = { it.packageName }) { bank ->
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bank.name,
                            color = FlowTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = bank.isEnabled,
                            onCheckedChange = { viewModel.toggleBank(bank.packageName, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlowTextPrimary,
                                checkedTrackColor = FlowPrimary,
                                uncheckedTrackColor = FlowDarkBorder
                            )
                        )
                    }
                }
            }

            // Daily Review Section
            item {
                Text(
                    text = "Tổng kết cuối ngày",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Nhắc tổng kết lúc %02d:%02d".format(state.reminderHour, state.reminderMinute),
                                color = FlowTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Nhắc phân loại các khoản thu chi còn sót lại",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = state.isDailyReminderEnabled,
                            onCheckedChange = { viewModel.toggleDailyReminder(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlowTextPrimary,
                                checkedTrackColor = FlowPrimary
                            )
                        )
                    }
                }
            }

            // Financial Management Links
            item {
                Text(
                    text = "Quản lý tài chính",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = onNavigateToAccounts
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Quản lý tài khoản thanh toán", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = onNavigateToBudgets
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Quản lý ngân sách chi tiêu", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = onNavigateToCategories
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Quản lý danh mục & Quy tắc", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Security & Privacy
            item {
                Text(
                    text = "Bảo mật & Quyền riêng tư",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Khóa ứng dụng bằng sinh trắc học", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Switch(
                            checked = state.isBiometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometric(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlowTextPrimary,
                                checkedTrackColor = FlowPrimary
                            )
                        )
                    }
                }
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = onNavigateToPrivacy
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Chính sách bảo mật & Quyền riêng tư", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Backup & Export
            item {
                Text(
                    text = "Sao lưu & Xuất dữ liệu",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = { viewModel.exportCsv(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Xuất toàn bộ giao dịch ra CSV", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface,
                    onClick = { viewModel.exportJsonBackup(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sao lưu toàn bộ dữ liệu (JSON)", color = FlowTextPrimary, fontSize = 14.sp)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = FlowTextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
