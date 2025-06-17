package com.example.ecommerceproject.supervisor

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.UserCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = remember { DatabaseHelper() }
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) } // Tambahan untuk orders
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }

    // LaunchedEffect untuk menangani efek samping dan pembatalan otomatis
    LaunchedEffect(Unit) {
        try {
            localIsLoading = true
            Log.d(
                "SupervisorDashboard",
                "Mengambil pengguna customer, aduan, dan pesanan untuk supervisor"
            )
            users = dbHelper.getAllUsers(DatabaseHelper.UserRole.CUSTOMER)
            complaints = dbHelper.getAllComplaints()
            orders = dbHelper.getAllOrders() // Tambahkan pengambilan orders
        } catch (e: Exception) {
            localMessage = e.message ?: "Gagal memuat data"
            Log.e("SupervisorDashboard", "Gagal memuat data: ${e.message}", e)
            launch {
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
                DatabaseHelper.UserRole.SUPERVISOR.replaceFirstChar { it.uppercase() },
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
                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val user: FirebaseUser? = auth.currentUser
                                    user?.sendEmailVerification()?.await()
                                    localMessage =
                                        "Email verifikasi dikirim. Periksa kotak masuk Anda."
                                    snackbarHostState.showSnackbar(
                                        message = localMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                } catch (e: Exception) {
                                    localMessage = "Gagal mengirim email verifikasi: ${e.message}"
                                    Log.e(
                                        "SupervisorDashboard",
                                        "Gagal mengirim email verifikasi: ${e.message}",
                                        e
                                    )
                                    snackbarHostState.showSnackbar(
                                        message = localMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(resendEmailScale),
                        enabled = !localIsLoading,
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
                    enabled = !localIsLoading,
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
                            UserCard(user = user, isLoading = localIsLoading)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Aduan Pelanggan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (complaints.isEmpty()) {
                    Text(
                        "Tidak ada aduan ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(complaints) { complaint ->
                            ComplaintCard(complaint = complaint, dateFormat = dateFormat)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Ulasan Pesanan",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (orders.isEmpty()) {
                    Text(
                        "Tidak ada ulasan pesanan ditemukan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(orders.filter { it["rating"] != null && it["review"] != null }) { order ->
                            OrderReviewCard(order = order, dateFormat = dateFormat)
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

    @Composable
    fun ComplaintCard(complaint: Map<String, Any>, dateFormat: SimpleDateFormat) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Complaint from User: ${complaint["userId"]}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = complaint["text"] as? String ?: "No description",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${complaint["status"] as? String ?: "Pending"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (complaint["status"] == "Resolved") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.7f
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date: ${dateFormat.format(Date(complaint["createdAt"] as? Long ?: 0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    fun OrderReviewCard(order: Map<String, Any>, dateFormat: SimpleDateFormat) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Order ID: ${order["orderId"]}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "User: ${order["userId"]}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rating: ${(order["rating"] as? Number)?.toDouble() ?: 0.0} / 5",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Review: ${order["review"] as? String ?: "No review"}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date: ${dateFormat.format(Date(order["updatedAt"] as? Long ?: order["createdAt"] as? Long ?: 0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun OrderReviewCard(order: Map<String, Any>, dateFormat: SimpleDateFormat) {
    TODO("Not yet implemented")
}

@Composable
fun ComplaintCard(complaint: Map<String, Any>, dateFormat: SimpleDateFormat) {
    TODO("Not yet implemented")
}
