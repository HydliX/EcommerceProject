package com.example.ecommerceproject

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ecommerceproject.admin.ManageUsersViewModel
import com.example.ecommerceproject.admin.OperationState

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
    val viewModel: ManageUsersViewModel = viewModel()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val operationState by viewModel.operationState.collectAsState()
    var supervisorUsername by remember { mutableStateOf("") }
    var supervisorEmail by remember { mutableStateOf("") }
    var supervisorPassword by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var showReAuthDialog by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }

    if (!isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Anda tidak memiliki izin untuk menambah akun Supervisor.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    LaunchedEffect(operationState) {
        when (operationState) {
            is OperationState.Loading -> onLoadingChange(true)
            is OperationState.Success -> {
                onLoadingChange(false)
                val msg = (operationState as OperationState.Success).message
                onMessageChange(msg)
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                if (msg.contains("Supervisor berhasil ditambahkan")) {
                    supervisorUsername = ""
                    supervisorEmail = ""
                    supervisorPassword = ""
                    emailError = null
                    showReAuthDialog = true
                    viewModel.resetOperationState()
                } else if (msg == "Re-autentikasi berhasil") {
                    showReAuthDialog = false
                    adminPassword = ""
                    onUsersUpdated()
                    viewModel.resetOperationState()
                }
            }
            is OperationState.Error -> {
                onLoadingChange(false)
                val msg = (operationState as OperationState.Error).message
                onMessageChange(msg)
                if (msg.contains("Email sudah digunakan")) {
                    emailError = msg
                } else {
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Short
                    )
                }
                viewModel.resetOperationState()
            }
            is OperationState.Idle -> Unit
        }
    }

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
                        viewModel.reAuthenticateAdmin(userProfile?.get("email") as? String, adminPassword)
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
                onValueChange = {
                    supervisorEmail = it
                    emailError = null // Reset error saat pengguna mengetik
                },
                label = { Text("Email Supervisor") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
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
            val addSupervisorInteractionSource = remember { MutableInteractionSource() }
            val addSupervisorIsPressed by addSupervisorInteractionSource.collectIsPressedAsState()
            val addSupervisorScale by animateFloatAsState(if (addSupervisorIsPressed) 0.95f else 1f)
            Button(
                onClick = {
                    if (supervisorEmail.isBlank()) {
                        emailError = "Email tidak boleh kosong"
                    } else if (!supervisorEmail.contains("@") || !supervisorEmail.contains(".")) {
                        emailError = "Format email tidak valid"
                    } else {
                        viewModel.createSupervisorAccount(
                            supervisorEmail,
                            supervisorPassword,
                            supervisorUsername,
                            userProfile
                        )
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