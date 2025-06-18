package com.example.ecommerceproject.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.AddSupervisor
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.EditUserProfile
import com.example.ecommerceproject.ManageUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = DatabaseHelper()
    val dbProduct = DatabaseHelper() // Instansiasi DatabaseProduct
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    var selectedFeature by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Definisi onUsersUpdated
    val onUsersUpdated: () -> Unit = {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    users = dbHelper.getAllUsers() // Perbarui daftar pengguna
                }
                localIsLoading = false
            } catch (e: Exception) {
                localMessage = e.message ?: "Gagal memperbarui data pengguna"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = localMessage,
                        duration = SnackbarDuration.Long
                    )
                }
                localIsLoading = false
            }
        }
    }

    // Muat data awal saat komponen pertama kali dibuat
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    users = dbHelper.getAllUsers()
                    products = dbProduct.getAllProducts()
                }
                localIsLoading = false
            } catch (e: Exception) {
                localMessage = e.message ?: "Gagal memuat data"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = localMessage,
                        duration = SnackbarDuration.Long
                    )
                }
                localIsLoading = false
            }
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
                DatabaseHelper.UserRole.ADMIN.replaceFirstChar { it.uppercase() },
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
                if (selectedFeature == null) {
                    Text(
                        "Pilih Fitur",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val features = listOf(
                        "Tambah Supervisor",
                        "Edit Profil Pengguna",
                        "Kelola Pengguna"
                    )

                    features.forEach { feature ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { selectedFeature = feature },
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            modifier = Modifier
                                .clickable { selectedFeature = null }
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = selectedFeature ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    when (selectedFeature) {
                        "Tambah Supervisor" -> AddSupervisor(
                            userProfile = userProfile,
                            isLoading = localIsLoading,
                            onLoadingChange = { localIsLoading = it },
                            message = localMessage,
                            onMessageChange = { localMessage = it },
                            snackbarHostState = snackbarHostState,
                            onUsersUpdated = onUsersUpdated
                        )
                        "Edit Profil Pengguna" -> EditUserProfile(
                            snackbarHostState = snackbarHostState,
                            onUsersUpdated = onUsersUpdated
                        )
                        "Kelola Pengguna" -> ManageUsers(
                            users = users,
                            isLoading = localIsLoading,
                            onLoadingChange = { localIsLoading = it },
                            message = localMessage,
                            onMessageChange = { localMessage = it },
                            snackbarHostState = snackbarHostState,
                            onUsersUpdated = onUsersUpdated
                        )
                    }
                }
            }
        }
    }
}