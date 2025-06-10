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
fun CheckoutScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // State untuk menampung data produk yang sudah digabung dengan kuantitas keranjang
    var checkoutItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var shippingAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var totalPrice by remember { mutableStateOf(0.0) }

    // Memuat item keranjang dan detail produknya
    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            try {
                // 1. Ambil data dasar dari keranjang (productId & quantity)
                val cartItems = dbHelper.getCart()
                if (cartItems.isEmpty()) {
                    snackbarHostState.showSnackbar("Keranjang Anda kosong.")
                    navController.popBackStack() // Kembali jika keranjang kosong
                    return@launch
                }

                // 2. Ambil detail lengkap untuk setiap produk di keranjang
                val detailedItems = cartItems.mapNotNull { cartItem ->
                    val productId = cartItem["productId"] as? String
                    val quantity = (cartItem["quantity"] as? Number)?.toInt() ?: 0
                    if (productId != null) {
                        // Ambil detail produk dari database
                        dbHelper.getProductById(productId)?.let { productDetail ->
                            // Gabungkan detail produk dengan kuantitas dari keranjang
                            productDetail + mapOf("quantity" to quantity)
                        }
                    } else {
                        null
                    }
                }
                checkoutItems = detailedItems

                // 3. Hitung total harga dari data yang sudah digabung
                totalPrice = detailedItems.sumOf { item ->
                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                    price * quantity
                }

            } catch (e: Exception) {
                Log.e("CheckoutScreen", "Gagal memuat item checkout: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Checkout") }) }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("Ringkasan Pesanan:", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                // Tampilkan daftar produk di checkout
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(checkoutItems) { item ->
                        val name = item["name"] as? String ?: "Produk Tidak Ditemukan"
                        val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                        val price = (item["price"] as? Number)?.toDouble() ?: 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$name (x$quantity)")
                            Text(text = "Rp${String.format("%,.0f", price * quantity)}")
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Harga:", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Rp${String.format("%,.0f", totalPrice)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Form Alamat dan Pembayaran
                OutlinedTextField(
                    value = shippingAddress,
                    onValueChange = { shippingAddress = it },
                    label = { Text("Alamat Pengiriman") },
                    modifier = Modifier.fillMaxWidth()
                )
                // (Anda bisa menambahkan logika pemilihan pembayaran di sini)

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        // Logika untuk menyelesaikan pesanan
                        coroutineScope.launch {
                            // ... (logika createOrder Anda) ...
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = checkoutItems.isNotEmpty()
                ) {
                    Text("Selesaikan Pesanan")
                }
            }
        }
    }
}