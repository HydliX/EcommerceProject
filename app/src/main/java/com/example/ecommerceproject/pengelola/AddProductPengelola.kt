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
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch

/**
 * Composable for adding a new product, designed for the PENGELOLA role.
 * Handles product input fields, image selection, and database operations.
 *
 * @param isLoading Indicates if the operation is in progress.
 * @param onLoadingChange Callback to update the loading state.
 * @param message Error or success message to display.
 * @param onMessageChange Callback to update the message state.
 * @param snackbarHostState State for displaying snackbar notifications.
 * @param onProductsUpdated Callback to refresh the product list after adding a product.
 */
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
    // Initialize DatabaseHelper for product operations
    val dbHelper = DatabaseHelper()

    // State for product input fields
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var productImageUrl by remember { mutableStateOf("") }

    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()

    // Launcher for picking product images from the device
    val productImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            productImageUri = it
            productImageUrl = "" // Clear URL since a new local image is selected
            Log.d("AddProductPengelola", "Product image selected: $uri")
        }
    }

    // Card container for the add product form
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
            // Title for the add product section
            Text(
                text = "Tambah Produk",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Product image picker
            ProductImagePicker(
                productImageUri = productImageUri,
                isLoading = isLoading,
                onImageClick = { productImagePickerLauncher.launch("image/*") }
            )

            // Input fields for product details
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

            // Submit button to add the product
            SubmitProductButton(
                isLoading = isLoading,
                onClick = {
                    coroutineScope.launch {
                        try {
                            onLoadingChange(true)
                            // Validate input fields
                            val price = productPrice.toDoubleOrNull()
                                ?: throw IllegalArgumentException("Harga harus berupa angka")
                            val stock = productStock.toIntOrNull()
                                ?: throw IllegalArgumentException("Stok harus berupa angka")
                            if (productName.isBlank()) throw IllegalArgumentException("Nama produk tidak boleh kosong")
                            if (productCategory.isBlank()) throw IllegalArgumentException("Kategori produk tidak boleh kosong")
                            if (productDescription.isBlank()) throw IllegalArgumentException("Deskripsi produk tidak boleh kosong")
                            if (stock < 0) throw IllegalArgumentException("Stok tidak boleh negatif")

                            // Add product to the database
                            dbHelper.addProduct(
                                name = productName,
                                price = price,
                                description = productDescription,
                                imageUri = productImageUri,
                                category = productCategory,
                                stock = stock
                            )

                            // Notify parent to refresh product list
                            onProductsUpdated()
                            onMessageChange("Produk berhasil ditambahkan")

                            // Reset input fields
                            productName = ""
                            productPrice = ""
                            productCategory = ""
                            productDescription = ""
                            productStock = ""
                            productImageUri = null
                            productImageUrl = ""

                            // Show success message
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Produk berhasil ditambahkan",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: Exception) {
                            // Handle errors and display message
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

/**
 * Composable for the product image picker UI.
 *
 * @param productImageUri The URI of the selected image.
 * @param isLoading Indicates if the operation is in progress.
 * @param onImageClick Callback to trigger image selection.
 */
@Composable
private fun ProductImagePicker(
    productImageUri: Uri?,
    isLoading: Boolean,
    onImageClick: () -> Unit
) {
    // Display the selected image or a placeholder
    Image(
        painter = if (productImageUri != null) {
            rememberAsyncImagePainter(
                model = productImageUri,
                placeholder = painterResource(R.drawable.ic_placeholder),
                error = painterResource(R.drawable.ic_error),
                onLoading = { Log.d("AddProductPengelola", "Loading product image: $productImageUri") },
                onSuccess = { Log.d("AddProductPengelola", "Successfully loaded product image") },
                onError = { error ->
                    Log.e("AddProductPengelola", "Failed to load product image: ${error.result.throwable.message}")
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
                onClick = onImageClick
            ),
        contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Text prompt to select an image
    Text(
        text = "Pilih Foto Produk",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(
            enabled = !isLoading,
            onClick = onImageClick
        )
    )
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Composable for product input fields (name, price, category, description, stock).
 *
 * @param productName The product name.
 * @param onProductNameChange Callback to update the product name.
 * @param productPrice The product price.
 * @param onProductPriceChange Callback to update the product price.
 * @param productCategory The product category.
 * @param onProductCategoryChange Callback to update the product category.
 * @param productDescription The product description.
 * @param onProductDescriptionChange Callback to update the product description.
 * @param productStock The product stock.
 * @param onProductStockChange Callback to update the product stock.
 * @param isLoading Indicates if the operation is in progress.
 */
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
    // Input field for product name
    OutlinedTextField(
        value = productName,
        onValueChange = onProductNameChange,
        label = { Text("Nama Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Input field for product price
    OutlinedTextField(
        value = productPrice,
        onValueChange = onProductPriceChange,
        label = { Text("Harga Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Input field for product category
    OutlinedTextField(
        value = productCategory,
        onValueChange = onProductCategoryChange,
        label = { Text("Kategori Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Input field for product description
    OutlinedTextField(
        value = productDescription,
        onValueChange = onProductDescriptionChange,
        label = { Text("Deskripsi Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Input field for product stock
    OutlinedTextField(
        value = productStock,
        onValueChange = onProductStockChange,
        label = { Text("Stok Produk") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    )
    Spacer(modifier = Modifier.height(24.dp))
}

/**
 * Composable for the submit button to add a product.
 *
 * @param isLoading Indicates if the operation is in progress.
 * @param onClick Callback to handle button click.
 */
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