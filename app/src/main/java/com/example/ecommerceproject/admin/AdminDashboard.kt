package com.example.ecommerceproject.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.AddSupervisor
import com.example.ecommerceproject.DatabaseHelper
import com.example.ecommerceproject.EditUserProfile
import com.example.ecommerceproject.ManageUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdminFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    navController: NavController,
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    message: String,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = DatabaseHelper()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var localMessage by remember { mutableStateOf(message) }
    var localIsLoading by remember { mutableStateOf(isLoading) }
    var selectedFeature by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val onUsersUpdated: () -> Unit = {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    users = dbHelper.getAllUsers()
                }
                localIsLoading = false
            } catch (e: Exception) {
                localMessage = e.message ?: "Gagal memperbarui data pengguna"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = localMessage, duration = SnackbarDuration.Long)
                }
                localIsLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                localIsLoading = true
                withContext(Dispatchers.IO) {
                    users = dbHelper.getAllUsers()
                }
                localIsLoading = false
            } catch (e: Exception) {
                localMessage = e.message ?: "Gagal memuat data"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = localMessage, duration = SnackbarDuration.Long)
                }
                localIsLoading = false
            }
        }
    }

    val features = listOf(
        AdminFeature(
            title = "Tambah Supervisor",
            description = "Buat akun supervisor baru",
            icon = Icons.Default.PersonAdd,
            color = Color(0xFF4CAF50),
            gradientColors = listOf(Color(0xFF81C784), Color(0xFF4CAF50))
        ),
        AdminFeature(
            title = "Edit Profil Pengguna",
            description = "Ubah data profil pengguna",
            icon = Icons.Default.Edit,
            color = Color(0xFF2196F3),
            gradientColors = listOf(Color(0xFF64B5F6), Color(0xFF2196F3))
        ),
        AdminFeature(
            title = "Kelola Pengguna",
            description = "Atur dan kelola semua pengguna",
            icon = Icons.Default.People,
            color = Color(0xFFFF9800),
            gradientColors = listOf(Color(0xFFFFB74D), Color(0xFFFF9800))
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.shadow(8.dp)
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Beranda",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Beranda", style = MaterialTheme.typography.labelSmall) },
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
                            contentDescription = "Profil",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Profil", style = MaterialTheme.typography.labelSmall) },
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
                            contentDescription = "Pengaturan",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Pengaturan", style = MaterialTheme.typography.labelSmall) },
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
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(800))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color = Color.White.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Dashboard Admin",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Text(
                                    text = "Selamat datang, ${userProfile?.get("name") ?: "Admin"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Text(
                                    text = DatabaseHelper.UserRole.ADMIN.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Loading Indicator
                if (localIsLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer { alpha = pulseAlpha },
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Memuat data...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    // Features Section
                    item {
                        Text(
                            text = "Fitur Admin",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (selectedFeature == null) 2 else 1),
                            modifier = Modifier.height(if (selectedFeature == null) 300.dp else 200.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(features) { feature ->
                                FeatureCard(
                                    feature = feature,
                                    isSelected = selectedFeature == feature.title,
                                    onClick = {
                                        selectedFeature = if (selectedFeature == feature.title) null else feature.title
                                    }
                                )
                            }
                        }
                    }
                }

                // Selected Feature Content
                item {
                    AnimatedVisibility(
                        visible = selectedFeature != null,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        selectedFeature?.let { feature ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = { selectedFeature = null },
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Kembali",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Text(
                                            text = feature,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    when (feature) {
                                        "Tambah Supervisor" -> AddSupervisor(
                                            userProfile = userProfile,
                                            isLoading = localIsLoading,
                                            onLoadingChange = { localIsLoading = it },
                                            message = localMessage,
                                            onMessageChange = { localMessage = it },
                                            snackbarHostState = snackbarHostState,
                                            onUsersUpdated = onUsersUpdated
                                        )
                                        "Edit Profil Pengguna" -> EditUserProfile(
                                            snackbarHostState = snackbarHostState,
                                            onUsersUpdated = onUsersUpdated
                                        )
                                        "Kelola Pengguna" -> ManageUsers(
                                            users = users,
                                            isLoading = localIsLoading,
                                            onLoadingChange = { localIsLoading = it },
                                            message = localMessage,
                                            onMessageChange = { localMessage = it },
                                            snackbarHostState = snackbarHostState,
                                            onUsersUpdated = onUsersUpdated
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
}

@Composable
private fun FeatureCard(
    feature: AdminFeature,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSelected) 140.dp else 120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 12.dp else 6.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                feature.color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            colors = feature.gradientColors.map { it.copy(alpha = 0.1f) }
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (isSelected)
                                feature.color.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = feature.title,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) feature.color else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) feature.color else MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                if (isSelected) {
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = feature.color.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}