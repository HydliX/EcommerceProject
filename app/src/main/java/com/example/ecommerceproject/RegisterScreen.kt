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
import androidx.compose.ui.draw.*
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RegisterScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Modern color scheme
    val primaryColor = Color(0xFF6366F1)
    val secondaryColor = Color(0xFF8B5CF6)
    val accentColor = Color(0xFF06B6D4)
    val surfaceColor = Color.White
    val onSurfaceColor = Color(0xFF1E293B)

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

    // Floating elements animation
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
    LaunchedEffect(Unit) { showContent = true }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                // Logo Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
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

                // Welcome Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor,
                            fontSize = 32.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sign up to join our amazing community!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Registration Form Card
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
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "Toggle password visibility")
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor,
                                focusedLeadingIconColor = primaryColor,
                                unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Register Button
                        Button(
                            onClick = {
                                if (!isLoading) {
                                    isLoading = true
                                    coroutineScope.launch {
                                        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() || username.isBlank()) {
                                            message = "All fields are required"
                                            snackbarHostState.showSnackbar(message)
                                            isLoading = false
                                            return@launch
                                        }
                                        if (password != confirmPassword) {
                                            message = "Passwords do not match"
                                            snackbarHostState.showSnackbar(message)
                                            isLoading = false
                                            return@launch
                                        }
                                        try {
                                            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                            val user = authResult.user ?: throw IllegalStateException("User creation failed")
                                            user.sendEmailVerification().await()
                                            dbHelper.saveUserProfile(user.uid, username, email)
                                            message = "Registration successful! Please verify your email."
                                            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
                                            navController.navigate("login") {
                                                popUpTo("register") { inclusive = true }
                                            }
                                        } catch (e: FirebaseAuthException) {
                                            message = when (e.errorCode) {
                                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email address is already in use"
                                                "ERROR_INVALID_EMAIL" -> "Invalid email format"
                                                "ERROR_WEAK_PASSWORD" -> "Password should be at least 6 characters"
                                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
                                                else -> "Registration failed: ${e.message}"
                                            }
                                            snackbarHostState.showSnackbar(message)
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("An error occurred: ${e.message}")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .scale(buttonScale),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLoading || email.isBlank() || password.isBlank() || confirmPassword.isBlank() || username.isBlank()) {
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
                                AnimatedContent(
                                    targetState = isLoading,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                                fadeOut(animationSpec = tween(90))
                                    },
                                    label = "register_button_content"
                                ) { loading ->
                                    if (loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Create Account", fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Create Account")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Login Link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Already have an account? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceColor.copy(alpha = 0.7f)
                            )
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor,
                                modifier = Modifier.clickable(enabled = !isLoading) { navController.popBackStack() }
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