package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllReviewScreen(
    productId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbProduct = remember { DatabaseHelper() }
    var ratings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var userProfiles by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

    // Fetch product details and ratings
    fun loadReviews() {
        coroutineScope.launch {
            isLoading = true
            try {
                // Fetch product to get the name
                product = dbProduct.getProductById(productId)
                if (product == null) {
                    throw Exception("Produk tidak ditemukan.")
                }
                // Fetch ratings
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
                // Fetch user profiles for ratings
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
                hasError = false
            } catch (e: Exception) {
                hasError = true
                Log.e("AllReviewScreen", "Gagal memuat ulasan: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat ulasan.")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(productId) {
        loadReviews()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Semua Ulasan",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = primaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Memuat ulasan...")
                        }
                    }
                }
            } else if (hasError || product == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Gagal memuat ulasan atau produk tidak ditemukan.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { loadReviews() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
            } else {
                // Product Name and Average Rating
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = product!!["name"] as? String ?: "Produk Tidak Diketahui",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (ratings.isNotEmpty()) {
                        val averageRating = ratings.mapNotNull { it["rating"] as? Number }
                            .map { it.toDouble() }
                            .average()
                        Text(
                            text = "Rating Rata-rata: ${String.format(Locale.getDefault(), "%.1f", averageRating)} / 5.0",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    } else {
                        Text(
                            text = "Belum ada ulasan untuk produk ini.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(thickness = 1.dp)
                }

                // List of All Reviews
                items(ratings.sortedByDescending { it["timestamp"] as? Long ?: 0L }) { rating ->
                    val userId = rating["userId"] as? String ?: ""
                    val userProfile = userProfiles[userId] ?: emptyMap()
                    val username = userProfile["username"] as? String ?: "Pengguna Anonim"
                    val profilePhotoUrl = userProfile["profilePhotoUrl"] as? String ?: ""
                    val ratingValue = (rating["rating"] as? Number)?.toDouble() ?: 0.0
                    val reviewText = rating["review"] as? String ?: "Tidak ada ulasan"
                    val timestamp = (rating["timestamp"] as? Number)?.toLong() ?: 0L
                    val formattedDate = SimpleDateFormat(
                        "dd MMM yyyy, HH:mm",
                        Locale.getDefault()
                    ).format(Date(timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = if (profilePhotoUrl.isNotEmpty()) profilePhotoUrl else R.drawable.ic_placeholder
                                ),
                                contentDescription = "Foto Profil $username",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = username,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$ratingValue / 5.0",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reviewText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Bottom Spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}