package com.example.ecommerceproject.pengelola

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
import com.example.ecommerceproject.DatabaseProduct
import com.example.ecommerceproject.R
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
    val dbProduct = DatabaseProduct() // Ganti ke DatabaseProduct
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
            Log.d("AddProductPengelola", "Product image selected: $uri")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tambah Produk",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            ProductImagePicker(
                productImageUri = productImageUri,
                isLoading = isLoading,
                onImageClick = { productImagePickerLauncher.launch("image/*") }
            )

            ProductInputFields(
                productName = productName,
                onProductNameChange = { productName = it },
                productPrice = productPrice,
                onProductPriceChange = { productPrice = it },
                productCategory = productCategory,
                onProductCategoryChange = { productCategory = it },
                productDescription = productDescription,
                onProductDescriptionChange = { productDescription = it },
                productStock = productStock,
                onProductStockChange = { productStock = it },
                isLoading = isLoading
            )

            SubmitProductButton(
                isLoading = isLoading,
                onClick = {
                    coroutineScope.launch {
                        try {
                            onLoadingChange(true)
                            val price = productPrice.toDoubleOrNull()
                                ?: throw IllegalArgumentException("Harga harus berupa angka")
                            val stock = productStock.toIntOrNull()
                                ?: throw IllegalArgumentException("Stok harus berupa angka")
                            if (productName.isBlank()) throw IllegalArgumentException("Nama produk tidak boleh kosong")
                            if (productCategory.isBlank()) throw IllegalArgumentException("Kategori produk tidak boleh kosong")
                            if (productDescription.isBlank()) throw IllegalArgumentException("Deskripsi produk tidak boleh kosong")
                            if (stock < 0) throw IllegalArgumentException("Stok tidak boleh negatif")

                            dbProduct.addProduct( // Panggil dari DatabaseProduct
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
                        } catch (e: Exception) {
                            onMessageChange(e.message ?: "Gagal menambah produk")
                            Log.e("AddProductPengelola", "Failed to add product: ${e.message}", e)
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
                }
            )
        }
    }
}

@Composable
private fun ProductImagePicker(
    productImageUri: Uri?,
    isLoading: Boolean,
    onImageClick: () -> Unit
) {
    Image(
        painter = if (productImageUri != null) {
            rememberAsyncImagePainter(
                model = productImageUri,
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error)
            )
        } else {
            painterResource(R.drawable.ic_placeholder)
        },
        contentDescription = "Foto Produk",
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .clickable(enabled = !isLoading, onClick = onImageClick),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Pilih Foto Produk",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(enabled = !isLoading, onClick = onImageClick)
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ProductInputFields(
    productName: String,
    onProductNameChange: (String) -> Unit,
    productPrice: String,
    onProductPriceChange: (String) -> Unit,
    productCategory: String,
    onProductCategoryChange: (String) -> Unit,
    productDescription: String,
    onProductDescriptionChange: (String) -> Unit,
    productStock: String,
    onProductStockChange: (String) -> Unit,
    isLoading: Boolean
) {
    OutlinedTextField(
        value = productName,
        onValueChange = onProductNameChange,
        label = { Text("Nama Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = productPrice,
        onValueChange = onProductPriceChange,
        label = { Text("Harga Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = productCategory,
        onValueChange = onProductCategoryChange,
        label = { Text("Kategori Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = productDescription,
        onValueChange = onProductDescriptionChange,
        label = { Text("Deskripsi Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = productStock,
        onValueChange = onProductStockChange,
        label = { Text("Stok Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun SubmitProductButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Text(text = if (isLoading) "Menambahkan..." else "Tambah Produk")
    }
}