package com.example.ecommerceproject.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cartProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var totalPrice by remember { mutableStateOf(0.0) }

    // Fetch cart items and corresponding product details
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                DatabaseHelper.initCloudinary(context)
                cartItems = DatabaseHelper().getCart()
                cartProducts = cartItems.mapNotNull { item ->
                    val productId = item["productId"] as? String
                    if (productId != null) DatabaseHelper().getProductById(productId) else null
                }
                totalPrice = cartProducts.sumOf { product ->
                    val quantity = (cartItems.find { it["productId"] == product["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                    val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                    price * quantity
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Gagal memuat keranjang: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keranjang Belanja") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(cartProducts) { product ->
                    val productId = product["productId"] as? String ?: ""
                    val cartItem = cartItems.find { it["productId"] == productId }
                    val quantity = (cartItem?.get("quantity") as? Number)?.toInt() ?: 0
                    val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                    val name = product["name"] as? String ?: "Produk Tidak Diketahui"
                    val category = product["category"] as? String ?: ""
                    val imageUrl = product["imageUrl"] as? String ?: ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = imageUrl.ifEmpty { painterResource(id = R.drawable.ic_placeholder) },
                                contentDescription = "Gambar Produk",
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(64.dp),
                                placeholder = painterResource(id = R.drawable.ic_placeholder),
                                error = painterResource(id = R.drawable.ic_error)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Harga: Rp${String.format("%.2f", price)}")
                                Text(text = "Kategori: $category")
                                Text(text = "Jumlah: $quantity")
                                Text(text = "Subtotal: Rp${String.format("%.2f", price * quantity)}")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                DatabaseHelper().updateCartItem(productId, quantity + 1)
                                                cartItems = DatabaseHelper().getCart()
                                                cartProducts = cartItems.mapNotNull { item ->
                                                    val pid = item["productId"] as? String
                                                    if (pid != null) DatabaseHelper().getProductById(pid) else null
                                                }
                                                totalPrice = cartProducts.sumOf { p ->
                                                    val q = (cartItems.find { it["productId"] == p["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                                                    val pr = (p["price"] as? Number)?.toDouble() ?: 0.0
                                                    pr * q
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Gagal memperbarui jumlah: ${e.message}")
                                            }
                                        }
                                    }
                                ) {
                                    Text("+")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val newQuantity = if (quantity > 1) quantity - 1 else 0
                                                DatabaseHelper().updateCartItem(productId, newQuantity)
                                                cartItems = DatabaseHelper().getCart()
                                                cartProducts = cartItems.mapNotNull { item ->
                                                    val pid = item["productId"] as? String
                                                    if (pid != null) DatabaseHelper().getProductById(pid) else null
                                                }
                                                totalPrice = cartProducts.sumOf { p ->
                                                    val q = (cartItems.find { it["productId"] == p["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                                                    val pr = (p["price"] as? Number)?.toDouble() ?: 0.0
                                                    pr * q
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Gagal memperbarui jumlah: ${e.message}")
                                            }
                                        }
                                    }
                                ) {
                                    Text("-")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                DatabaseHelper().updateCartItem(productId, 0)
                                                cartItems = DatabaseHelper().getCart()
                                                cartProducts = cartItems.mapNotNull { item ->
                                                    val pid = item["productId"] as? String
                                                    if (pid != null) DatabaseHelper().getProductById(pid) else null
                                                }
                                                totalPrice = cartProducts.sumOf { p ->
                                                    val q = (cartItems.find { it["productId"] == p["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                                                    val pr = (p["price"] as? Number)?.toDouble() ?: 0.0
                                                    pr * q
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Gagal menghapus item: ${e.message}")
                                            }
                                        }
                                    }
                                ) {
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Total Belanja: Rp${String.format("%.2f", totalPrice)}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate("checkout") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = cartProducts.isNotEmpty()
            ) {
                Text("Lanjut ke Checkout")
            }
        }
    }
}