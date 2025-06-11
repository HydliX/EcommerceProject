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
    var shippingService by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var totalPrice by remember { mutableStateOf(0.0) }
    var shippingExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    // Opsi jasa pengiriman dan metode pembayaran
    val shippingServices = listOf("JNE", "J&T", "SiCepat", "AnterAja")
    val paymentMethods = listOf("COD", "Transfer Bank", "Pay Later")

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

                // Form Alamat, Jasa Pengiriman, dan Metode Pembayaran
                OutlinedTextField(
                    value = shippingAddress,
                    onValueChange = { shippingAddress = it },
                    label = { Text("Alamat Pengiriman") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown untuk Jasa Pengiriman
                ExposedDropdownMenuBox(
                    expanded = shippingExpanded,
                    onExpandedChange = { shippingExpanded = !shippingExpanded }
                ) {
                    OutlinedTextField(
                        value = shippingService,
                        onValueChange = {},
                        label = { Text("Jasa Pengiriman") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shippingExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = shippingExpanded,
                        onDismissRequest = { shippingExpanded = false }
                    ) {
                        shippingServices.forEach { service ->
                            DropdownMenuItem(
                                text = { Text(service) },
                                onClick = {
                                    shippingService = service
                                    shippingExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown untuk Metode Pembayaran
                ExposedDropdownMenuBox(
                    expanded = paymentExpanded,
                    onExpandedChange = { paymentExpanded = !paymentExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        label = { Text("Metode Pembayaran") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // Validasi input
                                if (shippingAddress.isBlank()) {
                                    snackbarHostState.showSnackbar("Alamat pengiriman harus diisi")
                                    return@launch
                                }
                                if (shippingService.isBlank()) {
                                    snackbarHostState.showSnackbar("Pilih jasa pengiriman")
                                    return@launch
                                }
                                if (paymentMethod.isBlank()) {
                                    snackbarHostState.showSnackbar("Pilih metode pembayaran")
                                    return@launch
                                }

                                // Update stok produk
                                checkoutItems.forEach { item ->
                                    val productId = item["productId"] as? String ?: return@forEach
                                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                    dbHelper.updateProductStock(productId, quantity)
                                }

                                // Format items untuk disimpan ke database
                                val orderItems = checkoutItems.associate { item ->
                                    val productId = item["productId"] as? String ?: ""
                                    productId to mapOf(
                                        "name" to (item["name"] as? String ?: ""),
                                        "price" to (item["price"] as? Number ?: 0.0),
                                        "quantity" to (item["quantity"] as? Number ?: 0),
                                        "imageUrl" to (item["imageUrl"] as? String ?: "")
                                    )
                                }

                                // Buat pesanan
                                val orderId = dbHelper.createOrder(
                                    items = orderItems,
                                    totalPrice = totalPrice,
                                    shippingAddress = shippingAddress,
                                    paymentMethod = paymentMethod,
                                    shippingService = shippingService
                                )

                                // Navigasi ke OrderConfirmationScreen
                                navController.navigate("orderConfirmation/$orderId") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }

                                snackbarHostState.showSnackbar("Pesanan berhasil dibuat!")
                            } catch (e: Exception) {
                                Log.e("CheckoutScreen", "Gagal menyelesaikan pesanan: ${e.message}", e)
                                snackbarHostState.showSnackbar("Gagal membuat pesanan: ${e.message}")
                            }
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