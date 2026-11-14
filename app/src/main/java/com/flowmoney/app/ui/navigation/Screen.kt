package com.flowmoney.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    // 5 Main Tabs
    object Home : Screen("home", "Tổng quan", Icons.Default.Home)
    object Transactions : Screen("transactions", "Sổ giao dịch", Icons.Default.AccountBalanceWallet)
    object Scan : Screen("scan", "Quét hóa đơn", Icons.Default.DocumentScanner)
    object Analytics : Screen("analytics", "Báo cáo", Icons.Default.Analytics)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)

    // Sub screens
    object Onboarding : Screen("onboarding", "Bắt đầu")
    object AddTransaction : Screen("add_transaction", "Thêm giao dịch")
    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Chi tiết giao dịch") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }
    object DailyReview : Screen("daily_review", "Tổng kết ngày")
    object Budget : Screen("budget", "Quản lý ngân sách")
    object CategoryManagement : Screen("category_management", "Quản lý danh mục")
    object AccountManagement : Screen("account_management", "Quản lý tài khoản")
    object PrivacySecurity : Screen("privacy_security", "Bảo mật & Quyền riêng tư")
    object Diagnostics : Screen("diagnostics", "Chẩn đoán thông báo ngân hàng")
}
