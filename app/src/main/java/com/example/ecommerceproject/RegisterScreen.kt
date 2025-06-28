package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RegisterScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerificationSent by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Enhanced color scheme
    val primaryColor = Color(0xFF6366F1) // Indigo
    val secondaryColor = Color(0xFF8B5CF6) // Purple
    val accentColor = Color(0xFF06B6D4) // Cyan
    val successColor = Color(0xFF10B981) // Green
    val surfaceColor = Color(0xFFF8FAFC)
    val onSurfaceColor = Color(0xFF1E293B)

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.05f),
            secondaryColor.copy(alpha = 0.08f),
            accentColor.copy(alpha = 0.05f)
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    // Animation for button press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100)
    )

    // Floating elements animation
    val floatingOffset by animateFloatAsState(
        targetValue = if (isLoading) 10f else 0f,
        animationSpec = tween(2000)
    )

    // Check email verification status
    LaunchedEffect(isVerificationSent) {
        if (isVerificationSent) {
            while (!isEmailVerified) {
                try {
                    auth.currentUser?.reload()?.await()
                    isEmailVerified = auth.currentUser?.isEmailVerified ?: false
                    if (isEmailVerified) break
                    delay(2000) // Check every 2 seconds
                } catch (e: Exception) {
                    Log.e("RegisterScreen", "Failed to check email verification: ${e.message}")
                }
            }
        }
    }

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
            // Floating decorative elements
            Box(
                modifier = Modifier
                    .offset(x = (-50).dp, y = (150 + floatingOffset).dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        secondaryColor.copy(alpha = 0.1f)
                    )
                    .blur(35.dp)
            )

            Box(
                modifier = Modifier
                    .offset(x = 280.dp, y = (300 - floatingOffset).dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        accentColor.copy(alpha = 0.12f)
                    )
                    .blur(45.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo/Brand
                Card(
                    modifier = Modifier
                        .size(120.dp)
                        .shadow(16.dp, CircleShape),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryColor, secondaryColor, accentColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🛍️",
                            fontSize = 48.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Welcome Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isVerificationSent) "Almost There!" else "Join Our Store",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isVerificationSent)
                            "Please verify your email to complete registration"
                        else
                            "Create an account to start your shopping journey",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Registration Form Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(24.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isVerificationSent) {
                            // Registration Form
                            // Username Field
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (username.isNotEmpty()) primaryColor else onSurfaceColor.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(animateFloatAsState(if (username.isNotEmpty()) 1.02f else 1f).value),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor,
                                    focusedLeadingIconColor = primaryColor,
                                    unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f),
                                    unfocusedLeadingIconColor = onSurfaceColor.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        tint = if (email.isNotEmpty()) primaryColor else onSurfaceColor.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(animateFloatAsState(if (email.isNotEmpty()) 1.02f else 1f).value),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor,
                                    focusedLeadingIconColor = primaryColor,
                                    unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f),
                                    unfocusedLeadingIconColor = onSurfaceColor.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Password Field
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
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
                                            tint = onSurfaceColor.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(animateFloatAsState(if (password.isNotEmpty()) 1.02f else 1f).value),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor,
                                    focusedLeadingIconColor = primaryColor,
                                    unfocusedBorderColor = onSurfaceColor.copy(alpha = 0.2f),
                                    unfocusedLeadingIconColor = onSurfaceColor.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Password Requirements
                            Text(
                                "Password must be at least 6 characters",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (password.length >= 6) successColor else onSurfaceColor.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Register Button
                            Button(
                                onClick = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            if (username.isBlank()) throw IllegalArgumentException("Username cannot be empty")
                                            if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")
                                            if (password.length < 6) throw IllegalArgumentException("Password must be at least 6 characters")
                                            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                            val user = authResult.user ?: throw IllegalStateException("User creation failed")
                                            user.sendEmailVerification().await()
                                            isVerificationSent = true
                                            message = "Verification email sent. Please check your inbox."
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        } catch (e: FirebaseAuthException) {
                                            message = when (e.errorCode) {
                                                "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
                                                "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                                                else -> "Registration failed: ${e.message}"
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: Exception) {
                                            message = e.message ?: "Registration failed."
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(scale),
                                enabled = !isLoading,
                                interactionSource = interactionSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(primaryColor, secondaryColor)
                                            ),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Text(
                                            if (isLoading) "Creating Account..." else "Create Account",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Email Verification Section
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(accentColor, primaryColor)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = accentColor.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Verification Email Sent!",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = onSurfaceColor
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "We've sent a verification link to:",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = onSurfaceColor.copy(alpha = 0.7f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        email,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = primaryColor
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Please check your inbox and click the button below after verification.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = onSurfaceColor.copy(alpha = 0.6f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Complete Registration Button
                            Button(
                                onClick = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            auth.currentUser?.reload()?.await()
                                            if (auth.currentUser?.isEmailVerified == true) {
                                                dbHelper.saveUserProfile(
                                                    userId = auth.currentUser!!.uid,
                                                    username = username,
                                                    email = email
                                                )
                                                message = "Registration complete! You will be redirected to login."
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                }
                                                delay(2000)
                                                navController.navigate("login") {
                                                    popUpTo(navController.graph.startDestinationId)
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                message = "Email not yet verified. Please check your email."
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = message,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            message = "Failed to save profile: ${e.message}"
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .scale(scale),
                                enabled = !isLoading,
                                interactionSource = interactionSource,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(successColor, accentColor)
                                            ),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        } else {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            if (isLoading) "Completing..." else "Complete Registration",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryColor,
                                modifier = Modifier.clickable(
                                    enabled = !isLoading,
                                    onClick = { navController.navigate("login") }
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}