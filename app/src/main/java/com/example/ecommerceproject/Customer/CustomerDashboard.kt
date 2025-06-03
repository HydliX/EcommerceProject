package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseException
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.product.CartHelper
import com.example.ecommerceproject.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val auth = FirebaseAuth.getInstance()
    val dbProduct = DatabaseHelper()
    val cartHelper = CartHelper()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wishlist by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    var hasError by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var selectedTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val username = userProfile?.get("name") as? String ?: "Customer"
    var cartItemCount by remember { mutableStateOf(0) }

    // Load cart items count in LaunchedEffect
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                cartItemCount = cartHelper.getCartItems().size
            } catch (e: Exception) {
                Log.e("CustomerDashboard", "Gagal memuat keranjang: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat keranjang: ${e.message}")
            }
        }
    }

    fun loadData() {
        coroutineScope.launch {
            try {
                localIsLoading = true
                hasError = false
                auth.currentUser?.reload()?.await()
                isEmailVerified = auth.currentUser?.isEmailVerified == true
                if (!isEmailVerified) {
                    throw IllegalStateException("Silakan verifikasi email Anda untuk mengakses dashboard.")
                }
                dbProduct.ensureUserProfile() // Ensure profile exists
                products = dbProduct.getAllProducts()
                wishlist = dbProduct.getWishlist()
                orders = dbProduct.getOrders()
                Log.d("CustomerDashboard", "Data diambil: Produk=${products.size}, Wishlist=${wishlist.size}, Orders=${orders.size}")
            } catch (e: DatabaseException) {
                hasError = true
                localMessage = when {
                    e.message?.contains("Permission denied") == true -> {
                        "Akses ditolak: Pastikan profil Anda lengkap dan email telah diverifikasi."
                    }
                    e.message?.contains("Email belum diverifikasi") == true -> {
                        "Silakan verifikasi email Anda untuk melihat pesanan."
                    }
                    else -> e.message ?: "Gagal memuat data"
                }
                Log.e("CustomerDashboard", "Gagal memuat data: ${e.message}", e)
                snackbarHostState.showSnackbar(localMessage, duration = SnackbarDuration.Long)
            } catch (e: Exception) {
                hasError = true
                localMessage = e.message ?: "Terjadi kesalahan tak terduga"
                Log.e("CustomerDashboard", "Gagal memuat data: ${e.message}", e)
                snackbarHostState.showSnackbar(localMessage, duration = SnackbarDuration.Long)
            } finally {
                localIsLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate("cart") }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Keranjang",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Badge(
                            modifier = Modifier.align(Alignment.Top),
                            content = {
                                if (cartItemCount > 0) {
                                    Text(text = cartItemCount.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                // ... NavigationBarItems (unchanged) ...
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Selamat datang, $username!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (!isEmailVerified) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Email Anda belum diverifikasi.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Silakan periksa email Anda untuk link verifikasi.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        auth.currentUser?.sendEmailVerification()?.await()
                                        localMessage = "Email verifikasi telah dikirim ulang."
                                        snackbarHostState.showSnackbar(
                                            message = localMessage,
                                            duration = SnackbarDuration.Long
                                        )
                                    } catch (e: Exception) {
                                        localMessage = "Gagal mengirim email verifikasi: ${e.message}"
                                        snackbarHostState.showSnackbar(
                                            message = localMessage,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Kirim Ulang Email Verifikasi")
                        }
                    }
                }
            } else {
                // Tab Layout
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Produk") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Wishlist") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Riwayat Pesanan") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (localIsLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Memuat data...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else if (hasError) {
                    Text(
                        text = localMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadData() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Coba Lagi")
                    }
                } else {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Produk",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (products.isEmpty()) {
                                Text(
                                    text = "Tidak ada produk ditemukan",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 600.dp)
                                ) {
                                    items(products) { product ->
                                        val productId = product["productId"] as? String
                                        if (productId != null) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = ripple()
                                                            ) {
                                                                navController.navigate("productDetail/$productId")
                                                            },
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Image(
                                                            painter = if (product["imageUrl"]?.toString()
                                                                    ?.isNotEmpty() == true
                                                            ) {
                                                                rememberAsyncImagePainter(
                                                                    model = product["imageUrl"],
                                                                    placeholder = painterResource(R.drawable.ic_placeholder),
                                                                    error = painterResource(R.drawable.ic_error),
                                                                )
                                                            } else {
                                                                painterResource(R.drawable.ic_placeholder)
                                                            },
                                                            contentDescription = "Foto Produk",
                                                            modifier = Modifier
                                                                .size(64.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(
                                                                    1.dp,
                                                                    MaterialTheme.colorScheme.outline,
                                                                    RoundedCornerShape(8.dp)
                                                                ),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column(
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = product["name"] as? String
                                                                    ?: "Nama Produk Tidak Tersedia",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Rp${(product["price"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%.2f", it) } ?: "0.00"}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    try {
                                                                        cartHelper.addToCart(product)
                                                                        snackbarHostState.showSnackbar(
                                                                            message = "Produk ditambahkan ke keranjang",
                                                                            duration = SnackbarDuration.Short
                                                                        )
                                                                    } catch (e: Exception) {
                                                                        snackbarHostState.showSnackbar(
                                                                            message = "Gagal menambah ke keranjang: ${e.message}",
                                                                            duration = SnackbarDuration.Long
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        ) {
                                                            Text("Tambah ke Keranjang")
                                                        }
                                                        Button(
                                                            onClick = {
                                                                coroutineScope.launch {
                                                                    try {
                                                                        val isInWishlist = wishlist.any { it["productId"] == productId }
                                                                        if (isInWishlist) {
                                                                            dbProduct.removeFromWishlist(productId)
                                                                            snackbarHostState.showSnackbar(
                                                                                message = "Dihapus dari Wishlist",
                                                                                duration = SnackbarDuration.Short
                                                                            )
                                                                        } else {
                                                                            dbProduct.addToWishlist(productId)
                                                                            snackbarHostState.showSnackbar(
                                                                                message = "Ditambahkan ke Wishlist",
                                                                                duration = SnackbarDuration.Short
                                                                            )
                                                                        }
                                                                        loadData() // Refresh wishlist
                                                                    } catch (e: Exception) {
                                                                        snackbarHostState.showSnackbar(
                                                                            message = "Gagal memperbarui wishlist: ${e.message}",
                                                                            duration = SnackbarDuration.Long
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        ) {
                                                            Text(if (wishlist.any { it["productId"] == productId }) "Hapus dari Wishlist" else "Tambah ke Wishlist")
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Log.w(
                                                "CustomerDashboard",
                                                "ProductId tidak ditemukan untuk produk: $product"
                                            )
                                            Text(
                                                text = "Informasi produk tidak lengkap",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Text(
                                text = "Wishlist",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (wishlist.isEmpty()) {
                                Text(
                                    text = "Wishlist kosong",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 600.dp)
                                ) {
                                    items(wishlist) { product ->
                                        val productId = product["productId"] as? String
                                        if (productId != null) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = ripple()
                                                            ) {
                                                                navController.navigate("productDetail/$productId")
                                                            },
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Image(
                                                            painter = if (product["imageUrl"]?.toString()
                                                                    ?.isNotEmpty() == true
                                                            ) {
                                                                rememberAsyncImagePainter(
                                                                    model = product["imageUrl"],
                                                                    placeholder = painterResource(R.drawable.ic_placeholder),
                                                                    error = painterResource(R.drawable.ic_error),
                                                                )
                                                            } else {
                                                                painterResource(R.drawable.ic_placeholder)
                                                            },
                                                            contentDescription = "Foto Produk",
                                                            modifier = Modifier
                                                                .size(64.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(
                                                                    1.dp,
                                                                    MaterialTheme.colorScheme.outline,
                                                                    RoundedCornerShape(8.dp)
                                                                ),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Column(
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(
                                                                text = product["name"] as? String
                                                                    ?: "Nama Produk Tidak Tersedia",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Rp${(product["price"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%.2f", it) } ?: "0.00"}",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                try {
                                                                    dbProduct.removeFromWishlist(productId)
                                                                    snackbarHostState.showSnackbar(
                                                                        message = "Dihapus dari Wishlist",
                                                                        duration = SnackbarDuration.Short
                                                                    )
                                                                    loadData() // Refresh wishlist
                                                                } catch (e: Exception) {
                                                                    snackbarHostState.showSnackbar(
                                                                        message = "Gagal menghapus dari wishlist: ${e.message}",
                                                                        duration = SnackbarDuration.Long
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Text("Hapus dari Wishlist")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "Riwayat Pesanan",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (orders.isEmpty()) {
                                Text(
                                    text = "Belum ada pesanan",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 600.dp)
                                ) {
                                    items(orders) { order ->
                                        val orderId = order["orderId"] as? String
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple()
                                                ) {
                                                    if (orderId != null) {
                                                        navController.navigate("orderConfirmation/$orderId")
                                                    }
                                                },
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "Order ID: ${order["orderId"]}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
                                                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
                                                Text(
                                                    text = "Tanggal: ${dateFormat.format(Date(createdAt))}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Total: Rp${(order["totalPrice"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%.2f", it) } ?: "0.00"}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Status: ${order["status"] as? String ?: "Unknown"}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
