package com.example.ecommerceproject.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.util.StatusBadge
import com.example.ecommerceproject.util.formatPrice
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderHistoryScreen(
    orders: List<Map<String, Any>>,
    navController: NavController
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Riwayat Kosong",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Anda belum memiliki riwayat pesanan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Urutkan pesanan dari yang terbaru
    val sortedOrders = remember(orders) {
        orders.sortedByDescending { it["createdAt"] as? Long ?: 0L }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sortedOrders, key = { it["orderId"] as String }) { order ->
            val orderId = order["orderId"] as? String
            if (orderId != null) {
                OrderItemCard(
                    order = order,
                    onClick = {
                        // Navigasi ke halaman detail pesanan yang baru
                        navController.navigate("orderDetail/$orderId")
                    }
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(order: Map<String, Any>, onClick: () -> Unit) {
    val orderId = order["orderId"] as? String ?: ""
    val status = order["status"] as? String ?: "UNKNOWN"
    val totalPrice = (order["totalPrice"] as? Number)?.toDouble() ?: 0.0
    val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Pesanan #${orderId.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tanggal: ${dateFormat.format(Date(createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = formatPrice(totalPrice),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(status = status)
            }
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = "Lihat Detail",
                tint = Color.Gray
            )
        }
    }
}