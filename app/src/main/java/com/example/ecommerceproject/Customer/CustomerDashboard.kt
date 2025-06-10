package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    snackbarHostState: SnackbarHostState
) {
    // Dependencies
    val auth = FirebaseAuth.getInstance()
    val dbProduct = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // States
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wishlist by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var selectedTab by remember { mutableStateOf(0) }
    val username = userProfile?.get("name") as? String ?: "Customer"
    var cartItemCount by remember { mutableStateOf(0) }

    // Data Loading Logic
    fun loadData(forceReload: Boolean = false) {
        coroutineScope.launch {
            if (!forceReload && !isLoading) return@launch
            isLoading = true
            hasError = false
            try {
                auth.currentUser?.reload()?.await()
                isEmailVerified = auth.currentUser?.isEmailVerified == true

                if (!isEmailVerified) {
                    throw IllegalStateException("Silakan verifikasi email Anda untuk mengakses dashboard.")
                }

                dbProduct.ensureUserProfile()
                products = dbProduct.getAllProducts()
                wishlist = dbProduct.getWishlist()
                orders = dbProduct.getOrders()
                cartItemCount = dbProduct.getCart().size
            } catch (e: Exception) {
                hasError = true
                errorMessage = e.message ?: "Gagal memuat data."
                Log.e("CustomerDashboard", "Error loading data: $errorMessage", e)
                snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Long)
            } finally {
                isLoading = false
            }
        }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        loadData(forceReload = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary // Judul warna ungu
                    )
                },
                actions = {
                    // Ikon keranjang dengan badge di pojok kanan atas
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge { Text("$cartItemCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Keranjang",
                            )
                        }
                    }
                },
                // Membuat TopAppBar transparan
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    selected = navController.currentDestination?.route == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = navController.currentDestination?.route == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Selamat datang, $username!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isEmailVerified) {
                EmailVerificationCard(auth, snackbarHostState, coroutineScope)
            } else {
                CustomerTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isLoading = isLoading,
                    hasError = hasError,
                    errorMessage = errorMessage,
                    products = products,
                    wishlist = wishlist,
                    orders = orders,
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    dbProduct = dbProduct,
                    onRefreshData = { loadData(forceReload = true) },
                    onCartUpdate = { newCount -> cartItemCount = newCount }
                )
            }
        }
    }
}
@Composable
private fun CustomerTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isLoading: Boolean,
    hasError: Boolean,
    errorMessage: String,
    products: List<Map<String, Any>>,
    wishlist: List<Map<String, Any>>,
    orders: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    dbProduct: DatabaseHelper,
    onRefreshData: () -> Unit,
    onCartUpdate: (Int) -> Unit
) {
    val tabs = listOf("Produk", "Wishlist", "Pesanan")

    TabRow(selectedTabIndex = selectedTab) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (hasError) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRefreshData) {
                    Text("Coba Lagi")
                }
            }
        }
    } else {
        when (selectedTab) {
            0 -> ProductGridScreen(products, wishlist, navController, snackbarHostState, coroutineScope, dbProduct, onRefreshData, onCartUpdate)
            1 -> WishlistScreen(wishlist, navController, snackbarHostState, coroutineScope, dbProduct, onRefreshData)
            2 -> OrderHistoryScreen(orders, navController)
        }
    }
}

@Composable
fun ProductGridScreen(
    products: List<Map<String, Any>>,
    wishlist: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    db: DatabaseHelper,
    onRefresh: () -> Unit,
    onCartUpdate: (Int) -> Unit
) {
    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada produk ditemukan.")
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it["productId"] as String }) { product ->
            val productId = product["productId"] as String
            val isInWishlist = wishlist.any { it["productId"] == productId }

            ProductGridItem(
                product = product,
                isInWishlist = isInWishlist,
                onProductClick = { navController.navigate("productDetail/$productId") },
                onAddToCartClick = {
                    scope.launch {
                        try {
                            // --- PERUBAHAN BACKEND DI SINI ---
                            // Langsung memanggil db.addToCart dengan kuantitas 1
                            db.addToCart(productId, 1)
                            onCartUpdate(db.getCart().size)
                            snackbarHostState.showSnackbar("Produk ditambahkan ke keranjang")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal: ${e.message}")
                        }
                    }
                },
                onWishlistClick = {
                    scope.launch {
                        try {
                            if (isInWishlist) {
                                db.removeFromWishlist(productId)
                                snackbarHostState.showSnackbar("Dihapus dari Wishlist")
                            } else {
                                db.addToWishlist(productId)
                                snackbarHostState.showSnackbar("Ditambahkan ke Wishlist")
                            }
                            onRefresh()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProductGridItem(
    product: Map<String, Any>,
    isInWishlist: Boolean,
    onProductClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    onWishlistClick: () -> Unit
) {
    val name = product["name"] as? String ?: "N/A"
    val price = (product["price"] as? Number)?.toDouble()?.let {
        String.format(Locale("id", "ID"), "%,.0f", it)
    } ?: "0"
    val imageUrl = product["imageUrl"]?.toString()

    Card(
        modifier = Modifier.clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl.takeIf { !it.isNullOrEmpty() } ?: R.drawable.ic_placeholder,
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                ),
                contentDescription = "Foto Produk",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp$price",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddToCartClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("+ Keranjang", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onWishlistClick) {
                    Icon(
                        imageVector = if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isInWishlist) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Composable untuk Wishlist, Order History, dan Email Verification tetap sama
// (Saya sertakan kembali untuk kelengkapan file)

@Composable
fun WishlistScreen(
    wishlist: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    db: DatabaseHelper,
    onRefresh: () -> Unit
) {
    if (wishlist.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Wishlist Anda masih kosong.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(wishlist, key = { it["productId"] as String }) { product ->
            val productId = product["productId"] as String
            WishlistItem(
                product = product,
                onItemClick = { navController.navigate("productDetail/$productId") },
                onRemoveClick = {
                    scope.launch {
                        try {
                            db.removeFromWishlist(productId)
                            snackbarHostState.showSnackbar("Dihapus dari wishlist")
                            onRefresh()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal menghapus: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun WishlistItem(
    product: Map<String, Any>,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = product["imageUrl"]?.toString().takeIf { !it.isNullOrEmpty() } ?: R.drawable.ic_placeholder
                ),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product["name"] as? String ?: "N/A",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Rp${(product["price"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onRemoveClick) {
                Text("Hapus")
            }
        }
    }
}


@Composable
fun OrderHistoryScreen(orders: List<Map<String, Any>>, navController: NavController) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Anda belum memiliki riwayat pesanan.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(orders, key = { it["orderId"] as String }) { order ->
            OrderItem(order) {
                val orderId = order["orderId"] as? String
                if (orderId != null) {
                    navController.navigate("orderConfirmation/$orderId")
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Map<String, Any>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
            val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }

            Text(
                text = "Order ID: ${order["orderId"]}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tanggal: ${dateFormat.format(Date(createdAt))}")
            Text("Total: Rp${(order["totalPrice"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0.00"}")
            Text("Status: ${order["status"] as? String ?: "Unknown"}")
        }
    }
}


@Composable
fun EmailVerificationCard(
    auth: FirebaseAuth,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Email Anda belum diverifikasi.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "Silakan periksa kotak masuk email Anda untuk link verifikasi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            auth.currentUser?.sendEmailVerification()?.await()
                            snackbarHostState.showSnackbar("Email verifikasi telah dikirim ulang.")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal mengirim email: ${e.message}")
                        }
                    }
                }
            ) {
                Text("Kirim Ulang Email")
            }
        }
    }
}
