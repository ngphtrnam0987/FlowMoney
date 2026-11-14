package com.flowmoney.app.domain.engine

import com.flowmoney.app.data.local.dao.CategoryDao
import com.flowmoney.app.data.local.dao.CategoryRuleDao
import com.flowmoney.app.data.local.dao.TransactionDao
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.CategoryRuleEntity
import com.flowmoney.app.data.model.TransactionDirection
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

data class ClassificationResult(
    val categoryId: Long,
    val categoryName: String,
    val confidenceScore: Float,
    val level: Int // 1 to 5
)

@Singleton
class CategoryClassifier @Inject constructor(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val transactionDao: TransactionDao
) {
    // Level 2: Merchant mappings for popular Vietnamese merchants
    private val merchantMappings = mapOf(
        // Ăn uống (Food & Dining)
        "highlands" to "Ăn uống",
        "phuc long" to "Ăn uống",
        "phúc long" to "Ăn uống",
        "the coffee house" to "Ăn uống",
        "starbucks" to "Ăn uống",
        "katinat" to "Ăn uống",
        "mixue" to "Ăn uống",
        "tocotoco" to "Ăn uống",
        "kfc" to "Ăn uống",
        "lotteria" to "Ăn uống",
        "jollibee" to "Ăn uống",
        "mcdonalds" to "Ăn uống",
        "haidilao" to "Ăn uống",
        "golden gate" to "Ăn uống",
        "kichi kichi" to "Ăn uống",
        "gogi" to "Ăn uống",
        "circle k" to "Ăn uống",
        "family mart" to "Ăn uống",
        "gs25" to "Ăn uống",
        "winmart" to "Ăn uống",
        "bach hoa xanh" to "Ăn uống",
        "bách hóa xanh" to "Ăn uống",
        "coopmart" to "Ăn uống",
        "co.opmart" to "Ăn uống",
        "big c" to "Ăn uống",
        "go!" to "Ăn uống",

        // Di chuyển (Transport)
        "grab" to "Di chuyển",
        "be group" to "Di chuyển",
        "be" to "Di chuyển",
        "xanh sm" to "Di chuyển",
        "petrolimex" to "Di chuyển",
        "pvoil" to "Di chuyển",
        "vietnam airlines" to "Di chuyển",
        "vietjet" to "Di chuyển",
        "bamboo airways" to "Di chuyển",

        // Mua sắm (Shopping)
        "shopee" to "Mua sắm",
        "tiki" to "Mua sắm",
        "lazada" to "Mua sắm",
        "tiktok shop" to "Mua sắm",
        "the gioi di dong" to "Mua sắm",
        "thế giới di động" to "Mua sắm",
        "dien may xanh" to "Mua sắm",
        "điện máy xanh" to "Mua sắm",
        "fpt shop" to "Mua sắm",
        "cellphones" to "Mua sắm",
        "uniqlo" to "Mua sắm",
        "zara" to "Mua sắm",
        "h&m" to "Mua sắm",

        // Giải trí (Entertainment)
        "cgv" to "Giải trí",
        "lotte cinema" to "Giải trí",
        "galaxy cinema" to "Giải trí",
        "bhd star" to "Giải trí",
        "netflix" to "Giải trí",
        "spotify" to "Giải trí",
        "youtube" to "Giải trí",
        "steam" to "Giải trí",

        // Hóa đơn (Bills)
        "evn" to "Hóa đơn",
        "dien luc" to "Hóa đơn",
        "điện lực" to "Hóa đơn",
        "cap nuoc" to "Hóa đơn",
        "cấp nước" to "Hóa đơn",
        "viettel" to "Hóa đơn",
        "vinaphone" to "Hóa đơn",
        "mobifone" to "Hóa đơn",
        "vnpt" to "Hóa đơn",
        "fpt telecom" to "Hóa đơn"
    )

    // Level 3: Common Vietnamese keywords
    private val keywordMappings = mapOf(
        "Ăn uống" to listOf("an uong", "ăn uống", "an sang", "an trua", "an toi", "an dem", "ca phe", "cà phê", "cafe", "tra sua", "trà sữa", "com", "cơm", "pho", "phở", "bun", "bún", "banh mi", "bánh mì", "nhau", "nhậu", "buffet", "lau", "lẩu", "nuoc", "nước"),
        "Di chuyển" to listOf("xang", "xăng", "xe om", "xe ôm", "taxi", "grab", "be", "gui xe", "gửi xe", "ve xe", "vé xe", "ve may bay", "vé máy bay", "sua xe", "sửa xe", "rua xe", "rửa xe", "cau duong", "cầu đường", "phi duong bo", "phí đường bộ"),
        "Mua sắm" to listOf("mua sam", "mua sắm", "quan ao", "quần áo", "giay dep", "giày dép", "my pham", "mỹ phẩm", "shopee", "tiki", "lazada", "sieu thi", "siêu thị", "do gia dung", "đồ gia dụng"),
        "Giải trí" to listOf("xem phim", "cinema", "rap", "rạp", "du lich", "du lịch", "khach san", "khách sạn", "karaoke", "game", "choi", "chơi", "netflix", "spotify"),
        "Nhà ở" to listOf("tien nha", "tiền nhà", "thue nha", "thuê nhà", "tro", "trọ", "sua nha", "sửa nhà", "noi that", "nội thất"),
        "Hóa đơn" to listOf("hoa don", "hóa đơn", "dien", "điện", "nuoc", "nước", "internet", "wifi", "dien thoai", "điện thoại", "cuoc", "cước", "nap tien dien thoai", "nạp tiền điện thoại"),
        "Sức khỏe" to listOf("kham benh", "khám bệnh", "thuoc", "thuốc", "nha khoa", "benh vien", "bệnh viện", "gym", "yoga", "the thao", "thể thao"),
        "Giáo dục" to listOf("hoc phi", "học phí", "sach", "sách", "khoa hoc", "khóa học", "dong hoc", "đóng học"),
        "Tiền lương" to listOf("luong", "lương", "salary", "thanh toan luong", "chuyen luong"),
        "Thưởng" to listOf("thuong", "thưởng", "bonus"),
        "Chuyển tiền nội bộ" to listOf("chuyen noi bo", "chuyển nội bộ", "chuyen tien qua lai", "rut tien", "rút tiền")
    )

    suspend fun classify(
        description: String,
        merchantName: String?,
        amountMinor: Long,
        direction: TransactionDirection
    ): ClassificationResult {
        val normDesc = normalize(description)
        val normMerchant = normalize(merchantName ?: "")

        // -------------------------------------------------------------
        // Level 1: User-defined rules
        // -------------------------------------------------------------
        val userRules = categoryRuleDao.getAllRules()
        for (rule in userRules) {
            val matchesKeyword = if (rule.isRegex) {
                try {
                    Regex(rule.keyword, RegexOption.IGNORE_CASE).containsMatchIn(description)
                } catch (e: Exception) {
                    false
                }
            } else {
                normDesc.contains(normalize(rule.keyword)) || (normMerchant.isNotEmpty() && normMerchant.contains(normalize(rule.keyword)))
            }

            val matchesMerchant = rule.merchantNamePattern.isNullOrBlank() ||
                    (normMerchant.isNotEmpty() && normMerchant.contains(normalize(rule.merchantNamePattern)))

            val matchesAmount = (rule.minAmountMinor == null || amountMinor >= rule.minAmountMinor) &&
                    (rule.maxAmountMinor == null || amountMinor <= rule.maxAmountMinor)

            if (matchesKeyword && matchesMerchant && matchesAmount) {
                val cat = categoryDao.getCategoryById(rule.categoryId)
                if (cat != null) {
                    return ClassificationResult(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        confidenceScore = 1.0f,
                        level = 1
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // Level 2: Merchant mapping
        // -------------------------------------------------------------
        for ((merchantKey, categoryName) in merchantMappings) {
            if (normMerchant.contains(merchantKey) || normDesc.contains(merchantKey)) {
                val cat = categoryDao.findByName(categoryName)
                if (cat != null) {
                    return ClassificationResult(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        confidenceScore = 0.95f,
                        level = 2
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // Level 3: Keyword matching
        // -------------------------------------------------------------
        for ((catName, keywords) in keywordMappings) {
            for (kw in keywords) {
                val normKw = normalize(kw)
                if (normDesc.contains(normKw) || normMerchant.contains(normKw)) {
                    val cat = categoryDao.findByName(catName)
                    if (cat != null) {
                        return ClassificationResult(
                            categoryId = cat.id,
                            categoryName = cat.name,
                            confidenceScore = 0.85f,
                            level = 3
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Level 4: Transaction history lookup
        // -------------------------------------------------------------
        // If direction is Income -> suggest Income or Tiền lương
        if (direction == TransactionDirection.INCOME) {
            val salaryCat = categoryDao.findByName("Tiền lương") ?: categoryDao.findByName("Thu nhập")
            if (salaryCat != null) {
                return ClassificationResult(
                    categoryId = salaryCat.id,
                    categoryName = salaryCat.name,
                    confidenceScore = 0.80f,
                    level = 4
                )
            }
        }

        // -------------------------------------------------------------
        // Level 5: Default fallback
        // -------------------------------------------------------------
        val fallbackName = if (direction == TransactionDirection.INCOME) "Tiền lương" else "Khác"
        val fallbackCat = categoryDao.findByName(fallbackName)
            ?: categoryDao.getAllCategories().firstOrNull()

        return ClassificationResult(
            categoryId = fallbackCat?.id ?: 1L,
            categoryName = fallbackCat?.name ?: "Khác",
            confidenceScore = 0.50f,
            level = 5
        )
    }

    /**
     * Learn from user edits: when user manually assigns a category,
     * persist a personal rule if it has a merchant or distinct keyword.
     */
    suspend fun learnFromUserEdit(description: String, merchantName: String?, categoryId: Long) {
        val keyword = when {
            !merchantName.isNullOrBlank() -> merchantName.trim()
            description.length in 3..25 -> description.trim()
            else -> return
        }

        val rule = CategoryRuleEntity(
            categoryId = categoryId,
            keyword = keyword,
            priority = 10
        )
        categoryRuleDao.insertRule(rule)
    }

    private fun normalize(str: String): String {
        val nfdNormalized = Normalizer.normalize(str.lowercase(), Normalizer.Form.NFD)
        val pattern = PatternCompileUtils.combiningMarks
        return pattern.matcher(nfdNormalized).replaceAll("").replace("đ", "d")
    }
}

object PatternCompileUtils {
    val combiningMarks: java.util.regex.Pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
}
