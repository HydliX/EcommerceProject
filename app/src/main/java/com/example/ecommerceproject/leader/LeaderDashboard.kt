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
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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
import java.util.Date
import java.util.Locale
import android.graphics.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
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
                            topProducts = topProducts,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportChartToPdf(context, "Produk Terpopuler", topProducts.map { it["name"].toString() to (it["quantity"] as? Number ?: 0).toInt() }, "bar", snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    val headers = listOf("Peringkat", "Nama Produk", "Jumlah Terjual (Unit)")
                                    val data = topProducts.mapIndexed { index, product ->
                                        listOf(index + 1, product["name"] ?: "N/A", product["quantity"] ?: 0)
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
                            topProducts = topProducts,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportChartToPdf(context, "Produk Trending", topProducts.map { it["name"].toString() to (it["quantity"] as? Number ?: 0).toInt() }, "line", snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    val headers = listOf("Peringkat", "Nama Produk", "Jumlah Terjual (Unit)")
                                    val data = topProducts.mapIndexed { index, product ->
                                        listOf(index + 1, product["name"] ?: "N/A", product["quantity"] ?: 0)
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
    topProducts: List<Map<String, Any>>,
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
                    message = "Belum ada data pembelian produk"
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
    topProducts: List<Map<String, Any>>,
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
                        text = "5 Produk yang Trending",
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
                    message = "Belum ada data pembelian produk"
                )
            } else {
                EnhancedLineChart(topProducts = topProducts)
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

    val maxQuantity = topProducts.maxOfOrNull { (it["quantity"] as? Int) ?: 0 }?.toFloat() ?: 1f
    val barWidth = 60.dp
    val chartHeight = 220.dp
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
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 40.dp.toPx()
            val spacing = (canvasWidth - topProducts.size * barWidth.toPx()) / (topProducts.size + 1)

            topProducts.forEachIndexed { index, product ->
                val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
                val normalizedHeight = if (maxQuantity > 0) (quantity / maxQuantity) * canvasHeight else 0f
                val xPosition = spacing * (index + 1) + barWidth.toPx() * index

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.1f),
                    topLeft = Offset(xPosition + 4.dp.toPx(), canvasHeight - normalizedHeight + 4.dp.toPx()),
                    size = Size(barWidth.toPx(), normalizedHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors[index % colors.size].copy(alpha = 0.9f),
                            colors[index % colors.size].copy(alpha = 0.7f)
                        )
                    ),
                    topLeft = Offset(xPosition, canvasHeight - normalizedHeight),
                    size = Size(barWidth.toPx(), normalizedHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        quantity.toInt().toString(),
                        xPosition + barWidth.toPx() / 2,
                        canvasHeight - normalizedHeight - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 14.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedLineChart(topProducts: List<Map<String, Any>>) {
    if (topProducts.isEmpty()) return

    val maxQuantity = topProducts.maxOfOrNull { (it["quantity"] as? Int) ?: 0 }?.toFloat() ?: 1f
    val chartHeight = 220.dp
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(16.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 40.dp.toPx()
            val pointSpacing = canvasWidth / (topProducts.size - 1).coerceAtLeast(1)
            val points = mutableListOf<Offset>()

            topProducts.forEachIndexed { index, product ->
                val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
                val normalizedHeight = if (maxQuantity > 0) (quantity / maxQuantity) * canvasHeight else 0f
                val xPosition = index * pointSpacing
                points.add(Offset(xPosition, canvasHeight - normalizedHeight))
            }

            val shadowPath = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x + 4.dp.toPx(), point.y + 4.dp.toPx())
                    else lineTo(point.x + 4.dp.toPx(), point.y + 4.dp.toPx())
                }
            }
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.1f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            val linePath = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y)
                    else lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = linePath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors[0].copy(alpha = 0.9f),
                        colors[4].copy(alpha = 0.7f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(canvasWidth, 0f)
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            points.forEachIndexed { index, point ->
                drawCircle(
                    color = Color.Black.copy(alpha = 0.1f),
                    radius = 6.dp.toPx(),
                    center = point + Offset(2.dp.toPx(), 2.dp.toPx())
                )

                drawCircle(
                    color = colors[index % colors.size],
                    radius = 6.dp.toPx(),
                    center = point
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        (topProducts[index]["quantity"] as? Int)?.toString() ?: "0",
                        point.x,
                        point.y - 12.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 14.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                    )
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 32.dp.toPx()
            var startAngle = 0f

            drawCircle(
                color = Color.Black.copy(alpha = 0.1f),
                radius = radius,
                center = center + Offset(4.dp.toPx(), 4.dp.toPx())
            )

            topManagers.forEachIndexed { index, (managerName, quantity) ->
                val sweepAngle = (quantity / totalOrders) * 360f

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

                val midAngle = startAngle + sweepAngle / 2
                val labelRadius = radius * 0.6f
                val labelX = center.x + labelRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
                val labelY = center.y + labelRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        managerName,
                        labelX,
                        labelY,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 12.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
                        }
                    )
                }

                startAngle += sweepAngle
            }
        }
    }
}

