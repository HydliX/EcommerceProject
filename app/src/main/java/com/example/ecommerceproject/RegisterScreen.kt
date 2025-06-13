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
                    "🛍️",
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
                        "Join Our Store",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                    Text(
                        "Create an account to start shopping",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (!isVerificationSent) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(animateFloatAsState(if (username.isNotEmpty()) 1.02f else 1f).value),
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
                        Spacer(modifier = Modifier.height(24.dp))
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
                                if (isLoading) "Registering..." else "Register",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            "Please verify your email ($email). Click the button below after verification.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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
                                if (isLoading) "Saving..." else "Complete Registration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            "Login",
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