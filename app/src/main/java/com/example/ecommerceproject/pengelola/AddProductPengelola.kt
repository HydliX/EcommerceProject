package com.example.ecommerceproject

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductPengelola(
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onProductsUpdated: () -> Unit
) {
    val dbHelper = DatabaseHelper()
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var productImageUrl by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val productImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            productImageUri = it
            productImageUrl = ""
            Log.d("AddProduct", "Foto produk dipilih: $uri")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Tambah Produk",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = if (productImageUri != null) {
                    rememberAsyncImagePainter(
                        model = productImageUri,
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error),
                        onLoading = { Log.d("AddProduct", "Memuat foto produk lokal: $productImageUri") },
                        onSuccess = { Log.d("AddProduct", "Berhasil memuat foto produk lokal") },
                        onError = { error ->
                            Log.e("AddProduct", "Gagal memuat foto produk lokal: ${error.result.throwable.message}")
                        }
                    )
                } else {
                    painterResource(R.drawable.ic_placeholder)
                },
                contentDescription = "Foto Produk",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .clickable(
                        enabled = !isLoading,
                        onClick = { productImagePickerLauncher.launch("image/*") }
                    ),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pilih Foto Produk",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    enabled = !isLoading,
                    onClick = { productImagePickerLauncher.launch("image/*") }
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = productPrice,
                onValueChange = { productPrice = it },
                label = { Text("Harga Produk") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = productCategory,
                onValueChange = { productCategory = it },
                label = { Text("Kategori Produk") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = productDescription,
                onValueChange = { productDescription = it },
                label = { Text("Deskripsi Produk") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = productStock,
                onValueChange = { productStock = it },
                label = { Text("Stok Produk") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            onLoadingChange(true)
                            val price = productPrice.toDoubleOrNull()
                                ?: throw IllegalArgumentException("Harga harus berupa angka")
                            if (price < 0) throw IllegalArgumentException("Harga tidak boleh negatif")
                            val stock = productStock.toIntOrNull()
                                ?: throw IllegalArgumentException("Stok harus berupa angka")
                            if (productName.isBlank()) throw IllegalArgumentException("Nama produk tidak boleh kosong")
                            if (productCategory.isBlank()) throw IllegalArgumentException("Kategori produk tidak boleh kosong")
                            if (productDescription.isBlank()) throw IllegalArgumentException("Deskripsi produk tidak boleh kosong")
                            if (stock < 0) throw IllegalArgumentException("Stok tidak boleh negatif")
                            dbHelper.addProduct(
                                name = productName,
                                price = price,
                                description = productDescription,
                                imageUri = productImageUri,
                                category = productCategory,
                                stock = stock
                            )
                            onProductsUpdated()
                            onMessageChange("Produk berhasil ditambahkan")
                            productName = ""
                            productPrice = ""
                            productCategory = ""
                            productDescription = ""
                            productStock = ""
                            productImageUri = null
                            productImageUrl = ""
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Produk berhasil ditambahkan",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: IllegalStateException) {
                            onMessageChange(e.message ?: "Hanya admin dan pengelola yang dapat menambah produk")
                            Log.e("AddProduct", "Gagal menambah produk: ${e.message}", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: IllegalArgumentException) {
                            onMessageChange(e.message ?: "Input tidak valid")
                            Log.e("AddProduct", "Gagal menambah produk: ${e.message}", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: Exception) {
                            onMessageChange(e.message ?: "Gagal menambah produk")
                            Log.e("AddProduct", "Gagal menambah produk: ${e.message}", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } finally {
                            onLoadingChange(false)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Menambahkan..." else "Tambah Produk")
            }
        }
    }
}
