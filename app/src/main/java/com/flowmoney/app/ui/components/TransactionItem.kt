package com.flowmoney.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.ui.theme.FlowPrimary
import com.flowmoney.app.ui.theme.FlowTextPrimary
import com.flowmoney.app.ui.theme.FlowTextSecondary
import com.flowmoney.app.util.DateUtils

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    categoryName: String?,
    categoryColorHex: String?,
    accountName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(
            categoryName = categoryName ?: "Khác",
            colorHex = categoryColorHex,
            size = 44.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchantName ?: transaction.description,
                color = FlowTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${accountName ?: "Tài khoản"} • ${DateUtils.formatRelativeDate(transaction.transactionTime)}",
                    color = FlowTextSecondary,
                    fontSize = 12.sp
                )
                if (transaction.linkedReceiptId != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Hóa đơn đính kèm",
                        tint = FlowPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            FlowAmountText(
                amountMinor = transaction.amountMinor,
                direction = transaction.direction,
                fontSize = 15.sp
            )
            if (transaction.classificationStatus == ClassificationStatus.PENDING) {
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(status = ClassificationStatus.PENDING)
            }
        }
    }
}
