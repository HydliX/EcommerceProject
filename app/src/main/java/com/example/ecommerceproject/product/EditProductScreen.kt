package com.example.ecommerceproject.pengelola

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
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
    var weight by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Launcher untuk memilih gambar
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        Log.d("EditProductScreen", "Selected image URI: $uri")
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
                weight = (it["weight"] as? Number)?.toDouble()?.toString() ?: ""
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

    // Fungsi untuk memeriksa koneksi jaringan
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Produk") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat produk...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (hasError || product == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Produk tidak ditemukan atau gagal dimuat",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
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
                                                weight = (it["weight"] as? Number)?.toDouble()?.toString() ?: ""
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
                                }
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                } else {
                    // Form Edit Produk
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = if (imageUri != null) imageUri else if (imageUrl.isNotEmpty()) "$imageUrl?${System.currentTimeMillis()}" else R.drawable.ic_placeholder,
                                    imageLoader = context.imageLoader
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
                                            if (!isNetworkAvailable(context)) {
                                                throw IllegalStateException("Tidak ada koneksi internet")
                                            }
                                            val priceDouble = price.toDoubleOrNull()
                                                ?: throw IllegalArgumentException("Harga harus berupa angka")
                                            val stockInt = stock.toIntOrNull()
                                                ?: throw IllegalArgumentException("Stok harus berupa angka")
                                            val weightDouble = weight.toDoubleOrNull()
                                                ?: throw IllegalArgumentException("Berat harus berupa angka")
                                            Log.d("EditProductScreen", "Menyimpan produk dengan imageUri: $imageUri, existing imageUrl: $imageUrl")
                                            val newImageUrl = if (imageUri != null) {
                                                dbHelper.uploadProductImage(productId, imageUri)
                                            } else {
                                                imageUrl
                                            }
                                            dbHelper.updateProduct(
                                                productId = productId,
                                                name = name,
                                                price = priceDouble,
                                                description = description,
                                                imageUri = null, // Set to null after upload
                                                category = category,
                                                stock = stockInt,
                                                weight = weightDouble
                                            )
                                            imageUrl = newImageUrl // Update state with new URL
                                            snackbarHostState.showSnackbar(
                                                message = "Produk berhasil diperbarui",
                                                duration = SnackbarDuration.Short
                                            )
                                            navController.popBackStack()
                                        } catch (e: Exception) {
                                            val errorMessage = when {
                                                e.message?.contains("Cloudinary belum diinisialisasi") == true ->
                                                    "Gagal: Cloudinary belum diinisialisasi. Hubungi admin."
                                                e.message?.contains("Upload Cloudinary gagal") == true ->
                                                    "Gagal mengunggah gambar. Pastikan koneksi internet stabil."
                                                e.message?.contains("Tidak ada koneksi internet") == true ->
                                                    "Tidak ada koneksi internet. Silakan cek jaringan Anda."
                                                else -> "Gagal memperbarui produk: ${e.message}"
                                            }
                                            snackbarHostState.showSnackbar(
                                                message = errorMessage,
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
                            Spacer(modifier = Modifier.height(16.dp)) // Extra space at the bottom
                        }
                    }
                }
            }
        }
    }
}
