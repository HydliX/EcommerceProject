package com.example.ecommerceproject

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var showPromoteDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedRole by remember { mutableStateOf(DatabaseHelper.UserRole.SUPERVISOR) }
    val coroutineScope = rememberCoroutineScope()

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
                        coroutineScope.launch {
                            try {
                                onLoadingChange(true)
                                val userId = selectedUser!!["userId"] as? String
                                    ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                Log.d("ManageUsers", "Mempromosikan pengguna dengan userId: $userId ke $selectedRole")
                                val newLevel = when (selectedRole) {
                                    DatabaseHelper.UserRole.SUPERVISOR -> DatabaseHelper.UserLevel.SUPERVISOR
                                    DatabaseHelper.UserRole.PENGELOLA -> DatabaseHelper.UserLevel.PENGELOLA
                                    else -> DatabaseHelper.UserLevel.USER
                                }
                                dbHelper.updateUserRole(userId, selectedRole, newLevel)
                                onUsersUpdated()
                                onMessageChange("Pengguna dipromosikan menjadi $selectedRole")
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Pengguna dipromosikan menjadi $selectedRole",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                showPromoteDialog = false
                            } catch (e: Exception) {
                                onMessageChange(e.message ?: "Gagal mempromosikan pengguna")
                                Log.e("ManageUsers", "Gagal mempromosikan pengguna: ${e.message}", e)
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
        Text(
            "Supervisor",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (users.filter { it["role"] == DatabaseHelper.UserRole.SUPERVISOR }.isEmpty()) {
            Text(
                "Tidak ada supervisor ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(users.filter { it["role"] == DatabaseHelper.UserRole.SUPERVISOR }) { user ->
                    UserCard(
                        user = user,
                        isLoading = isLoading,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    val userId = user["userId"] as? String
                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                    Log.d("ManageUsers", "Menghapus supervisor dengan userId: $userId")
                                    dbHelper.deleteUser(userId)
                                    onUsersUpdated()
                                    onMessageChange("Supervisor berhasil dihapus")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Supervisor berhasil dihapus",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } catch (e: Exception) {
                                    onMessageChange(e.message ?: "Gagal menghapus supervisor")
                                    Log.e("ManageUsers", "Gagal menghapus supervisor: ${e.message}", e)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Pengelola",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (users.filter { it["role"] == DatabaseHelper.UserRole.PENGELOLA }.isEmpty()) {
            Text(
                "Tidak ada pengelola ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(users.filter { it["role"] == DatabaseHelper.UserRole.PENGELOLA }) { user ->
                    UserCard(
                        user = user,
                        isLoading = isLoading,
                        onDelete = {
                            coroutineScope.launch {
                                try {
                                    val userId = user["userId"] as? String
                                        ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                                    Log.d("ManageUsers", "Menghapus pengelola dengan userId: $userId")
                                    dbHelper.deleteUser(userId)
                                    onUsersUpdated()
                                    onMessageChange("Pengelola berhasil dihapus")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Pengelola berhasil dihapus",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } catch (e: Exception) {
                                    onMessageChange(e.message ?: "Gagal menghapus pengelola")
                                    Log.e("ManageUsers", "Gagal menghapus pengelola: ${e.message}", e)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Pelanggan",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (users.filter { it["role"] == DatabaseHelper.UserRole.CUSTOMER }.isEmpty()) {
            Text(
                "Tidak ada pelanggan ditemukan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(users.filter { it["role"] == DatabaseHelper.UserRole.CUSTOMER }) { user ->
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
                                    Log.d("ManageUsers", "Menghapus pelanggan dengan userId: $userId")
                                    dbHelper.deleteUser(userId)
                                    onUsersUpdated()
                                    onMessageChange("Pelanggan berhasil dihapus")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Pelanggan berhasil dihapus",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } catch (e: Exception) {
                                    onMessageChange(e.message ?: "Gagal menghapus pelanggan")
                                    Log.e("ManageUsers", "Gagal menghapus pelanggan: ${e.message}", e)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}