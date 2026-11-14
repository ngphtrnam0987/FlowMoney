# FlowMoney — Smart Personal Finance Application for Android

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)
![Android](https://img.shields.io/badge/Platform-Android_10+_(API_29+)-green.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material_3-blue.svg)
![Architecture](https://img.shields.io/badge/Architecture-Clean_Architecture_+_MVVM-orange.svg)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)
[![Download APK](https://img.shields.io/badge/Download-FlowMoney_APK_v1.0.0-purple?logo=android&logoColor=white)](https://github.com/ngphtrnam0987/FlowMoney/releases/download/v1.0.0/FlowMoney-v1.0.0.apk)
![License](https://img.shields.io/badge/License-Apache_2.0-lightgrey.svg)

**FlowMoney** là ứng dụng quản lý tài chính cá nhân thông minh dành cho nền tảng Android, giải quyết triệt để rào cản người dùng phải nhập tay từng giao dịch thu chi hàng ngày. Ứng dụng tự động phát hiện giao dịch ngân hàng từ thông báo Android, tích hợp AI quét hóa đơn ngoại tuyến bằng Google ML Kit, tự động phân loại chi tiêu 5 cấp độ và nhắc nhở người dùng tổng kết tài chính cuối ngày.

---

## 🌟 Tính năng nổi bật

### 1. Tự động nhận diện giao dịch ngân hàng (Notification Listener)
- Lắng nghe thông báo biến động số dư theo kiến trúc plugin mở rộng `BankNotificationParser`.
- Hỗ trợ đầy đủ các ngân hàng hàng đầu tại Việt Nam: **MB Bank, Vietcombank, Techcombank, BIDV, VietinBank, ACB, TPBank, VPBank, Sacombank, VIB** và parser tổng quát.
- **Bảo mật tuyệt đối**: Bộ lọc bảo mật chặn mọi thông báo chứa mã OTP hoặc xác thực tài khoản; không bao giờ yêu cầu mật khẩu hay quyền truy cập tài khoản ngân hàng.

### 2. Smart Transaction Engine & Chống trùng lặp (Deduplication)
- Pipeline xử lý: `Notification Received -> Source Validation -> Bank Parser -> Normalization -> Duplicate Detection -> Transaction Matching -> Category Suggestion -> Room Database -> Dashboard Update`.
- Thuật toán chống trùng lặp đa yếu tố: mã tham chiếu ngoài (External Reference), băm dấu vân tay (Fingerprint Hash), số tiền, chiều giao dịch và cửa sổ thời gian (Time window).
- Lưu trữ số tiền chính xác theo đơn vị minor unit (`Long` trong VND), tránh hoàn toàn sai số dấu phẩy động (Float/Double).

### 3. AI Receipt Scanner (Quét hóa đơn thông minh)
- Tích hợp Google ML Kit Text Recognition v2 chạy hoàn toàn On-Device (ngoại tuyến).
- Trích xuất: Tên cửa hàng, ngày giờ, tổng tiền, thuế VAT, tiền giảm giá và từng món hàng chi tiết (Line Items).
- Giao diện trực quan cho phép chỉnh sửa, thêm bớt món hàng trước khi xác nhận.

### 4. Đối chiếu hóa đơn & Giao dịch ngân hàng (Intelligent Matching)
- Tự động gợi ý ghép hóa đơn với giao dịch ngân hàng tương ứng dựa trên số tiền, thời gian và tên cửa hàng.
- **Tránh tính trùng chi phí**: Khi ghép, chi tiết hóa đơn được liên kết với giao dịch ngân hàng mà không cộng dồn gấp đôi số tiền chi tiêu.

### 5. Phân loại danh mục thông minh (Smart Category Engine)
- Hệ thống phân loại 5 tầng:
  1. **Tầng 1**: Quy tắc cá nhân do người dùng thiết lập (`category_rules`)
  2. **Tầng 2**: Ánh xạ thương hiệu Việt phổ biến (Highlands, Shopee, Grab, WinMart, CGV,...)
  3. **Tầng 3**: Đối soát từ khóa tiếng Việt (cà phê, trà sữa, xăng xe, tiền nhà, học phí,...)
  4. **Tầng 4**: Lịch sử giao dịch & dòng tiền thu nhập (tiền lương, thưởng)
  5. **Tầng 5**: Danh mục mặc định
- Tự động học từ hành vi phân loại của người dùng.

### 6. Tổng kết tài chính cuối ngày (Daily Financial Review)
- Nhắc nhở định kỳ (mặc định 22:00) qua `WorkManager`.
- Màn hình tổng kết nhanh: xem tổng thu, tổng chi, số giao dịch trong ngày và 1-tap phân loại cho các giao dịch chờ duyệt.

### 7. Báo cáo & Phân tích trực quan (Analytics Dashboard)
- Biểu đồ Donut tỷ trọng chi tiêu vẽ trên Compose Canvas gốc mượt mà.
- Biểu đồ Bar so sánh Thu nhập vs Chi tiêu trong tuần.
- Thống kê top điểm chi tiêu và tính toán tỷ lệ tiết kiệm ròng:
  $$\text{Savings Rate} = \frac{\text{Income} - \text{Expense}}{\text{Income}} \times 100\%$$

### 8. Quản lý ngân sách (Budget Management)
- Thiết lập hạn mức chi tiêu hàng tháng theo từng danh mục hoặc toàn hệ thống.
- Cảnh báo trực quan theo các ngưỡng 50%, 80% (Near Limit) và 100% (Over Limit).

### 9. Bảo mật & Quyền riêng tư (Local-First Privacy)
- **Local-First**: Toàn bộ dữ liệu lưu trữ cục bộ trong Room Database trên thiết bị.
- Không yêu cầu tạo tài khoản đám mây.
- Hỗ trợ khóa ứng dụng bằng vân tay / khuôn mặt (Biometric Authentication).
- Xuất dữ liệu ra file CSV và sao lưu/khôi phục toàn diện qua JSON.

---

## 🎨 Design System

Phong cách thiết kế **Modern Fintech Dark UI**:
- **Background**: `#0B0F19`
- **Surface**: `#151B2B`
- **Surface Variant**: `#1E2638`
- **Border**: `#2E384D`
- **Primary**: `#8B5CF6` (Tím Fintech hiện đại)
- **Secondary**: `#A78BFA`
- **Income**: `#22C55E` (Xanh lá tài chính)
- **Expense**: `#F87171` (Đỏ cảnh báo)
- **Warning**: `#F59E0B`
- **Text Primary**: `#F8FAFC`
- **Text Secondary**: `#94A3B8`

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.0.21
- **Target SDK**: Android 15 (API 35), Min SDK 10 (API 29)
- **UI Framework**: Jetpack Compose + Material 3
- **Dependency Injection**: Hilt 2.51.1 (với KSP)
- **Database**: Room 2.6.1 (SQLite local)
- **Async & Reactive**: Kotlin Coroutines + StateFlow
- **Background Work**: WorkManager 2.9.1 + Hilt Work
- **Preferences**: Jetpack DataStore Preferences
- **OCR / AI**: Google ML Kit Text Recognition v2 (`19.0.1`)
- **Biometric**: AndroidX Biometric 1.2.0

---

## 📁 Cấu trúc thư mục

```
com.flowmoney.app
├── data
│   ├── local
│   │   ├── dao             # Room DAOs (Account, Transaction, Category, Receipt, Budget, DailyReview)
│   │   ├── entity          # Room Entities
│   │   ├── FlowMoneyDatabase.kt
│   │   └── Converters.kt
│   ├── model               # Enums & Domain Models
│   └── repository          # Clean Architecture Repositories
├── di                      # Hilt Dependency Injection Modules
├── domain
│   ├── engine              # Smart Transaction, Duplicate, Category, Receipt OCR, Receipt Matcher
│   └── parser              # Extensible Bank Notification Parsers (VCB, MB, TCB, BIDV, Generic)
├── service                 # BankNotificationListenerService & DailyReviewWorker
├── ui
│   ├── components          # Reusable Fintech UI components & Native Compose Charts
│   ├── navigation          # Navigation definitions & routes
│   ├── screens
│   │   ├── home            # Dashboard tab
│   │   ├── transactions    # Transactions tab
│   │   ├── scan            # Receipt Scanner tab
│   │   ├── analytics       # Analytics tab
│   │   ├── settings        # Settings tab
│   │   ├── onboarding      # First launch onboarding flow
│   │   ├── dailyreview     # Daily review flow
│   │   ├── budget          # Budget manager
│   │   └── accounts        # Accounts & initial balance
│   └── theme               # Typography, Colors, Theme
└── util                    # CurrencyUtils (VND formatting), DateUtils, BackupUtils
```

---

## 🚀 Hướng dẫn Build & Chạy

### 1. Yêu cầu môi trường
- JDK 17
- Android SDK 35 (hoặc tương thích)
- Gradle 8.10.2

### 2. Chạy kiểm thử tự động
```bash
./gradlew testDebugUnitTest
```

### 3. Tạo Debug APK
```bash
./gradlew assembleDebug
```
File APK cài đặt được lưu tại:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 Bản quyền
Phát triển bởi đội ngũ FlowMoney.
Mã nguồn phát hành theo giấy phép Apache License 2.0.
