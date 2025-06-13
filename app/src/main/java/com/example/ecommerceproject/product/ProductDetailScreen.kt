package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(productId: String, navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbProduct = remember { DatabaseHelper() }
    val chatRepository = remember { ChatRepository() }
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var promotions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isInWishlist by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val currentTime = System.currentTimeMillis()

    fun loadProductDetails() {
        coroutineScope.launch {
            isLoading = true
            try {
                product = dbProduct.getProductById(productId)
                if (product != null) {
                    promotions = dbProduct.getPromotions()
                    val wishlist = dbProduct.getWishlist()
                    isInWishlist = wishlist.any { it["productId"] == productId }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Memuat produk...")
        } else if (hasError || product == null) {
            Text("Produk tidak ditemukan atau gagal dimuat.", color = MaterialTheme.colorScheme.error)
            Button(onClick = { loadProductDetails() }) {
                Text("Coba Lagi")
            }
        } else {
            val productData = product!! // Aman karena sudah dicek
            val imageUrl = productData["imageUrl"] as? String ?: ""
            Image(
                painter = rememberAsyncImagePainter(model = if (imageUrl.isNotEmpty()) imageUrl else R.drawable.ic_placeholder),
                contentDescription = "Foto Produk",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = productData["name"] as? String ?: "Tidak diketahui",
                style = MaterialTheme.typography.headlineSmall
            )

            // Logika Harga dan Diskon
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

            if (discountPercentage > 0) {
                Text("Sedang Diskon!", color = MaterialTheme.colorScheme.error)
                Text(
                    text = "Harga Asli: Rp${String.format("%,.0f", price)}",
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
                Text("Harga Diskon: Rp${String.format("%,.0f", discountedPrice)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Harga: Rp${String.format("%,.0f", price)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text("Stok: ${(productData["stock"] as? Number)?.toInt() ?: 0}")
            Spacer(modifier = Modifier.height(16.dp))

            // ===============================================
            // REVISI BAGIAN TOMBOL "TANYA PENJUAL" DI SINI
            // ===============================================
            Button(
                onClick = {
                    val TAG = "DEBUG_CHAT_BTN"
                    coroutineScope.launch {
                        try {
                            Log.d(TAG, "Tombol 'Tanya Penjual' diklik.")

                            val currentUserId = dbProduct.CurrentUserId()
                            if (currentUserId == null) {
                                snackbarHostState.showSnackbar("Gagal mendapatkan info pengguna. Silakan login ulang.")
                                Log.e(TAG, "Gagal: currentUserId adalah null.")
                                return@launch
                            }
                            Log.d(TAG, "Current User ID: $currentUserId")

                            val pengelolaId = dbProduct.findFirstPengelolaId()
                            if (pengelolaId == null) {
                                snackbarHostState.showSnackbar("Penjual tidak ditemukan saat ini.")
                                Log.e(TAG, "GAGAL: Tidak ada pengguna dengan role 'pengelola' ditemukan di database.")
                                return@launch
                            }
                            Log.d(TAG, "Pengelola ID ditemukan: $pengelolaId")

                            if (currentUserId == pengelolaId) {
                                snackbarHostState.showSnackbar("Anda tidak dapat mengirim pesan ke diri sendiri.")
                                return@launch
                            }

                            Log.d(TAG, "Memulai sesi chat dengan $pengelolaId...")
                            val chatRoomId = chatRepository.startChatSession(pengelolaId)
                            Log.d(TAG, "Sesi chat berhasil dimulai/diperbarui. Room ID: $chatRoomId")

                            val pengelolaProfileSnapshot = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/").reference.child("users").child(pengelolaId).get().await()
                            val pengelolaName = (pengelolaProfileSnapshot.value as? Map<*, *>)?.get("username") as? String ?: "Penjual"
                            val encodedName = URLEncoder.encode(pengelolaName, StandardCharsets.UTF_8.toString())

                            val navigationRoute = "chat/$chatRoomId/$pengelolaId/$encodedName"
                            Log.d(TAG, "Berhasil, Navigasi ke: $navigationRoute")

                            navController.navigate(navigationRoute)

                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal memulai chat: ${e.message}")
                            Log.e(TAG, "Error saat memulai chat", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tanya Penjual")
            }
            // ===============================================
            // AKHIR REVISI
            // ===============================================

            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Wishlist
            Button(
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isInWishlist) "Hapus dari Wishlist" else "Tambah ke Wishlist")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Tombol Tambah ke Keranjang
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val stock = (productData["stock"] as? Number)?.toInt() ?: 0
                            if (stock < 1) {
                                snackbarHostState.showSnackbar("Stok produk habis.")
                                return@launch
                            }
                            dbProduct.addToCart(productId, 1)
                            snackbarHostState.showSnackbar("Produk ditambahkan ke keranjang")
                        } catch (e: Exception) {
                            Log.e("ProductDetailScreen", "Gagal menambah ke keranjang: ${e.message}", e)
                            snackbarHostState.showSnackbar("Gagal: ${e.message}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tambah ke Keranjang")
            }
        }
    }
}