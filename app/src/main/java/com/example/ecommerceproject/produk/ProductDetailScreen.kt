package com.example.ecommerceproject.Customer

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
import com.example.ecommerceproject.DatabaseProduct
import com.example.ecommerceproject.R

@Composable
fun ProductDetailScreen(productId: String, navController: NavController) {
    val dbProduct = DatabaseProduct()
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val cartHelper = CartHelper()

    // Memanggil getProductById dalam korutin menggunakan LaunchedEffect
    LaunchedEffect(productId) {
        try {
            isLoading = true
            product = dbProduct.getProductById(productId)
        } catch (e: Exception) {
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
                modifier = Modifier.align(Alignment.Center as Alignment.Horizontal)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Memuat produk...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center as Alignment.Horizontal)
            )
        } else if (product == null) {
            Text(
                "Produk tidak ditemukan",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(
                    model = product!!["imageUrl"] ?: R.drawable.ic_placeholder
                ),
                contentDescription = "Foto Produk",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = product!!["name"] as? String ?: "Tidak diketahui",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Rp${product!!["price"]}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    cartHelper.addToCart(product!!)
                    navController.navigate("cart")
                }
            ) {
                Text("Tambah ke Keranjang")
            }
        }
    }
}