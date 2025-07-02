package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
fun LoginScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val passwordFocusRequester = remember { FocusRequester() }

    // Enhanced color scheme
    val primaryColor = Color(0xFF667EEA)
    val secondaryColor = Color(0xFF764BA2)
    val accentColor = Color(0xFFF093FB)
    val gradientColors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB)
    )
    val cardGradient = listOf(
        Color.White,
        Color(0xFFFAFAFA)
    )

    // Animations
    val infiniteTransition = rememberInfiniteTransition()
    val backgroundRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        )
    )

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Enhanced Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Reset Password",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor,
                            focusedLeadingIconColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
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
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                        return@launch
                                    }
                                    auth.sendPasswordResetEmail(resetEmail).await()
                                    message = "Password reset email sent to $resetEmail"
                                    showResetDialog = false
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Long
                                    )
                                } catch (e: FirebaseAuthException) {
                                    Log.e("LoginScreen", "Reset password error: ${e.errorCode}", e)
                                    message = when (e.errorCode) {
                                        "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                        "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                                        else -> e.message ?: "Failed to send reset email."
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Unexpected error: ${e.message}", e)
                                    message = e.message ?: "Failed to send reset email."
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
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
                        Text("Send Reset Link", fontWeight = FontWeight.Medium)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isLoading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.sweepGradient(
                    colors = gradientColors,
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                )
            )
            .rotate(backgroundRotation * 0.1f)
    ) {
        // Floating background elements
        repeat(6) { index ->
            FloatingElement(
                delay = index * 1000,
                size = (40 + index * 20).dp,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .offset(
                        x = (50 + index * 60).dp,
                        y = (100 + index * 150).dp
                    )
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.9f)
                        ),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = snackbarData.visuals.message,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Enhanced Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = primaryColor.copy(alpha = 0.3f),
                        spotColor = primaryColor.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enhanced Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = 0.1f),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Welcome text
                    Text(
                        "Welcome Back!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black.copy(alpha = 0.8f)
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Sign in to continue your shopping journey",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Enhanced Email Field
                    EnhancedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onImeAction = { passwordFocusRequester.requestFocus() },
                        isEnabled = !isLoading,
                        primaryColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Enhanced Password Field
                    EnhancedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { keyboardController?.hide() },
                        isEnabled = !isLoading,
                        primaryColor = primaryColor,
                        isPassword = true,
                        isPasswordVisible = isPasswordVisible,
                        onPasswordVisibilityChange = { isPasswordVisible = it },
                        modifier = Modifier.focusRequester(passwordFocusRequester)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forgot Password
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
                            .padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Enhanced Login Button
                    Button(
                        onClick = {
                            if (!isLoading) {
                                keyboardController?.hide()
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

                                        // Success animation delay
                                        delay(500)

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
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: IllegalArgumentException) {
                                        Log.e("LoginScreen", "Validation error: ${e.message}", e)
                                        message = e.message ?: "Login failed."
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: IllegalStateException) {
                                        Log.e("LoginScreen", "Verification error: ${e.message}", e)
                                        message = e.message ?: "Login failed."
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: Exception) {
                                        Log.e("LoginScreen", "Unexpected error: ${e.message}", e)
                                        message = e.message ?: "Login failed."
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(buttonScale)
                            .shadow(
                                elevation = if (isPressed) 2.dp else 8.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = primaryColor.copy(alpha = 0.3f),
                                spotColor = primaryColor.copy(alpha = 0.3f)
                            ),
                        enabled = !isLoading,
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Register link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Don't have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                        Text(
                            "Sign Up",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryColor,
                            modifier = Modifier
                                .clickable(
                                    enabled = !isLoading,
                                    onClick = { navController.navigate("register") }
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isEnabled: Boolean = true,
    primaryColor: Color,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = if (value.isNotEmpty()) primaryColor else Color.Gray.copy(alpha = 0.6f)
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(
                    onClick = { onPasswordVisibilityChange(!isPasswordVisible) }
                ) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = modifier.fillMaxWidth(),
        enabled = isEnabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            focusedLabelColor = primaryColor,
            focusedLeadingIconColor = primaryColor,
            cursorColor = primaryColor,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            unfocusedLabelColor = Color.Gray.copy(alpha = 0.6f),
            disabledBorderColor = Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        singleLine = true
    )
}

@Composable
fun FloatingElement(
    delay: Int,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000 + delay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000 + delay,
                easing = LinearEasing
            )
        )
    )

    Box(
        modifier = modifier
            .offset(y = offsetY.dp)
            .rotate(rotation)
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
