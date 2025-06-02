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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(productId: String, navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbProduct = DatabaseHelper()
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var promotions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isInWishlist by remember { mutableStateOf(false) }
    val cartHelper = CartHelper()
    val coroutineScope = rememberCoroutineScope()
    val currentTime = 1743114420000L // 27 Mei 2025, 23:27 WIB

    LaunchedEffect(productId) {
        try {
            isLoading = true
            hasError = false
            product = dbProduct.getProductById(productId)
            promotions = dbProduct.getPromotions()
            val wishlist = dbProduct.getWishlist()
            isInWishlist = wishlist.any { it["productId"] == productId }
        } catch (e: Exception) {
            hasError = true
            Log.e("ProductDetailScreen", "Gagal memuat produk: ${e.message}", e)
            product = null
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Memuat produk...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (hasError || product == null) {
            Text(
                text = "Produk tidak ditemukan atau gagal dimuat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            isLoading = true
                            hasError = false
                            product = dbProduct.getProductById(productId)
                            promotions = dbProduct.getPromotions()
                            val wishlist = dbProduct.getWishlist()
                            isInWishlist = wishlist.any { it["productId"] == productId }
                        } catch (e: Exception) {
                            hasError = true
                            Log.e("ProductDetailScreen", "Gagal memuat produk: ${e.message}", e)
                            product = null
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Coba Lagi")
            }
        } else {
            val imageUrl = product!!["imageUrl"] as? String ?: ""
            Image(
                painter = rememberAsyncImagePainter(
                    model = if (imageUrl.isNotEmpty()) imageUrl else R.drawable.ic_placeholder
                ),
                contentDescription = "Foto Produk",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product!!["name"] as? String ?: "Tidak diketahui",
                style = MaterialTheme.typography.headlineSmall
            )

            val category = product!!["category"] as? String ?: ""
            val price = (product!!["price"] as? Number)?.toDouble() ?: 0.0
            val activePromotion = promotions.find { promo ->
                val promoCategory = promo["category"] as? String
                val startDate = (promo["startDate"] as? Number)?.toLong() ?: 0L
                val endDate = (promo["endDate"] as? Number)?.toLong() ?: Long.MAX_VALUE
                promoCategory == category && currentTime in startDate..endDate
            }
            val discountPercentage = (activePromotion?.get("discountPercentage") as? Number)?.toDouble() ?: 0.0
            val discountedPrice = price * (1 - discountPercentage / 100)

            if (discountPercentage > 0) {
                Text(
                    text = "Sedang Diskon!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Harga Asli: Rp${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
                Text(
                    text = "Diskon: Rp${String.format("%.2f", discountedPrice)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Rp${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Stok: ${(product!!["stock"] as? Number)?.toInt() ?: 0}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
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
                            isInWishlist = !isInWishlist
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Gagal memperbarui wishlist: ${e.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isInWishlist) "Hapus dari Wishlist" else "Tambah ke Wishlist")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val stock = (product!!["stock"] as? Number)?.toInt() ?: 0
                            val cartItems = cartHelper.getCartItems()
                            val existingItem = cartItems.find { it["productId"] == productId }
                            val currentQuantity = (existingItem?.get("quantity") as? Number)?.toInt() ?: 0
                            if (currentQuantity + 1 > stock) {
                                snackbarHostState.showSnackbar(
                                    message = "Stok tidak cukup. Tersedia: $stock, Di keranjang: $currentQuantity",
                                    duration = SnackbarDuration.Short
                                )
                                return@launch
                            }
                            cartHelper.addToCart(product!!)
                            navController.navigate("cart")
                            snackbarHostState.showSnackbar(
                                message = "Produk ditambahkan ke keranjang",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            Log.e("ProductDetailScreen", "Gagal menambah ke keranjang: ${e.message}", e)
                            snackbarHostState.showSnackbar(
                                message = "Gagal menambah ke keranjang: ${e.message}",
                                duration = SnackbarDuration.Long
                            )
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
