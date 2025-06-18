package com.example.ecommerceproject

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PimpinanScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // State untuk data statistik
    var totalSoldItems by remember { mutableStateOf(0) }
    var topProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var topPengelola by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Warna acak untuk diagram
    val colors = listOf(
        Color(0xFF6200EE), Color(0xFF03DAC5), Color(0xFFFF5722),
        Color(0xFF3F51B5), Color(0xFFE91E63)
    )

    // Memuat data statistik
    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                // Ambil semua pesanan
                val orders = dbHelper.getAllOrders() // Gunakan getAllOrders untuk pimpinan
                // Hitung total barang terjual
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

                // Ambil top 5 pengelola populer (berdasarkan jumlah pesanan)
                val pengelolaSales = mutableMapOf<String, Pair<String, Int>>()
                orders.forEach { order ->
                    val pengelolaId = (order["pengelolaId"] as? String) ?: return@forEach
                    val userProfile = dbHelper.getUserProfile(pengelolaId, true)
                    val pengelolaName = userProfile?.get("username")?.toString() ?: "Unknown"
                    if (pengelolaSales.containsKey(pengelolaId)) {
                        val current = pengelolaSales[pengelolaId]!!
                        pengelolaSales[pengelolaId] = Pair(pengelolaName, current.second + 1)
                    } else {
                        pengelolaSales[pengelolaId] = Pair(pengelolaName, 1)
                    }
                }
                topPengelola = pengelolaSales.map { (id, pair) ->
                    mapOf("pengelolaId" to id, "name" to pair.first, "orderCount" to pair.second)
                }.sortedByDescending { it["orderCount"] as? Int ?: 0 }.take(5)

            } catch (e: Exception) {
                Log.e("PimpinanScreen", "Gagal memuat statistik: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat statistik: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Fungsi untuk menggambar diagram lingkaran
    @Composable
    fun PieChart(
        data: List<Pair<String, Float>>,
        modifier: Modifier = Modifier,
        colors: List<Color>
    ) {
        Canvas(modifier = modifier.size(200.dp)) {
            val total = data.sumOf { it.second.toDouble() }.toFloat()
            var startAngle = 0f

            data.forEachIndexed { index, (name, value) ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }

        // Legenda
        Column(modifier = Modifier.padding(start = 16.dp)) {
            data.forEachIndexed { index, (name, _) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index % colors.size])
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp
                    )
                }
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

                // Diagram Produk Terjual
                item {
                    Text(
                        text = "Distribusi Penjualan Produk",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (topProducts.isEmpty()) {
                        Text(
                            text = "Belum ada data penjualan produk",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PieChart(
                                data = topProducts.map { product ->
                                    Pair(
                                        product["name"]?.toString() ?: "Unknown",
                                        (product["quantity"] as? Int)?.toFloat() ?: 0f
                                    )
                                },
                                colors = colors
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

                // Diagram Pengelola Populer
                item {
                    Text(
                        text = "Pengelola Populer",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    if (topPengelola.isEmpty()) {
                        Text(
                            text = "Belum ada data pengelola",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PieChart(
                                data = topPengelola.map { pengelola ->
                                    Pair(
                                        pengelola["name"]?.toString() ?: "Unknown",
                                        (pengelola["orderCount"] as? Int)?.toFloat() ?: 0f
                                    )
                                },
                                colors = colors
                            )
                        }
                    }
                }

                // Top 5 Pengelola Populer
                item {
                    Text(
                        text = "Top 5 Pengelola Berdasarkan Pesanan",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(topPengelola) { pengelola ->
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
                                text = pengelola["name"]?.toString() ?: "Unknown",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${pengelola["orderCount"]} Pesanan",
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