package com.example.ecommerceproject.supervisor

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.util.StatusBadge
import com.example.ecommerceproject.util.formatPrice
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupervisorDashboard(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // State untuk semua data yang dibutuhkan di dashboard ini
    var reports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var supervisorProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadData() {
        coroutineScope.launch {
            isLoading = true
            try {
                // Mengambil semua data secara bersamaan
                val supervisorId = FirebaseAuth.getInstance().currentUser?.uid
                if (supervisorId != null) {
                    supervisorProfile = dbHelper.getUserProfileById(supervisorId)
                }
                reports = dbHelper.getAllReports()
                orders = dbHelper.getAllOrders()
            } catch (e: Exception) {
                Log.e("SupervisorDashboard", "Gagal memuat data: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // REVISI: TopAppBar dengan sapaan selamat datang
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dashboard Supervisor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        supervisorProfile?.get("username")?.let {
                            Text(
                                "Welcome, $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        // REVISI: Menambahkan Bottom Navigation Bar
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { /* Anda sudah di sini */ },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profil") },
                    label = { Text("Profil") },
                    selected = false,
                    onClick = { navController.navigate("profile") }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Pengaturan") },
                    label = { Text("Pengaturan") },
                    selected = false,
                    onClick = { navController.navigate("settings") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- BAGIAN RINGKASAN ---
            item {
                Text(
                    "Ringkasan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    InfoCard(modifier = Modifier.weight(1f), title = "Total Laporan", value = reports.size.toString())
                    Spacer(modifier = Modifier.width(16.dp))
                    InfoCard(modifier = Modifier.weight(1f), title = "Total Pesanan", value = orders.size.toString())
                }
            }

            // --- BAGIAN LAPORAN PENGGUNA ---
            item {
                Text(
                    text = "Laporan Pengguna Masuk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (reports.isEmpty()) {
                item {
                    Text("Tidak ada laporan ditemukan.", modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(reports, key = { it["reportId"] as String }) { report ->
                    ReportCard(
                        report = report,
                        onBlockUser = { userId, isBlocked ->
                            coroutineScope.launch {
                                try {
                                    dbHelper.blockUser(userId, isBlocked)
                                    snackbarHostState.showSnackbar("Status blokir pengguna diperbarui.")
                                    loadData()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }

            // --- BAGIAN PEMANTAUAN PESANAN ---
            item {
                Text(
                    text = "Pemantauan Semua Pesanan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            if (isLoading) {
                // Indikator loading sudah ditampilkan di atas
            } else if (orders.isEmpty()) {
                item { Text("Tidak ada pesanan ditemukan.", modifier = Modifier.padding(vertical = 8.dp)) }
            } else {
                items(orders, key = { "order-${it["orderId"]}" }) { order ->
                    SupervisorOrderCard(order = order)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun ReportCard(report: Map<String, Any>, onBlockUser: (String, Boolean) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    val reporterProfile = report["reporterProfile"] as? Map<String, Any>
    val reportedProfile = report["reportedUserProfile"] as? Map<String, Any>

    val reporterName = reporterProfile?.get("username") as? String ?: "N/A"
    val reportedName = reportedProfile?.get("username") as? String ?: "N/A"
    val reportedUserId = reportedProfile?.get("userId") as? String ?: ""
    val isBlocked = reportedProfile?.get("isBlocked") as? Boolean ?: false

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Laporan untuk: $reportedName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pelapor: $reporterName",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                "Alasan: \"${report["reason"] as? String ?: ""}\"",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (isExpanded && reportedUserId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onBlockUser(reportedUserId, !isBlocked) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBlocked) "Buka Blokir Akun" else "Blokir Akun Ini")
                }
            }
        }
    }
}


@Composable
fun InfoCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SupervisorOrderCard(order: Map<String, Any>) {
    val dateFormat = remember { SimpleDateFormat("dd MMM YYYY, HH:mm", Locale("id", "ID")) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: #${(order["orderId"] as? String)?.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = order["status"] as? String ?: "UNKNOWN")
            }
            val customerProfile = order["customerProfile"] as? Map<String, Any>
            val customerName = customerProfile?.get("username") as? String ?: "N/A"
            Text(text = "Customer: $customerName", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Total: ${formatPrice(order["totalPrice"] as? Double ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = dateFormat.format(Date(order["createdAt"] as? Long ?: 0L)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
