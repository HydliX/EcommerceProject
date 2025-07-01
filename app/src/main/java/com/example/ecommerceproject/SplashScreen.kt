package com.example.ecommerceproject

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val dbHelper = remember { DatabaseHelper() }

    // Animation states
    var startAnimation by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }

    // Animasi fade-in untuk logo
    val logoAlpha = animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    // Animasi scale untuk logo dengan bounce effect
    val logoScale = animateFloatAsState(
        targetValue = if (showContent) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Animasi slide up untuk text
    val textOffset = animateFloatAsState(
        targetValue = if (showContent) 0f else 100f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "text_offset"
    )

    // Progress bar animation
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    // Floating animation untuk background elements
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatingOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_offset"
    )

    LaunchedEffect(key1 = true) {
        // Sequenced animations
        delay(300)
        showContent = true

        delay(1200)
        showProgress = true

        // Simulate loading progress
        for (i in 1..10) {
            delay(100)
            progress = i / 10f
        }

        delay(500) // Extra delay for smooth transition

        // Navigation logic (unchanged)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            try {
                val profile = dbHelper.getUserProfileById(currentUser.uid)
                val role = profile?.get("role") as? String

                val destination = when (role) {
                    DatabaseHelper.UserRole.ADMIN -> "adminDashboard"
                    DatabaseHelper.UserRole.SUPERVISOR -> "supervisor_dashboard"
                    DatabaseHelper.UserRole.PENGELOLA -> "pengelola_dashboard"
                    DatabaseHelper.UserRole.PIMPINAN -> "leader_dashboard"
                    else -> "customerDashboard"
                }

                navController.navigate(destination) {
                    popUpTo("splash") { inclusive = true }
                }
            } catch (e: Exception) {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    // Modern gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea), // Blue gradient start
                        Color(0xFF764ba2), // Purple gradient middle
                        Color(0xFFF093fb)  // Pink gradient end
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating background elements
        FloatingBackgroundElements(floatingOffset.value)

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo container with shadow and modern styling
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .background(
                        Color.White.copy(alpha = 0.95f),
                        CircleShape
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_bag2),
                    contentDescription = "Logo Aplikasi KlikMart",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name with modern typography
            Box(
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .graphicsLayer {
                        translationY = textOffset.value * density
                    }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "KlikMart",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Shopping Companion",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Modern loading indicator
            if (showProgress) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(
                            animateFloatAsState(
                                targetValue = if (showProgress) 1f else 0f,
                                animationSpec = tween(500),
                                label = "progress_alpha"
                            ).value
                        )
                ) {
                    // Custom progress bar
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(4.dp)
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress.value)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White,
                                            Color(0xFFFFF8E1)
                                        )
                                    ),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Loading...",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingBackgroundElements(animationValue: Float) {
    val density = LocalDensity.current.density

    // Floating circles
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.1f)
    ) {
        // Circle 1
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(
                    x = 50.dp,
                    y = (50 + sin(animationValue * 2 * Math.PI) * 20).dp
                )
                .background(
                    Color.White.copy(alpha = 0.2f),
                    CircleShape
                )
        )

        // Circle 2
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(
                    x = (-20).dp,
                    y = (200 + sin((animationValue + 0.5f) * 2 * Math.PI) * 15).dp
                )
                .background(
                    Color.White.copy(alpha = 0.15f),
                    CircleShape
                )
        )

        // Circle 3
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(
                    x = (-80).dp,
                    y = (-100 + sin((animationValue + 0.3f) * 2 * Math.PI) * 25).dp
                )
                .background(
                    Color.White.copy(alpha = 0.1f),
                    CircleShape
                )
        )
    }
}