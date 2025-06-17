package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
    val dbProduct = remember { DatabaseHelper() }
    val chatRepository = remember { ChatRepository() }
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var promotions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isInWishlist by remember { mutableStateOf(false) }
    var ratings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val currentTime = System.currentTimeMillis()
    var userProfiles by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }

    // Theme Colors
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.05f),
        secondaryColor.copy(alpha = 0.05f)
    )

    fun loadProductDetails() {
        coroutineScope.launch {
            isLoading = true
            try {
                product = dbProduct.getProductById(productId)
                if (product != null) {
                    promotions = dbProduct.getPromotions()
                    val wishlist = dbProduct.getWishlist()
                    isInWishlist = wishlist.any { it["productId"] == productId }
                    val ratingSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .reference.child("products").child(productId).child("ratings").get().await()
                    ratings = if (ratingSnapshot.exists()) {
                        ratingSnapshot.children.mapNotNull { snap ->
                            val ratingData = snap.value as? Map<String, Any>
                            ratingData?.plus("userId" to snap.key!!)
                        }
                    } else {
                        emptyList()
                    }
                    val profiles = mutableMapOf<String, Map<String, Any>>()
                    ratings.forEach { rating ->
                        val userId = rating["userId"] as? String ?: return@forEach
                        val profileSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .reference.child("users").child(userId).get().await()
                        if (profileSnapshot.exists()) {
                            profiles[userId] = profileSnapshot.value as Map<String, Any>
                        }
                    }
                    userProfiles = profiles
                    val complaintSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .reference.child("products").child(productId).child("complaints").get().await()
                    complaints = if (complaintSnapshot.exists()) {
                        complaintSnapshot.children.mapNotNull { it.value as? Map<String, Any> }
                    } else {
                        emptyList()
                    }
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
        modifier = Modifier.background(Brush.verticalGradient(gradientColors)),
        topBar = {
            TopAppBar(
                title = { Text("Detail Produk", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                ),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = primaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Memuat data produk...", color = Color.Gray)
                        }
                    }
                }
            } else if (hasError || product == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Produk tidak ditemukan atau gagal dimuat.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { loadProductDetails() },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
            } else {
                val productData = product!!
                val imageUrl = productData["imageUrl"] as? String ?: ""

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = if (imageUrl.isNotEmpty()) imageUrl else R.drawable.ic_placeholder,
                                    placeholder = painterResource(R.drawable.ic_placeholder),
                                    error = painterResource(R.drawable.ic_error)
                                ),
                                contentDescription = "Foto Produk",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = productData["name"] as? String ?: "Tidak diketahui",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Price and Discount Logic
                            val category = productData["category"] as? String ?: ""
                            val price = (productData["price"] as? Number)?.toDouble() ?: 0.0
                            val activePromotion = promotions.find { promo ->
                                val promoCategory = promo["category"] as? String
                                val startDate = (promo["startDate"] as? Number)?.toLong() ?: 0L
                                val endDate = (promo["endDate"] as? Number)?.toLong() ?: Long.MAX_VALUE
                                (promoCategory == category || promoCategory == "All") && currentTime in startDate..endDate
                            }
                            val discountPercentage = (activePromotion?.get("discountPercentage") as? Number)?.toDouble() ?: 0.0
                            val discountedPrice = price * (1 - discountPercentage / 100)

                            Spacer(modifier = Modifier.height(8.dp))
                            if (discountPercentage > 0) {
                                Text(
                                    "Sedang Diskon!",
                                    color = secondaryColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Harga Asli: Rp${String.format(Locale.getDefault(), "%,.0f", price)}",
                                    textDecoration = TextDecoration.LineThrough,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Harga Diskon: Rp${String.format(Locale.getDefault(), "%,.0f", discountedPrice)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = secondaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "Harga: Rp${String.format(Locale.getDefault(), "%,.0f", price)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = secondaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Stok: ${(productData["stock"] as? Number)?.toInt() ?: 0}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButton(
                            text = "Tanya Penjual",
                            icon = Icons.Default.Chat,
                            color = secondaryColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val currentUserId = dbProduct.CurrentUserId()
                                        if (currentUserId == null) {
                                            snackbarHostState.showSnackbar("Silakan login ulang.")
                                            return@launch
                                        }
                                        val pengelolaId = dbProduct.findFirstPengelolaId()
                                        if (pengelolaId == null) {
                                            snackbarHostState.showSnackbar("Penjual tidak ditemukan.")
                                            return@launch
                                        }
                                        if (currentUserId == pengelolaId) {
                                            snackbarHostState.showSnackbar("Tidak dapat chat diri sendiri.")
                                            return@launch
                                        }
                                        val chatRoomId = chatRepository.startChatSession(pengelolaId)
                                        val pengelolaProfileSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                            .reference.child("users").child(pengelolaId).get().await()
                                        val pengelolaName = (pengelolaProfileSnapshot.value as? Map<*, *>)?.get("username") as? String ?: "Penjual"
                                        val encodedName = URLEncoder.encode(pengelolaName, StandardCharsets.UTF_8.toString())
                                        navController.navigate("chat/$chatRoomId/$pengelolaId/$encodedName")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal memulai chat: ${e.message}")
                                    }
                                }
                            }
                        )
                        ActionButton(
                            text = if (isInWishlist) "Hapus Wishlist" else "Tambah Wishlist",
                            icon = if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            color = primaryColor,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        if (isInWishlist) {
                                            dbProduct.removeFromWishlist(productId)
                                            snackbarHostState.showSnackbar("Dihapus dari Wishlist")
                                        } else {
                                            dbProduct.addToWishlist(productId)
                                            snackbarHostState.showSnackbar("Ditambahkan ke Wishlist")
                                        }
                                        isInWishlist = !isInWishlist
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionButton(
                        text = "Tambah ke Keranjang",
                        icon = Icons.Default.ShoppingCart,
                        color = primaryColor,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val stock = (productData["stock"] as? Number)?.toInt() ?: 0
                                    if (stock < 1) {
                                        snackbarHostState.showSnackbar("Stok produk habis.")
                                        return@launch
                                    }
                                    dbProduct.addToCart(productId, 1)
                                    snackbarHostState.showSnackbar("Ditambahkan ke keranjang")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Ratings Section
                item {
                    Divider(
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Text(
                        text = "Ulasan Pelanggan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (ratings.isNotEmpty()) {
                        val averageRating = ratings.mapNotNull { it["rating"] as? Number }
                            .map { it.toDouble() }
                            .average()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", averageRating)} / 5.0",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = secondaryColor
                            )
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (star <= averageRating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "$star star",
                                    tint = secondaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text(
                            text = "Belum ada ulasan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Review Items
                items(ratings.sortedByDescending { it["timestamp"] as? Long ?: 0L }.take(5)) { rating ->
                    val userId = rating["userId"] as? String ?: ""
                    val userProfile = userProfiles[userId] ?: emptyMap()
                    val username = userProfile["username"] as? String ?: "Pengguna Anonim"
                    val profilePhotoUrl = userProfile["profilePhotoUrl"] as? String ?: ""
                    val ratingValue = (rating["rating"] as? Number)?.toDouble() ?: 0.0
                    val reviewText = rating["review"] as? String ?: "Tidak ada ulasan"
                    val timestamp = (rating["timestamp"] as? Number)?.toLong() ?: 0L
                    val formattedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

                    ReviewCard(
                        username = username,
                        profilePhotoUrl = profilePhotoUrl,
                        ratingValue = ratingValue,
                        reviewText = reviewText,
                        formattedDate = formattedDate,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }

                // Show All Reviews Button
                if (ratings.size > 5) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ActionButton(
                            text = "Tampilkan Semua Ulasan",
                            icon = Icons.Default.ArrowForward,
                            color = secondaryColor,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate("allReviews/$productId") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150)
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ReviewCard(
    username: String,
    profilePhotoUrl: String,
    ratingValue: Double,
    reviewText: String,
    formattedDate: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = if (profilePhotoUrl.isNotEmpty()) profilePhotoUrl else R.drawable.ic_placeholder,
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                ),
                contentDescription = "Foto Profil $username",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$ratingValue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor
                        )
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reviewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
