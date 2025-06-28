package com.example.ecommerceproject

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserProfile(
    snackbarHostState: SnackbarHostState,
    onUsersUpdated: () -> Unit
) {
    val dbHelper = DatabaseHelper()
    val viewModel: EditUserProfileViewModel = viewModel()
    var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch users initially and whenever onUsersUpdated is called
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            users = dbHelper.getAllUsers()
        }
    }

    // Ambil state dari ViewModel
    val isLoading by viewModel.isLoading
    val message by viewModel.message
    val snackbarMessage by viewModel.snackbarMessage

    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf("") }
    var tempAddress by remember { mutableStateOf("") }
    var tempContactPhone by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Filter pengguna yang memiliki setidaknya satu field yang terisi
    val filledUsers = users.filter { user ->
        val username = user["username"] as? String ?: ""
        val address = user["address"] as? String ?: ""
        val contactPhone = user["contactPhone"] as? String ?: ""
        username.isNotBlank() || address.isNotBlank() || contactPhone.isNotBlank()
    }

    Column {
        Text(
            "Edit Profil Pengguna",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown untuk memilih pengguna
        if (filledUsers.isEmpty()) {
            Text(
                text = "Tidak ada pengguna dengan profil yang terisi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedUser?.get("username") as? String ?: "Pilih pengguna",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Pilih Pengguna") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filledUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user["username"] as? String ?: "Tidak diketahui") },
                            onClick = {
                                selectedUser = user
                                tempUsername = user["username"] as? String ?: ""
                                tempAddress = user["address"] as? String ?: ""
                                tempContactPhone = user["contactPhone"] as? String ?: ""
                                showDialog = true
                                expanded = false
                                errorMessage = null // Reset error message when selecting a new user
                            }
                        )
                    }
                }
            }
        }

        // Tampilkan error message jika ada
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Tampilkan dialog
        if (showDialog && selectedUser != null) {
            selectedUser?.let { currentSelectedUser ->
                val originalAddress = currentSelectedUser["address"] as? String ?: ""
                val originalContactPhone = currentSelectedUser["contactPhone"] as? String ?: ""

                // Inisialisasi tempUsername dengan nilai awal dari selectedUser
                if (tempUsername.isBlank()) {
                    tempUsername = currentSelectedUser["username"] as? String ?: ""
                }

                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        selectedUser = null
                        errorMessage = null
                    },
                    title = { Text("Edit Profil ${currentSelectedUser["username"] as? String ?: "Pengguna"}") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = tempUsername,
                                onValueChange = {
                                    tempUsername = it
                                    if (it.isBlank()) {
                                        errorMessage = "Username tidak boleh kosong"
                                    } else {
                                        errorMessage = null
                                    }
                                    Log.d("EditUserProfile", "selectedUser username: '${currentSelectedUser["username"]}'")
                                },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = tempUsername.isBlank(),
                                supportingText = {
                                    if (tempUsername.isBlank()) {
                                        Text(
                                            text = "Username tidak boleh kosong",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempAddress,
                                onValueChange = { tempAddress = it },
                                label = { Text("Alamat${if (originalAddress.isBlank()) " (opsional)" else ""}") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = originalAddress.isNotBlank() && tempAddress.isBlank(),
                                supportingText = {
                                    if (originalAddress.isNotBlank() && tempAddress.isBlank()) {
                                        Text(
                                            text = "Alamat tidak boleh kosong karena sudah diisi sebelumnya",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tempContactPhone,
                                onValueChange = { tempContactPhone = it },
                                label = { Text("Nomor Telepon${if (originalContactPhone.isBlank()) " (opsional)" else ""}") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = originalContactPhone.isNotBlank() && tempContactPhone.isNotBlank() && !tempContactPhone.replace("[^0-9+]".toRegex(), "").matches(Regex("^(\\+?[0-9]{10,15})$")),
                                supportingText = {
                                    if (originalContactPhone.isNotBlank() && tempContactPhone.isNotBlank() && !tempContactPhone.replace("[^0-9+]".toRegex(), "").matches(Regex("^(\\+?[0-9]{10,15})$"))) {
                                        Text(
                                            text = "Nomor telepon harus berupa 10-15 digit dan dapat diawali dengan '+'",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                Log.d("EditUserProfile", "Mencoba simpan dengan tempUsername: '$tempUsername', errorMessage: $errorMessage")
                                if (tempUsername.isBlank()) {
                                    errorMessage = "Username tidak boleh kosong"
                                    return@Button
                                }
                                if (originalAddress.isNotBlank() && tempAddress.isBlank()) {
                                    errorMessage = "Alamat tidak boleh kosong karena sudah diisi sebelumnya"
                                    return@Button
                                }
                                val cleanedPhone = tempContactPhone.replace("[^0-9+]".toRegex(), "")
                                if (originalContactPhone.isNotBlank() && tempContactPhone.isNotBlank() && !cleanedPhone.matches(Regex("^(\\+?[0-9]{10,15})$"))) {
                                    errorMessage = "Nomor telepon harus berupa 10-15 digit dan dapat diawali dengan '+'"
                                    return@Button
                                }

                                val userId = currentSelectedUser["userId"] as? String ?: run {
                                    errorMessage = "User ID tidak ditemukan"
                                    return@Button
                                }
                                viewModel.updateUserProfile(
                                    userId = userId,
                                    username = tempUsername,
                                    address = tempAddress,
                                    contactPhone = cleanedPhone,
                                    profilePhotoUrl = currentSelectedUser["profilePhotoUrl"] as? String ?: "",
                                    role = currentSelectedUser["role"] as? String ?: DatabaseHelper.UserRole.CUSTOMER,
                                    level = currentSelectedUser["level"] as? String ?: DatabaseHelper.UserLevel.USER,
                                    hobbies = currentSelectedUser["hobbies"] as? List<Map<String, String>> ?: emptyList(),
                                    onUsersUpdated = onUsersUpdated
                                )
                                showDialog = false
                                selectedUser = null
                                errorMessage = null
                            },
                            enabled = errorMessage == null && tempUsername.isNotBlank() // Pastikan tombol hanya aktif jika valid
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                            selectedUser = null
                            errorMessage = null
                        }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }

        // Tampilkan snackbar berdasarkan snackbarMessage
        LaunchedEffect(snackbarMessage) {
            snackbarMessage?.let { msg ->
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                // Tidak perlu reset snackbarMessage di sini karena ViewModel mengelolanya
            }
        }

        // Tampilkan indikator loading jika diperlukan
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}