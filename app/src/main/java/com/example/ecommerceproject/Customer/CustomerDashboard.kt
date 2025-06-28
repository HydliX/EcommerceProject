package com.example.ecommerceproject.customer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
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
import com.google.firebase.database.DatabaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
    val auth = FirebaseAuth.getInstance()
    val dbProduct = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wishlist by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isEmailVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified == true) }
    var selectedTab by remember { mutableStateOf(0) }
    val username = userProfile?.get("username")
    var cartItemCount by remember { mutableStateOf(0) }

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

    LaunchedEffect(Unit) {
        loadData(forceReload = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFF6200EE).copy(alpha = 0.05f), Color(0xFFFF5722).copy(alpha = 0.05f)))),
            topBar = {
                // Menggunakan CenterAlignedTopAppBar untuk tata letak yang benar-benar di tengah dan responsif
                CenterAlignedTopAppBar(
                    title = {
                        // Box tidak lagi diperlukan, komponen ini otomatis menempatkan judul di tengah
                        Text(
                            "Shop Now",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    },
                    actions = {
                        // Semua IconButton aksi Anda tetap sama persis
                        val interactionSourceChat = remember { MutableInteractionSource() }
                        val isPressedChat by interactionSourceChat.collectIsPressedAsState()
                        val scaleChat by animateFloatAsState(
                            targetValue = if (isPressedChat) 0.9f else 1f,
                            animationSpec = tween(100)
                        )
                        IconButton(
                            onClick = { navController.navigate("chatList") },
                            modifier = Modifier.scale(scaleChat)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = "Message",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // ... (IconButton untuk Complaint dan Cart tetap sama persis)
                        val interactionSourceCart = remember { MutableInteractionSource() }
                        val isPressedCart by interactionSourceCart.collectIsPressedAsState()
                        val scaleCart by animateFloatAsState(
                            targetValue = if (isPressedCart) 0.9f else 1f,
                            animationSpec = tween(100)
                        )
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFFF5722), // Warna Anda dipertahankan
                                        contentColor = Color.White,
                                        modifier = Modifier
                                            .offset(x = (-6).dp, y = 6.dp)
                                            .size(16.dp)
                                    ) {
                                        Text(
                                            "$cartItemCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = { navController.navigate("cart") },
                                modifier = Modifier.scale(scaleCart)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    // Warna dan modifier Anda dipertahankan sepenuhnya
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF6200EE), Color(0xFFFF5722)))
                        )
                        .shadow(4.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(horizontal = 8.dp)
                )
            },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = Color(0xFF6200EE),
                modifier = Modifier.shadow(8.dp)
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (navController.currentDestination?.route == "dashboard") Color(0xFF6200EE) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Home",
                            color = if (navController.currentDestination?.route == "dashboard") Color(0xFF6200EE) else Color.Gray
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
                            tint = if (navController.currentDestination?.route == "profile") Color(0xFF6200EE) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Profile",
                            color = if (navController.currentDestination?.route == "profile") Color(0xFF6200EE) else Color.Gray
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
                            tint = if (navController.currentDestination?.route == "settings") Color(0xFF6200EE) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            color = if (navController.currentDestination?.route == "settings") Color(0xFF6200EE) else Color.Gray
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
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = "Order Status",
                            tint = if (navController.currentDestination?.route == "orderStatus") Color(0xFF6200EE) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Order Status",
                            color = if (navController.currentDestination?.route == "orderStatus") Color(0xFF6200EE) else Color.Gray
                        )
                    },
                    selected = navController.currentDestination?.route == "orderStatus",
                    onClick = {
                        navController.navigate("orderStatus") {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome, $username!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                )
                IconButton(onClick = { loadData(forceReload = true) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF6200EE)
                    )
                }
            }
            Text(
                text = "Discover the Latest Trends",
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = Color(0xFF6200EE),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = Color(0xFFFF5722),
                height = 3.dp
            )
        },
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) Color(0xFF6200EE) else Color.Gray
                        )
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (index) {
                            0 -> Icons.Default.Shop
                            1 -> Icons.Default.Favorite
                            2 -> Icons.Default.History
                            else -> Icons.Default.Info
                        },
                        contentDescription = "$title Icon",
                        tint = if (selectedTab == index) Color(0xFF6200EE) else Color.Gray
                    )
                },
                selectedContentColor = Color(0xFF6200EE),
                unselectedContentColor = Color.Gray
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = Color(0xFF6200EE),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
        }
    } else if (hasError) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        containerColor = Color(0xFF6200EE),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )
    val categories = listOf("All") + products.map { it["category"] as? String ?: "" }.distinct().filter { it.isNotEmpty() }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            placeholder = { Text("Search products...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Color(0xFF6200EE)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Search",
                            tint = Color.Gray
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6200EE),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                cursorColor = Color(0xFF6200EE),
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed || isSelected) 1.05f else 1f,
                    animationSpec = tween(durationMillis = 200)
                )

                val categoryIcon = when (category) {
                    "All" -> Icons.Default.Category
                    "Electronics" -> Icons.Default.Devices
                    "Fashion" -> Icons.Default.Checkroom
                    "Books" -> Icons.Default.Book
                    "Home & Garden" -> Icons.Default.Home
                    "Sports" -> Icons.AutoMirrored.Filled.DirectionsRun
                    "Toys" -> Icons.Default.Toys
                    else -> Icons.AutoMirrored.Filled.Label
                }

                Card(
                    modifier = Modifier
                        .scale(scale)
                        .shadow(elevation = if (isSelected) 8.dp else 2.dp, shape = RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { selectedCategory = category },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                brush = if (isSelected) Brush.linearGradient(
                                    colors = listOf(Color(0xFF6200EE), Color(0xFFFF5722))
                                ) else Brush.linearGradient(
                                    colors = listOf(Color.White, Color.White)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = "$category Icon",
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 4.dp),
                            tint = if (isSelected) Color.White else Color(0xFF6200EE)
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        )
                    }
                }
            }
        }

        val filteredProducts = products
            .filter { product ->
                val category = product["category"] as? String ?: ""
                (selectedCategory == "All" || category == selectedCategory)
            }
            .filter { product ->
                val name = product["name"] as? String ?: ""
                name.contains(debouncedQuery, ignoreCase = true)
            }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No Products",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            debouncedQuery.isNotEmpty() -> "No products match \"$debouncedQuery\"."
                            selectedCategory != "All" -> "No products found in $selectedCategory."
                            else -> "No products available."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts, key = { it["productId"] as String }) { product ->
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
                                    snackbarHostState.showSnackbar("Added to cart!")
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
                                        snackbarHostState.showSnackbar("Removed from wishlist")
                                    } else {
                                        db.addToWishlist(productId)
                                        snackbarHostState.showSnackbar("Added to wishlist")
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )
    val interactionSource = remember { MutableInteractionSource() }
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
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
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
                IconButton(
                    onClick = onWishlistClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isInWishlist) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isInWishlist) Color(0xFFFF5722) else Color.Gray
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp$price",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                )
            }

            Button(
                onClick = onAddToCartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    if (wishlist.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Empty Wishlist",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        containerColor = Color(0xFF6200EE),
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onItemClick
            )
            .shadow(4.dp, RoundedCornerShape(16.dp))
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
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product["name"] as? String ?: "N/A",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp${(product["price"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = Color(0xFFFF5722).copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color(0xFFFF5722)
                )
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(orders: List<Map<String, Any>>, navController: NavController) {
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Empty Orders",
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        containerColor = Color(0xFF6200EE),
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
    val db = remember { DatabaseHelper() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val orderId = order["orderId"] as? String ?: ""
    val currentStatus = order["status"] as? String ?: "Unknown"
    var isRatingExpanded by remember { mutableStateOf(false) }
    var rating by remember { mutableStateOf((order["rating"] as? Number)?.toDouble() ?: 0.0) }
    var review by remember { mutableStateOf(order["review"] as? String ?: "") }
    val isDelivered = currentStatus == "DELIVERED"
    val canRate = isDelivered && rating == 0.0

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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
            val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }
            val totalPrice = (order["totalPrice"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order ID: $orderId",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6200EE)
                    )
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Date: ${dateFormat.format(Date(createdAt))}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                )
            )
            Text(
                text = "Total: Rp$totalPrice",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF5722)
                )
            )
            Text(
                text = "Status: $currentStatus",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = when (currentStatus) {
                        "PENDING" -> Color.Yellow
                        "PACKING" -> Color.Blue
                        "SHIPPED" -> Color.Green
                        "DELIVERED" -> Color(0xFFFF5722)
                        else -> Color.Gray
                    }
                )
            )

            if (canRate) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRatingExpanded = !isRatingExpanded }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rate this order",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6200EE)
                        )
                    )
                    Icon(
                        imageVector = if (isRatingExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isRatingExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF6200EE)
                    )
                }

                AnimatedVisibility(visible = isRatingExpanded) {
                    Column {
                        RatingBar(
                            rating = rating,
                            onRatingChanged = { newRating ->
                                rating = newRating.coerceIn(0.0, 5.0)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        OutlinedTextField(
                            value = review,
                            onValueChange = { review = it },
                            label = { Text("Add a review (required)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6200EE),
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f)
                            )
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        if (rating <= 0.0) {
                                            snackbarHostState.showSnackbar("Please select a rating.")
                                            return@launch
                                        }
                                        if (review.isBlank()) {
                                            snackbarHostState.showSnackbar("Please provide a review.")
                                            return@launch
                                        }
                                        db.updateOrderRatingAndReview(orderId, rating, review)
                                        snackbarHostState.showSnackbar("Rating submitted successfully!")
                                        isRatingExpanded = false
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Failed to submit rating: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EE),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = rating > 0.0 && review.isNotBlank()
                        ) {
                            Text("Submit Rating", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else if (rating > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$star star",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                val reviewText = order["review"] as? String
                if (!reviewText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Review: $reviewText",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Double,
    onRatingChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "$star star",
                tint = Color(0xFFFF5722),
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        onRatingChanged(star.toDouble())
                    }
                    .padding(horizontal = 2.dp)
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
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                            snackbarHostState.showSnackbar("Verification email sent!")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to send email: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5722),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resend Email")
            }
        }
    }
}

@Composable
fun OrderStatusScreen(
    orders: List<Map<String, Any>>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    db: DatabaseHelper
) {
    val gradientColors = listOf(
        Color(0xFF6200EE).copy(alpha = 0.05f),
        Color(0xFFFF5722).copy(alpha = 0.05f)
    )

    val sortedOrders = orders.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradientColors))
            .padding(16.dp)
    ) {
        Text(
            text = "Track Your Orders",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EE),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.2f),
                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                    blurRadius = 4f
                )
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (sortedOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var scaleTarget by remember { mutableStateOf(1.0f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            scaleTarget = 1.1f
                            delay(1000)
                            scaleTarget = 1.0f
                            delay(1000)
                        }
                    }
                    val scaleAnim by animateFloatAsState(
                        targetValue = scaleTarget,
                        animationSpec = tween(durationMillis = 1000)
                    )
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "No Orders",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scaleAnim)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Orders Found",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6200EE)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start shopping to track your orders!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("dashboard") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EE),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Shop Now",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sortedOrders, key = { it["orderId"] as String }) { order ->
                    EnhancedOrderItem(
                        order = order,
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                        onClick = {
                            val orderId = order["orderId"] as? String
                            if (orderId != null) {
                                if (order["status"] as? String == "DELIVERED") {
                                    navController.navigate("ratingReview/$orderId")
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "belum dapat memberikan rating dan review",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Button(
                onClick = { navController.navigate("dashboard") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(6.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Back to Dashboard",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedOrderItem(
    order: Map<String, Any>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150)
    )

    val orderId = order["orderId"] as? String ?: ""
    val currentStatus = order["status"] as? String ?: "Unknown"
    val createdAt = (order["createdAt"] as? Number)?.toLong() ?: 0L
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")) }
    val totalPrice = (order["totalPrice"] as? Number)?.toDouble()?.let {
        String.format(Locale("id", "ID"), "%,.0f", it)
    } ?: "0"
    var showErrorMessage by remember { mutableStateOf(false) }

    // Hide error message after 3 seconds
    LaunchedEffect(showErrorMessage) {
        if (showErrorMessage) {
            delay(3000)
            showErrorMessage = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onClick()
                    if (currentStatus != "DELIVERED") {
                        showErrorMessage = true
                    }
                }
            )
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6200EE).copy(alpha = 0.1f), Color(0xFFFF5722).copy(alpha = 0.1f))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order ID: $orderId",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6200EE)
                    )
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = Color(0xFF6200EE),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Date: ${dateFormat.format(Date(createdAt))}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total: Rp$totalPrice",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    )
                }
                StatusBadge(
                    status = currentStatus,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OrderStatusStepper(status = currentStatus)

            AnimatedVisibility(visible = showErrorMessage) {
                Text(
                    text = "belum dapat memberikan rating dan review",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        "PENDING" -> Pair(Color.Yellow.copy(alpha = 0.3f), Color.Black)
        "PACKING" -> Pair(Color.Blue.copy(alpha = 0.3f), Color.White)
        "SHIPPED" -> Pair(Color.Green.copy(alpha = 0.3f), Color.White)
        "DELIVERED" -> Pair(Color(0xFFFF5722).copy(alpha = 0.3f), Color.White)
        else -> Pair(Color.Gray.copy(alpha = 0.3f), Color.White)
    }

    Text(
        text = status,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = textColor
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun OrderStatusStepper(
    status: String,
    modifier: Modifier = Modifier
) {
    val statusList = listOf("PENDING", "PACKING", "SHIPPED", "DELIVERED")
    val currentIndex = statusList.indexOf(status).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        statusList.forEachIndexed { index, step ->
            val isActive = index <= currentIndex
            val isCompleted = index < currentIndex

            Column(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isActive) Color(0xFF6200EE) else Color.Gray.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isActive) Color.White else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (step) {
                            "PENDING" -> Icons.Default.HourglassEmpty
                            "PACKING" -> Icons.Default.LocalShipping
                            "SHIPPED" -> Icons.Default.Flight
                            "DELIVERED" -> Icons.Default.CheckCircle
                            else -> Icons.Default.Info
                        },
                        contentDescription = step,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = step,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isActive) Color(0xFF6200EE) else Color.Gray
                    ),
                    textAlign = TextAlign.Center
                )

                if (index < statusList.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                color = if (isCompleted) Color(0xFF6200EE) else Color.Gray.copy(alpha = 0.2f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedStatusIcon(status: String) {
    val animationProgress by animateFloatAsState(
        targetValue = if (status == "PENDING") 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 200)
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.LightGray.copy(alpha = 0.1f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            "PENDING" -> {
                CircularProgressIndicator(
                    progress = { animationProgress },
                    color = Color.Yellow,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
            "PACKING" -> Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = "Packing",
                tint = Color.Blue,
                modifier = Modifier.size(24.dp)
            )
            "SHIPPED" -> Icon(
                imageVector = Icons.Default.Flight,
                contentDescription = "Shipped",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
            "DELIVERED" -> Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Delivered",
                tint = Color(0xFFFF5722),
                modifier = Modifier.size(24.dp)
            )
            else -> Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Unknown",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

suspend fun DatabaseHelper.updateOrderStatus(orderId: String, newStatus: String) {
    require(newStatus in listOf("PENDING", "PACKING", "SHIPPED", "DELIVERED")) { "Invalid status" }
    val snapshot = database.child("orders").child(orderId).get().await()
    if (!snapshot.exists()) {
        throw IllegalStateException("Order not found: id=$orderId")
    }
    val updates = mapOf(
        "status" to newStatus,
        "updatedAt" to System.currentTimeMillis()
    )
    database.child("orders").child(orderId).updateChildren(updates).await()
}

suspend fun DatabaseHelper.updateOrderRatingAndReview(orderId: String, rating: Double, review: String) {
    val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    require(rating in 0.0..5.0) { "Rating must be between 0 and 5" }
    require(review.isNotBlank()) { "Review cannot be empty" }
    try {
        val snapshot = database.child("orders").child(orderId).get().await()
        if (!snapshot.exists()) {
            Log.w("DatabaseHelper", "Order not found: orderId=$orderId")
            throw IllegalStateException("Order not found")
        }
        val existingOrder = snapshot.value as? Map<String, Any>
            ?: throw IllegalStateException("Invalid order data")
        if (existingOrder["userId"] != userId) {
            Log.w("DatabaseHelper", "User not authorized to update order: orderId=$orderId, userId=$userId")
            throw IllegalStateException("Only the order owner can provide a rating")
        }
        val updates = mapOf(
            "rating" to rating,
            "review" to review,
            "updatedAt" to System.currentTimeMillis()
        )
        database.child("orders").child(orderId).updateChildren(updates).await()
        Log.d(
            "DatabaseHelper",
            "Order rating and review updated successfully: orderId=$orderId, rating=$rating"
        )
    } catch (e: Exception) {
        Log.e("DatabaseHelper", "Failed to update order rating: ${e.message}", e)
    }
}
