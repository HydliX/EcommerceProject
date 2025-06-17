package com.example.ecommerceproject.product

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cartProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var totalPrice by remember { mutableStateOf(0.0) }

    // Colors for e-commerce theme
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.05f),
        secondaryColor.copy(alpha = 0.05f)
    )

    // Fetch cart items and corresponding product details
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                DatabaseHelper.initCloudinary(context)
                cartItems = DatabaseHelper().getCart()
                cartProducts = cartItems.mapNotNull { item ->
                    val productId = item["productId"] as? String
                    if (productId != null) DatabaseHelper().getProductById(productId) else null
                }
                totalPrice = cartProducts.sumOf { product ->
                    val quantity = (cartItems.find { it["productId"] == product["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                    val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                    price * quantity
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Gagal memuat keranjang: ${e.message}")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.background(Brush.verticalGradient(gradientColors)),
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keranjang Belanja",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
                    )
                    .shadow(4.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .height(64.dp)
                    .padding(horizontal = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (cartProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Empty Cart",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Keranjang Anda kosong.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate("dashboard") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Jelajahi Produk")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartProducts, key = { it["productId"] as String }) { product ->
                        val productId = product["productId"] as? String ?: ""
                        val cartItem = cartItems.find { it["productId"] == productId }
                        val quantity = (cartItem?.get("quantity") as? Number)?.toInt() ?: 0
                        val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                        val name = product["name"] as? String ?: "Produk Tidak Diketahui"
                        val category = product["category"] as? String ?: ""
                        val imageUrl = product["imageUrl"] as? String ?: ""

                        CartItem(
                            productId = productId,
                            name = name,
                            price = price,
                            quantity = quantity,
                            category = category,
                            imageUrl = imageUrl,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            onQuantityChange = { newQuantity ->
                                scope.launch {
                                    try {
                                        DatabaseHelper().updateCartItem(productId, newQuantity)
                                        cartItems = DatabaseHelper().getCart()
                                        cartProducts = cartItems.mapNotNull { item ->
                                            val pid = item["productId"] as? String
                                            if (pid != null) DatabaseHelper().getProductById(pid) else null
                                        }
                                        totalPrice = cartProducts.sumOf { p ->
                                            val q = (cartItems.find { it["productId"] == p["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                                            val pr = (p["price"] as? Number)?.toDouble() ?: 0.0
                                            pr * q
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal memperbarui jumlah: ${e.message}")
                                    }
                                }
                            },
                            onRemoveClick = {
                                scope.launch {
                                    try {
                                        DatabaseHelper().updateCartItem(productId, 0)
                                        cartItems = DatabaseHelper().getCart()
                                        cartProducts = cartItems.mapNotNull { item ->
                                            val pid = item["productId"] as? String
                                            if (pid != null) DatabaseHelper().getProductById(pid) else null
                                        }
                                        totalPrice = cartProducts.sumOf { p ->
                                            val q = (cartItems.find { it["productId"] == p["productId"] }?.get("quantity") as? Number)?.toInt() ?: 0
                                            val pr = (p["price"] as? Number)?.toDouble() ?: 0.0
                                            pr * q
                                        }
                                        snackbarHostState.showSnackbar("Item dihapus dari keranjang")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal menghapus item: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Belanja:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        )
                        Text(
                            text = "Rp${String.format("%.2f", totalPrice)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = secondaryColor
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val interactionSourceCheckout = remember { MutableInteractionSource() }
                val isPressedCheckout by interactionSourceCheckout.collectIsPressedAsState()
                val scaleCheckout by animateFloatAsState(
                    targetValue = if (isPressedCheckout) 0.95f else 1f,
                    animationSpec = tween(150)
                )

                Button(
                    onClick = { navController.navigate("checkout") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .scale(scaleCheckout)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    enabled = cartProducts.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Lanjut ke Checkout",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CartItem(
    productId: String,
    name: String,
    price: Double,
    quantity: Int,
    category: String,
    imageUrl: String,
    primaryColor: Color,
    secondaryColor: Color,
    onQuantityChange: (Int) -> Unit,
    onRemoveClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Optional: Navigate to product detail */ },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl.ifEmpty { painterResource(id = R.drawable.ic_placeholder) },
                contentDescription = "Gambar Produk",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                placeholder = painterResource(id = R.drawable.ic_placeholder),
                error = painterResource(id = R.drawable.ic_error)
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
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Harga: Rp${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = secondaryColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Kategori: $category",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )
                Text(
                    text = "Subtotal: Rp${String.format("%.2f", price * quantity)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = secondaryColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Kurangi Jumlah",
                            tint = secondaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "$quantity",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        ),
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { onQuantityChange(quantity + 1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = primaryColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Jumlah",
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = secondaryColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = secondaryColor
                    )
                }
            }
        }
    }
}
