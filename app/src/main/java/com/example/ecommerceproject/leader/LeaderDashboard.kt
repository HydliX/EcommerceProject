package com.example.ecommerceproject.leader

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import org.apache.poi.hssf.usermodel.HSSFWorkbook
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val dbHelper = DatabaseHelper()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    var totalSoldItems by remember { mutableStateOf(0) }
    var topProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var topManagers by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Animated visibility for content
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    val users = dbHelper.getAllUsers()
                    products = dbHelper.getAllProducts()
                    val orders = dbHelper.getAllOrders()

                    // Calculate total sold items
                    totalSoldItems = orders.sumOf { order ->
                        (order["items"] as? Map<*, *>)?.values?.sumOf { item ->
                            ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                        } ?: 0
                    }

                    // Calculate top products
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

                    // Calculate top pengelola based on product sales
                    val managerSales = mutableMapOf<String, Int>()
                    val productToCreator = products.associate {
                        it["productId"] as String to (it["createdBy"] as? String ?: "Unknown")
                    }
                    orders.forEach { order ->
                        (order["items"] as? Map<*, *>)?.forEach { (productId, item) ->
                            val creatorId = productToCreator[productId.toString()] ?: "Unknown"
                            val quantity = ((item as? Map<*, *>)?.get("quantity") as? Number)?.toInt() ?: 0
                            managerSales[creatorId] = (managerSales[creatorId] ?: 0) + quantity
                        }
                    }

                    // Map creator IDs to usernames
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
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Beranda",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Beranda", fontWeight = FontWeight.Medium) },
                    selected = navController.currentDestination?.route == "leader_dashboard",
                    onClick = { navController.navigate("leader_dashboard") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profil",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Profil", fontWeight = FontWeight.Medium) },
                    selected = navController.currentDestination?.route == "profile",
                    onClick = { navController.navigate("profile") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Pengaturan",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Pengaturan", fontWeight = FontWeight.Medium) },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = { navController.navigate("settings") { popUpTo(navController.graph.startDestinationId); launchSingleTop = true } }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Dashboard Pimpinan",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Kelola & Monitor Bisnis Anda",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = contentVisible,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                    ) {
                        if (localIsLoading) {
                            LoadingCard()
                        } else {
                            EnhancedStatsCard(totalSoldItems = totalSoldItems)
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = contentVisible && !localIsLoading,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                    ) {
                        EnhancedTopProductsCard(
                            topProducts = topProducts,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportBarChartToPdf(context, topProducts, snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    exportBarChartToExcel(context, topProducts, snackbarHostState)
                                }
                            }
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = contentVisible && !localIsLoading,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                    ) {
                        EnhancedTrendingProductsCard(
                            topProducts = topProducts,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportLineChartToPdf(context, topProducts, snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    exportLineChartToExcel(context, topProducts, snackbarHostState)
                                }
                            }
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = contentVisible && !localIsLoading,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
                    ) {
                        EnhancedTopManagersCard(
                            topManagers = topManagers,
                            onExportPdf = {
                                coroutineScope.launch {
                                    exportPieChartToPdf(context, topManagers, snackbarHostState)
                                }
                            },
                            onExportExcel = {
                                coroutineScope.launch {
                                    exportPieChartToExcel(context, topManagers, snackbarHostState)
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
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

suspend fun exportBarChartToPdf(
    context: Context,
    topProducts: List<Map<String, Any>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "BarChart_$timeStamp.pdf"
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Title
        paint.textSize = 16f
        paint.isAntiAlias = true
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Produk Terpopuler", 40f, 40f, paint)

        // Chart parameters
        val maxQuantity = topProducts.maxOfOrNull { (it["quantity"] as? Int) ?: 0 }?.toFloat() ?: 1f
        val barWidth = 60f
        val chartHeight = 220f
        val canvasWidth = 595f - 80f // Page width minus margins (40f each side)
        val canvasHeight = chartHeight - 40f
        val spacing = (canvasWidth - topProducts.size * barWidth) / (topProducts.size + 1)
        val colors = listOf(
            android.graphics.Color.parseColor("#6366F1"),
            android.graphics.Color.parseColor("#8B5CF6"),
            android.graphics.Color.parseColor("#06B6D4"),
            android.graphics.Color.parseColor("#10B981"),
            android.graphics.Color.parseColor("#F59E0B")
        )

        // Draw bars
        paint.style = android.graphics.Paint.Style.FILL
        topProducts.forEachIndexed { index, product ->
            val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
            val normalizedHeight = if (maxQuantity > 0) (quantity / maxQuantity) * canvasHeight else 0f
            val xPosition = 40f + spacing * (index + 1) + barWidth * index

            // Draw bar
            paint.color = colors[index % colors.size]
            canvas.drawRect(
                xPosition,
                80f + canvasHeight - normalizedHeight,
                xPosition + barWidth,
                80f + canvasHeight,
                paint
            )

            // Draw quantity text
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 12f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText(
                quantity.toInt().toString(),
                xPosition + barWidth / 2,
                80f + canvasHeight - normalizedHeight - 8f,
                paint
            )
        }

        // Draw product list below chart
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 12f
        var yPosition = 80f + chartHeight + 40f
        topProducts.forEachIndexed { index, product ->
            val name = product["name"]?.toString() ?: "Unknown"
            val quantity = product["quantity"] as? Int ?: 0
            canvas.drawText("${index + 1}. $name: $quantity Unit", 40f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
            output.flush()
        }
        document.close()
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("PDF disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor PDF: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun exportLineChartToPdf(
    context: Context,
    topProducts: List<Map<String, Any>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LineChart_$timeStamp.pdf"
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Title
        paint.textSize = 16f
        paint.isAntiAlias = true
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("5 Produk yang Trending", 40f, 40f, paint)

        // Chart parameters
        val maxQuantity = topProducts.maxOfOrNull { (it["quantity"] as? Int) ?: 0 }?.toFloat() ?: 1f
        val chartHeight = 220f
        val canvasWidth = 595f - 80f // Page width minus margins
        val canvasHeight = chartHeight - 40f
        val pointSpacing = canvasWidth / (topProducts.size - 1).coerceAtLeast(1)
        val lineColor = android.graphics.Color.parseColor("#6366F1")
        val points = mutableListOf<android.graphics.PointF>()

        // Calculate points
        topProducts.forEachIndexed { index, product ->
            val quantity = (product["quantity"] as? Int)?.toFloat() ?: 0f
            val normalizedHeight = if (maxQuantity > 0) (quantity / maxQuantity) * canvasHeight else 0f
            val xPosition = 40f + index * pointSpacing
            points.add(android.graphics.PointF(xPosition, 80f + canvasHeight - normalizedHeight))
        }

        // Draw line
        paint.color = lineColor
        paint.strokeWidth = 4f
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeCap = android.graphics.Paint.Cap.ROUND
        paint.strokeJoin = android.graphics.Paint.Join.ROUND
        for (i in 0 until points.size - 1) {
            canvas.drawLine(
                points[i].x, points[i].y,
                points[i + 1].x, points[i + 1].y,
                paint
            )
        }

        // Draw points and labels
        paint.style = android.graphics.Paint.Style.FILL
        points.forEachIndexed { index, point ->
            // Draw point
            canvas.drawCircle(point.x, point.y, 6f, paint)

            // Draw quantity text
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 12f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText(
                (topProducts[index]["quantity"] as? Int)?.toString() ?: "0",
                point.x,
                point.y - 12f,
                paint
            )
        }

        // Draw product list below chart
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 12f
        var yPosition = 80f + chartHeight + 40f
        topProducts.forEachIndexed { index, product ->
            val name = product["name"]?.toString() ?: "Unknown"
            val quantity = product["quantity"] as? Int ?: 0
            canvas.drawText("${index + 1}. $name: $quantity Unit", 40f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
            output.flush()
        }
        document.close()
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("PDF disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor PDF: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun exportPieChartToPdf(
    context: Context,
    topManagers: List<Pair<String, Int>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PieChart_$timeStamp.pdf"
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Title
        paint.textSize = 16f
        paint.isAntiAlias = true
        paint.color = android.graphics.Color.BLACK
        canvas.drawText("Pengelola Terpopuler", 40f, 40f, paint)

        // Chart parameters
        val totalOrders = topManagers.sumOf { it.second }.toFloat()
        val centerX = 595f / 2
        val centerY = 80f + 220f / 2
        val radius = minOf(595f, 220f) / 2 - 32f
        val colors = listOf(
            android.graphics.Color.parseColor("#E57373"),
            android.graphics.Color.parseColor("#4FC3F7"),
            android.graphics.Color.parseColor("#FFB300"),
            android.graphics.Color.parseColor("#81C784"),
            android.graphics.Color.parseColor("#BA68C8")
        )
        var startAngle = 0f

        // Draw pie slices
        paint.style = android.graphics.Paint.Style.FILL
        topManagers.forEachIndexed { index, (managerName, quantity) ->
            val sweepAngle = (quantity / totalOrders) * 360f
            paint.color = colors[index % colors.size]
            canvas.drawArc(
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                startAngle, sweepAngle, true, paint
            )

            // Draw label
            val midAngle = startAngle + sweepAngle / 2
            val labelRadius = radius * 1.2f // Place labels outside the pie
            val labelX = centerX + labelRadius * cos(Math.toRadians(midAngle.toDouble())).toFloat()
            val labelY = centerY + labelRadius * sin(Math.toRadians(midAngle.toDouble())).toFloat()
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 10f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText(managerName, labelX, labelY, paint)

            startAngle += sweepAngle
        }

        // Draw manager list below chart
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 12f
        var yPosition = 80f + 220f + 40f
        topManagers.forEachIndexed { index, (managerName, quantity) ->
            canvas.drawText("${index + 1}. $managerName: $quantity Unit", 40f, yPosition, paint)
            yPosition += 20f
        }

        document.finishPage(page)
        FileOutputStream(file).use { output ->
            document.writeTo(output)
            output.flush()
        }
        document.close()
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("PDF disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor PDF: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun exportBarChartToExcel(
    context: Context,
    topProducts: List<Map<String, Any>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "BarChart_$timeStamp.xls" // Using .xls for HSSF
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)

        // Use HSSFWorkbook for .xls
        val workbook = HSSFWorkbook()
        // Alternative: Use XSSFWorkbook for .xlsx (uncomment to test)
        // val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Produk Terpopuler")

        // Log input data for debugging
        Log.d("ExcelExport", "topProducts size: ${topProducts.size}")
        topProducts.forEachIndexed { index, product ->
            Log.d("ExcelExport", "Product $index: name=${product["name"]}, quantity=${product["quantity"]}")
        }

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Rank")
        headerRow.createCell(1).setCellValue("Product Name")
        headerRow.createCell(2).setCellValue("Quantity")

        // Validate and populate data rows
        if (topProducts.isEmpty()) {
            throw Exception("Data produk kosong, tidak dapat membuat Excel")
        }
        topProducts.forEachIndexed { index, product ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            val productName = product["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown"
            row.createCell(1).setCellValue(productName)
            val quantity = when (val qty = product["quantity"]) {
                is Number -> qty.toDouble()
                else -> {
                    Log.w("ExcelExport", "Invalid quantity for product $productName: $qty")
                    0.0
                }
            }
            row.createCell(2).setCellValue(quantity)
        }

        // Set fixed column widths (in units of 1/256th of a character width)
        sheet.setColumnWidth(0, 10 * 256) // Rank
        sheet.setColumnWidth(1, 30 * 256) // Product Name
        sheet.setColumnWidth(2, 15 * 256) // Quantity

        // Write to file with logging
        FileOutputStream(file).use { output ->
            workbook.write(output)
            output.flush()
            Log.d("ExcelExport", "Wrote ${file.length()} bytes to ${file.absolutePath}")
        }
        workbook.close()

        // Verify file exists and has reasonable size
        if (!file.exists()) {
            throw Exception("File Excel gagal dibuat")
        }
        if (file.length() < 1024) {
            Log.w("ExcelExport", "File size is only ${file.length()} bytes, may be corrupt")
            throw Exception("File Excel terlalu kecil, mungkin korup")
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Excel disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("ExcelExport", "Export failed: ${e.message}", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor Excel: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun exportLineChartToExcel(
    context: Context,
    topProducts: List<Map<String, Any>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LineChart_$timeStamp.xls" // Using .xls for HSSF
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)

        // Use HSSFWorkbook for .xls
        val workbook = HSSFWorkbook()
        // Alternative: Use XSSFWorkbook for .xlsx (uncomment to test)
        // val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("5 Produk yang Trending")

        // Log input data for debugging
        Log.d("ExcelExport", "topProducts size: ${topProducts.size}")
        topProducts.forEachIndexed { index, product ->
            Log.d("ExcelExport", "Product $index: name=${product["name"]}, quantity=${product["quantity"]}")
        }

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Rank")
        headerRow.createCell(1).setCellValue("Product Name")
        headerRow.createCell(2).setCellValue("Quantity")

        // Validate and populate data rows
        if (topProducts.isEmpty()) {
            throw Exception("Data produk kosong, tidak dapat membuat Excel")
        }
        topProducts.forEachIndexed { index, product ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            val productName = product["name"]?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown"
            row.createCell(1).setCellValue(productName)
            val quantity = when (val qty = product["quantity"]) {
                is Number -> qty.toDouble()
                else -> {
                    Log.w("ExcelExport", "Invalid quantity for product $productName: $qty")
                    0.0
                }
            }
            row.createCell(2).setCellValue(quantity)
        }

        // Set fixed column widths (in units of 1/256th of a character width)
        sheet.setColumnWidth(0, 10 * 256) // Rank
        sheet.setColumnWidth(1, 30 * 256) // Product Name
        sheet.setColumnWidth(2, 15 * 256) // Quantity

        // Write to file with logging
        FileOutputStream(file).use { output ->
            workbook.write(output)
            output.flush()
            Log.d("ExcelExport", "Wrote ${file.length()} bytes to ${file.absolutePath}")
        }
        workbook.close()

        // Verify file exists and has reasonable size
        if (!file.exists()) {
            throw Exception("File Excel gagal dibuat")
        }
        if (file.length() < 1024) {
            Log.w("ExcelExport", "File size is only ${file.length()} bytes, may be corrupt")
            throw Exception("File Excel terlalu kecil, mungkin korup")
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Excel disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("ExcelExport", "Export failed: ${e.message}", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor Excel: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun exportPieChartToExcel(
    context: Context,
    topManagers: List<Pair<String, Int>>,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "PieChart_$timeStamp.xls" // Using .xls for HSSF
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)

        // Use HSSFWorkbook for .xls
        val workbook = HSSFWorkbook()
        // Alternative: Use XSSFWorkbook for .xlsx (uncomment to test)
        // val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Pengelola Terpopuler")

        // Log input data for debugging
        Log.d("ExcelExport", "topManagers size: ${topManagers.size}")
        topManagers.forEachIndexed { index, (name, qty) ->
            Log.d("ExcelExport", "Manager $index: name=$name, quantity=$qty")
        }

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Rank")
        headerRow.createCell(1).setCellValue("Manager Name")
        headerRow.createCell(2).setCellValue("Quantity")

        // Validate and populate data rows
        if (topManagers.isEmpty()) {
            throw Exception("Data pengelola kosong, tidak dapat membuat Excel")
        }
        topManagers.forEachIndexed { index, (managerName, quantity) ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue((index + 1).toDouble())
            val safeName = managerName?.takeIf { it.isNotBlank() } ?: "Unknown"
            row.createCell(1).setCellValue(safeName)
            row.createCell(2).setCellValue(quantity.toDouble())
        }

        // Set fixed column widths (in units of 1/256th of a character width)
        sheet.setColumnWidth(0, 10 * 256) // Rank
        sheet.setColumnWidth(1, 30 * 256) // Manager Name
        sheet.setColumnWidth(2, 15 * 256) // Quantity

        // Write to file with logging
        FileOutputStream(file).use { output ->
            workbook.write(output)
            output.flush()
            Log.d("ExcelExport", "Wrote ${file.length()} bytes to ${file.absolutePath}")
        }
        workbook.close()

        // Verify file exists and has reasonable size
        if (!file.exists()) {
            throw Exception("File Excel gagal dibuat")
        }
        if (file.length() < 1024) {
            Log.w("ExcelExport", "File size is only ${file.length()} bytes, may be corrupt")
            throw Exception("File Excel terlalu kecil, mungkin korup")
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Excel disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("ExcelExport", "Export failed: ${e.message}", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor Excel: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}

suspend fun testExportSimpleExcel(
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "TestExcel_$timeStamp.xls" // Using .xls for HSSF
        val storageDir = context.getExternalFilesDir(null)
        if (storageDir != null && !storageDir.exists()) {
            val created = storageDir.mkdirs()
            if (!created) throw Exception("Gagal membuat direktori penyimpanan")
        }
        if (storageDir == null || !storageDir.canWrite()) {
            throw Exception("Direktori penyimpanan tidak dapat ditulis")
        }
        val file = File(storageDir, fileName)

        // Use HSSFWorkbook for .xls
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Test Sheet")

        // Create a simple row
        val row = sheet.createRow(0)
        row.createCell(0).setCellValue("Test")
        row.createCell(1).setCellValue(123.0)
        row.createCell(2).setCellValue("Hello World")

        // Set fixed column widths
        sheet.setColumnWidth(0, 10 * 256)
        sheet.setColumnWidth(1, 10 * 256)
        sheet.setColumnWidth(2, 20 * 256)

        // Write to file with logging
        FileOutputStream(file).use { output ->
            workbook.write(output)
            output.flush()
            Log.d("ExcelExport", "Wrote ${file.length()} bytes to ${file.absolutePath}")
        }
        workbook.close()

        // Verify file exists and has reasonable size
        if (!file.exists()) {
            throw Exception("File Excel gagal dibuat")
        }
        if (file.length() < 1024) {
            Log.w("ExcelExport", "File size is only ${file.length()} bytes, may be corrupt")
            throw Exception("File Excel terlalu kecil, mungkin korup")
        }

        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Test Excel disimpan di ${file.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("ExcelExport", "Test export failed: ${e.message}", e)
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Gagal mengekspor Test Excel: ${e.message ?: "Kesalahan tidak diketahui"}")
        }
    }
}