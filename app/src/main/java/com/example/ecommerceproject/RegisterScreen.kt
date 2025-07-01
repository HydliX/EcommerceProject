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
fun RegisterScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerificationSent by remember { mutableStateOf(false) }
    var isEmailVerified by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Focus requesters
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Enhanced color scheme (consistent with LoginScreen)
    val primaryColor = Color(0xFF667EEA)
    val secondaryColor = Color(0xFF764BA2)
    val accentColor = Color(0xFFF093FB)
    val successColor = Color(0xFF10B981)
    val gradientColors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB)
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

    // Password strength indicator
    val passwordStrength = remember(password) {
        when {
            password.length < 6 -> "Weak"
            password.length < 8 -> "Fair"
            password.length >= 8 && password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> "Strong"
            password.length >= 8 -> "Good"
            else -> "Weak"
        }
    }

    val passwordStrengthColor = remember(passwordStrength) {
        when (passwordStrength) {
            "Weak" -> Color(0xFFEF4444)
            "Fair" -> Color(0xFFF59E0B)
            "Good" -> Color(0xFF3B82F6)
            "Strong" -> Color(0xFF10B981)
            else -> Color.Gray
        }
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
        repeat(8) { index ->
            FloatingElement(
                delay = index * 800,
                size = (30 + index * 15).dp,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .offset(
                        x = (40 + index * 50).dp,
                        y = (80 + index * 120).dp
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
                            containerColor = if (snackbarData.visuals.message.contains("complete", ignoreCase = true) ||
                                snackbarData.visuals.message.contains("sent", ignoreCase = true)) {
                                successColor.copy(alpha = 0.9f)
                            } else {
                                Color.Black.copy(alpha = 0.9f)
                            }
                        ),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (snackbarData.visuals.message.contains("complete", ignoreCase = true) ||
                                    snackbarData.visuals.message.contains("sent", ignoreCase = true)) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = snackbarData.visuals.message,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
            Spacer(modifier = Modifier.height(20.dp))

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
                    Icons.Default.PersonAdd,
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
                    if (!isVerificationSent) {
                        // Welcome text
                        Text(
                            "Join Our Store",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.8f)
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            "Create an account to start your shopping journey",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Username Field
                        EnhancedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            icon = Icons.Default.Person,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                            onImeAction = { emailFocusRequester.requestFocus() },
                            isEnabled = !isLoading,
                            primaryColor = primaryColor
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Email Field
                        EnhancedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email Address",
                            icon = Icons.Default.Email,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            onImeAction = { passwordFocusRequester.requestFocus() },
                            isEnabled = !isLoading,
                            primaryColor = primaryColor,
                            modifier = Modifier.focusRequester(emailFocusRequester)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Field
                        EnhancedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            icon = Icons.Default.Lock,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                            onImeAction = { confirmPasswordFocusRequester.requestFocus() },
                            isEnabled = !isLoading,
                            primaryColor = primaryColor,
                            isPassword = true,
                            isPasswordVisible = isPasswordVisible,
                            onPasswordVisibilityChange = { isPasswordVisible = it },
                            modifier = Modifier.focusRequester(passwordFocusRequester)
                        )

                        // Password Strength Indicator
                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Password strength: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Text(
                                    passwordStrength,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = passwordStrengthColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                LinearProgressIndicator(
                                    progress = when (passwordStrength) {
                                        "Weak" -> 0.25f
                                        "Fair" -> 0.5f
                                        "Good" -> 0.75f
                                        "Strong" -> 1f
                                        else -> 0f
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = passwordStrengthColor,
                                    trackColor = Color.Gray.copy(alpha = 0.2f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Confirm Password Field
                        EnhancedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            icon = Icons.Default.Lock,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            onImeAction = { keyboardController?.hide() },
                            isEnabled = !isLoading,
                            primaryColor = primaryColor,
                            isPassword = true,
                            isPasswordVisible = isConfirmPasswordVisible,
                            onPasswordVisibilityChange = { isConfirmPasswordVisible = it },
                            modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Register Button
                        Button(
                            onClick = {
                                if (!isLoading) {
                                    keyboardController?.hide()
                                    isLoading = true
                                    coroutineScope.launch {
                                        try {
                                            if (username.isBlank()) throw IllegalArgumentException("Username cannot be empty")
                                            if (email.isBlank()) throw IllegalArgumentException("Email cannot be empty")
                                            if (password.length < 6) throw IllegalArgumentException("Password must be at least 6 characters")
                                            if (password != confirmPassword) throw IllegalArgumentException("Passwords do not match")

                                            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                            val user = authResult.user ?: throw IllegalStateException("User creation failed")
                                            user.sendEmailVerification().await()
                                            isVerificationSent = true
                                            message = "Verification email sent to $email"
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Long
                                            )
                                        } catch (e: FirebaseAuthException) {
                                            Log.e("RegisterScreen", "Registration error: ${e.errorCode}", e)
                                            message = when (e.errorCode) {
                                                "ERROR_INVALID_EMAIL" -> "Invalid email format."
                                                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
                                                "ERROR_WEAK_PASSWORD" -> "Password is too weak."
                                                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                                                else -> e.message ?: "Registration failed."
                                            }
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        } catch (e: IllegalArgumentException) {
                                            Log.e("RegisterScreen", "Validation error: ${e.message}", e)
                                            message = e.message ?: "Registration failed."
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        } catch (e: Exception) {
                                            Log.e("RegisterScreen", "Unexpected error: ${e.message}", e)
                                            message = e.message ?: "Registration failed."
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Create Account",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Email Verification UI
                        Icon(
                            Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = primaryColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Verify Your Email",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black.copy(alpha = 0.8f)
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "We've sent a verification email to:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Please check your inbox and click the verification link, then tap the button below to complete your registration.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )

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
                                            message = "Registration complete! Redirecting to login..."
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Long
                                            )
                                            delay(2000)
                                            navController.navigate("login") {
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        } else {
                                            message = "Email not yet verified. Please check your email and try again."
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("RegisterScreen", "Failed to complete registration: ${e.message}", e)
                                        message = "Failed to complete registration: ${e.message}"
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    } finally {
                                        isLoading = false
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
                                    ambientColor = successColor.copy(alpha = 0.3f),
                                    spotColor = successColor.copy(alpha = 0.3f)
                                ),
                            enabled = !isLoading,
                            interactionSource = interactionSource,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = successColor,
                                contentColor = Color.White
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Complete Registration",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend Email Button
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        auth.currentUser?.sendEmailVerification()?.await()
                                        snackbarHostState.showSnackbar(
                                            message = "Verification email resent!",
                                            duration = SnackbarDuration.Short
                                        )
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            message = "Failed to resend email: ${e.message}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                "Resend Verification Email",
                                color = primaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Already have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                        Text(
                            "Sign In",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryColor,
                            modifier = Modifier
                                .clickable(
                                    enabled = !isLoading,
                                    onClick = { navController.navigate("login") }
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
