package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Animation for button press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(150)
    )

    // Colors for e-commerce theme
    val primaryColor = Color(0xFF6200EE)
    val secondaryColor = Color(0xFF03DAC5)
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.1f),
        secondaryColor.copy(alpha = 0.1f)
    )

    // Reset password dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    "Reset Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your email to receive a password reset link.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = primaryColor,
                            focusedLabelColor = primaryColor
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryColor
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.background(
            Brush.verticalGradient(gradientColors)
        )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // E-commerce logo or icon placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(primaryColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🛍",
                    fontSize = 40.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                    Text(
                        "Sign in to continue shopping",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(animateFloatAsState(if (email.isNotEmpty()) 1.02f else 1f).value),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = primaryColor,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                            disabledIndicatorColor = Color.Gray.copy(alpha = 0.3f),
                            focusedLabelColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(animateFloatAsState(if (password.isNotEmpty()) 1.02f else 1f).value),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = primaryColor,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                            disabledIndicatorColor = Color.Gray.copy(alpha = 0.3f),
                            focusedLabelColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = primaryColor,
                        modifier = Modifier
                            .clickable(
                                enabled = !isLoading,
                                onClick = { showResetDialog = true }
                            )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                                        navController.navigate("dashboard") {
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
                            .scale(scale)
                            .clip(RoundedCornerShape(12.dp)),
                        enabled = !isLoading,
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text(
                            if (isLoading) "Logging in..." else "Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Don't have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
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
                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}