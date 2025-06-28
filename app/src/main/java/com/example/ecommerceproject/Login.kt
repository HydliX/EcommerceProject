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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Enhanced color scheme
    val primaryColor = Color(0xFF6366F1) // Indigo
    val secondaryColor = Color(0xFF8B5CF6) // Purple
    val accentColor = Color(0xFF06B6D4) // Cyan
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

    // Enhanced Reset Password Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.shadow(24.dp, RoundedCornerShape(24.dp)),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                        "Enter your email to receive a password reset link.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
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
                        if (!isLoading) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    if (resetEmail.isBlank()) {
                                        message = "Email cannot be empty"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        return@launch
                                    }
                                    auth.sendPasswordResetEmail(resetEmail).await()
                                    message = "Password reset email sent to $resetEmail"
                                    showResetDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                } catch (e: FirebaseAuthException) {
                                    Log.e("LoginScreen", "Reset password error: ${e.errorCode}", e)
                                    message = when (e.errorCode) {
                                        "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                        "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                                        else -> e.message ?: "Failed to send reset email."
                                    }
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Unexpected error: ${e.message}", e)
                                    message = e.message ?: "Failed to send reset email."
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
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
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
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = onSurfaceColor.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
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
                    .offset(x = (-50).dp, y = (100 + floatingOffset).dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        primaryColor.copy(alpha = 0.1f)
                    )
                    .blur(40.dp)
            )

            Box(
                modifier = Modifier
                    .offset(x = 250.dp, y = (200 - floatingOffset).dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        accentColor.copy(alpha = 0.15f)
                    )
                    .blur(30.dp)
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
                        "Welcome Back!",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sign in to continue your shopping journey",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = onSurfaceColor.copy(alpha = 0.7f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Login Form Card
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Forgot Password Link
                        Text(
                            "Forgot Password?",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = primaryColor,
                            modifier = Modifier
                                .clickable(
                                    enabled = !isLoading,
                                    onClick = { showResetDialog = true }
                                )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Login Button
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
                                                throw IllegalStateException("Please verify your email before logging in.")
                                            }
                                            Log.d("LoginScreen", "Login successful: userId=${user.uid}")
                                            Log.d("CURRENT_USER", "UID: ${user.uid}")
                                            // Fetch user profile to determine role
                                            val userProfile = dbHelper.getUserProfile()
                                            if (userProfile == null) {
                                                throw IllegalStateException("Profil pengguna tidak ditemukan")
                                            }
                                            val role = userProfile["role"] as? String ?: DatabaseHelper.UserRole.CUSTOMER
                                            // Navigate based on role
                                            val destination = when (role) {
                                                DatabaseHelper.UserRole.ADMIN -> "adminDashboard"
                                                DatabaseHelper.UserRole.SUPERVISOR -> "supervisor_dashboard"
                                                DatabaseHelper.UserRole.PENGELOLA -> "pengelola_dashboard"
                                                DatabaseHelper.UserRole.PIMPINAN -> "leader_dashboard"
                                                DatabaseHelper.UserRole.CUSTOMER -> "customerDashboard"
                                                else -> "customerDashboard"
                                            }
                                            navController.navigate(destination) {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        } catch (e: FirebaseAuthException) {
                                            Log.e("LoginScreen", "Authentication error: ${e.errorCode}", e)
                                            message = when (e.errorCode) {
                                                "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                                "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                                                "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                                                else -> e.message ?: "Login failed."
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: IllegalArgumentException) {
                                            Log.e("LoginScreen", "Validation error: ${e.message}", e)
                                            message = e.message ?: "Login failed."
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: IllegalStateException) {
                                            Log.e("LoginScreen", "Verification error: ${e.message}", e)
                                            message = e.message ?: "Login failed."
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LoginScreen", "Unexpected error: ${e.message}", e)
                                            message = e.message ?: "Login failed."
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
                                        if (isLoading) "Signing In..." else "Sign In",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Register Link
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
                                "Register",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = primaryColor,
                                modifier = Modifier
                                    .clickable(
                                        enabled = !isLoading,
                                        onClick = { navController.navigate("register") }
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