package com.example.ecommerceproject

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupervisor(
    userProfile: Map<String, Any>?,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onUsersUpdated: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var supervisorUsername by remember { mutableStateOf("") }
    var supervisorEmail by remember { mutableStateOf("") }
    var supervisorPassword by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var showReAuthDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showReAuthDialog) {
        val confirmInteractionSource = remember { MutableInteractionSource() }
        val confirmIsPressed by confirmInteractionSource.collectIsPressedAsState()
        val confirmScale by animateFloatAsState(if (confirmIsPressed) 0.95f else 1f)

        AlertDialog(
            onDismissRequest = { showReAuthDialog = false },
            title = { Text("Re-autentikasi Admin", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Masukkan kata sandi admin untuk melanjutkan.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Kata Sandi Admin") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                onLoadingChange(true)
                                val email = userProfile?.get("email") as? String
                                    ?: throw IllegalStateException("Email tidak ditemukan")
                                Log.d("AddSupervisor", "Mencoba re-autentikasi untuk email: $email")
                                auth.signInWithEmailAndPassword(email, adminPassword).await()
                                onMessageChange("Re-autentikasi berhasil")
                                showReAuthDialog = false
                                adminPassword = ""
                                onUsersUpdated()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Re-autentikasi berhasil",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } catch (e: FirebaseAuthException) {
                                onMessageChange(
                                    when (e.errorCode) {
                                        "ERROR_WRONG_PASSWORD" -> "Kata sandi salah."
                                        "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                                        else -> e.message ?: "Re-autentikasi gagal"
                                    }
                                )
                                Log.e("AddSupervisor", "Kesalahan re-autentikasi: ${e.errorCode}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } catch (e: Exception) {
                                onMessageChange(e.message ?: "Re-autentikasi gagal")
                                Log.e("AddSupervisor", "Kesalahan tak terduga selama re-autentikasi: ${e.message}", e)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } finally {
                                onLoadingChange(false)
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.scale(confirmScale),
                    interactionSource = confirmInteractionSource,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Konfirmasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReAuthDialog = false }) {
                    Text("Batal")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val addSupervisorInteractionSource = remember { MutableInteractionSource() }
    val addSupervisorIsPressed by addSupervisorInteractionSource.collectIsPressedAsState()
    val addSupervisorScale by animateFloatAsState(if (addSupervisorIsPressed) 0.95f else 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Tambah Supervisor",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = supervisorUsername,
                onValueChange = { supervisorUsername = it },
                label = { Text("Nama Pengguna Supervisor") },
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
                value = supervisorEmail,
                onValueChange = { supervisorEmail = it },
                label = { Text("Email Supervisor") },
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
                value = supervisorPassword,
                onValueChange = { supervisorPassword = it },
                label = { Text("Kata Sandi Supervisor") },
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
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            onLoadingChange(true)
                            if (supervisorUsername.isBlank()) throw IllegalArgumentException("Nama pengguna tidak boleh kosong")
                            if (supervisorEmail.isBlank()) throw IllegalArgumentException("Email tidak boleh kosong")
                            if (supervisorPassword.length < 6) throw IllegalArgumentException("Kata sandi harus minimal 6 karakter")
                            Log.d("AddSupervisor", "Mencoba membuat supervisor: $supervisorEmail")
                            val authResult = auth.createUserWithEmailAndPassword(supervisorEmail, supervisorPassword).await()
                            val user = authResult.user ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                            val userId = user.uid
                            Log.d("AddSupervisor", "Autentikasi berhasil, mengirim verifikasi email untuk userId: $userId")
                            user.sendEmailVerification().await()
                            Log.d("AddSupervisor", "Verifikasi email terkirim, menyimpan profil untuk userId: $userId")
                            try {
                                dbHelper.saveUserProfile(
                                    userId = userId,
                                    username = supervisorUsername,
                                    email = supervisorEmail,
                                    role = DatabaseHelper.UserRole.SUPERVISOR,
                                    level = DatabaseHelper.UserLevel.SUPERVISOR
                                )
                                Log.d("AddSupervisor", "Profil berhasil disimpan untuk userId: $userId")
                            } catch (e: Exception) {
                                Log.w("AddSupervisor", "Gagal menyimpan profil, menghapus pengguna auth: $userId")
                                user.delete().await()
                                throw e
                            }
                            onUsersUpdated()
                            onMessageChange("Supervisor berhasil ditambahkan. Verifikasi email terkirim. Silakan re-autentikasi.")
                            supervisorUsername = ""
                            supervisorEmail = ""
                            supervisorPassword = ""
                            showReAuthDialog = true
                        } catch (e: FirebaseAuthException) {
                            onMessageChange(
                                when (e.errorCode) {
                                    "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah digunakan."
                                    "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah."
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                                    else -> e.message ?: "Gagal menambah supervisor"
                                }
                            )
                            Log.e("AddSupervisor", "Kesalahan autentikasi: ${e.errorCode}, ${e.message}", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } catch (e: Exception) {
                            onMessageChange(e.message ?: "Gagal menambah supervisor")
                            Log.e("AddSupervisor", "Kesalahan tak terduga: ${e.message}", e)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } finally {
                            onLoadingChange(false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(addSupervisorScale),
                enabled = !isLoading,
                interactionSource = addSupervisorInteractionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(4.dp)
            ) {
                Text(if (isLoading) "Menambahkan..." else "Tambah Supervisor")
            }
        }
    }
}