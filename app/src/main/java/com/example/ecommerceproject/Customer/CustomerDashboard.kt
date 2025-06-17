package com.example.ecommerceproject.customer

import android.util.Log
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
    val username = userProfile?.get("username")
    var cartItemCount by remember { mutableStateOf(0) }

    // Colors for e-commerce theme
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.05f),
        secondaryColor.copy(alpha = 0.05f)
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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Shop Now",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                actions = {
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

                    val interactionSourceComplaint = remember { MutableInteractionSource() }
                    val isPressedComplaint by interactionSourceComplaint.collectIsPressedAsState()
                    val scaleComplaint by animateFloatAsState(
                        targetValue = if (isPressedComplaint) 0.9f else 1f,
                        animationSpec = tween(100)
                    )

                    IconButton(
                        onClick = { navController.navigate("complaint") },
                        modifier = Modifier.scale(scaleComplaint)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Report,
                            contentDescription = "Submit Complaint",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

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
                                    containerColor = secondaryColor,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome, $username!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
                IconButton(onClick = { loadData(forceReload = true) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = primaryColor
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
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = primaryColor,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = secondaryColor,
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
                            color = if (selectedTab == index) primaryColor else Color.Gray
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
                        tint = if (selectedTab == index) primaryColor else Color.Gray
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
            CircularProgressIndicator(
                color = primaryColor,
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
                        containerColor = primaryColor,
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
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)
    val categories = listOf("All") + products.map { it["category"] as? String ?: "" }.distinct().filter { it.isNotEmpty() }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(300) // Debounce 300ms
        debouncedQuery = searchQuery
    }

    Column {
        // Search Bar
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
                    tint = primaryColor
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
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                cursorColor = primaryColor,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        // Category Filters
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

                // Define icons for each category
                val categoryIcon = when (category) {
                    "All" -> Icons.Default.Category
                    "Electronics" -> Icons.Default.Devices
                    "Fashion" -> Icons.Default.Checkroom
                    "Books" -> Icons.Default.Book
                    "Home & Garden" -> Icons.Default.Home
                    "Sports" -> Icons.Default.DirectionsRun
                    "Toys" -> Icons.Default.Toys
                    else -> Icons.Default.Label
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
                                    colors = listOf(primaryColor, secondaryColor)
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
                            tint = if (isSelected) Color.White else primaryColor
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

        // Filtered Products
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
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)
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
                        tint = if (isInWishlist) secondaryColor else Color.Gray
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
                        color = secondaryColor
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
    val secondaryColor = Color(0xFFFF5722)

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
    val secondaryColor = Color(0xFFFF5722)
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
                        color = secondaryColor
                    )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = secondaryColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = secondaryColor
                )
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(orders: List<Map<String, Any>>, navController: NavController) {
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)

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
    val secondaryColor = Color(0xFFFF5722)
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
            val rating = (order["rating"] as? Number)?.toDouble() ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order ID: ${order["orderId"]}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
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
                text = "Total: Rp${(order["totalPrice"] as? Number)?.toDouble()?.let { String.format(Locale("id", "ID"), "%,.0f", it) } ?: "0"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = secondaryColor
                )
            )
            Text(
                text = "Status: ${order["status"] as? String ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (order["status"] == "Completed") secondaryColor else Color.Gray
                )
            )
            if (rating > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating.toInt()) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$star star",
                            tint = secondaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                val review = order["review"] as? String
                if (!review.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Review: $review",
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
fun EmailVerificationCard(
    auth: FirebaseAuth,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFFFF5722)

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
                    containerColor = secondaryColor,
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
