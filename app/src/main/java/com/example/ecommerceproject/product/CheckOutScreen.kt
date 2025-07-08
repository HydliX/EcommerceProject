package com.example.ecommerceproject.product

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val dbHelper = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // Enhanced color scheme
    val primaryColor = Color(0xFF6366F1) // Indigo
    val secondaryColor = Color(0xFFEF4444) // Red
    val accentColor = Color(0xFF10B981) // Emerald
    val surfaceColor = Color(0xFFF8FAFC) // Light gray
    val gradientColors = listOf(
        Color(0xFFE0E7FF), // Light indigo
        Color(0xFFDDD6FE), // Light purple
        Color(0xFFFEE2E2)  // Light red
    )

    // State untuk menampung data produk yang sudah digabung dengan kuantitas keranjang
    var checkoutItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var shippingAddress by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var shippingService by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var totalPrice by remember { mutableStateOf(0.0) }
    var shippingExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }

    // Opsi jasa pengiriman dan metode pembayaran
    val shippingServices = listOf("JNE", "J&T", "SiCepat", "AnterAja")
    val shippingCostPerKg = mapOf(
        "JNE" to 10000.0, // Cost per kg in IDR
        "J&T" to 12000.0,
        "SiCepat" to 11000.0,
        "AnterAja" to 9000.0
    )
    val paymentMethods = listOf("COD", "Transfer Bank", "Pay Later")

    // Memuat item keranjang dan detail produknya
    LaunchedEffect(Unit, shippingService) { // Add shippingService to trigger recalculation
        isLoading = true
        coroutineScope.launch {
            try {
                // 1. Ambil data dasar dari keranjang (productId & quantity)
                val cartItems = dbHelper.getCart()
                if (cartItems.isEmpty()) {
                    snackbarHostState.showSnackbar("Keranjang Anda kosong.")
                    navController.popBackStack() // Kembali jika keranjang kosong
                    return@launch
                }

                // 2. Ambil detail lengkap untuk setiap produk di keranjang
                val detailedItems = cartItems.mapNotNull { cartItem ->
                    val productId = cartItem["productId"] as? String
                    val quantity = (cartItem["quantity"] as? Number)?.toInt() ?: 0
                    if (productId != null) {
                        // Ambil detail produk dari database
                        dbHelper.getProductById(productId)?.let { productDetail ->
                            // Gabungkan detail produk dengan kuantitas dari keranjang
                            productDetail + mapOf("quantity" to quantity)
                        }
                    } else {
                        null
                    }
                }
                checkoutItems = detailedItems

                // 3. Hitung total harga dari data yang sudah digabung
                totalPrice = detailedItems.sumOf { item ->
                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                    price * quantity
                } + detailedItems.sumOf { item ->
                    val weight = (item["weight"] as? Number)?.toDouble() ?: 0.0
                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                    val costPerKg = shippingCostPerKg[shippingService] ?: 0.0
                    weight * quantity * costPerKg
                }

            } catch (e: Exception) {
                Log.e("CheckoutScreen", "Gagal memuat item checkout: ${e.message}", e)
                snackbarHostState.showSnackbar("Gagal memuat data: ${e.message}")
            } finally {
                isLoading = false
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
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Checkout",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Checkout",
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .size(120.dp)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 20.dp)
                ) {
                    // Order Summary Section
                    item {
                        EnhancedSectionCard(
                            title = "Ringkasan Pesanan",
                            icon = Icons.Default.ShoppingCart,
                            primaryColor = primaryColor
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                checkoutItems.forEach { item ->
                                    val name = item["name"] as? String ?: "Produk Tidak Ditemukan"
                                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                                    val weight = (item["weight"] as? Number)?.toDouble() ?: 0.0
                                    val shippingCost = weight * quantity * (shippingCostPerKg[shippingService] ?: 0.0)

                                    OrderItemRow(
                                        name = name,
                                        quantity = quantity,
                                        price = price,
                                        totalPrice = price * quantity,
                                        shippingCost = shippingCost, // Add shipping cost parameter
                                        primaryColor = primaryColor,
                                        accentColor = accentColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = primaryColor.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Subtotal Produk:",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryColor
                                                )
                                            )
                                            Text(
                                                "Rp${String.format("%.2f", checkoutItems.sumOf { item ->
                                                    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                                                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                                    price * quantity
                                                })}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Biaya Pengiriman:",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryColor
                                                )
                                            )
                                            Text(
                                                "Rp${String.format("%.2f", checkoutItems.sumOf { item ->
                                                    val weight = (item["weight"] as? Number)?.toDouble() ?: 0.0
                                                    val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                                    val costPerKg = shippingCostPerKg[shippingService] ?: 0.0
                                                    weight * quantity * costPerKg
                                                })}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Total Harga:",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryColor
                                                )
                                            )
                                            Text(
                                                "Rp${String.format("%.2f", totalPrice)}",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor,
                                                    fontSize = 22.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shipping Address Section
                    item {
                        EnhancedSectionCard(
                            title = "Alamat Pengiriman",
                            icon = Icons.Default.Home,
                            primaryColor = primaryColor
                        ) {
                            EnhancedTextField(
                                value = shippingAddress,
                                onValueChange = { shippingAddress = it },
                                label = "Masukkan alamat lengkap",
                                placeholder = "Jl. Contoh No. 123, Kota, Provinsi",
                                primaryColor = primaryColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Shipping Service Section
                    item {
                        EnhancedSectionCard(
                            title = "Jasa Pengiriman",
                            icon = Icons.Default.LocalShipping,
                            primaryColor = primaryColor
                        ) {
                            EnhancedDropdown(
                                value = shippingService,
                                options = shippingServices,
                                label = "Pilih jasa pengiriman",
                                expanded = shippingExpanded,
                                onExpandedChange = { shippingExpanded = it },
                                onSelectionChange = { shippingService = it },
                                primaryColor = primaryColor
                            )
                        }
                    }

                    // Payment Method Section
                    item {
                        EnhancedSectionCard(
                            title = "Metode Pembayaran",
                            icon = Icons.Default.CreditCard,
                            primaryColor = primaryColor
                        ) {
                            EnhancedDropdown(
                                value = paymentMethod,
                                options = paymentMethods,
                                label = "Pilih metode pembayaran",
                                expanded = paymentExpanded,
                                onExpandedChange = { paymentExpanded = it },
                                onSelectionChange = { paymentMethod = it },
                                primaryColor = primaryColor
                            )
                        }
                    }

                    // Checkout Button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.96f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        // Validasi input
                                        if (shippingAddress.isBlank()) {
                                            snackbarHostState.showSnackbar("Alamat pengiriman harus diisi")
                                            return@launch
                                        }
                                        if (shippingService.isBlank()) {
                                            snackbarHostState.showSnackbar("Pilih jasa pengiriman")
                                            return@launch
                                        }
                                        if (paymentMethod.isBlank()) {
                                            snackbarHostState.showSnackbar("Pilih metode pembayaran")
                                            return@launch
                                        }

                                        // Update stok produk
                                        checkoutItems.forEach { item ->
                                            val productId = item["productId"] as? String ?: return@forEach
                                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                                            dbHelper.updateProductStock(productId, quantity)
                                        }

                                        // Format items untuk disimpan ke database
                                        val orderItems = checkoutItems.associate { item ->
                                            val productId = item["productId"] as? String ?: ""
                                            productId to mapOf(
                                                "name" to (item["name"] as? String ?: ""),
                                                "price" to (item["price"] as? Number ?: 0.0),
                                                "quantity" to (item["quantity"] as? Number ?: 0),
                                                "imageUrl" to (item["imageUrl"] as? String ?: "")
                                            )
                                        }

                                        // Buat pesanan
                                        val orderId = dbHelper.createOrder(
                                            items = orderItems,
                                            totalPrice = totalPrice,
                                            shippingAddress = shippingAddress,
                                            paymentMethod = paymentMethod,
                                            shippingService = shippingService
                                        )

                                        // Navigasi ke OrderConfirmationScreen
                                        navController.navigate("orderConfirmation/$orderId") {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }

                                        snackbarHostState.showSnackbar("Pesanan berhasil dibuat!")
                                    } catch (e: Exception) {
                                        Log.e("CheckoutScreen", "Gagal menyelesaikan pesanan: ${e.message}", e)
                                        snackbarHostState.showSnackbar("Gagal membuat pesanan: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(scale)
                                .shadow(12.dp, RoundedCornerShape(16.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White
                            ),
                            enabled = checkoutItems.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            interactionSource = interactionSource
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(accentColor, Color(0xFF059669))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Complete Order",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Selesaikan Pesanan",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedSectionCard(
    title: String,
    icon: ImageVector,
    primaryColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            primaryColor.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
            content()
        }
    }
}

@Composable
fun OrderItemRow(
    name: String,
    quantity: Int,
    price: Double,
    totalPrice: Double,
    shippingCost: Double, // New parameter for shipping cost
    primaryColor: Color,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        ),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Qty: $quantity × Rp${String.format("%.2f", price)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biaya Pengiriman: Rp${String.format("%.2f", shippingCost)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                }
                Text(
                    text = "Rp${String.format("%.2f", totalPrice + shippingCost)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.Gray) },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            focusedLabelColor = primaryColor,
            cursorColor = primaryColor
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = false,
        maxLines = 3
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDropdown(
    value: String,
    options: List<String>,
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectionChange: (String) -> Unit,
    primaryColor: Color
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = primaryColor
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                focusedLabelColor = primaryColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(Color.White)
                .clip(RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelectionChange(option)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
