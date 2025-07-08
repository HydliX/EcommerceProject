package com.example.ecommerceproject.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
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
fun EditProductScreen(
    productId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = DatabaseHelper()
    var product by remember { mutableStateOf<Map<String, Any>?>(null) }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var weight by remember { mutableStateOf("") } // New state for weight

    // Launcher untuk memilih gambar
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // Muat data produk saat layar dimuat
    LaunchedEffect(productId) {
        try {
            isLoading = true
            hasError = false
            product = dbHelper.getProductById(productId)
            product?.let {
                name = it["name"] as? String ?: ""
                price = (it["price"] as? Number)?.toDouble()?.toString() ?: ""
                description = it["description"] as? String ?: ""
                category = it["category"] as? String ?: ""
                stock = (it["stock"] as? Number)?.toInt()?.toString() ?: ""
                weight = (it["weight"] as? Number)?.toDouble()?.toString() ?: "" // Load weight
                imageUrl = it["imageUrl"] as? String ?: ""
            }
        } catch (e: Exception) {
            hasError = true
            snackbarHostState.showSnackbar(
                message = "Gagal memuat produk: ${e.message}",
                duration = SnackbarDuration.Long
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Produk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                product = dbHelper.getProductById(productId)
                                product?.let {
                                    name = it["name"] as? String ?: ""
                                    price = (it["price"] as? Number)?.toDouble()?.toString() ?: ""
                                    description = it["description"] as? String ?: ""
                                    category = it["category"] as? String ?: ""
                                    stock = (it["stock"] as? Number)?.toInt()?.toString() ?: ""
                                    imageUrl = it["imageUrl"] as? String ?: ""
                                }
                            } catch (e: Exception) {
                                hasError = true
                                snackbarHostState.showSnackbar(
                                    message = "Gagal memuat produk: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
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
                // Form Edit Produk
                Image(
                    painter = rememberAsyncImagePainter(
                        model = if (imageUri != null) imageUri else if (imageUrl.isNotEmpty()) imageUrl else R.drawable.ic_placeholder
                    ),
                    contentDescription = "Foto Produk",
                    modifier = Modifier.size(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pilih Gambar Baru")
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Harga (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = { Text("Stok") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Berat (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Tombol Simpan
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                val priceDouble = price.toDoubleOrNull()
                                    ?: throw IllegalArgumentException("Harga harus berupa angka")
                                val stockInt = stock.toIntOrNull()
                                    ?: throw IllegalArgumentException("Stok harus berupa angka")
                                val weightDouble = weight.toDoubleOrNull()
                                    ?: throw IllegalArgumentException("Berat harus berupa angka")
                                dbHelper.updateProduct(
                                    productId = productId,
                                    name = name,
                                    price = priceDouble,
                                    description = description,
                                    imageUri = imageUri,
                                    category = category,
                                    stock = stockInt,
                                    weight = weightDouble // Add weight parameter
                                )
                                snackbarHostState.showSnackbar(
                                    message = "Produk berhasil diperbarui",
                                    duration = SnackbarDuration.Short
                                )
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    message = "Gagal memperbarui produk: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Menyimpan..." else "Simpan Perubahan")
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Hapus
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                dbHelper.deleteProduct(productId)
                                snackbarHostState.showSnackbar(
                                    message = "Produk berhasil dihapus",
                                    duration = SnackbarDuration.Short
                                )
                                navController.popBackStack()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    message = "Gagal menghapus produk: ${e.message}",
                                    duration = SnackbarDuration.Long
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(if (isLoading) "Menghapus..." else "Hapus Produk")
                }
            }
        }
    }
}
