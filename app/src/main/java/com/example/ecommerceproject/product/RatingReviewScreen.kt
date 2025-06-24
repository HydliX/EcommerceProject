package com.example.ecommerceproject.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingReviewScreen(
    orderId: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbProduct = DatabaseHelper()
    var order by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var rating by remember { mutableStateOf(0) }
    var review by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    LaunchedEffect(orderId) {
        try {
            isLoading = true
            val orders = dbProduct.getOrders()
            order = orders.find { it["orderId"] == orderId }
            if (order == null) {
                snackbarHostState.showSnackbar(
                    message = "Pesanan tidak ditemukan",
                    duration = SnackbarDuration.Long
                )
            } else {
                // Load existing rating and review if available
                val existingRating = order!!["rating"] as? Double ?: 0.0
                val existingReview = order!!["review"] as? String ?: ""
                rating = existingRating.toInt()
                review = existingReview
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar(
                message = "Gagal memuat data penilaian: ${e.message}",
                duration = SnackbarDuration.Long
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rate Your Order",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    scrolledContainerColor = Color(0xFF6200EE)
                ),
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF6200EE),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Memuat data penilaian...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF6200EE),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            } else if (order == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pesanan tidak ditemukan",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Order Header
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Order ID: ${order!!["orderId"]}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6200EE)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Status: ${order!!["status"] as? String ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFFF5722),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Ordered Items Section
                    item {
                        Text(
                            text = "Ordered Items",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EE)
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val items = order!!["items"] as? Map<String, Map<String, Any>> ?: emptyMap()
                    items(items.entries.toList()) { entry ->
                        val productId = entry.key
                        val item = entry.value
                        OrderItem(
                            item = item,
                            productId = productId
                        )
                    }

                    // Rating and Review Section
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Rate Your Experience",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6200EE)
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Star Rating
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    (1..5).forEach { star ->
                                        IconButton(
                                            onClick = { rating = star },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = if (star <= rating) Color(0xFFFF5722).copy(alpha = 0.1f) else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                contentDescription = "$star star",
                                                tint = Color(0xFFFF5722),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                // Review Text Field
                                OutlinedTextField(
                                    value = review,
                                    onValueChange = { review = it },
                                    label = { Text("Your Review") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6200EE),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                                        cursorColor = Color(0xFF6200EE)
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Submit Button
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                // Update order rating and review
                                                dbProduct.updateOrderRatingAndReview(orderId, rating.toDouble(), review)

                                                // Add rating to each product in the order
                                                items.forEach { (productId, _) ->
                                                    dbProduct.addProductRating(productId, rating.toDouble(), review)
                                                }

                                                snackbarHostState.showSnackbar(
                                                    message = "Penilaian dan ulasan berhasil disimpan",
                                                    duration = SnackbarDuration.Short
                                                )
                                                navController.popBackStack()
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(
                                                    message = "Gagal menyimpan penilaian: ${e.message}",
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    },
                                    enabled = rating > 0 && review.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6200EE),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Submit Rating",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    item: Map<String, Any>,
    productId: String
) {
    val name = item["name"] as? String ?: "N/A"
    val price = (item["price"] as? Number)?.toDouble()?.let {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(it)
    } ?: "Rp0"
    val imageUrl = item["imageUrl"]?.toString()
    val quantity = (item["quantity"] as? Number)?.toInt() ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl.takeIf { !it.isNullOrEmpty() } ?: R.drawable.ic_placeholder,
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                ),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$price x $quantity",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF5722)
                    )
                )
            }
        }
    }
}