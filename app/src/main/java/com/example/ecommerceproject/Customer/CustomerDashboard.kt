package com.example.ecommerceproject.Customer

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseProduct
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val dbProduct = DatabaseProduct()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    val coroutineScope = rememberCoroutineScope()
    val username = userProfile?.get("name") as? String ?: "Customer"

    LaunchedEffect(Unit) {
        try {
            localIsLoading = true
            Log.d("CustomerDashboard", "Mengambil produk untuk customer")
            products = dbProduct.getAllProducts()
            Log.d("CustomerDashboard", "Produk diambil: ${products.size} item")
        } catch (e: Exception) {
            localMessage = e.message ?: "Gagal memuat data"
            Log.e("CustomerDashboard", "Gagal memuat data: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = localMessage,
                    duration = SnackbarDuration.Long
                )
            }
        } finally {
            localIsLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                // ... NavigationBarItems ...
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
            // ... Teks Dashboard dan Username ...
            Spacer(modifier = Modifier.height(24.dp))

            if (localIsLoading) {
                // ... CircularProgressIndicator ...
            } else {
                Text(
                    text = "Produk",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (products.isEmpty()) {
                    Text(
                        text = "Tidak ada produk ditemukan",
                        // ...
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 600.dp) // Pastikan ini tidak menyebabkan masalah layout yang tidak terduga
                    ) {
                        items(products) { product ->
                            val productId = product["productId"] as? String
                            if (productId != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple() // Atau biarkan null untuk default M3, atau LocalIndication.current
                                        ) {
                                            Log.d(
                                                "CustomerDashboard",
                                                "Produk diklik: $productId, Current route: ${navController.currentDestination?.route}"
                                            )
                                            coroutineScope.launch {
                                                try {
                                                    navController.navigate("productDetail/$productId")
                                                } catch (e: Exception) {
                                                    Log.e(
                                                        "CustomerDashboard",
                                                        "Navigasi gagal: ${e.message}",
                                                        e
                                                    )
                                                    snackbarHostState.showSnackbar(
                                                        message = "Gagal membuka detail produk",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
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
                                                    // ... listener Coil ...
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
                                                    MaterialTheme.colorScheme.outline, // Contoh warna border
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = product["name"] as? String ?: "Nama Produk Tidak Tersedia",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Rp ${product["price"] as? Double ?: 0.0}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } // Akhir Card
                            } else { // Ini adalah else yang sesuai
                                Log.w(
                                    "CustomerDashboard",
                                    "ProductId tidak ditemukan untuk produk: $product"
                                )
                                // Anda mungkin ingin menampilkan sesuatu di UI jika productId null
                                // misalnya, Text("Informasi produk tidak lengkap")
                            }
                        } // Akhir items
                    } // Akhir LazyColumn
                } // Akhir else dari if (products.isEmpty())
            } // Akhir else dari if (localIsLoading)
        } // Akhir Column utama
    } // Akhir Scaffold
}