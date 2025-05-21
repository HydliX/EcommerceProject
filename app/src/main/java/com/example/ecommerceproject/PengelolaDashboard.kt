package com.example.ecommerceproject

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengelolaDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = DatabaseHelper()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) }
    var productImageUrl by remember { mutableStateOf("") }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    val coroutineScope = rememberCoroutineScope()

    val productImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            productImageUri = it
            productImageUrl = ""
            Log.d("PengelolaDashboard", "Foto produk dipilih: $uri")
        }
    }

    LaunchedEffect(Unit) {
        try {
            localIsLoading = true
            Log.d("PengelolaDashboard", "Mengambil semua produk untuk pengelola")
            products = dbHelper.getAllProducts()
        } catch (e: Exception) {
            localMessage = e.message ?: "Gagal memuat data"
            Log.e("PengelolaDashboard", "Gagal memuat data: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = localMessage,
                    duration = SnackbarDuration.Long
                )
            }
        } finally {
            localIsLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                    label = { Text("Beranda") },
                    selected = navController.currentDestination?.route == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = navController.currentDestination?.route == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Pengaturan") },
                    label = { Text("Pengaturan") },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                DatabaseHelper.UserRole.PENGELOLA.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (localIsLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Memuat data...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
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
                            "Kelola Produk",
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
                                    onLoading = { Log.d("PengelolaDashboard", "Memuat foto produk lokal: $productImageUri") },
                                    onSuccess = { Log.d("PengelolaDashboard", "Berhasil memuat foto produk lokal") },
                                    onError = { error ->
                                        Log.e("PengelolaDashboard", "Gagal memuat foto produk lokal: ${error.result.throwable.message}")
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
                                    enabled = !localIsLoading,
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
                                enabled = !localIsLoading,
                                onClick = { productImagePickerLauncher.launch("image/*") }
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text("Nama Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = productPrice,
                            onValueChange = { productPrice = it },
                            label = { Text("Harga Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = productCategory,
                            onValueChange = { productCategory = it },
                            label = { Text("Kategori Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = productDescription,
                            onValueChange = { productDescription = it },
                            label = { Text("Deskripsi Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = productStock,
                            onValueChange = { productStock = it },
                            label = { Text("Stok Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        localIsLoading = true
                                        val price = productPrice.toDoubleOrNull()
                                            ?: throw IllegalArgumentException("Harga harus berupa angka")
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
                                        products = dbHelper.getAllProducts()
                                        localMessage = "Produk berhasil ditambahkan"
                                        productName = ""
                                        productPrice = ""
                                        productCategory = ""
                                        productDescription = ""
                                        productStock = ""
                                        productImageUri = null
                                        productImageUrl = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = localMessage,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } catch (e: Exception) {
                                        localMessage = e.message ?: "Gagal menambah produk"
                                        Log.e("PengelolaDashboard", "Gagal menambah produk: ${e.message}", e)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = localMessage,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } finally {
                                        localIsLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !localIsLoading
                        ) {
                            Text(if (localIsLoading) "Menambahkan..." else "Tambah Produk")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Daftar Produk",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (products.isEmpty()) {
                    Text(
                        "Tidak ada produk ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(products) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        product["name"] as? String ?: "Tidak diketahui",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Rp${product["price"]}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (localMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    localMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}