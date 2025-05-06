package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Animation for button press
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    // Reset password dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email to receive a password reset link.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
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
                    enabled = !isLoading
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
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
                                            auth.signOut() // Sign out if not verified
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
                            .scale(scale),
                        enabled = !isLoading,
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text(if (isLoading) "Logging in..." else "Login")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Don't have an account? Register",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(
                                enabled = !isLoading,
                                onClick = { navController.navigate("register") }
                            )
                    )
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