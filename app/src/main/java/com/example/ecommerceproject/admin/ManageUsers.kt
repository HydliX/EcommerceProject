package com.example.ecommerceproject

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ecommerceproject.admin.ManageUsersViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageUsers(
    users: List<Map<String, Any>>,
    isLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onUsersUpdated: () -> Unit
) {
    val dbHelper = DatabaseHelper()
    val viewModel: ManageUsersViewModel = viewModel()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showPromoteDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedRole by remember { mutableStateOf(DatabaseHelper.UserRole.SUPERVISOR) }

    if (!isAdmin) {
        Column {
            Text("Anda tidak memiliki izin untuk mengelola pengguna.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    if (showPromoteDialog && selectedUser != null) {
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            title = { Text("Promosikan Pengguna: ${selectedUser!!["username"]}") },
            text = {
                Column {
                    Text("Pilih Role Baru:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedRole,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Role") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf(
                                DatabaseHelper.UserRole.SUPERVISOR,
                                DatabaseHelper.UserRole.PENGELOLA
                            ).forEach { roleOption ->
                                DropdownMenuItem(
                                    text = { Text(roleOption) },
                                    onClick = {
                                        selectedRole = roleOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userId = selectedUser!!["userId"] as? String
                            ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                        val newLevel = when (selectedRole) {
                            DatabaseHelper.UserRole.SUPERVISOR -> DatabaseHelper.UserLevel.SUPERVISOR
                            DatabaseHelper.UserRole.PENGELOLA -> DatabaseHelper.UserLevel.PENGELOLA
                            else -> DatabaseHelper.UserLevel.USER
                        }

                        onLoadingChange(true)
                        viewModel.promoteUser(
                            userId = userId,
                            newRole = selectedRole,
                            newLevel = newLevel,
                            onSuccess = {
                                onUsersUpdated()
                                onMessageChange("Pengguna dipromosikan menjadi $selectedRole")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Pengguna dipromosikan menjadi $selectedRole",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                showPromoteDialog = false
                                onLoadingChange(false)
                            },
                            onError = {
                                onMessageChange(it)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = it,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onLoadingChange(false)
                            }
                        )
                    },
                    enabled = !isLoading
                ) {
                    Text("Promosikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column {
        Text("Supervisor", style = MaterialTheme.typography.titleLarge)
        UserList(users, DatabaseHelper.UserRole.SUPERVISOR, dbHelper, isLoading, onUsersUpdated, onMessageChange, snackbarHostState)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Pengelola", style = MaterialTheme.typography.titleLarge)
        UserList(users, DatabaseHelper.UserRole.PENGELOLA, dbHelper, isLoading, onUsersUpdated, onMessageChange, snackbarHostState)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Pelanggan", style = MaterialTheme.typography.titleLarge)
        val customerUsers = users.filter { it["role"] == DatabaseHelper.UserRole.CUSTOMER }
        if (customerUsers.isEmpty()) {
            Text("Tidak ada pelanggan ditemukan")
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(customerUsers) { user ->
                    UserCard(
                        user = user,
                        isLoading = isLoading,
                        onPromote = {
                            selectedUser = user
                            showPromoteDialog = true
                        },
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    val userId = user["userId"] as? String
                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                    dbHelper.deleteUser(userId)
                                    onUsersUpdated()
                                    onMessageChange("Pelanggan berhasil dihapus")
                                    snackbarHostState.showSnackbar(
                                        message = "Pelanggan berhasil dihapus",
                                        duration = SnackbarDuration.Short
                                    )
                                } catch (e: Exception) {
                                    onMessageChange(e.message ?: "Gagal menghapus pelanggan")
                                    snackbarHostState.showSnackbar(
                                        message = e.message ?: "Gagal menghapus pelanggan",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserList(
    users: List<Map<String, Any>>,
    role: String,
    dbHelper: DatabaseHelper,
    isLoading: Boolean,
    onUsersUpdated: () -> Unit,
    onMessageChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val filteredUsers = users.filter { it["role"] == role }
    val coroutineScope = rememberCoroutineScope()
    if (filteredUsers.isEmpty()) {
        Text("Tidak ada $role ditemukan")
    } else {
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(filteredUsers) { user ->
                UserCard(
                    user = user,
                    isLoading = isLoading,
                    onDelete = {
                        coroutineScope.launch {
                            try {
                                val userId = user["userId"] as? String
                                    ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                dbHelper.deleteUser(userId)
                                onUsersUpdated()
                                onMessageChange("$role berhasil dihapus")
                                snackbarHostState.showSnackbar(
                                    message = "$role berhasil dihapus",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                onMessageChange(e.message ?: "Gagal menghapus $role")
                                snackbarHostState.showSnackbar(
                                    message = e.message ?: "Gagal menghapus $role",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
