package com.flowmoney.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.ui.theme.FlowDarkBorder
import com.flowmoney.app.ui.theme.FlowExpense
import com.flowmoney.app.ui.theme.FlowIncome
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.CurrencyUtils

data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color
)

@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    totalAmountMinor: Long,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 36f
) {
    if (slices.isEmpty() || totalAmountMinor <= 0L) {
        Box(
            modifier = modifier.height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có dữ liệu chi tiêu",
                color = FlowTextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.size(180.dp)) {
                var currentAngle = -90f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arcSize = Size(diameter, diameter)

                for (slice in slices) {
                    val sweepAngle = (slice.value.toFloat() / totalAmountMinor) * 360f * animatedProgress.value
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = slice.color,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        currentAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tổng chi",
                    color = FlowTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = CurrencyUtils.formatVnd(totalAmountMinor),
                    color = FlowTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            slices.take(5).forEach { slice ->
                val percentage = if (totalAmountMinor > 0) {
                    (slice.value.toFloat() / totalAmountMinor * 100f)
                } else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.label,
                            color = FlowTextPrimary,
                            fontSize = 13.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = CurrencyUtils.formatVnd(slice.value),
                            color = FlowTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "%.1f%%".format(percentage),
                            color = FlowTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

data class BarData(
    val label: String,
    val incomeMinor: Long,
    val expenseMinor: Long
)

@Composable
fun IncomeExpenseBarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.height(160.dp), contentAlignment = Alignment.Center) {
            Text("Chưa có dữ liệu", color = FlowTextSecondary, fontSize = 13.sp)
        }
        return
    }

    val maxVal = data.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            for (item in data) {
                val incomeHeightRatio = (item.incomeMinor.toFloat() / maxVal).coerceIn(0.05f, 1f)
                val expenseHeightRatio = (item.expenseMinor.toFloat() / maxVal).coerceIn(0.05f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(120.dp)
                    ) {
                        // Income Bar (Green)
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height((110 * incomeHeightRatio).dp)
                                .clip(CircleShape)
                                .background(if (item.incomeMinor > 0) FlowIncome else FlowDarkBorder)
                        )
                        // Expense Bar (Red)
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height((110 * expenseHeightRatio).dp)
                                .clip(CircleShape)
                                .background(if (item.expenseMinor > 0) FlowExpense else FlowDarkBorder)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.label,
                        color = FlowTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FlowIncome))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Thu nhập", color = FlowTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FlowExpense))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Chi tiêu", color = FlowTextSecondary, fontSize = 11.sp)
        }
    }
}
