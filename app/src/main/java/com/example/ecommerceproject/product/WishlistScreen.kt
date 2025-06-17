package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()
    var wishlistItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Load wishlist items
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                wishlistItems = dbHelper.getWishlist()
                if (wishlistItems.isEmpty()) {
                    snackbarHostState.showSnackbar("Wishlist Anda kosong.")
                }
            } catch (e: Exception) {
                Log.e("WishlistScreen", "Gagal memuat wishlist: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat wishlist: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Wishlist") }) }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (wishlistItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Wishlist Anda kosong",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Produk di Wishlist Anda",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(wishlistItems) { item ->
                        val productId = item["productId"] as? String ?: ""
                        val name = item["name"] as? String ?: "Produk Tidak Ditemukan"
                        val price = (item["price"] as? Number)?.toDouble() ?: 0.0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Rp${String.format("%,.0f", price)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                dbHelper.removeFromWishlist(productId)
                                                wishlistItems = wishlistItems.filter { it["productId"] != productId }
                                                snackbarHostState.showSnackbar("Produk dihapus dari wishlist")
                                            } catch (e: Exception) {
                                                Log.e("WishlistScreen", "Gagal menghapus dari wishlist: ${e.message}", e)
                                                snackbarHostState.showSnackbar("Gagal menghapus: ${e.message}")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}