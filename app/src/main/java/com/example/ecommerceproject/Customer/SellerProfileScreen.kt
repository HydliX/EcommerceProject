package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    sellerId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    var sellerProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var sellerProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val currentUserId = dbHelper.CurrentUserId()

    LaunchedEffect(sellerId) {
        isLoading = true
        try {
            sellerProfile = dbHelper.getUserProfileById(sellerId)
            sellerProducts = dbHelper.getProductsByCreator(sellerId)
        } catch (e: Exception) {
            Log.e("SellerProfileScreen", "Gagal memuat profil penjual", e)
            snackbarHostState.showSnackbar("Gagal memuat data penjual.")
            navController.popBackStack()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Penjual") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol Report hanya muncul jika yg melihat bukan si penjual itu sendiri
                    if (currentUserId != sellerId) {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Laporkan Akun") },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Report, contentDescription = "Laporkan")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (sellerProfile != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val photoUrl = sellerProfile!!["profilePhotoUrl"] as? String ?: ""
                Image(
                    painter = rememberAsyncImagePainter(model = if (photoUrl.isNotEmpty()) photoUrl else R.drawable.ic_placeholder),
                    contentDescription = "Foto Profil Penjual",
                    modifier = Modifier.size(120.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = sellerProfile!!["username"] as? String ?: "Nama Penjual",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Penjual Sejak: ${java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sellerProfile!!["createdAt"] as? Long ?: 0L))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Produk dari Penjual Ini",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Start)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sellerProducts) { product ->
                        // Anda bisa membuat Composable card produk yang lebih kecil di sini
                        Card(onClick = { navController.navigate("productDetail/${product["productId"]}") }) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(product["name"] as? String ?: "N/A", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (showReportDialog) {
            ReportDialog(
                onDismiss = { showReportDialog = false },
                onSubmit = { reason ->
                    coroutineScope.launch {
                        try {
                            dbHelper.submitReport(reportedUserId = sellerId, reason = reason)
                            snackbarHostState.showSnackbar("Laporan telah dikirim. Terima kasih.")
                            showReportDialog = false
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Gagal mengirim laporan: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Laporkan Akun", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; isError = false },
                    label = { Text("Alasan Laporan") },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text("Alasan tidak boleh kosong", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (reason.isBlank()) {
                            isError = true
                        } else {
                            onSubmit(reason)
                        }
                    }) {
                        Text("Kirim")
                    }
                }
            }
        }
    }
}
