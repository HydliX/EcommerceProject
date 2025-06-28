package com.example.ecommerceproject.supervisor

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
// REVISI: Menghapus import yang salah dan memastikan hanya import dari 'util' yang digunakan
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
    userProfile: Map<String, Any>?,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    var complaints by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    var localIsLoading by remember { mutableStateOf(true) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        localIsLoading = true
        try {
            Log.d("SupervisorDashboard", "Mengambil data untuk supervisor")
            complaints = dbHelper.getAllComplaints()
            orders = dbHelper.getAllOrders().sortedByDescending { it["createdAt"] as? Long ?: 0L }
        } catch (e: Exception) {
            localMessage = e.message ?: "Gagal memuat data"
            Log.e("SupervisorDashboard", "Gagal memuat data: ${e.message}", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = localMessage!!,
                    duration = SnackbarDuration.Long
                )
            }
        } finally {
            localIsLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Supervisor", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Bottom navigation bar can be added here if needed
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Ringkasan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)) {
                    InfoCard(modifier = Modifier.weight(1f), title = "Total Aduan", value = complaints.size.toString())
                    Spacer(modifier = Modifier.width(16.dp))
                    InfoCard(modifier = Modifier.weight(1f), title = "Total Pesanan", value = orders.size.toString())
                }
            }

            // Bagian Aduan Pelanggan
            item {
                Text(
                    text = "Aduan Pelanggan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (localIsLoading) {
                item {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (complaints.isEmpty()) {
                item { Text("Tidak ada aduan ditemukan.", modifier = Modifier.padding(vertical = 16.dp)) }
            } else {
                items(complaints, key = { "complaint-${it["complaintId"]}" }) { complaint ->
                    ComplaintCard(complaint, dbHelper, snackbarHostState) {
                        // Refresh logic on update
                        coroutineScope.launch {
                            localIsLoading = true
                            complaints = dbHelper.getAllComplaints()
                            localIsLoading = false
                        }
                    }
                }
            }

            // Bagian Pemantauan Pesanan
            item {
                Text(
                    text = "Pemantauan Pesanan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            if (localIsLoading) {
                item {
                    // Loading indicator is already shown above
                }
            } else if (orders.isEmpty()) {
                item { Text("Tidak ada pesanan ditemukan.", modifier = Modifier.padding(vertical = 16.dp)) }
            } else {
                items(orders, key = { "order-${it["orderId"]}" }) { order ->
                    SupervisorOrderCard(order = order, dateFormat = dateFormat)
                }
            }
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ComplaintCard(
    complaint: Map<String, Any>,
    dbHelper: DatabaseHelper,
    snackbarHostState: SnackbarHostState,
    onUpdate: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val complaintId = complaint["complaintId"] as? String ?: ""
    val status = complaint["status"] as? String ?: "Pending"
    val coroutineScope = rememberCoroutineScope()


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isExpanded = !isExpanded }
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aduan #${complaintId.takeLast(6)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    color = if (status == "Resolved") Color.Green else Color.Red,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                "Pengguna: ${complaint["userId"] as? String ?: "N/A"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = complaint["text"] as? String ?: "",
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            AnimatedVisibility(visible = isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (status == "Pending") {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        dbHelper.updateComplaint(complaintId, mapOf("status" to "Resolved"))
                                        onUpdate()
                                        snackbarHostState.showSnackbar("Status aduan diperbarui.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Tandai Selesai")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SupervisorOrderCard(order: Map<String, Any>, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Text(
                text = "Customer: $customerName",
                style = MaterialTheme.typography.bodyMedium
            )
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