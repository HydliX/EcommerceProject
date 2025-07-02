package com.example.ecommerceproject.leader

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlin.math.cos
import kotlin.math.sin

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
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.shadow(8.dp)
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
        Box(
            modifier = Modifier.fillMaxSize().background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(innerPadding).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(
                                brush = Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            ).padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Dashboard Pimpinan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Kelola & Monitor Bisnis Anda", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                                }
                                Icon(Icons.Default.Analytics, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(visible = contentVisible, enter = slideInVertically(initialOffsetY = { it }) + fadeIn()) {
                        if (localIsLoading) LoadingCard() else EnhancedStatsCard(totalSoldItems = totalSoldItems)
                    }
                }

                item {
                    AnimatedVisibility(visible = contentVisible && !localIsLoading) {
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
                                    val headers = listOf("Peringkat", "Nama Produk", "Jumlah Terjual (Unit)")
                                    val data = topProducts.mapIndexed { index, product ->
                                        listOf(index + 1, product["name"] as String, product["quantity"] as Int)
                                    }
                                    exportToExcel(context, "Produk_Terpopuler", headers, data, snackbarHostState)
                                }
                            }
                        )
                    }
                }

                item {
                    AnimatedVisibility(visible = contentVisible && !localIsLoading) {
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
                                    val headers = listOf("Nama Produk") + dayLabels
                                    val data = topProducts.map { product ->
                                        val productName = product["name"] as String
                                        val dailyQuantities = dayLabels.map { day ->
                                            salesDataByDay[day]?.get(productName) ?: 0
                                        }
                                        listOf(productName) + dailyQuantities
                                    }
                                    exportToExcel(context, "Produk_Trending", headers, data, snackbarHostState)
                                }
                            }
                        )
                    }
                }

                item {
                    AnimatedVisibility(visible = contentVisible && !localIsLoading) {
                        EnhancedTopManagersCard(
                            topManagers = topManagers,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportChartToPdf(context, "Pengelola Terpopuler", topManagers, "pie", snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    val headers = listOf("Peringkat", "Nama Pengelola", "Total Penjualan (Unit)")
                                    val data = topManagers.mapIndexed { index, (name, qty) ->
                                        listOf(index + 1, name, qty)
                                    }
                                    exportToExcel(context, "Pengelola_Terpopuler", headers, data, snackbarHostState)
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
fun LoadingCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Statistik Penjualan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
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
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "Total Item Terjual",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Produk Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                EmptyStateCard(
                    icon = Icons.Default.Inventory,
                    message = "Belum ada data produk terpopuler"
                )
            } else {
                EnhancedBarChart(topProducts = topProducts)
                Spacer(modifier = Modifier.height(8.dp))
                topProducts.forEachIndexed { index, product ->
                    ProductRankCard(
                        rank = index + 1,
                        product = product
                    )
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Tren Produk Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                EmptyStateCard(
                    icon = Icons.Default.Inventory,
                    message = "Belum ada data produk trending"
                )
            } else {
                EnhancedLineChart(topProducts = topProducts, dayLabels = dayLabels, salesDataByDay = salesDataByDay)
                Spacer(modifier = Modifier.height(8.dp))
                topProducts.forEachIndexed { index, product ->
                    ProductRankCard(
                        rank = index + 1,
                        product = product
                    )
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
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Pengelola Terpopuler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                EmptyStateCard(
                    icon = Icons.Default.People,
                    message = "Belum ada data penjualan oleh pengelola"
                )
            } else {
                EnhancedPieChart(topManagers = topManagers)
                Spacer(modifier = Modifier.height(8.dp))
                topManagers.forEachIndexed { index, (managerName, quantity) ->
                    ManagerRankCard(
                        rank = index + 1,
                        managerName = managerName,
                        orderCount = quantity
                    )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
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

@Composable
fun EnhancedBarChart(topProducts: List<Map<String, Any>>) {
    if (topProducts.isEmpty()) return

    val totalQuantity = topProducts.sumOf { it["quantity"] as? Int ?: 0 }.toFloat()
    val barWidth = 40.dp
    val chartHeight = 280.dp
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFF10B981), Color(0xFFF59E0B)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("BarChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(start = 32.dp, end = 16.dp, top = 32.dp, bottom = 48.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val leftPadding = 60.dp.toPx()
            val bottomPadding = 60.dp.toPx()
            val topPadding = 40.dp.toPx()
            val chartAreaWidth = canvasWidth - leftPadding
            val chartAreaHeight = canvasHeight - bottomPadding - topPadding

            val spacing = (chartAreaWidth - topProducts.size * barWidth.toPx()) / (topProducts.size + 1)

            // Draw Y-axis
            drawLine(
                start = Offset(leftPadding, topPadding),
                end = Offset(leftPadding, canvasHeight - bottomPadding),
                color = Color.Black,
                strokeWidth = 2.dp.toPx()
            )

            // Draw X-axis
            drawLine(
                start = Offset(leftPadding, canvasHeight - bottomPadding),
                end = Offset(canvasWidth, canvasHeight - bottomPadding),
                color = Color.Black,
                strokeWidth = 2.dp.toPx()
            )

            // Draw Y-axis labels (quantities)
            val yLabelCount = 5
            val maxLabelValue = (totalQuantity.toInt() / yLabelCount + 1) * yLabelCount
            for (i in 0..yLabelCount) {
                val quantity = (maxLabelValue * i / yLabelCount).toInt()
                val yPosition = canvasHeight - bottomPadding - (quantity / totalQuantity) * chartAreaHeight

                // Draw grid lines
                if (i > 0) {
                    drawLine(
                        start = Offset(leftPadding, yPosition),
                        end = Offset(canvasWidth, yPosition),
                        color = Color.Gray.copy(alpha = 0.3f),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        quantity.toString(),
                        leftPadding - 8.dp.toPx(),
                        yPosition + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 12.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }

            // Draw bars and labels
            topProducts.forEachIndexed { index, product ->
                val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
                val normalizedHeight = if (totalQuantity > 0) (quantity / totalQuantity) * chartAreaHeight else 0f
                val xPosition = leftPadding + spacing * (index + 1) + barWidth.toPx() * index

                // Draw shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.1f),
                    topLeft = Offset(xPosition + 3.dp.toPx(), canvasHeight - bottomPadding - normalizedHeight + 3.dp.toPx()),
                    size = Size(barWidth.toPx(), normalizedHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                // Draw bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors[index % colors.size].copy(alpha = 0.9f),
                            colors[index % colors.size].copy(alpha = 0.7f)
                        )
                    ),
                    topLeft = Offset(xPosition, canvasHeight - bottomPadding - normalizedHeight),
                    size = Size(barWidth.toPx(), normalizedHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                // Draw product name label above bar
                if (normalizedHeight > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            (product["name"] as String).take(8),
                            xPosition + barWidth.toPx() / 2,
                            canvasHeight - bottomPadding - normalizedHeight - 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                        )
                    }
                }

                // Draw product name label below bar
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        (product["name"] as String).take(8),
                        xPosition + barWidth.toPx() / 2,
                        canvasHeight - bottomPadding + 20.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedLineChart(
    topProducts: List<Map<String, Any>>,
    dayLabels: List<String>,
    salesDataByDay: Map<String, Map<String, Int>>
) {
    if (topProducts.isEmpty()) return

    val maxDailyQuantity = dayLabels.flatMap { day ->
        topProducts.map { product ->
            salesDataByDay[day]?.get(product["name"] as String)?.toFloat() ?: 0f
        }
    }.maxOrNull() ?: 1f
    val chartHeight = 280.dp
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF06B6D4),
        Color(0xFF10B981), Color(0xFFF59E0B)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("LineChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .padding(start = 32.dp, end = 16.dp, top = 32.dp, bottom = 48.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val leftPadding = 60.dp.toPx()
                val bottomPadding = 60.dp.toPx()
                val topPadding = 40.dp.toPx()
                val chartAreaWidth = canvasWidth - leftPadding
                val chartAreaHeight = canvasHeight - bottomPadding - topPadding

                val pointSpacing = chartAreaWidth / (dayLabels.size - 1).coerceAtLeast(1)
                val productPoints = topProducts.map { mutableListOf<Offset>() }
                val productMaxPoints = topProducts.map { mutableListOf<Pair<Offset, Float>>() }

                // Prepare points for each product
                dayLabels.forEachIndexed { index, day ->
                    topProducts.forEachIndexed { productIndex, product ->
                        val productName = product["name"] as String
                        val quantity = salesDataByDay[day]?.get(productName)?.toFloat() ?: 0f
                        val normalizedHeight = if (maxDailyQuantity > 0) (quantity / maxDailyQuantity) * chartAreaHeight else 0f
                        val xPosition = leftPadding + index * pointSpacing
                        val point = Offset(xPosition, canvasHeight - bottomPadding - normalizedHeight)
                        productPoints[productIndex].add(point)
                        if (quantity == maxDailyQuantity) {
                            productMaxPoints[productIndex].add(Pair(point, quantity))
                        }
                    }
                }

                // Draw Y-axis
                drawLine(
                    start = Offset(leftPadding, topPadding),
                    end = Offset(leftPadding, canvasHeight - bottomPadding),
                    color = Color.Black,
                    strokeWidth = 2.dp.toPx()
                )

                // Draw X-axis
                drawLine(
                    start = Offset(leftPadding, canvasHeight - bottomPadding),
                    end = Offset(canvasWidth, canvasHeight - bottomPadding),
                    color = Color.Black,
                    strokeWidth = 2.dp.toPx()
                )

                // Draw Y-axis labels and grid
                val yLabelCount = 5
                val maxLabelValue = ((maxDailyQuantity.toInt() / yLabelCount + 1) * yLabelCount).coerceAtLeast(1)
                for (i in 0..yLabelCount) {
                    val quantity = (maxLabelValue * i / yLabelCount).toInt()
                    val yPosition = canvasHeight - bottomPadding - (quantity / maxDailyQuantity) * chartAreaHeight

                    if (i > 0) {
                        drawLine(
                            start = Offset(leftPadding, yPosition),
                            end = Offset(canvasWidth, yPosition),
                            color = Color.Gray.copy(alpha = 0.3f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            quantity.toString(),
                            leftPadding - 8.dp.toPx(),
                            yPosition + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }
                }

                // Draw X-axis labels
                dayLabels.forEachIndexed { index, day ->
                    val xPosition = leftPadding + index * pointSpacing
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            day,
                            xPosition,
                            canvasHeight - bottomPadding + 20.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }

                // Draw smooth lines and points
                topProducts.forEachIndexed { productIndex, product ->
                    val points = productPoints[productIndex]
                    val productName = product["name"] as String

                    if (points.size > 1) {
                        // Draw shadow path
                        val shadowPath = Path().apply {
                            points.forEachIndexed { index, point ->
                                if (index == 0) moveTo(point.x + 3.dp.toPx(), point.y + 3.dp.toPx())
                                else lineTo(point.x + 3.dp.toPx(), point.y + 3.dp.toPx())
                            }
                        }
                        drawPath(
                            path = shadowPath,
                            color = Color.Black.copy(alpha = 0.1f),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw smooth line using quadratic Bezier
                        val linePath = Path().apply {
                            points.forEachIndexed { index, point ->
                                if (index == 0) {
                                    moveTo(point.x, point.y)
                                } else {
                                    val prevPoint = points[index - 1]
                                    val controlX = (prevPoint.x + point.x) / 2
                                    val controlY = prevPoint.y
                                    quadraticBezierTo(controlX, controlY, point.x, point.y)
                                }
                            }
                        }
                        drawPath(
                            path = linePath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colors[productIndex % colors.size].copy(alpha = 0.9f),
                                    colors[productIndex % colors.size].copy(alpha = 0.7f)
                                )
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Draw points and peak annotations
                    points.forEachIndexed { index, point ->
                        // Draw shadow circle
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.1f),
                            radius = 5.dp.toPx(),
                            center = point + Offset(2.dp.toPx(), 2.dp.toPx())
                        )

                        // Draw point
                        drawCircle(
                            color = colors[productIndex % colors.size],
                            radius = 5.dp.toPx(),
                            center = point
                        )
                    }

                    // Draw peak annotations
                    productMaxPoints[productIndex].forEach { (point, quantity) ->
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                "${quantity.toInt()}",
                                point.x,
                                point.y - 15.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                                }
                            )
                        }
                    }
                }
            }

            // Legend
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(topProducts) { index, product ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                colors[index % colors.size].copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    colors[index % colors.size],
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${(product["name"] as String).take(10)}: ${product["quantity"]}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedPieChart(topManagers: List<Pair<String, Int>>) {
    if (topManagers.isEmpty()) return

    val totalOrders = topManagers.sumOf { it.second }.toFloat()
    val colors = listOf(
        Color(0xFFE57373), Color(0xFF4FC3F7), Color(0xFFFFB300),
        Color(0xFF81C784), Color(0xFFBA68C8)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("PieChart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(24.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2 - 40.dp.toPx()).coerceAtLeast(60.dp.toPx())
                var startAngle = -90f

                // Draw shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.1f),
                    radius = radius,
                    center = center + Offset(4.dp.toPx(), 4.dp.toPx())
                )

                topManagers.forEachIndexed { index, (managerName, quantity) ->
                    val percentage = quantity / totalOrders
                    val sweepAngle = percentage * 360f

                    // Draw arc
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors[index % colors.size].copy(alpha = 0.9f),
                                colors[index % colors.size].copy(alpha = 0.7f)
                            ),
                            center = center,
                            radius = radius
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw labels only for significant segments (>5%)
                    if (percentage > 0.05f) {
                        val midAngle = startAngle + sweepAngle / 2
                        val labelRadius = radius * 0.7f
                        val labelX = center.x + labelRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                        val labelY = center.y + labelRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()

                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                if (managerName.length > 8) managerName.take(8) + "..." else managerName,
                                labelX,
                                labelY,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 11.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isFakeBoldText = true
                                    setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                                }
                            )
                        }
                    }

                    startAngle += sweepAngle
                }
            }

            // Legend
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(topManagers) { index, (managerName, quantity) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                colors[index % colors.size].copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    colors[index % colors.size],
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${managerName.take(10)}: $quantity",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private suspend fun exportChartToPdf(
    context: Context,
    title: String,
    data: List<Pair<String, Int>>,
    chartType: String,
    snackbarHostState: SnackbarHostState
) {
    withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Laporan_${title.replace(" ", "")}_$timeStamp.pdf"
        val storageDir = File(context.getExternalFilesDir(null), "Laporan")
        if (!storageDir.exists()) storageDir.mkdirs()
        val file = File(storageDir, fileName)

        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background color
            canvas.drawColor(android.graphics.Color.parseColor("#F8F9FA"))

            // Header with rounded background
            val headerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#6366F1")
                isAntiAlias = true
            }
            val headerRect = android.graphics.RectF(30f, 30f, 565f, 80f)
            canvas.drawRoundRect(headerRect, 16f, 16f, headerPaint)

            // Title
            val titlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Laporan $title", 297.5f, 60f, titlePaint)

            // Chart area with card background
            val cardPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
                setShadowLayer(8f, 2f, 2f, android.graphics.Color.parseColor("#20000000"))
            }
            val cardRect = android.graphics.RectF(40f, 100f, 555f, 420f)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)

            when (chartType) {
                "bar" -> drawEnhancedBarChartOnPdf(canvas, data)
                "line" -> drawEnhancedLineChartOnPdf(canvas, data)
                "pie" -> drawEnhancedPieChartOnPdf(canvas, data)
            }

            // Data table with enhanced styling
            drawEnhancedDataTable(canvas, data, title)

            // Footer
            val footerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#6B7280")
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val currentTime = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
            canvas.drawText("Generated on $currentTime", 297.5f, 820f, footerPaint)

            document.finishPage(page)
            FileOutputStream(file).use { out -> document.writeTo(out) }
            document.close()

            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("PDF berhasil dibuat!")
                openFile(context, file, "application/pdf")
            }
        } catch (e: Exception) {
            Log.e("PdfExport", "Gagal: ${e.message}", e)
            withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Gagal: ${e.message}") }
        }
    }
}

