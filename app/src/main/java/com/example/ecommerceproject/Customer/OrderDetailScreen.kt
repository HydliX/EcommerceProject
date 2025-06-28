package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.util.StatusBadge
import com.example.ecommerceproject.util.formatPrice
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = remember { DatabaseHelper() }
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showRatingDialog by remember { mutableStateOf(false) }

    fun loadOrderDetails() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                order = dbHelper.getOrderById(orderId)
                if (order == null) throw Exception("Pesanan tidak ditemukan.")
            } catch (e: Exception) {
                errorMessage = "Gagal memuat detail: ${e.message}"
                snackbarHostState.showSnackbar(errorMessage!!)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        loadOrderDetails()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detail Pesanan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }
            }
            order != null -> {
                val currentOrder = order!!
                val status = currentOrder["status"] as? String ?: DatabaseHelper.OrderStatus.PENDING

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Detail
                    OrderHeader(currentOrder)

                    // Status Tracker
                    OrderStatusTracker(status = status)

                    // Tombol Aksi Customer
                    if (status == DatabaseHelper.OrderStatus.DIKIRIM) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        dbHelper.updateOrderStatus(orderId, DatabaseHelper.OrderStatus.DITERIMA)
                                        snackbarHostState.showSnackbar("Pesanan telah diterima!")
                                        loadOrderDetails() // Refresh
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Konfirmasi Pesanan Diterima")
                        }
                    }

                    // Bagian Ulasan dan Rating
                    ReviewSection(
                        order = currentOrder,
                        onGiveReviewClick = { showRatingDialog = true }
                    )

                    // Rincian Item (jika perlu)
                    ItemListSection(currentOrder)
                }

                if (showRatingDialog) {
                    RatingDialog(
                        onDismiss = { showRatingDialog = false },
                        onSubmit = { rating, review ->
                            coroutineScope.launch {
                                try {
                                    dbHelper.updateOrderRatingAndReview(orderId, rating, review)
                                    // Setelah rating, ubah status menjadi SELESAI
                                    dbHelper.updateOrderStatus(orderId, DatabaseHelper.OrderStatus.SELESAI)
                                    snackbarHostState.showSnackbar("Ulasan berhasil dikirim!")
                                    showRatingDialog = false
                                    loadOrderDetails() // Refresh
                                } catch(e: Exception) {
                                    snackbarHostState.showSnackbar("Gagal mengirim ulasan: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderHeader(order: Map<String, Any>) {
    val orderId = order["orderId"] as? String ?: ""
    val createdAt = (order["createdAt"] as? Long) ?: 0L
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }

    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ID Pesanan: #${orderId.takeLast(8)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusBadge(status = order["status"] as? String ?: "N/A")
            }
            Text("Tanggal: ${dateFormat.format(Date(createdAt))}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Total: ${formatPrice(order["totalPrice"] as? Double ?: 0.0)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun OrderStatusTracker(status: String) {
    val allStatuses = listOf(
        DatabaseHelper.OrderStatus.PENDING,
        DatabaseHelper.OrderStatus.DIKEMAS,
        DatabaseHelper.OrderStatus.DIKIRIM,
        DatabaseHelper.OrderStatus.DITERIMA,
    )
    val currentStatusIndex = allStatuses.indexOf(status)

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        allStatuses.forEachIndexed { index, stepStatus ->
            val isActive = index <= currentStatusIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Icon(Icons.Default.Check, contentDescription = "Selesai", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (index < allStatuses.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(32.dp)
                                .background(if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = stepStatus.replaceFirstChar { it.titlecase(Locale.ROOT) },
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else Color.Gray
                    )
                    val description = when(stepStatus) {
                        DatabaseHelper.OrderStatus.PENDING -> "Pesanan Anda sedang menunggu konfirmasi."
                        DatabaseHelper.OrderStatus.DIKEMAS -> "Pesanan Anda sedang disiapkan dan dikemas."
                        DatabaseHelper.OrderStatus.DIKIRIM -> "Pesanan Anda telah diserahkan ke kurir."
                        DatabaseHelper.OrderStatus.DITERIMA -> "Anda telah menerima pesanan."
                        else -> ""
                    }
                    if(description.isNotEmpty()) {
                        Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewSection(order: Map<String, Any>, onGiveReviewClick: () -> Unit) {
    val status = order["status"] as? String ?: ""
    val rating = (order["rating"] as? Number)?.toDouble()
    val review = order["review"] as? String

    if (status == DatabaseHelper.OrderStatus.DITERIMA) {
        OutlinedButton(
            onClick = onGiveReviewClick,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Beri Ulasan & Rating")
        }
    } else if (status == DatabaseHelper.OrderStatus.SELESAI && rating != null && review != null) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ulasan Anda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rating: ", fontWeight = FontWeight.SemiBold)
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(" ($rating/5.0)", style = MaterialTheme.typography.bodySmall)
                }
                Text("Ulasan: \"$review\"", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun ItemListSection(order: Map<String, Any>) {
    val items = order["items"] as? Map<String, Map<String, Any>>
    if (items != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rincian Produk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            items.values.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(item["quantity"] as? Long)?.toInt() ?: 0}x",
                        modifier = Modifier.width(30.dp)
                    )
                    Text(
                        text = item["name"] as? String ?: "Nama Produk",
                        modifier = Modifier.weight(1f)
                    )
                    Text(formatPrice(item["price"] as? Double ?: 0.0))
                }
                Divider()
            }
        }
    }
}

@Composable
fun RatingDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Double, review: String) -> Unit
) {
    var rating by remember { mutableStateOf(0.0) }
    var review by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Beri Ulasan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Text("Bagaimana penilaian Anda?", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star.toDouble() }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star Bintang",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Tulis ulasan Anda di sini") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )

                if (showError) {
                    Text(
                        "Rating dan ulasan tidak boleh kosong.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (rating > 0 && review.isNotBlank()) {
                            onSubmit(rating, review)
                        } else {
                            showError = true
                        }
                    }) {
                        Text("Kirim")
                    }
                }
            }
        }
    }
}