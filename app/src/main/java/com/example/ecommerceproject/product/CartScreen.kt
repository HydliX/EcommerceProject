package com.example.ecommerceproject.product

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

    // Enhanced color scheme with more vibrant colors
    val primaryColor = Color(0xFF6366F1) // Indigo
    val secondaryColor = Color(0xFFEF4444) // Red
    val accentColor = Color(0xFF10B981) // Emerald
    val surfaceColor = Color(0xFFF8FAFC) // Light gray
    val gradientColors = listOf(
        Color(0xFFE0E7FF), // Light indigo
        Color(0xFFDDD6FE), // Light purple
        Color(0xFFFEE2E2)  // Light red
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, Color(0xFF8B5CF6))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Keranjang Belanja",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                if (cartProducts.isEmpty()) {
                    EmptyCartState(
                        primaryColor = primaryColor,
                        onExploreClick = { navController.navigate("dashboard") }
                    )
                } else {
                    // Cart items count badge
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = "Items",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${cartProducts.size} Item${if (cartProducts.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryColor
                                    )
                                )
                            }
                            Text(
                                text = "Swipe untuk menghapus",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cartProducts, key = { it["productId"] as String }) { product ->
                            val productId = product["productId"] as? String ?: ""
                            val cartItem = cartItems.find { it["productId"] == productId }
                            val quantity = (cartItem?.get("quantity") as? Number)?.toInt() ?: 0
                            val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                            val name = product["name"] as? String ?: "Produk Tidak Diketahui"
                            val category = product["category"] as? String ?: ""
                            val imageUrl = product["imageUrl"] as? String ?: ""

                            EnhancedCartItem(
                                productId = productId,
                                name = name,
                                price = price,
                                quantity = quantity,
                                category = category,
                                imageUrl = imageUrl,
                                primaryColor = primaryColor,
                                secondaryColor = secondaryColor,
                                accentColor = accentColor,
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

                    // Enhanced total price card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            primaryColor.copy(alpha = 0.1f),
                                            accentColor.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Belanja:",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    )
                                    Text(
                                        text = "Rp${String.format("%.2f", totalPrice)}",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            fontSize = 24.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Termasuk pajak dan biaya pengiriman",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enhanced checkout button
                    val interactionSourceCheckout = remember { MutableInteractionSource() }
                    val isPressedCheckout by interactionSourceCheckout.collectIsPressedAsState()
                    val scaleCheckout by animateFloatAsState(
                        targetValue = if (isPressedCheckout) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    Button(
                        onClick = { navController.navigate("checkout") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(scaleCheckout)
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, Color(0xFF8B5CF6))
                                )
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        enabled = cartProducts.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        interactionSource = interactionSourceCheckout
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Checkout",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Lanjut ke Checkout",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyCartState(
    primaryColor: Color,
    onExploreClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.2f),
                                    primaryColor.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Empty Cart",
                        tint = primaryColor,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Keranjang Anda Kosong",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Mulai berbelanja dan tambahkan produk\nke keranjang Anda",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onExploreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Shop",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Jelajahi Produk",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedCartItem(
    productId: String,
    name: String,
    price: Double,
    quantity: Int,
    category: String,
    imageUrl: String,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    onQuantityChange: (Int) -> Unit,
    onRemoveClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            primaryColor.copy(alpha = 0.02f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced product image
                Card(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AsyncImage(
                        model = imageUrl.ifEmpty { painterResource(id = R.drawable.ic_placeholder) },
                        contentDescription = "Gambar Produk",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        placeholder = painterResource(id = R.drawable.ic_placeholder),
                        error = painterResource(id = R.drawable.ic_error)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Category badge
                    Card(
                        modifier = Modifier.wrapContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rp${String.format("%.2f", price)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = secondaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Text(
                        text = "Subtotal: Rp${String.format("%.2f", price * quantity)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Enhanced quantity controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.wrapContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (quantity > 1) secondaryColor.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Kurangi Jumlah",
                                    tint = if (quantity > 1) secondaryColor else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                ),
                                modifier = Modifier.widthIn(min = 32.dp),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = { onQuantityChange(quantity + 1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = primaryColor.copy(alpha = 0.1f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Jumlah",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Delete button
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = secondaryColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .border(1.dp, secondaryColor.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}