private fun drawEnhancedBarChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return

    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(70f, 130f, 525f, 380f)
    val barColors = listOf(
        android.graphics.Color.parseColor("#6366F1"),
        android.graphics.Color.parseColor("#8B5CF6"),
        android.graphics.Color.parseColor("#06B6D4"),
        android.graphics.Color.parseColor("#10B981"),
        android.graphics.Color.parseColor("#F59E0B")
    )

    // Draw axes
    val axisPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Y-axis
    canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint)
    // X-axis
    canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)

    // Draw grid lines and Y-axis labels
    val yLabelCount = 5
    val maxLabelValue = ((maxQuantity.toInt() / yLabelCount + 1) * yLabelCount).coerceAtLeast(1)
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E5E7EB")
        strokeWidth = 1f
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }

    for (i in 0..yLabelCount) {
        val quantity = (maxLabelValue * i / yLabelCount).toInt()
        val yPosition = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()

        if (i > 0) {
            canvas.drawLine(chartRect.left, yPosition, chartRect.right, yPosition, gridPaint)
        }
        canvas.drawText(quantity.toString(), chartRect.left - 8f, yPosition + 4f, labelPaint)
    }

    // Calculate bar dimensions
    val totalWidth = chartRect.width()
    val barWidth = (totalWidth / data.size) * 0.6f
    val spacing = (totalWidth - (barWidth * data.size)) / (data.size + 1)

    // Draw bars with shadows and gradients
    data.forEachIndexed { index, (name, quantity) ->
        val barHeight = (quantity / maxQuantity) * chartRect.height()
        val left = chartRect.left + spacing + (index * (barWidth + spacing))
        val top = chartRect.bottom - barHeight

        // Shadow
        val shadowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#20000000")
            isAntiAlias = true
        }
        val shadowRect = android.graphics.RectF(left + 3f, top + 3f, left + barWidth + 3f, chartRect.bottom + 3f)
        canvas.drawRoundRect(shadowRect, 8f, 8f, shadowPaint)

        // Gradient effect simulation with multiple colors
        val baseColor = barColors[index % barColors.size]
        val lightColor = adjustColorBrightness(baseColor, 1.2f)

        // Main bar
        val barPaint = android.graphics.Paint().apply {
            color = baseColor
            isAntiAlias = true
        }
        val barRect = android.graphics.RectF(left, top, left + barWidth, chartRect.bottom)
        canvas.drawRoundRect(barRect, 8f, 8f, barPaint)

        // Light gradient overlay
        val gradientPaint = android.graphics.Paint().apply {
            color = lightColor
            alpha = 80
            isAntiAlias = true
        }
        val gradientRect = android.graphics.RectF(left, top, left + barWidth, top + barHeight * 0.3f)
        canvas.drawRoundRect(gradientRect, 8f, 8f, gradientPaint)

        // Value label above bar
        if (barHeight > 0) {
            val valuePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText(quantity.toString(), left + barWidth / 2, top - 8f, valuePaint)
        }

        // Product name label below bar
        val namePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(name.take(8), left + barWidth / 2, chartRect.bottom + 15f, namePaint)
    }
}

