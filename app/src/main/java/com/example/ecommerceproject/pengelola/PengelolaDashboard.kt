package com.example.ecommerceproject.pengelola

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.AddProductPengelola
import com.example.ecommerceproject.DatabaseHelper
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
    val dbProduct = DatabaseHelper()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            localIsLoading = true
            Log.d("PengelolaDashboard", "Fetching all products for PENGELOLA")
            products = dbProduct.getAllProducts()
        } catch (e: Exception) {
            localMessage = e.message ?: "Gagal memuat data"
            Log.e("PengelolaDashboard", "Failed to load data: ${e.message}", e)
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
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = DatabaseHelper.UserRole.PENGELOLA.replaceFirstChar { it.uppercase() },
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
                    text = "Memuat data...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                AddProductPengelola(
                    isLoading = localIsLoading,
                    onLoadingChange = { localIsLoading = it },
                    message = localMessage,
                    onMessageChange = { localMessage = it },
                    snackbarHostState = snackbarHostState,
                    onProductsUpdated = {
                        coroutineScope.launch {
                            try {
                                products = dbProduct.getAllProducts()
                            } catch (e: Exception) {
                                localMessage = e.message ?: "Gagal memuat produk"
                                Log.e(
                                    "PengelolaDashboard",
                                    "Failed to refresh products: ${e.message}",
                                    e
                                )
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = localMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ProductList(
                    products = products,
                    navController = navController,
                    onProductDeleted = {
                        coroutineScope.launch {
                            try {
                                products = dbProduct.getAllProducts()
                            } catch (e: Exception) {
                                localMessage = e.message ?: "Gagal memuat produk setelah penghapusan"
                                Log.e(
                                    "PengelolaDashboard",
                                    "Failed to refresh products after deletion: ${e.message}",
                                    e
                                )
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = localMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            if (localMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = localMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductList(
    products: List<Map<String, Any>>,
    navController: NavController,
    onProductDeleted: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = DatabaseHelper()

    Text(
        text = "Daftar Produk",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (products.isEmpty()) {
        Text(
            text = "Tidak ada produk ditemukan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    } else {
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(products) { product ->
                val productId = product["productId"] as? String ?: return@items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            navController.navigate("editProduct/$productId")
                        },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = product["name"] as? String ?: "Tidak diketahui",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Rp${product["price"]}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    navController.navigate("editProduct/$productId")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Produk",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            dbHelper.deleteProduct(productId)
                                            snackbarHostState.showSnackbar(
                                                message = "Produk berhasil dihapus",
                                                duration = SnackbarDuration.Short
                                            )
                                            onProductDeleted()
                                        } catch (e: Exception) {
                                            Log.e(
                                                "ProductList",
                                                "Gagal menghapus produk: ${e.message}",
                                                e
                                            )
                                            snackbarHostState.showSnackbar(
                                                message = "Gagal menghapus produk: ${e.message}",
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Produk",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}