package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DatabaseException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Komponen layar dashboard untuk menampilkan data berdasarkan role pengguna
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(navController: NavController) {
    // Inisialisasi Firebase Authentication dan DatabaseHelper
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    // State untuk menyimpan data
    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var supervisorUsername by remember { mutableStateOf("") }
    var supervisorEmail by remember { mutableStateOf("") }
    var supervisorPassword by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("") }
    var showReAuthDialog by remember { mutableStateOf(false) }
    var showPromoteDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedRole by remember { mutableStateOf(DatabaseHelper.UserRole.SUPERVISOR) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Memeriksa autentikasi pengguna
    if (auth.currentUser == null) {
        LaunchedEffect(Unit) {
            Log.w("Dashboard", "Pengguna tidak terautentikasi, mengalihkan ke login")
            message = "Silakan login untuk mengakses dashboard."
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
        return
    }

    // Dialog re-autentikasi untuk admin
    if (showReAuthDialog) {
        val confirmInteractionSource = remember { MutableInteractionSource() }
        val confirmIsPressed by confirmInteractionSource.collectIsPressedAsState()
        val confirmScale by animateFloatAsState(if (confirmIsPressed) 0.95f else 1f)

        AlertDialog(
            onDismissRequest = { showReAuthDialog = false },
            title = { Text("Re-autentikasi Admin", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Masukkan kata sandi admin untuk melanjutkan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Kata Sandi Admin") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                val email = userProfile?.get("email") as? String
                                    ?: throw IllegalStateException("Email tidak ditemukan")
                                Log.d("Dashboard", "Mencoba re-autentikasi untuk email: $email")
                                auth.signInWithEmailAndPassword(email, adminPassword).await()
                                message = "Re-autentikasi berhasil"
                                showReAuthDialog = false
                                adminPassword = ""
                                users = dbHelper.getAllUsers()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } catch (e: FirebaseAuthException) {
                                message = when (e.errorCode) {
                                    "ERROR_WRONG_PASSWORD" -> "Kata sandi salah."
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                                    else -> e.message ?: "Re-autentikasi gagal"
                                }
                                Log.e("Dashboard", "Kesalahan re-autentikasi: ${e.errorCode}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } catch (e: Exception) {
                                message = e.message ?: "Re-autentikasi gagal"
                                Log.e("Dashboard", "Kesalahan tak terduga selama re-autentikasi: ${e.message}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.scale(confirmScale),
                    interactionSource = confirmInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Konfirmasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReAuthDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog untuk memilih role saat mempromosikan pengguna
    if (showPromoteDialog && selectedUser != null) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            title = { Text("Promosikan Pengguna: ${selectedUser!!["username"]}") },
            text = {
                Column {
                    Text("Pilih Role Baru:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(
                                DatabaseHelper.UserRole.SUPERVISOR,
                                DatabaseHelper.UserRole.PENGELOLA
                            ).forEach { roleOption ->
                                DropdownMenuItem(
                                    text = { Text(roleOption) },
                                    onClick = {
                                        selectedRole = roleOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                isLoading = true
                                val userId = selectedUser!!["userId"] as? String
                                    ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                Log.d("Dashboard", "Mempromosikan pengguna dengan userId: $userId ke $selectedRole")
                                val newLevel = when (selectedRole) {
                                    DatabaseHelper.UserRole.SUPERVISOR -> DatabaseHelper.UserLevel.SUPERVISOR
                                    DatabaseHelper.UserRole.PENGELOLA -> DatabaseHelper.UserLevel.PENGELOLA
                                    else -> DatabaseHelper.UserLevel.USER
                                }
                                dbHelper.updateUserRole(userId, selectedRole, newLevel)
                                users = dbHelper.getAllUsers()
                                message = "Pengguna dipromosikan menjadi $selectedRole"
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                showPromoteDialog = false
                            } catch (e: Exception) {
                                message = e.message ?: "Gagal mempromosikan pengguna"
                                Log.e("Dashboard", "Gagal mempromosikan pengguna: ${e.message}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Promosikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Memuat profil pengguna dan data berdasarkan role
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            Log.d("Dashboard", "Memuat profil pengguna untuk userId=${auth.currentUser?.uid}")
            val currentUser = auth.currentUser ?: throw IllegalStateException("Pengguna tidak terautentikasi")
            if (!currentUser.isEmailVerified && userProfile?.get("role") != DatabaseHelper.UserRole.CUSTOMER) {
                Log.w("Dashboard", "Email belum diverifikasi untuk userId=${currentUser.uid}")
                throw IllegalStateException("Verifikasi email Anda untuk mengakses dashboard.")
            }
            userProfile = dbHelper.getUserProfile()
            if (userProfile == null) {
                Log.w("Dashboard", "Profil pengguna tidak ditemukan untuk userId=${currentUser.uid}")
                throw IllegalStateException("Profil pengguna tidak ditemukan")
            }
            val role = userProfile?.get("role") as? String ?: DatabaseHelper.UserRole.CUSTOMER
            Log.d("Dashboard", "Role pengguna: $role")
            when (role) {
                DatabaseHelper.UserRole.ADMIN -> {
                    Log.d("Dashboard", "Mengambil semua pengguna untuk admin")
                    users = dbHelper.getAllUsers()
                    products = dbHelper.getAllProducts()
                }
                DatabaseHelper.UserRole.PENGELOLA -> {
                    Log.d("Dashboard", "Mengambil semua produk untuk pengelola")
                    products = dbHelper.getAllProducts()
                }
                DatabaseHelper.UserRole.SUPERVISOR -> {
                    Log.d("Dashboard", "Mengambil pengguna customer untuk supervisor")
                    users = dbHelper.getAllUsers(DatabaseHelper.UserRole.CUSTOMER)
                }
                DatabaseHelper.UserRole.CUSTOMER -> {
                    Log.d("Dashboard", "Mengambil produk untuk customer")
                    products = dbHelper.getAllProducts()
                }
            }
        } catch (e: DatabaseException) {
            message = when {
                e.message?.contains("Permission denied") == true -> {
                    "Akses ditolak. Pastikan akun Anda memiliki role yang benar (Admin, Pengelola, atau Supervisor)."
                }
                e.message?.contains("network") == true -> {
                    "Kesalahan jaringan. Periksa koneksi internet Anda."
                }
                else -> {
                    "Gagal memuat data: ${e.message}"
                }
            }
            Log.e("Dashboard", "Gagal memuat data: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
        } catch (e: Exception) {
            message = e.message ?: "Gagal memuat data"
            Log.e("Dashboard", "Gagal memuat data: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
        } finally {
            isLoading = false
        }
    }

    // Struktur UI utama dengan Scaffold
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
            val role = userProfile?.get("role") as? String ?: DatabaseHelper.UserRole.CUSTOMER
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                role.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
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
                when (role) {
                    DatabaseHelper.UserRole.ADMIN -> {
                        // Fitur untuk admin: menambah supervisor
                        val addSupervisorInteractionSource = remember { MutableInteractionSource() }
                        val addSupervisorIsPressed by addSupervisorInteractionSource.collectIsPressedAsState()
                        val addSupervisorScale by animateFloatAsState(if (addSupervisorIsPressed) 0.95f else 1f)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Tambah Supervisor",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = supervisorUsername,
                                    onValueChange = { supervisorUsername = it },
                                    label = { Text("Nama Pengguna Supervisor") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = supervisorEmail,
                                    onValueChange = { supervisorEmail = it },
                                    label = { Text("Email Supervisor") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = supervisorPassword,
                                    onValueChange = { supervisorPassword = it },
                                    label = { Text("Kata Sandi Supervisor") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                isLoading = true
                                                if (supervisorUsername.isBlank()) throw IllegalArgumentException("Nama pengguna tidak boleh kosong")
                                                if (supervisorEmail.isBlank()) throw IllegalArgumentException("Email tidak boleh kosong")
                                                if (supervisorPassword.length < 6) throw IllegalArgumentException("Kata sandi harus minimal 6 karakter")
                                                Log.d("Dashboard", "Mencoba membuat supervisor: $supervisorEmail")
                                                val authResult = auth.createUserWithEmailAndPassword(supervisorEmail, supervisorPassword).await()
                                                val user = authResult.user ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                                val userId = user.uid
                                                Log.d("Dashboard", "Autentikasi berhasil, mengirim verifikasi email untuk userId: $userId")
                                                user.sendEmailVerification().await()
                                                Log.d("Dashboard", "Verifikasi email terkirim, menyimpan profil untuk userId: $userId")
                                                try {
                                                    dbHelper.saveUserProfile(
                                                        userId = userId,
                                                        username = supervisorUsername,
                                                        email = supervisorEmail,
                                                        role = DatabaseHelper.UserRole.SUPERVISOR,
                                                        level = DatabaseHelper.UserLevel.SUPERVISOR
                                                    )
                                                    Log.d("Dashboard", "Profil berhasil disimpan untuk userId: $userId")
                                                } catch (e: Exception) {
                                                    Log.w("Dashboard", "Gagal menyimpan profil, menghapus pengguna auth: $userId")
                                                    user.delete().await()
                                                    throw e
                                                }
                                                Log.d("Dashboard", "Memperbarui daftar pengguna")
                                                users = dbHelper.getAllUsers()
                                                message = "Supervisor berhasil ditambahkan. Verifikasi email terkirim. Silakan re-autentikasi."
                                                supervisorUsername = ""
                                                supervisorEmail = ""
                                                supervisorPassword = ""
                                                showReAuthDialog = true
                                            } catch (e: FirebaseAuthException) {
                                                message = when (e.errorCode) {
                                                    "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                                                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah digunakan."
                                                    "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah."
                                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                                                    else -> e.message ?: "Gagal menambah supervisor"
                                                }
                                                Log.e("Dashboard", "Kesalahan autentikasi: ${e.errorCode}, ${e.message}", e)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                message = e.message ?: "Gagal menambah supervisor"
                                                Log.e("Dashboard", "Kesalahan tak terduga: ${e.message}", e)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(addSupervisorScale),
                                    enabled = !isLoading,
                                    interactionSource = addSupervisorInteractionSource,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(4.dp)
                                ) {
                                    Text(if (isLoading) "Menambahkan..." else "Tambah Supervisor")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Fitur untuk admin: mengelola produk
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Tambah Produk",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
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
                                                isLoading = true
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
                                                    imageUri = null, // Placeholder, implement image picker if needed
                                                    category = productCategory,
                                                    stock = stock
                                                )
                                                products = dbHelper.getAllProducts()
                                                message = "Produk berhasil ditambahkan"
                                                productName = ""
                                                productPrice = ""
                                                productCategory = ""
                                                productDescription = ""
                                                productStock = ""
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                message = e.message ?: "Gagal menambah produk"
                                                Log.e("Dashboard", "Gagal menambah produk: ${e.message}", e)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } finally {
                                                isLoading = false
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
                        Spacer(modifier = Modifier.height(24.dp))

                        // Daftar supervisor
                        Text(
                            "Supervisor",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (users.filter { it["role"] == DatabaseHelper.UserRole.SUPERVISOR }.isEmpty()) {
                            Text(
                                "Tidak ada supervisor ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(users.filter { it["role"] == DatabaseHelper.UserRole.SUPERVISOR }) { user ->
                                    UserCard(
                                        user = user,
                                        isLoading = isLoading,
                                        onDelete = {
                                            coroutineScope.launch {
                                                try {
                                                    val userId = user["userId"] as? String
                                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                                    Log.d("Dashboard", "Menghapus supervisor dengan userId: $userId")
                                                    dbHelper.deleteUser(userId)
                                                    users = dbHelper.getAllUsers()
                                                    message = "Supervisor berhasil dihapus"
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    message = e.message ?: "Gagal menghapus supervisor"
                                                    Log.e("Dashboard", "Gagal menghapus supervisor: ${e.message}", e)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Daftar pengelola
                        Text(
                            "Pengelola",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (users.filter { it["role"] == DatabaseHelper.UserRole.PENGELOLA }.isEmpty()) {
                            Text(
                                "Tidak ada pengelola ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(users.filter { it["role"] == DatabaseHelper.UserRole.PENGELOLA }) { user ->
                                    UserCard(
                                        user = user,
                                        isLoading = isLoading,
                                        onDelete = {
                                            coroutineScope.launch {
                                                try {
                                                    val userId = user["userId"] as? String
                                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                                    Log.d("Dashboard", "Menghapus pengelola dengan userId: $userId")
                                                    dbHelper.deleteUser(userId)
                                                    users = dbHelper.getAllUsers()
                                                    message = "Pengelola berhasil dihapus"
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    message = e.message ?: "Gagal menghapus pengelola"
                                                    Log.e("Dashboard", "Gagal menghapus pengelola: ${e.message}", e)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Daftar pelanggan
                        Text(
                            "Pelanggan",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (users.filter { it["role"] == DatabaseHelper.UserRole.CUSTOMER }.isEmpty()) {
                            Text(
                                "Tidak ada pelanggan ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(users.filter { it["role"] == DatabaseHelper.UserRole.CUSTOMER }) { user ->
                                    UserCard(
                                        user = user,
                                        isLoading = isLoading,
                                        onPromote = {
                                            selectedUser = user
                                            showPromoteDialog = true
                                        },
                                        onDelete = {
                                            coroutineScope.launch {
                                                try {
                                                    val userId = user["userId"] as? String
                                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                                    Log.d("Dashboard", "Menghapus pelanggan dengan userId: $userId")
                                                    dbHelper.deleteUser(userId)
                                                    users = dbHelper.getAllUsers()
                                                    message = "Pelanggan berhasil dihapus"
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    message = e.message ?: "Gagal menghapus pelanggan"
                                                    Log.e("Dashboard", "Gagal menghapus pelanggan: ${e.message}", e)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = message,
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    DatabaseHelper.UserRole.PENGELOLA -> {
                        // Fitur untuk pengelola: mengelola produk
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Kelola Produk",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
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
                                                isLoading = true
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
                                                    imageUri = null, // Placeholder, implement image picker if needed
                                                    category = productCategory,
                                                    stock = stock
                                                )
                                                products = dbHelper.getAllProducts()
                                                message = "Produk berhasil ditambahkan"
                                                productName = ""
                                                productPrice = ""
                                                productCategory = ""
                                                productDescription = ""
                                                productStock = ""
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                message = e.message ?: "Gagal menambah produk"
                                                Log.e("Dashboard", "Gagal menambah produk: ${e.message}", e)
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            } finally {
                                                isLoading = false
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
                        Spacer(modifier = Modifier.height(24.dp))

                        // Daftar produk untuk pengelola
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

                    DatabaseHelper.UserRole.SUPERVISOR -> {
                        val myProfileInteractionSource = remember { MutableInteractionSource() }
                        val myProfileIsPressed by myProfileInteractionSource.collectIsPressedAsState()
                        val myProfileScale by animateFloatAsState(if (myProfileIsPressed) 0.95f else 1f)
                        val resendEmailInteractionSource = remember { MutableInteractionSource() }
                        val resendEmailIsPressed by resendEmailInteractionSource.collectIsPressedAsState()
                        val resendEmailScale by animateFloatAsState(if (resendEmailIsPressed) 0.95f else 1f)

                        if (!auth.currentUser?.isEmailVerified!!) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            auth.currentUser?.sendEmailVerification()?.await()
                                            message = "Email verifikasi dikirim. Periksa kotak masuk Anda."
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: Exception) {
                                            message = "Gagal mengirim email verifikasi: ${e.message}"
                                            Log.e("Dashboard", "Gagal mengirim email verifikasi: ${e.message}", e)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(resendEmailScale),
                                enabled = !isLoading,
                                interactionSource = resendEmailInteractionSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                elevation = ButtonDefaults.buttonElevation(4.dp)
                            ) {
                                Text("Kirim Ulang Email Verifikasi")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                navController.navigate("profile") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(myProfileScale),
                            enabled = !isLoading,
                            interactionSource = myProfileInteractionSource,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text("Profil Saya")
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Pelanggan",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (users.isEmpty()) {
                            Text(
                                "Tidak ada pelanggan ditemukan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(users) { user ->
                                    UserCard(user = user, isLoading = isLoading)
                                }
                            }
                        }
                    }

                    DatabaseHelper.UserRole.CUSTOMER -> {
                        Text(
                            "Produk",
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
                }
            }

            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Komponen untuk menampilkan kartu pengguna
@Composable
fun UserCard(
    user: Map<String, Any>,
    isLoading: Boolean,
    onPromote: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = if (user["profilePhotoUrl"]?.toString()?.isNotEmpty() == true) {
                    rememberAsyncImagePainter(
                        model = user["profilePhotoUrl"],
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error),
                        onLoading = { Log.d("UserCard", "Memuat foto pengguna: ${user["profilePhotoUrl"]}") },
                        onSuccess = { Log.d("UserCard", "Berhasil memuat foto pengguna") },
                        onError = { error ->
                            Log.e("UserCard", "Gagal memuat foto pengguna: ${error.result.throwable.message}")
                        }
                    )
                } else {
                    painterResource(R.drawable.ic_placeholder)
                },
                contentDescription = "Foto Pengguna",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Nama Pengguna: ${user["username"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Email: ${user["email"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "Role: ${user["role"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                val createdAt = user["createdAt"] as? Long
                if (createdAt != null) {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(createdAt))
                    Text(
                        text = "Dibuat: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            if (onPromote != null || onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (onPromote != null) {
                        IconButton(
                            onClick = { onPromote() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Promosikan Pengguna",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { onDelete() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}