private fun drawEnhancedLineChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.size < 2) return

    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(70f, 130f, 525f, 380f)

    // Draw axes
    val axisPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint)
    canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint)

    // Draw grid and Y-axis labels
    val yLabelCount = 5
    val maxLabelValue = ((maxQuantity.toInt() / yLabelCount + 1) * yLabelCount).coerceAtLeast(1)
    val gridPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#E5E7EB")
        strokeWidth = 1f
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        textAlign = android.graphics.Paint.Align.RIGHT
        isAntiAlias = true
    }

    for (i in 0..yLabelCount) {
        val quantity = (maxLabelValue * i / yLabelCount).toInt()
        val yPosition = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()

        if (i > 0) {
            canvas.drawLine(chartRect.left, yPosition, chartRect.right, yPosition, gridPaint)
        }
        canvas.drawText(quantity.toString(), chartRect.left - 8f, yPosition + 4f, labelPaint)
    }

    // Calculate points
    val points = data.mapIndexed { index, (_, quantity) ->
        val x = chartRect.left + (index.toFloat() / (data.size - 1)) * chartRect.width()
        val y = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()
        android.graphics.PointF(x, y)
    }

    // Draw shadow line
    val shadowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#20000000")
        strokeWidth = 4f
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        pathEffect = android.graphics.CornerPathEffect(8f)
    }
    val shadowPath = android.graphics.Path()
    points.forEachIndexed { index, point ->
        if (index == 0) {
            shadowPath.moveTo(point.x + 2f, point.y + 2f)
        } else {
            shadowPath.lineTo(point.x + 2f, point.y + 2f)
        }
    }
    canvas.drawPath(shadowPath, shadowPaint)

    // Draw main line
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#6366F1")
        strokeWidth = 4f
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        pathEffect = android.graphics.CornerPathEffect(8f)
    }
    val linePath = android.graphics.Path()
    points.forEachIndexed { index, point ->
        if (index == 0) {
            linePath.moveTo(point.x, point.y)
        } else {
            linePath.lineTo(point.x, point.y)
        }
    }
    canvas.drawPath(linePath, linePaint)

    // Draw points and labels
    points.forEachIndexed { index, point ->
        // Shadow circle
        val shadowCirclePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#20000000")
            isAntiAlias = true
        }
        canvas.drawCircle(point.x + 2f, point.y + 2f, 6f, shadowCirclePaint)

        // Main circle
        val circlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#6366F1")
            isAntiAlias = true
        }
        canvas.drawCircle(point.x, point.y, 6f, circlePaint)

        // Inner white circle
        val innerCirclePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(point.x, point.y, 3f, innerCirclePaint)

        // Value label
        val valuePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(data[index].second.toString(), point.x, point.y - 15f, valuePaint)

        // X-axis labels
        val xLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(data[index].first.take(8), point.x, chartRect.bottom + 15f, xLabelPaint)
    }
}

