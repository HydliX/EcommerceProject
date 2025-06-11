package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    snackbarHostState: SnackbarHostState
) {
    // Dependencies
    val auth = FirebaseAuth.getInstance()
    val dbProduct = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()

    // States
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wishlist by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var selectedTab by remember { mutableStateOf(0) }
    val username = userProfile?.get("name") as? String ?: "Customer"
    var cartItemCount by remember { mutableStateOf(0) }

    // Colors for e-commerce theme
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFF03DAC5)
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.1f),
        secondaryColor.copy(alpha = 0.1f)
    )

    // Data Loading Logic
    fun loadData(forceReload: Boolean = false) {
        coroutineScope.launch {
            if (!forceReload && !isLoading) return@launch
            isLoading = true
            hasError = false
            try {
                auth.currentUser?.reload()?.await()
                isEmailVerified = auth.currentUser?.isEmailVerified == true

                if (!isEmailVerified) {
                    throw IllegalStateException("Please verify your email to access the dashboard.")
                }

                dbProduct.ensureUserProfile()
                products = dbProduct.getAllProducts()
                wishlist = dbProduct.getWishlist()
                orders = dbProduct.getOrders()
                cartItemCount = dbProduct.getCart().size
            } catch (e: Exception) {
                hasError = true
                errorMessage = e.message ?: "Failed to load data."
                Log.e("CustomerDashboard", "Error loading data: $errorMessage", e)
                snackbarHostState.showSnackbar(errorMessage, duration = SnackbarDuration.Long)
            } finally {
                isLoading = false
            }
        }
    }

    // Initial data load
    LaunchedEffect(Unit) {
        loadData(forceReload = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.background(Brush.verticalGradient(gradientColors)),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Shop Now",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = secondaryColor,
                                    contentColor = Color.White
                                ) { Text("$cartItemCount") }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = primaryColor,
                modifier = Modifier.shadow(8.dp)
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (navController.currentDestination?.route == "dashboard") primaryColor else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Home",
                            color = if (navController.currentDestination?.route == "dashboard") primaryColor else Color.Gray
                        )
                    },
                    selected = navController.currentDestination?.route == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = if (navController.currentDestination?.route == "profile") primaryColor else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Profile",
                            color = if (navController.currentDestination?.route == "profile") primaryColor else Color.Gray
                        )
                    },
                    selected = navController.currentDestination?.route == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (navController.currentDestination?.route == "settings") primaryColor else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            color = if (navController.currentDestination?.route == "settings") primaryColor else Color.Gray
                        )
                    },
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
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Welcome, $username!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            )
            Text(
                text = "Explore our latest collections",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isEmailVerified) {
                EmailVerificationCard(auth, snackbarHostState, coroutineScope)
            } else {
                CustomerTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    isLoading = isLoading,
                    hasError = hasError,
                    errorMessage = errorMessage,
                    products = products,
                    wishlist = wishlist,
                    orders = orders,
                    navController = navController,
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope,
                    dbProduct = dbProduct,
                    onRefreshData = { loadData(forceReload = true) },
                    onCartUpdate = { newCount -> cartItemCount = newCount }
                )
            }
        }
    }
}

@Composable
private fun CustomerTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isLoading: Boolean,
    hasError: Boolean,
    errorMessage: String,
    products: List<Map<String, Any>>,
    wishlist: List<Map<String, Any>>,
    orders: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    dbProduct: DatabaseHelper,
    onRefreshData: () -> Unit,
    onCartUpdate: (Int) -> Unit
) {
    val tabs = listOf("Products", "Wishlist", "Orders")
    val primaryColor = Color(0xFF6200EE)

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = primaryColor,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = primaryColor
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                selectedContentColor = primaryColor,
                unselectedContentColor = Color.Gray
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = primaryColor)
        }
    } else if (hasError) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRefreshData,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Try Again")
                }
            }
        }
    } else {
        when (selectedTab) {
            0 -> ProductGridScreen(products, wishlist, navController, snackbarHostState, coroutineScope, dbProduct, onRefreshData, onCartUpdate)
            1 -> WishlistScreen(wishlist, navController, snackbarHostState, coroutineScope, dbProduct, onRefreshData)
            2 -> OrderHistoryScreen(orders, navController)
        }
    }
}

