package com.flowmoney.app.ui.screens.onboarding

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.SettingsRepository
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
fun OnboardingScreen(
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) }

    var accountName by remember { mutableStateOf("Tài khoản chính") }
    var rawOpeningBalance by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("Vietcombank") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Step Indicators
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentStep) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i <= currentStep) FlowPrimary else FlowDarkBorder)
                    )
                    if (i < 4) Spacer(modifier = Modifier.width(6.dp))
                }
            }

            // Step Content
            when (currentStep) {
                1 -> {
                    // Welcome & Vision
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(FlowPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = FlowPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Chào mừng đến với FlowMoney",
                            style = MaterialTheme.typography.headlineLarge,
                            color = FlowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tự động ghi nhận thu chi từ thông báo ngân hàng, quét hóa đơn bằng AI và quản lý tài chính cá nhân an toàn.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FlowTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                2 -> {
                    // Privacy First
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(FlowIncome.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = FlowIncome,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Bảo mật & Quyền riêng tư",
                            style = MaterialTheme.typography.headlineMedium,
                            color = FlowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = FlowDarkSurfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("• Hoạt động hoàn toàn Offline trên máy của bạn", color = FlowTextPrimary, fontSize = 13.sp)
                                Text("• Không yêu cầu đăng ký tài khoản đám mây", color = FlowTextPrimary, fontSize = 13.sp)
                                Text("• Không đọc mã OTP hay tin nhắn xác thực", color = FlowTextPrimary, fontSize = 13.sp)
                                Text("• Không yêu cầu mật khẩu hay quyền truy cập ngân hàng", color = FlowTextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                3 -> {
                    // First Account & Opening Balance
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Tài khoản đầu tiên của bạn",
                            style = MaterialTheme.typography.headlineMedium,
                            color = FlowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thiết lập số dư ban đầu để ứng dụng theo dõi chính xác tổng tài sản.",
                            color = FlowTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = accountName,
                            onValueChange = { accountName = it },
                            label = { Text("Tên tài khoản", color = FlowTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = FlowDarkBorder,
                                focusedTextColor = FlowTextPrimary,
                                unfocusedTextColor = FlowTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Ngân hàng (vd: Vietcombank, MB, Techcombank)", color = FlowTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = FlowDarkBorder,
                                focusedTextColor = FlowTextPrimary,
                                unfocusedTextColor = FlowTextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawOpeningBalance,
                            onValueChange = { rawOpeningBalance = it },
                            label = { Text("Số dư ban đầu (VND)", color = FlowTextSecondary) },
                            placeholder = { Text("vd: 2000000 hoặc 2tr", color = FlowTextSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlowPrimary,
                                unfocusedBorderColor = FlowDarkBorder,
                                focusedTextColor = FlowTextPrimary,
                                unfocusedTextColor = FlowTextPrimary
                            )
                        )
                    }
                }
                4 -> {
                    // Notification Permission Guide
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(FlowPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = FlowPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Tự động ghi nhận giao dịch",
                            style = MaterialTheme.typography.headlineMedium,
                            color = FlowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Cho phép FlowMoney đọc thông báo biến động số dư từ ứng dụng ngân hàng của bạn. Bạn có thể cấp quyền ngay bây giờ hoặc cài đặt sau.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlowTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mở Cài đặt cấp quyền đọc thông báo", color = FlowPrimary)
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1 },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Quay lại", color = FlowTextSecondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 4) {
                            currentStep += 1
                        } else {
                            // Finish Onboarding
                            coroutineScope.launch {
                                val balance = CurrencyUtils.parseAmount(rawOpeningBalance) ?: 0L
                                accountRepository.createAccount(
                                    name = accountName.ifBlank { "Tài khoản chính" },
                                    type = AccountType.BANK,
                                    openingBalanceMinor = balance,
                                    bankName = bankName.ifBlank { null },
                                    isDefault = true
                                )
                                settingsRepository.setOnboardingCompleted(true)
                                onComplete()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentStep == 4) "Bắt đầu sử dụng" else "Tiếp tục", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(if (currentStep == 4) Icons.Default.Check else Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
