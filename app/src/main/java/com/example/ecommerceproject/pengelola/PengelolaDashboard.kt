package com.example.ecommerceproject.pengelola

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    fun refreshProducts() {
        coroutineScope.launch {
            isLoading = true
            try {
                products = dbHelper.getAllProducts()
            } catch (e: Exception) {
                Log.e("PengelolaDashboard", "Gagal memuat produk: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat daftar produk")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshProducts()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Pengelola") },
                actions = {
                    IconButton(onClick = { navController.navigate("chatList") }) {
                        Icon(Icons.Default.MailOutline, contentDescription = "Pesan")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Default.Home, "Beranda") }, label = { Text("Beranda") }, selected = true, onClick = {})
                NavigationBarItem(icon = { Icon(Icons.Default.Person, "Profil") }, label = { Text("Profil") }, selected = false, onClick = { navController.navigate("profile") })
                NavigationBarItem(icon = { Icon(Icons.Default.Settings, "Pengaturan") }, label = { Text("Pengaturan") }, selected = false, onClick = { navController.navigate("settings") })
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Kita gunakan LazyColumn untuk seluruh layar agar bisa di-scroll
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
            ) {
                item {
                    Text("Dashboard Pengelola", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    // ==========================================================
                    // PERUBAHAN UTAMA DI SINI: PEMANGGILAN MENJADI SANGAT BERSIH
                    // ==========================================================
                    AddProductPengelola(
                        snackbarHostState = snackbarHostState,
                        onProductsUpdated = { refreshProducts() }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Menampilkan daftar produk
                item {
                    Text("Daftar Produk", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (products.isEmpty()) {
                    item {
                        Text("Belum ada produk yang ditambahkan.")
                    }
                } else {
                    items(products, key = { it["productId"] as String }) { product ->
                        ProductListItem(
                            product = product,
                            navController = navController,
                            onProductDeleted = { refreshProducts() },
                            snackbarHostState = snackbarHostState
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: Map<String, Any>,
    navController: NavController,
    onProductDeleted: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val dbHelper = remember { DatabaseHelper() }
    val productId = product["productId"] as String

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate("editProduct/$productId") }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product["name"] as? String ?: "N/A", style = MaterialTheme.typography.titleMedium)
                Text("Rp${product["price"]}", color = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = { navController.navigate("editProduct/$productId") }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                dbHelper.deleteProduct(productId)
                                snackbarHostState.showSnackbar("Produk berhasil dihapus")
                                onProductDeleted()
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Gagal: ${e.message}")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}