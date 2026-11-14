package com.flowmoney.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkSurfaceVariant
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        text = "Chính sách bảo mật",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = FlowPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cam kết bảo mật dữ liệu tài chính",
                                style = MaterialTheme.typography.titleMedium,
                                color = FlowTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "1. Kiến trúc Local-First (Ưu tiên cục bộ):\nToàn bộ dữ liệu giao dịch, tài khoản ngân hàng và hóa đơn của bạn được lưu trữ an toàn trong cơ sở dữ liệu SQLite/Room ngay trên bộ nhớ cục bộ của thiết bị. Ứng dụng không tự ý gửi bất kỳ dữ liệu tài chính nào lên máy chủ bên thứ ba.",
                            color = FlowTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Text(
                            text = "2. Không đọc mã OTP hoặc mật khẩu:\nBộ lọc thông báo của FlowMoney tuyệt đối từ chối và bỏ qua mọi thông báo có chứa từ khóa OTP, mã xác thực hoặc nội dung nhạy cảm. Ứng dụng không bao giờ yêu cầu mật khẩu ngân hàng của bạn.",
                            color = FlowTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Text(
                            text = "3. Quét hóa đơn ngoại tuyến:\nTính năng nhận diện hóa đơn sử dụng thư viện Google ML Kit chạy hoàn toàn On-Device (trên thiết bị). Hình ảnh hóa đơn không được tải lên bất kỳ đám mây nào.",
                            color = FlowTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Text(
                            text = "4. Toàn quyền xóa dữ liệu:\nBạn có thể xuất sao lưu dữ liệu ra file CSV/JSON hoặc xóa toàn bộ dữ liệu tài chính bất cứ lúc nào trong mục Cài đặt.",
                            color = FlowTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
