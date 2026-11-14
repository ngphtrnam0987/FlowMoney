package com.flowmoney.app.ui.screens.category

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.ui.components.CategoryIconBadge
import com.flowmoney.app.ui.components.FlowCard
import com.flowmoney.app.ui.theme.FlowDarkBackground
import com.flowmoney.app.ui.theme.FlowDarkSurface
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary

@Composable
fun CategoryManagementScreen(
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by categoryRepository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    val rules by categoryRepository.getAllRulesFlow().collectAsState(initial = emptyList())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = FlowDarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Danh mục & Quy tắc phân loại",
                        style = MaterialTheme.typography.titleLarge,
                        color = FlowTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Text(
                    text = "Danh mục chi tiêu & thu nhập (${categories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = FlowTextPrimary
                )
            }

            items(categories, key = { it.id }) { cat ->
                FlowCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = FlowDarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryIconBadge(
                            categoryName = cat.name,
                            colorHex = cat.colorHex,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                color = FlowTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (cat.isIncome) "Khoản thu nhập" else "Khoản chi tiêu",
                                color = FlowTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (rules.isNotEmpty()) {
                item {
                    Text(
                        text = "Quy tắc phân loại cá nhân (${rules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = FlowTextPrimary
                    )
                }

                items(rules, key = { it.id }) { rule ->
                    val cat = categories.firstOrNull { it.id == rule.categoryId }
                    FlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FlowDarkSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Từ khóa: \"${rule.keyword}\"",
                                    color = FlowTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tự động phân loại vào: ${cat?.name ?: "Khác"}",
                                    color = FlowTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
