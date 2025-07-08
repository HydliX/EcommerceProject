package com.example.ecommerceproject

import android.net.Uri
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.pengelola.AddProductUiState
import com.example.ecommerceproject.pengelola.AddProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductPengelola(
    snackbarHostState: SnackbarHostState,
    onProductsUpdated: () -> Unit,
    // ViewModel diinisialisasi di sini secara otomatis oleh library
    viewModel: AddProductViewModel = viewModel()
) {
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var productWeight by remember { mutableStateOf("") } // New state for weight

    // Mengamati state dari ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Secara otomatis menampilkan Snackbar/mereset form saat state di ViewModel berubah
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AddProductUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                // Reset semua field setelah berhasil
                productName = ""
                productPrice = ""
                productCategory = ""
                productDescription = ""
                productStock = ""
                productImageUri = null
                onProductsUpdated()
                viewModel.resetState()
            }
            is AddProductUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val productImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> productImageUri = uri }
    )

    val isLoading = uiState is AddProductUiState.Loading

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tambah Produk", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = if (productImageUri != null) rememberAsyncImagePainter(model = productImageUri)
                else painterResource(R.drawable.ic_placeholder),
                contentDescription = "Foto Produk",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isLoading) { productImagePickerLauncher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Nama Produk") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = productPrice, onValueChange = { productPrice = it }, label = { Text("Harga Produk") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = productCategory, onValueChange = { productCategory = it }, label = { Text("Kategori Produk") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = productDescription, onValueChange = { productDescription = it }, label = { Text("Deskripsi Produk") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = productStock, onValueChange = { productStock = it }, label = { Text("Stok Produk") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = productWeight, onValueChange = { productWeight = it }, label = { Text("Berat Produk (kg)") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading)
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.addProduct(
                        productName,
                        productPrice,
                        productCategory,
                        productDescription,
                        productStock,
                        productImageUri,
                        productWeight // Add weight parameter
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LocalContentColor.current)
                } else {
                    Text("Tambah Produk")
                }
            }
        }
    }
}
