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
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

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
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Daftar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (!isVerificationSent) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Kata Sandi") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                coroutineScope.launch {
                                    try {
                                        if (username.isBlank()) throw IllegalArgumentException("Username tidak boleh kosong")
                                        if (email.isBlank()) throw IllegalArgumentException("Email tidak boleh kosong")
                                        if (password.length < 6) throw IllegalArgumentException("Kata sandi harus minimal 6 karakter")
                                        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                                        val user = authResult.user ?: throw IllegalStateException("Pembuatan pengguna gagal")
                                        user.sendEmailVerification().await()
                                        isVerificationSent = true
                                        message = "Email verifikasi telah dikirim. Silakan periksa kotak masuk Anda."
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    } catch (e: FirebaseAuthException) {
                                        message = when (e.errorCode) {
                                            "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                                            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah digunakan."
                                            "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah."
                                            else -> "Pendaftaran gagal: ${e.message}"
                                        }
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message = message)
                                        }
                                    } catch (e: Exception) {
                                        message = e.message ?: "Pendaftaran gagal."
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message = message)
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale),
                            enabled = !isLoading,
                            interactionSource = interactionSource
                        ) {
                            Text(if (isLoading) "Mendaftar..." else "Daftar")
                        }
                    } else {
                        Text(
                            "Silakan verifikasi email Anda ($email). Klik tombol di bawah setelah verifikasi.",
                            style = MaterialTheme.typography.bodyMedium
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
                                            message = "Pendaftaran selesai! Anda akan diarahkan ke halaman login."
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
                                            message = "Email belum diverifikasi. Silakan periksa email Anda."
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(message = message)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        message = "Gagal menyimpan profil: ${e.message}"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(message = message)
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale),
                            enabled = !isLoading,
                            interactionSource = interactionSource
                        ) {
                            Text(if (isLoading) "Menyimpan..." else "Selesaikan Pendaftaran")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sudah punya akun? Masuk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(
                            enabled = !isLoading,
                            onClick = { navController.navigate("login") }
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