private fun drawEnhancedPieChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.second }.toFloat()
    val center = android.graphics.PointF(297.5f, 255f)
    val radius = 100f
    var startAngle = -90f

    val colors = listOf(
        android.graphics.Color.parseColor("#E57373"),
        android.graphics.Color.parseColor("#4FC3F7"),
        android.graphics.Color.parseColor("#FFB300"),
        android.graphics.Color.parseColor("#81C784"),
        android.graphics.Color.parseColor("#BA68C8")
    )

    // Draw shadow
    val shadowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#20000000")
        isAntiAlias = true
    }
    canvas.drawCircle(center.x + 4f, center.y + 4f, radius, shadowPaint)

    data.forEachIndexed { index, (name, quantity) ->
        val percentage = quantity / total
        val sweepAngle = percentage * 360f

        // Main slice
        val slicePaint = android.graphics.Paint().apply {
            color = colors[index % colors.size]
            isAntiAlias = true
        }
        val rect = android.graphics.RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )
        canvas.drawArc(rect, startAngle, sweepAngle, true, slicePaint)

        // Highlight edge
        val highlightPaint = android.graphics.Paint().apply {
            color = adjustColorBrightness(colors[index % colors.size], 1.3f)
            isAntiAlias = true
        }
        val highlightRect = android.graphics.RectF(
            center.x - radius + 5f, center.y - radius + 5f,
            center.x + radius - 5f, center.y + radius - 5f
        )
        canvas.drawArc(highlightRect, startAngle, sweepAngle * 0.3f, true, highlightPaint)

        // Labels for significant segments
        if (percentage > 0.05f) {
            val midAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val labelRadius = radius * 0.7f
            val labelX = center.x + labelRadius * Math.cos(midAngle).toFloat()
            val labelY = center.y + labelRadius * Math.sin(midAngle).toFloat()

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                isAntiAlias = true
                setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
            }

            val displayName = if (name.length > 8) name.take(8) + "..." else name
            canvas.drawText(displayName, labelX, labelY, labelPaint)
        }

        startAngle += sweepAngle
    }
}

