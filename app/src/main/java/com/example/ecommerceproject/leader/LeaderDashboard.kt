package com.example.ecommerceproject.leader

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    var totalSoldItems by remember { mutableStateOf(0) }
    var topProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var topManagers by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var salesDataByDay by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    val users = dbHelper.getAllUsers()
                    products = dbHelper.getAllProducts()
                    val orders = dbHelper.getAllOrders()
                    salesDataByDay = dbHelper.getSalesDataForPeriod(7)

                    totalSoldItems = orders.sumOf { order ->
                        (order["items"] as? Map<*, *>)?.values?.sumOf { item ->
                            ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                        } ?: 0
                    }
                    val productSales = mutableMapOf<String, Pair<String, Int>>()
                    orders.forEach { order ->
                        (order["items"] as? Map<*, *>)?.forEach { (productId, item) ->
                            val id = productId.toString()
                            val name = (item as? Map<*, *>)?.get("name")?.toString() ?: "Unknown"
                            val quantity = ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                            val current = productSales[id]
                            if (current != null) {
                                productSales[id] = Pair(current.first, current.second + quantity)
                            } else {
                                productSales[id] = Pair(name, quantity)
                            }
                        }
                    }
                    topProducts = productSales.map { (id, pair) ->
                        mapOf("productId" to id, "name" to pair.first, "quantity" to pair.second)
                    }.sortedByDescending { it["quantity"] as? Int ?: 0 }.take(5)

                    val managerSales = mutableMapOf<String, Int>()
                    val productToCreator = products.associate {
                        it["productId"] as String to (it["createdBy"] as? String ?: "Unknown")
                    }
                    orders.forEach { order ->
                        (order["items"] as? Map<*, *>)?.forEach { (productId, item) ->
                            val creatorId = productToCreator[productId.toString()] ?: "Unknown"
                            if (creatorId != "Unknown") {
                                val quantity = ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                                managerSales[creatorId] = (managerSales[creatorId] ?: 0) + quantity
                            }
                        }
                    }
                    val userIdToName = users.associate {
                        it["userId"] as String to (it["username"] as? String ?: "Unknown")
                    }
                    topManagers = managerSales.entries
                        .map { (creatorId, quantity) ->
                            Pair(userIdToName[creatorId] ?: creatorId, quantity)
                        }
                        .filter { it.second > 0 }
                        .sortedByDescending { it.second }
                        .take(5)
                }
                localIsLoading = false
                contentVisible = true
            } catch (e: Exception) {
                localMessage = e.message ?: "Gagal memuat data"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = localMessage, duration = SnackbarDuration.Long)
                }
                localIsLoading = false
            }
        }
    }

    val daysOfWeek = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val calendar = Calendar.getInstance().apply { time = Date() }
    val dayLabels = (0 until 7).map { i ->
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1
        daysOfWeek[dayIndex]
    }.reversed()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.shadow(4.dp)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Beranda", modifier = Modifier.size(24.dp)) },
                    label = { Text("Beranda", fontWeight = FontWeight.Medium) },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profil", modifier = Modifier.size(24.dp)) },
                    label = { Text("Profil", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { navController.navigate("profile") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Pengaturan", modifier = Modifier.size(24.dp)) },
                    label = { Text("Pengaturan", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            item {
                HeaderCard()
            }

            item {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn()
                ) {
                    if (localIsLoading) LoadingCard() else EnhancedStatsCard(totalSoldItems = totalSoldItems)
                }
            }

            item {
                AnimatedVisibility(
                    visible = contentVisible && !localIsLoading
                ) {
                    EnhancedTopProductsCard(
                        salesDataByDay = salesDataByDay,
                        topProducts = topProducts,
                        dayLabels = dayLabels,
                        onExportPdf = {
                            coroutineScope.launch {
                                exportChartToPdf(context, "Produk Terpopuler", topProducts.map { it["name"] as String to (it["quantity"] as Int) }, "bar", snackbarHostState)
                            }
                        },
                        onExportExcel = {
                            coroutineScope.launch {
                                val data = topProducts.map { it["name"] as String to (it["quantity"] as Int) }
                                exportToExcel(context, "Produk_Terpopuler", data, snackbarHostState)
                            }
                        }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = contentVisible && !localIsLoading
                ) {
                    EnhancedTrendingProductsCard(
                        salesDataByDay = salesDataByDay,
                        topProducts = topProducts,
                        dayLabels = dayLabels,
                        onExportPdf = {
                            coroutineScope.launch {
                                exportChartToPdf(context, "Produk Trending", topProducts.map { it["name"] as String to (it["quantity"] as Int) }, "line", snackbarHostState)
                            }
                        },
                        onExportExcel = {
                            coroutineScope.launch {
                                val data = topProducts.map { product ->
                                    val productName = product["name"] as String
                                    val totalQuantity = dayLabels.sumOf { day ->
                                        (salesDataByDay[day]?.get(productName) ?: 0)
                                    }
                                    productName to totalQuantity
                                }
                                exportToExcel(context, "Produk_Trending", data, snackbarHostState)
                            }
                        }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = contentVisible && !localIsLoading
                ) {
                    EnhancedTopManagersCard(
                        topManagers = topManagers,
                        onExportPdf = {
                            coroutineScope.launch {
                                exportChartToPdf(context, "Pengelola Terpopuler", topManagers, "pie", snackbarHostState)
                            }
                        },
                        onExportExcel = {
                            coroutineScope.launch {
                                exportToExcel(context, "Pengelola_Terpopuler", topManagers, snackbarHostState)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Dashboard Pimpinan",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Kelola & Monitor Bisnis Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Memuat data dashboard...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EnhancedStatsCard(totalSoldItems: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Statistik Penjualan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Total Terjual",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Item Terjual",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalSoldItems",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedTopProductsCard(
    salesDataByDay: Map<String, Map<String, Int>>,
    topProducts: List<Map<String, Any>>,
    dayLabels: List<String>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Produk Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onExportExcel) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export to Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (topProducts.isEmpty()) {
                EmptyStateCard(icon = Icons.Default.Inventory, message = "Belum ada data produk terpopuler")
            } else {
                EnhancedBarChart(topProducts = topProducts)
                Spacer(modifier = Modifier.height(12.dp))
                topProducts.forEachIndexed { index, product ->
                    ProductRankCard(rank = index + 1, product = product)
                }
            }
        }
    }
}

@Composable
fun EnhancedTrendingProductsCard(
    salesDataByDay: Map<String, Map<String, Int>>,
    topProducts: List<Map<String, Any>>,
    dayLabels: List<String>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Tren Produk Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onExportExcel) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export to Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (topProducts.isEmpty()) {
                EmptyStateCard(icon = Icons.Default.Inventory, message = "Belum ada data produk trending")
            } else {
                EnhancedLineChart(topProducts = topProducts, dayLabels = dayLabels, salesDataByDay = salesDataByDay)
                Spacer(modifier = Modifier.height(12.dp))
                topProducts.forEachIndexed { index, product ->
                    ProductRankCard(rank = index + 1, product = product)
                }
            }
        }
    }
}

@Composable
fun EnhancedTopManagersCard(
    topManagers: List<Pair<String, Int>>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Pengelola Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export to PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onExportExcel) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Export to Excel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (topManagers.isEmpty()) {
                EmptyStateCard(icon = Icons.Default.People, message = "Belum ada data penjualan oleh pengelola")
            } else {
                EnhancedPieChart(topManagers = topManagers)
                Spacer(modifier = Modifier.height(12.dp))
                topManagers.forEachIndexed { index, (managerName, quantity) ->
                    ManagerRankCard(rank = index + 1, managerName = managerName, orderCount = quantity)
                }
            }
        }
    }
}

@Composable
fun ProductRankCard(rank: Int, product: Map<String, Any>) {
    val rankColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32), // Bronze
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        rankColors.getOrNull(rank - 1) ?: MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product["name"]?.toString() ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Produk Populer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${product["quantity"]} Unit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ManagerRankCard(rank: Int, managerName: String, orderCount: Int) {
    val rankColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32), // Bronze
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        rankColors.getOrNull(rank - 1) ?: MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = managerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pengelola Populer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$orderCount Unit Terjual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EmptyStateCard(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