// Helper functions for PDF and Excel export

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
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                textSize = 18f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            canvas.drawText("Laporan $title", (595 / 2).toFloat(), 60f, paint)

            when (chartType) {
                "bar" -> drawBarChartOnPdf(canvas, data)
                "line" -> drawLineChartOnPdf(canvas, data)
                "pie" -> drawPieChartOnPdf(canvas, data)
            }

            paint.textAlign = android.graphics.Paint.Align.LEFT
            paint.textSize = 12f
            paint.isFakeBoldText = true
            var yPosition = 450f
            val headers = listOf("Peringkat", "Nama", "Jumlah")
            val columnWidths = floatArrayOf(80f, 315f, 100f)
            var xPosition = 40f
            headers.forEachIndexed { index, header ->
                canvas.drawText(header, xPosition, yPosition, paint)
                xPosition += columnWidths[index]
            }
            yPosition += 25f
            paint.isFakeBoldText = false

            data.forEachIndexed { index, (name, qty) ->
                xPosition = 40f
                canvas.drawText((index + 1).toString(), xPosition, yPosition, paint)
                xPosition += columnWidths[0]
                canvas.drawText(name, xPosition, yPosition, paint)
                xPosition += columnWidths[1]
                canvas.drawText("$qty Unit", xPosition, yPosition, paint)
                yPosition += 20f
            }

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

private fun drawBarChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return

    val paint = android.graphics.Paint()
    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(40f, 100f, 555f, 350f) // Area untuk chart

    val barWidth = (chartRect.width() / data.size) * 0.6f
    val spacing = (chartRect.width() / data.size) * 0.4f

    // 🎨 Warna berbeda untuk setiap bar (loop jika jumlah bar > 5)
    val barColors = listOf(
        android.graphics.Color.parseColor("#6366F1"), // Indigo
        android.graphics.Color.parseColor("#8B5CF6"), // Purple
        android.graphics.Color.parseColor("#06B6D4"), // Cyan
        android.graphics.Color.parseColor("#10B981"), // Green
        android.graphics.Color.parseColor("#F59E0B")  // Yellow
    )

    data.forEachIndexed { index, (name, quantity) ->
        val barHeight = (quantity / maxQuantity) * chartRect.height()
        val left = chartRect.left + (index * (barWidth + spacing))
        val top = chartRect.bottom - barHeight

        paint.color = barColors[index % barColors.size] // 🌈 Ubah warna per bar
        canvas.drawRect(left, top, left + barWidth, chartRect.bottom, paint)

        // Label
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 10f
        canvas.save()
        canvas.rotate(-45f, left + barWidth / 2, chartRect.bottom + 5)
        canvas.drawText(name, left + barWidth / 2, chartRect.bottom + 20, paint)
        canvas.restore()
    }
}

private fun drawLineChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.size < 2) return
    val paint = android.graphics.Paint().apply { strokeWidth = 3f; isAntiAlias = true }
    val maxQuantity = data.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val chartRect = android.graphics.RectF(60f, 100f, 535f, 350f)

    val points = data.mapIndexed { index, (_, quantity) ->
        val x = chartRect.left + (index.toFloat() / (data.size - 1)) * chartRect.width()
        val y = chartRect.bottom - (quantity / maxQuantity) * chartRect.height()
        android.graphics.PointF(x, y)
    }

    paint.color = android.graphics.Color.parseColor("#6366F1") //
    for (i in 0 until points.size - 1) {
        canvas.drawLine(points[i].x, points[i].y, points[i + 1].x, points[i + 1].y, paint)
    }

    points.forEach { point ->
        canvas.drawCircle(point.x, point.y, 6f, paint)
    }
}

private fun drawPieChartOnPdf(canvas: android.graphics.Canvas, data: List<Pair<String, Int>>) {
    if (data.isEmpty()) return
    val paint = android.graphics.Paint().apply { isAntiAlias = true }
    val total = data.sumOf { it.second }.toFloat()
    val chartRect = android.graphics.RectF(150f, 100f, 450f, 400f) // Area untuk pie chart di tengah
    var startAngle = -90f

    val colors = listOf(
        android.graphics.Color.parseColor("#E57373"), // Merah lembut
        android.graphics.Color.parseColor("#4FC3F7"), // Biru muda
        android.graphics.Color.parseColor("#FFB300"), // Oranye terang
        android.graphics.Color.parseColor("#81C784"), // Hijau sejuk
        android.graphics.Color.parseColor("#BA68C8")  // Ungu pastel
    )

    data.forEachIndexed { index, (_, quantity) ->
        val sweepAngle = (quantity / total) * 360f
        paint.color = colors[index % colors.size]
        canvas.drawArc(chartRect, startAngle, sweepAngle, true, paint)
        startAngle += sweepAngle
    }
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

            sheet.setColumnWidth(0, 15 * 256)
            sheet.setColumnWidth(1, 45 * 256)
            sheet.setColumnWidth(2, 20 * 256)

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