private fun drawEnhancedDataTable(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>, title: String) {
    val tableRect = android.graphics.RectF(40f, 440f, 555f, 750f)

    // Table background
    val tableBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        setShadowLayer(4f, 1f, 1f, android.graphics.Color.parseColor("#20000000"))
    }
    canvas.drawRoundRect(tableRect, 12f, 12f, tableBgPaint)

    // Table title
    val tableTitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 16f
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    canvas.drawText("Detail Data $title", 55f, 465f, tableTitlePaint)

    // Header background
    val headerBgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#F3F4F6")
        isAntiAlias = true
    }
    val headerRect = android.graphics.RectF(50f, 475f, 545f, 500f)
    canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)

    // Table headers
    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 12f
        textAlign = android.graphics.Paint.Align.LEFT
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    val headers = listOf("Peringkat", "Nama", "Jumlah", "Persentase")
    val columnPositions = listOf(65f, 140f, 350f, 450f)

    headers.forEachIndexed { index, header ->
        canvas.drawText(header, columnPositions[index], 492f, headerPaint)
    }

    // Table rows
    val rowPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#374151")
        textSize = 11f
        textAlign = android.graphics.Paint.Align.LEFT
        isAntiAlias = true
    }

    val totalQuantity = data.sumOf { it.second }
    var yPosition = 520f

    data.forEachIndexed { index, (name, quantity) ->
        // Alternate row background
        if (index % 2 == 1) {
            val rowBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F9FAFB")
                isAntiAlias = true
            }
            val rowBgRect = android.graphics.RectF(50f, yPosition - 15f, 545f, yPosition + 5f)
            canvas.drawRoundRect(rowBgRect, 4f, 4f, rowBgPaint)
        }

        val percentage = if (totalQuantity > 0) (quantity.toFloat() / totalQuantity * 100) else 0f

        canvas.drawText((index + 1).toString(), columnPositions[0], yPosition, rowPaint)
        canvas.drawText(name, columnPositions[1], yPosition, rowPaint)
        canvas.drawText("$quantity Unit", columnPositions[2], yPosition, rowPaint)
        canvas.drawText("${String.format("%.1f", percentage)}%", columnPositions[3], yPosition, rowPaint)

        yPosition += 20f

        if (yPosition > 730f) return // Prevent overflow
    }
}

