package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class) // Diperlukan untuk AnimatedContent
@Composable
fun LoginScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Modern color scheme with better contrast
    val primaryColor = Color(0xFF6366F1) // Indigo
    val secondaryColor = Color(0xFF8B5CF6) // Purple
    val accentColor = Color(0xFF06B6D4) // Cyan
    val surfaceColor = Color.White
    val onSurfaceColor = Color(0xFF1E293B)
    val errorColor = Color(0xFFEF4444)
    val successColor = Color(0xFF10B981)

    // Enhanced gradient background
    val gradientBackground = Brush.radialGradient(
        colors = listOf(
            Color(0xFF667eea).copy(alpha = 0.1f),
            Color(0xFF764ba2).copy(alpha = 0.08f),
            Color(0xFFF093fb).copy(alpha = 0.06f),
            Color.White
        ),
        radius = 1200f
    )

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatingOffset1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating1"
    )

    val floatingOffset2 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating2"
    )

    // Content animation
    LaunchedEffect(Unit) {
        showContent = true
    }

    val contentAlpha = animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "content_alpha"
    )

    val contentOffset = animateFloatAsState(
        targetValue = if (showContent) 0f else 50f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "content_offset"
    )

    // Button press animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    // Enhanced Reset Password Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.shadow(32.dp, RoundedCornerShape(28.dp)),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, secondaryColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Reset Password",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = if (resetEmail.isNotEmpty()) primaryColor else onSurfaceColor.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            focusedLeadingIconColor = primaryColor,
                            unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isLoading && resetEmail.isNotBlank()) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    auth.sendPasswordResetEmail(resetEmail).await()
                                    message = "Password reset email sent successfully!"
                                    showResetDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                } catch (e: FirebaseAuthException) {
                                    message = when (e.errorCode) {
                                        "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                        "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection"
                                        else -> "Failed to send reset email"
                                    }
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading && resetEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White,
                        disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Reset Link", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = onSurfaceColor.copy(alpha = 0.7f))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = if (snackbarData.visuals.message.contains("success", ignoreCase = true))
                            successColor else errorColor,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground)
                .padding(innerPadding)
        ) {
            FloatingElements(floatingOffset1.value, floatingOffset2.value)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .graphicsLayer {
                        alpha = contentAlpha.value
                        translationY = contentOffset.value * density.density
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = CircleShape,
                                ambientColor = primaryColor.copy(alpha = 0.3f),
                                spotColor = primaryColor.copy(alpha = 0.3f)
                            )
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_bag2),
                            contentDescription = "KlikMart Logo",
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(id = R.drawable.logo_text2),
                        contentDescription = "KlikMart Text",
                        modifier = Modifier
                            .height(60.dp)
                            .graphicsLayer {
                                shadowElevation = 8.dp.toPx()
                            }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Welcome Back!",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor,
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sign in to continue your amazing shopping experience",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color.Black.copy(alpha = 0.1f),
                            spotColor = Color.Black.copy(alpha = 0.1f)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter your email", color = onSurfaceColor.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (email.isNotEmpty()) primaryColor else onSurfaceColor.copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(
                                    animateFloatAsState(
                                        if (email.isNotEmpty()) 1.02f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "email_scale"
                                    ).value
                                ),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f),
                                unfocusedLeadingIconColor = onSurfaceColor.copy(alpha = 0.5f),
                                focusedContainerColor = primaryColor.copy(alpha = 0.05f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password", color = onSurfaceColor.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (password.isNotEmpty()) primaryColor else onSurfaceColor.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible }
                                ) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = onSurfaceColor.copy(alpha = 0.6f)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(
                                    animateFloatAsState(
                                        if (password.isNotEmpty()) 1.02f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "password_scale"
                                    ).value
                                ),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f),
                                unfocusedLeadingIconColor = onSurfaceColor.copy(alpha = 0.5f),
                                focusedContainerColor = primaryColor.copy(alpha = 0.05f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = primaryColor,
                            modifier = Modifier
                                .clickable(
                                    enabled = !isLoading,
                                    onClick = {
                                        resetEmail = email
                                        showResetDialog = true
                                    }
                                )
                                .padding(vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                if (!isLoading) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")
                                            if (password.isBlank()) throw IllegalArgumentException("Password cannot be empty")

                                            val authResult = auth.signInWithEmailAndPassword(email, password).await()
                                            val user = authResult.user ?: throw IllegalStateException("Login failed")

                                            if (!user.isEmailVerified) {
                                                auth.signOut()
                                                throw IllegalStateException("Please verify your email before logging in")
                                            }

                                            val userProfile = dbHelper.getUserProfileById(user.uid)
                                            if (userProfile == null) {
                                                throw IllegalStateException("User profile not found")
                                            }

                                            val role = userProfile["role"] as? String ?: DatabaseHelper.UserRole.CUSTOMER
                                            val destination = when (role) {
                                                DatabaseHelper.UserRole.ADMIN -> "adminDashboard"
                                                DatabaseHelper.UserRole.SUPERVISOR -> "supervisor_dashboard"
                                                DatabaseHelper.UserRole.PENGELOLA -> "pengelola_dashboard"
                                                DatabaseHelper.UserRole.PIMPINAN -> "leader_dashboard"
                                                else -> "customerDashboard"
                                            }

                                            navController.navigate(destination) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        } catch (e: FirebaseAuthException) {
                                            message = when (e.errorCode) {
                                                "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                                "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                                                "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Check your connection"
                                                else -> "Login failed. Please try again"
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: Exception) {
                                            message = e.message ?: "Login failed. Please try again"
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .scale(buttonScale),
                            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                            interactionSource = interactionSource,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White,
                                disabledContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLoading || email.isBlank() || password.isBlank()) {
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.5f),
                                                    secondaryColor.copy(alpha = 0.5f)
                                                )
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(primaryColor, secondaryColor)
                                            )
                                        },
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // REVISI: Menggunakan AnimatedContent untuk transisi yang mulus
                                AnimatedContent(
                                    targetState = isLoading,
                                    transitionSpec = {
                                        if (targetState) {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        } else {
                                            slideInVertically { height -> -height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> height } + fadeOut()
                                        }
                                    },
                                    label = "login_button_content"
                                ) { loading ->
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Sign In",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Don't have an account? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceColor.copy(alpha = 0.7f)
                            )
                            Text(
                                "Register Now",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryColor,
                                modifier = Modifier
                                    .clickable(
                                        enabled = !isLoading,
                                        onClick = { navController.navigate("register") }
                                    )
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FloatingElements(offset1: Float, offset2: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.6f)
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = (-60).dp,
                    y = (120 + sin(offset1 * 2 * Math.PI) * 30).dp
                )
                .size(140.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.15f),
                            Color(0xFF6366F1).copy(alpha = 0.05f)
                        )
                    )
                )
                .blur(25.dp)
        )

        Box(
            modifier = Modifier
                .offset(
                    x = 280.dp,
                    y = (80 + sin((offset2 + 0.5f) * 2 * Math.PI) * 40).dp
                )
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            Color(0xFF8B5CF6).copy(alpha = 0.08f)
                        )
                    )
                )
                .blur(20.dp)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = (-40).dp,
                    y = (-150 + sin((offset1 + 0.3f) * 2 * Math.PI) * 20).dp
                )
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF06B6D4).copy(alpha = 0.18f),
                            Color(0xFF06B6D4).copy(alpha = 0.06f)
                        )
                    )
                )
                .blur(18.dp)
        )
    }
}
