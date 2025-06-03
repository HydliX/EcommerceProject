package com.example.ecommerceproject.product

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    orderId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbProduct = DatabaseHelper()
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(orderId) {
        try {
            isLoading = true
            val orders = dbProduct.getOrders()
            order = orders.find { it["orderId"] == orderId }
            if (order == null) {
                snackbarHostState.showSnackbar(
                    message = "Pesanan tidak ditemukan",
                    duration = SnackbarDuration.Long
                )
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                message = "Gagal memuat detail pesanan: ${e.message}",
                duration = SnackbarDuration.Long
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Konfirmasi Pesanan",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Memuat detail pesanan...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (order == null) {
                Text(
                    text = "Pesanan tidak ditemukan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Pesanan Anda telah berhasil ditempatkan!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Order ID: ${order!!["orderId"]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val createdAt = (order!!["createdAt"] as? Number)?.toLong() ?: 0L
                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
                Text(
                    text = "Tanggal Pesanan: ${dateFormat.format(Date(createdAt))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Total: Rp${(order!!["totalPrice"] as? Number)?.toDouble()?.let { String.format("%.2f", it) } ?: "0.00"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Alamat Pengiriman: ${order!!["shippingAddress"]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Metode Pembayaran: ${order!!["paymentMethod"]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    navController.navigate("dashboard") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            ) {
                Text("Kembali ke Beranda")
            }
        }
    }
}