private fun adjustColorBrightness(color: Int, factor: Float): Int {
    val red = ((android.graphics.Color.red(color) * factor).toInt().coerceAtMost(255))
    val green = ((android.graphics.Color.green(color) * factor).toInt().coerceAtMost(255))
    val blue = ((android.graphics.Color.blue(color) * factor).toInt().coerceAtMost(255))
    return android.graphics.Color.rgb(red, green, blue)
}

private suspend fun exportToExcel(
    context: Context,
    title: String,
    headers: List<String>,
    data: List<List<Any>>,
    snackbarHostState: SnackbarHostState
) {
    withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Laporan_${title}_$timeStamp.xlsx"
        val storageDir = File(context.getExternalFilesDir(null), "Laporan")
        if (!storageDir.exists()) storageDir.mkdirs()
        val file = File(storageDir, fileName)

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(title)
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                headerRow.createCell(index).setCellValue(header)
            }
            data.forEachIndexed { rowIndex, rowData ->
                val row = sheet.createRow(rowIndex + 1)
                rowData.forEachIndexed { cellIndex, cellData ->
                    when (cellData) {
                        is String -> row.createCell(cellIndex).setCellValue(cellData)
                        is Number -> row.createCell(cellIndex).setCellValue(cellData.toDouble())
                        else -> row.createCell(cellIndex).setCellValue(cellData.toString())
                    }
                }
            }

            sheet.setColumnWidth(0, 25 * 256)
            headers.forEachIndexed { index, _ ->
                if (index > 0) sheet.setColumnWidth(index, 15 * 256)
            }

            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()

            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Excel berhasil dibuat!")
                openFile(
                    context,
                    file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            }
        } catch (e: Exception) {
            Log.e("ExcelExport", "Gagal: ${e.message}", e)
            withContext(Dispatchers.Main) { snackbarHostState.showSnackbar("Gagal mengekspor Excel: ${e.message}") }
        }
    }
}

private fun openFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Buka dengan"))
    } catch (e: Exception) {
        Log.e("OpenFile", "Tidak ada aplikasi untuk membuka file tipe $mimeType", e)
    }
}