@Composable
fun ProductGridScreen(
    products: List<Map<String, Any>>,
    wishlist: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    db: DatabaseHelper,
    onRefresh: () -> Unit,
    onCartUpdate: (Int) -> Unit
) {
    val primaryColor = Color(0xFF6200EE)

    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No products found.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products, key = { it["productId"] as String }) { product ->
            val productId = product["productId"] as String
            val isInWishlist = wishlist.any { it["productId"] == productId }

            ProductGridItem(
                product = product,
                isInWishlist = isInWishlist,
                onProductClick = { navController.navigate("productDetail/$productId") },
                onAddToCartClick = {
                    scope.launch {
                        try {
                            db.addToCart(productId, 1)
                            onCartUpdate(db.getCart().size)
                            snackbarHostState.showSnackbar("Product added to cart")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed: ${e.message}")
                        }
                    }
                },
                onWishlistClick = {
                    scope.launch {
                        try {
                            if (isInWishlist) {
                                db.removeFromWishlist(productId)
                                snackbarHostState.showSnackbar("Removed from Wishlist")
                            } else {
                                db.addToWishlist(productId)
                                snackbarHostState.showSnackbar("Added to Wishlist")
                            }
                            onRefresh()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ProductGridItem(
    product: Map<String, Any>,
    isInWishlist: Boolean,
    onProductClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    onWishlistClick: () -> Unit
) {
    val primaryColor = Color(0xFF6200EE)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150)
    )

    val name = product["name"] as? String ?: "N/A"
    val price = (product["price"] as? Number)?.toDouble()?.let {
        String.format(Locale("id", "ID"), "%,.0f", it)
    } ?: "0"
    val imageUrl = product["imageUrl"]?.toString()

    Card(
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onProductClick
            )
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl.takeIf { !it.isNullOrEmpty() } ?: R.drawable.ic_placeholder,
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                ),
                contentDescription = "Product Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rp$price",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddToCartClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        "Add to Cart",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onWishlistClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = primaryColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isInWishlist) MaterialTheme.colorScheme.error else primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistScreen(
    wishlist: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    db: DatabaseHelper,
    onRefresh: () -> Unit
) {
    val primaryColor = Color(0xFF6200EE)

    if (wishlist.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Your wishlist is empty.",
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
                    Text("Explore Products")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(wishlist, key = { it["productId"] as String }) { product ->
            val productId = product["productId"] as String
            WishlistItem(
                product = product,
                onItemClick = { navController.navigate("productDetail/$productId") },
                onRemoveClick = {
                    scope.launch {
                        try {
                            db.removeFromWishlist(productId)
                            snackbarHostState.showSnackbar("Removed from wishlist")
                            onRefresh()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to remove: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun WishlistItem(
    product: Map<String, Any>,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    val primaryColor = Color(0xFF6200EE)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onItemClick
            )
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = product["imageUrl"]?.toString().takeIf { !it.isNullOrEmpty() } ?: R.drawable.ic_placeholder,
                    placeholder = painterResource(R.drawable.ic_placeholder),
                    error = painterResource(R.drawable.ic_error)
                ),
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product["name"] as? String ?: "N/A",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp${(product["price"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            TextButton(
                onClick = onRemoveClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(orders: List<Map<String, Any>>, navController: NavController) {
    val primaryColor = Color(0xFF6200EE)

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "You have no order history.",
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
                    Text("Shop Now")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders, key = { it["orderId"] as String }) { order ->
            OrderItem(order) {
                val orderId = order["orderId"] as? String
                if (orderId != null) {
                    navController.navigate("orderConfirmation/$orderId")
                }
            }
        }
    }
}

@Composable
fun OrderItem(order: Map<String, Any>, onClick: () -> Unit) {
    val primaryColor = Color(0xFF6200EE)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
            val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }

            Text(
                text = "Order ID: ${order["orderId"]}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Date: ${dateFormat.format(Date(createdAt))}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                )
            )
            Text(
                text = "Total: Rp${(order["totalPrice"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = primaryColor
                )
            )
            Text(
                text = "Status: ${order["status"] as? String ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (order["status"] == "Completed") primaryColor else Color.Gray
                )
            )
        }
    }
}

@Composable
fun EmailVerificationCard(
    auth: FirebaseAuth,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val primaryColor = Color(0xFF6200EE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your email is not verified.",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please check your inbox for the verification link.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            auth.currentUser?.sendEmailVerification()?.await()
                            snackbarHostState.showSnackbar("Verification email sent again.")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to send email: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Resend Email")
            }
        }
    }
}