package com.flowmoney.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
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

@Composable
fun FlowCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = FlowDarkSurfaceVariant,
    borderColor: Color = FlowDarkBorder,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        content()
    }
}

@Composable
fun FlowAmountText(
    amountMinor: Long,
    direction: TransactionDirection,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier
) {
    val (color, prefix) = when (direction) {
        TransactionDirection.INCOME -> Pair(FlowIncome, "+ ")
        TransactionDirection.EXPENSE -> Pair(FlowExpense, "- ")
        TransactionDirection.TRANSFER -> Pair(FlowTextPrimary, "")
    }

    Text(
        text = "$prefix${CurrencyUtils.formatVnd(amountMinor)}",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}

@Composable
fun CategoryIconBadge(
    categoryName: String,
    colorHex: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val baseColor = try {
        if (!colorHex.isNullOrBlank()) Color(android.graphics.Color.parseColor(colorHex)) else FlowPrimary
    } catch (e: Exception) {
        FlowPrimary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(baseColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = categoryName,
            tint = baseColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun StatusBadge(
    status: ClassificationStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (status) {
        ClassificationStatus.CONFIRMED -> listOf(
            FlowIncome.copy(alpha = 0.15f),
            FlowIncome,
            "Đã phân loại",
            Icons.Default.CheckCircle
        )
        ClassificationStatus.SUGGESTED -> listOf(
            FlowPrimary.copy(alpha = 0.15f),
            FlowPrimary,
            "Gợi ý",
            Icons.Default.Info
        )
        ClassificationStatus.PENDING -> listOf(
            FlowWarning.copy(alpha = 0.15f),
            FlowWarning,
            "Chờ xác nhận",
            Icons.Default.HourglassEmpty
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor as Color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = null,
            tint = textColor as Color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label as String,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FlowDarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = FlowTextSecondary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FlowTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = FlowTextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(16.dp))
            actionButton()
        }
    }
}
