package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.foundation.layout.*
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
fun CheckoutScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val cartHelper = CartHelper()
    val dbProduct = DatabaseHelper()
    var cartItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var shippingAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            cartItems = cartHelper.getCartItems()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                message = "Gagal memuat keranjang: ${e.message}",
                duration = SnackbarDuration.Long
            )
        }
    }

    val totalPrice = cartItems.sumOf {
        val quantity = (it["quantity"] as? Number)?.toInt() ?: 1
        ((it["price"] as? Number)?.toDouble() ?: 0.0) * quantity
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ringkasan Pesanan:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (cartItems.isEmpty()) {
                Text(
                    text = "Tidak ada item di pesanan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            } else {
                cartItems.forEach { item ->
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                    Text(
                        text = "${item["name"] as? String ?: "Produk Tidak Diketahui"} (x$quantity) - Rp${(item["price"] as? Number)?.toDouble()?.let { String.format("%.2f", it) } ?: "0.00"}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total: Rp${String.format("%.2f", totalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alamat Pengiriman:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedTextField(
                value = shippingAddress,
                onValueChange = { shippingAddress = it },
                label = { Text("Masukkan Alamat Pengiriman") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pilih Metode Pembayaran:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { paymentMethod = "Kartu Kredit" },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Kartu Kredit")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { paymentMethod = "Transfer Bank" },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text("Transfer Bank")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            isLoading = true
                            if (cartItems.isEmpty()) {
                                throw IllegalStateException("Keranjang kosong, tambahkan produk terlebih dahulu")
                            }
                            if (shippingAddress.isBlank()) {
                                throw IllegalArgumentException("Alamat pengiriman tidak boleh kosong")
                            }
                            if (paymentMethod.isBlank()) {
                                throw IllegalArgumentException("Pilih metode pembayaran terlebih dahulu")
                            }

                            val products = dbProduct.getAllProducts()
                            cartItems.forEach { item ->
                                val productId = item["productId"] as? String
                                val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                                val product = products.find { it["productId"] == productId }
                                    ?: throw IllegalStateException("Produk ${item["name"]} tidak ditemukan")
                                val stock = (product["stock"] as? Number)?.toInt() ?: 0
                                if (quantity > stock) {
                                    throw IllegalStateException("Stok ${item["name"]} tidak cukup. Tersedia: $stock")
                                }
                            }

                            val itemsForOrder = cartItems.associateBy({ it["productId"] as String }, { it })
                            val orderId = dbProduct.createOrder(
                                items = itemsForOrder,
                                totalPrice = totalPrice,
                                shippingAddress = shippingAddress,
                                paymentMethod = paymentMethod
                            )

                            cartItems.forEach { item ->
                                val productId = item["productId"] as String
                                val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                                val product = products.find { it["productId"] == productId }!!
                                val currentStock = (product["stock"] as? Number)?.toInt() ?: 0
                                dbProduct.updateProduct(
                                    productId = productId,
                                    name = product["name"] as String,
                                    price = (product["price"] as Number).toDouble(),
                                    description = product["description"] as String,
                                    imageUri = null,
                                    category = product["category"] as String,
                                    stock = currentStock - quantity
                                )
                            }

                            cartHelper.clearCart()
                            navController.navigate("orderConfirmation/$orderId")
                            snackbarHostState.showSnackbar(
                                message = "Pesanan berhasil diselesaikan",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            Log.e("CheckoutScreen", "Gagal menyelesaikan pesanan: ${e.message}", e)
                            snackbarHostState.showSnackbar(
                                message = e.message ?: "Gagal menyelesaikan pesanan",
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Memproses..." else "Selesaikan Pesanan")
            }
        }
    }
}