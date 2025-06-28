package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import com.example.ecommerceproject.chat.ChatRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    val chatRepository = remember { ChatRepository() }
    val coroutineScope = rememberCoroutineScope()

    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var promotions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var sellerProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var isInWishlist by remember { mutableStateOf(false) }
    var ratings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var userProfiles by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    val currentTime = System.currentTimeMillis()

    fun loadProductDetails() {
        coroutineScope.launch {
            isLoading = true
            try {
                product = dbHelper.getProductById(productId)
                if (product != null) {
                    val sellerId = product!!["createdBy"] as? String
                    if (sellerId != null) {
                        sellerProfile = dbHelper.getUserProfileById(sellerId)
                    }
                    promotions = try { dbHelper.getPromotions() } catch (e: Exception) { emptyList() }
                    isInWishlist = try { dbHelper.getWishlist().any { it["productId"] == productId } } catch(e: Exception) { false }

                    val ratingSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .reference.child("products").child(productId).child("ratings").get().await()
                    ratings = if (ratingSnapshot.exists()) {
                        ratingSnapshot.children.mapNotNull { snap ->
                            (snap.value as? Map<String, Any>)?.plus("userId" to snap.key!!)
                        }
                    } else { emptyList() }

                    val profiles = mutableMapOf<String, Map<String, Any>>()
                    ratings.forEach { rating ->
                        val userId = rating["userId"] as? String ?: return@forEach
                        dbHelper.getUserProfileById(userId)?.let { profiles[userId] = it }
                    }
                    userProfiles = profiles
                    hasError = false
                } else {
                    throw Exception("Produk tidak ditemukan.")
                }
            } catch (e: Exception) {
                hasError = true
                Log.e("ProductDetailScreen", "Gagal memuat produk: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat detail produk.")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(productId) {
        loadProductDetails()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detail Produk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (hasError || product == null) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Gagal memuat produk. Silakan coba lagi.", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                val productData = product!!
                val imageUrl = productData["imageUrl"] as? String ?: ""

                // --- BAGIAN 1: GAMBAR & INFO UTAMA ---
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUrl.ifEmpty { R.drawable.ic_placeholder }),
                                contentDescription = "Foto Produk",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = productData["name"] as? String ?: "Tidak diketahui",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Stok: ${(productData["stock"] as? Number)?.toInt() ?: 0}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Price and Discount Logic
                                val category = productData["category"] as? String ?: ""
                                val price = (productData["price"] as? Number)?.toDouble() ?: 0.0
                                val activePromotion = promotions.find { promo ->
                                    (promo["category"] as? String == category || promo["category"] as? String == "All") &&
                                            currentTime in ((promo["startDate"] as? Long) ?: 0L)..((promo["endDate"] as? Long) ?: Long.MAX_VALUE)
                                }
                                val discountPercentage = (activePromotion?.get("discountPercentage") as? Number)?.toDouble() ?: 0.0
                                val discountedPrice = price * (1 - discountPercentage / 100)

                                if (discountPercentage > 0) {
                                    Text(
                                        text = "Rp${String.format(Locale.GERMAN, "%,.0f", price)}",
                                        textDecoration = TextDecoration.LineThrough,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Rp${String.format(Locale.GERMAN, "%,.0f", discountedPrice)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Rp${String.format(Locale.GERMAN, "%,.0f", price)}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = productData["description"] as? String ?: "", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                // --- BAGIAN 2: INFO PENJUAL ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SellerInfoCard(
                        sellerProfile = sellerProfile,
                        onClick = {
                            val sellerId = sellerProfile?.get("userId") as? String
                            if (sellerId != null) {
                                navController.navigate("sellerProfile/$sellerId")
                            }
                        }
                    )
                }

                // --- BAGIAN 3: TOMBOL AKSI ---
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val currentUserId = dbHelper.CurrentUserId()
                                    val sellerId = productData["createdBy"] as? String
                                    if (currentUserId != null && sellerId != null && currentUserId != sellerId) {
                                        val chatRoomId = chatRepository.startChatSession(sellerId)
                                        val sellerName = sellerProfile?.get("username") as? String ?: "Penjual"
                                        val encodedName = URLEncoder.encode(sellerName, StandardCharsets.UTF_8.toString())
                                        navController.navigate("chat/$chatRoomId/$sellerId/$encodedName")
                                    } else if (currentUserId == sellerId) {
                                        snackbarHostState.showSnackbar("Anda tidak dapat chat dengan diri sendiri.")
                                    } else {
                                        snackbarHostState.showSnackbar("Login diperlukan untuk chat.")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Tanya Penjual") }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        if (isInWishlist) dbHelper.removeFromWishlist(productId) else dbHelper.addToWishlist(productId)
                                        isInWishlist = !isInWishlist
                                        snackbarHostState.showSnackbar(if (isInWishlist) "Ditambahkan ke wishlist" else "Dihapus dari wishlist")
                                    } catch(e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isInWishlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Wishlist")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isInWishlist) "Wishlisted" else "Wishlist")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    dbHelper.addToCart(productId, 1)
                                    snackbarHostState.showSnackbar("Ditambahkan ke keranjang")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Tambah ke Keranjang") }
                }

                // --- BAGIAN 4: ULASAN ---
                item {
                    Divider(modifier = Modifier.padding(vertical = 24.dp))
                    Text("Ulasan Pelanggan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (ratings.isEmpty()) {
                    item { Text("Belum ada ulasan.", color = Color.Gray) }
                } else {
                    items(ratings.sortedByDescending { it["timestamp"] as? Long ?: 0L }) { rating ->
                        ReviewCard(
                            userProfile = userProfiles[rating["userId"] as? String],
                            rating = rating
                        )
                    }
                    if(ratings.size > 3) {
                        item {
                            TextButton(onClick = { navController.navigate("allReviews/$productId") }, modifier = Modifier.fillMaxWidth()) {
                                Text("Lihat Semua Ulasan")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// Composable SellerInfoCard dan ReviewCard ditempatkan di sini agar tetap terorganisir
@Composable
fun SellerInfoCard(sellerProfile: Map<String, Any>?, onClick: () -> Unit) {
    if (sellerProfile == null) return
    val sellerName = sellerProfile["username"] as? String ?: "Nama Penjual Tidak Tersedia"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Storefront, contentDescription = "Ikon Toko", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dijual oleh", style = MaterialTheme.typography.bodySmall)
                Text(sellerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Lihat Profil Toko")
        }
    }
}

@Composable
fun ReviewCard(userProfile: Map<String, Any>?, rating: Map<String, Any>) {
    val username = userProfile?.get("username") as? String ?: "Pengguna Anonim"
    val profilePhotoUrl = userProfile?.get("profilePhotoUrl") as? String ?: ""
    val ratingValue = (rating["rating"] as? Number)?.toDouble() ?: 0.0
    val reviewText = rating["review"] as? String ?: "Tidak ada ulasan"
    val timestamp = (rating["timestamp"] as? Number)?.toLong() ?: 0L
    val formattedDate = remember(timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(model = profilePhotoUrl.ifEmpty { R.drawable.ic_placeholder }),
                contentDescription = "Foto Profil $username",
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { star ->
                        Icon(
                            Icons.Filled.Star, contentDescription = null,
                            tint = if (star <= ratingValue) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(reviewText, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Divider(modifier = Modifier.padding(top = 12.dp))
    }
}
