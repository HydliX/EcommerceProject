package com.example.ecommerceproject.pengelola

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch

/**
 * Composable for the PENGELOLA role dashboard.
 * Displays a form to add products and a list of existing products.
 *
 * @param navController Navigation controller for screen transitions.
 * @param userProfile The current user's profile data.
 * @param isLoading Indicates if data is being loaded.
 * @param message Error or success message to display.
 * @param snackbarHostState State for displaying snackbar notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengelolaDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    // Initialize DatabaseHelper for product operations
    val dbHelper = DatabaseHelper()

    // State for the product list
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Local state for loading and messages
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }

    // Coroutine scope for asynchronous operations
    val coroutineScope = rememberCoroutineScope()

    // Load products when the composable is first composed
    LaunchedEffect(Unit) {
        try {
            localIsLoading = true
            Log.d("PengelolaDashboard", "Fetching all products for PENGELOLA")
            products = dbHelper.getAllProducts()
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

    // Main UI structure with Scaffold
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Navigation bar for switching screens
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
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Dashboard title
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // User role display
            Text(
                text = DatabaseHelper.UserRole.PENGELOLA.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Show loading indicator if data is being fetched
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
                // Product addition form
                AddProductPengelola(
                    isLoading = localIsLoading,
                    onLoadingChange = { localIsLoading = it },
                    message = localMessage,
                    onMessageChange = { localMessage = it },
                    snackbarHostState = snackbarHostState,
                    onProductsUpdated = {
                        coroutineScope.launch {
                            try {
                                products = dbHelper.getAllProducts()
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

                // Product list section
                ProductList(products = products)
            }

            // Display error or success message
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

/**
 * Composable for displaying the list of products.
 *
 * @param products List of products to display.
 */
@Composable
private fun ProductList(products: List<Map<String, Any>>) {
    // Title for the product list
    Text(
        text = "Daftar Produk",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Display message if no products are found
    if (products.isEmpty()) {
        Text(
            text = "Tidak ada produk ditemukan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    } else {
        // List of products in a scrollable column
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                        // Product name
                        Text(
                            text = product["name"] as? String ?: "Tidak diketahui",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // Product price
                        Text(
                            text = "Rp${product["price"]}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}