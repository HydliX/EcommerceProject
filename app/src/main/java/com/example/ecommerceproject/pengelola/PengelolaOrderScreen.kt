package com.example.ecommerceproject.pengelola

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper

// REVISI: Pastikan baris import ini ada dan menunjuk ke file CommonComposables.kt Anda
import com.example.ecommerceproject.util.StatusBadge
import com.example.ecommerceproject.util.formatPrice

import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengelolaOrderScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val pengelolaId = dbHelper.CurrentUserId()

    val tabTitles = listOf("Baru", "Dikemas", "Dikirim", "Selesai")

    fun loadOrders() {
        if (pengelolaId == null) {
            errorMessage = "Tidak dapat mengidentifikasi pengelola."
            isLoading = false
            return
        }
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                // REVISI: Menggunakan fungsi baru untuk mendapatkan pesanan yang relevan
                val myOrders = dbHelper.getOrdersForPengelola(pengelolaId)
                orders = myOrders.sortedByDescending { it["createdAt"] as? Long ?: 0L }
            } catch (e: Exception) {
                Log.e("PengelolaOrderScreen", "Error loading orders: ${e.message}", e)
                errorMessage = "Gagal memuat pesanan: ${e.message}"
                snackbarHostState.showSnackbar(errorMessage!!)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(pengelolaId) {
        loadOrders()
    }

    val filteredOrders = remember(selectedTabIndex, orders) {
        when (selectedTabIndex) {
            0 -> orders.filter { it["status"] == DatabaseHelper.OrderStatus.PENDING }
            1 -> orders.filter { it["status"] == DatabaseHelper.OrderStatus.DIKEMAS }
            2 -> orders.filter { it["status"] == DatabaseHelper.OrderStatus.DIKIRIM }
            3 -> orders.filter {
                it["status"] in listOf(
                    DatabaseHelper.OrderStatus.DITERIMA,
                    DatabaseHelper.OrderStatus.SELESAI,
                    DatabaseHelper.OrderStatus.DIBATALKAN
                )
            }
            else -> emptyList()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Pesanan Saya", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (filteredOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada pesanan dalam kategori ini.", textAlign = TextAlign.Center)
                    }

                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it["orderId"] as String }) { order ->
                        PengelolaOrderItem(
                            order = order,
                            onUpdateStatus = { orderId, newStatus ->
                                coroutineScope.launch {
                                    try {
                                        dbHelper.updateOrderStatus(orderId, newStatus)
                                        snackbarHostState.showSnackbar("Status pesanan #...${orderId.takeLast(6)} diperbarui ke $newStatus")
                                        loadOrders() // Refresh list
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PengelolaOrderItem(
    order: Map<String, Any>,
    onUpdateStatus: (String, String) -> Unit
) {
    val orderId = order["orderId"] as? String ?: "N/A"
    val status = order["status"] as? String ?: "N/A"
    val customerProfile = order["customerProfile"] as? Map<String, Any>
    val customerName = customerProfile?.get("username") as? String ?: "Customer"
    val totalPrice = (order["totalPrice"] as? Number)?.toDouble() ?: 0.0
    val createdAt = (order["createdAt"] as? Long) ?: 0L
    val formattedDate = remember {
        SimpleDateFormat("dd MMM kk:mm", Locale.getDefault()).format(Date(createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pesanan #${orderId.takeLast(6)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Oleh: $customerName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                StatusBadge(status = status)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            val items = order["items"] as? Map<String, Map<String, Any>>
            Text(
                text = "${items?.size ?: 0} item • Total: ${formatPrice(totalPrice)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (status) {
                DatabaseHelper.OrderStatus.PENDING -> {
                    Button(
                        onClick = { onUpdateStatus(orderId, DatabaseHelper.OrderStatus.DIKEMAS) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Proses & Kemas Pesanan")
                    }
                }
                DatabaseHelper.OrderStatus.DIKEMAS -> {
                    Button(
                        onClick = { onUpdateStatus(orderId, DatabaseHelper.OrderStatus.DIKIRIM) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Kirim Pesanan")
                    }
                }
                else -> {
                    // No action needed for other statuses from pengelola side
                    Text(
                        "Status pesanan tidak memerlukan tindakan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}