package com.example.ecommerceproject

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PimpinanScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // State untuk data statistik
    var totalSoldItems by remember { mutableStateOf(0) }
    var topProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Memuat data statistik
    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                // Ambil semua pesanan
                val orders = dbHelper.getOrders()
                // Hitung total barang terjual dari semua pesanan
                totalSoldItems = orders.sumOf { order ->
                    (order["items"] as? Map<*, *>)?.values?.sumOf { item ->
                        ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                    } ?: 0
                }

                // Ambil top 5 produk terjual
                val productSales = mutableMapOf<String, Pair<String, Int>>()
                orders.forEach { order ->
                    (order["items"] as? Map<*, *>)?.forEach { (productId, item) ->
                        val id = productId.toString()
                        val name = (item as? Map<*, *>)?.get("name")?.toString() ?: "Unknown"
                        val quantity = ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                        if (productSales.containsKey(id)) {
                            val current = productSales[id]!!
                            productSales[id] = Pair(current.first, current.second + quantity)
                        } else {
                            productSales[id] = Pair(name, quantity)
                        }
                    }
                }
                topProducts = productSales.map { (id, pair) ->
                    mapOf("productId" to id, "name" to pair.first, "quantity" to pair.second)
                }.sortedByDescending { it["quantity"] as? Int ?: 0 }.take(5)

            } catch (e: Exception) {
                Log.e("PimpinanScreen", "Gagal memuat statistik: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat statistik: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Dashboard Pimpinan") }) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Total Barang Terjual
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Barang Terjual",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$totalSoldItems Unit",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Top 5 Produk Terjual
                item {
                    Text(
                        text = "Top 5 Produk Terjual",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (topProducts.isEmpty()) {
                        Text(
                            text = "Belum ada data penjualan produk",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
                items(topProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = product["name"]?.toString() ?: "Unknown",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${product["quantity"]} Unit",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}