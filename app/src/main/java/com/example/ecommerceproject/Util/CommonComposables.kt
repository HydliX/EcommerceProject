package com.example.ecommerceproject.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ecommerceproject.DatabaseHelper
import java.text.NumberFormat
import java.util.*

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (backgroundColor, contentColor) = when (status) {
        DatabaseHelper.OrderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        DatabaseHelper.OrderStatus.DIKEMAS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        DatabaseHelper.OrderStatus.DIKIRIM -> Color(0xFF1E88E5).copy(alpha = 0.2f) to Color(0xFF0D47A1) // Blue
        DatabaseHelper.OrderStatus.DITERIMA -> Color(0xFF43A047).copy(alpha = 0.2f) to Color(0xFF1B5E20) // Green
        DatabaseHelper.OrderStatus.SELESAI -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        DatabaseHelper.OrderStatus.DIBATALKAN -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> Color.Gray.copy(alpha = 0.2f) to Color.DarkGray
    }

    Text(
        text = status,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = contentColor,
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.labelMedium
    )
}

fun formatPrice(price: Double): String {
    val localeID = Locale("in", "ID")
    val numberFormat = NumberFormat.getCurrencyInstance(localeID)
    numberFormat.maximumFractionDigits = 0
    return numberFormat.format(price)
}