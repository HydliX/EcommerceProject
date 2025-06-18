package com.example.ecommerceproject.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceproject.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

class ManageUsersViewModel : ViewModel() {

    private val dbHelper = DatabaseHelper()
    private val auth = FirebaseAuth.getInstance()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    init {
        checkAdminStatus()
    }

    fun checkAdminStatus() {
        viewModelScope.launch {
            try {
                val result = dbHelper.isAdmin()
                _isAdmin.value = result
                Log.d("ManageUsersVM", "isAdmin: $result")
            } catch (e: Exception) {
                Log.e("ManageUsersVM", "Error checking admin status: ${e.message}", e)
                _isAdmin.value = false
            }
        }
    }

    fun createPimpinanAccount(email: String, password: String, username: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                dbHelper.createPimpinanAccount(email, password, username)
                _operationState.value = OperationState.Success("Akun Pimpinan berhasil dibuat. Verifikasi email terkirim. Silakan re-autentikasi.")
            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah digunakan oleh akun lain. Silakan gunakan email lain atau reset kata sandi."
                    "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                    "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah."
                    else -> e.message ?: "Gagal membuat akun Pimpinan"
                }
                Log.e("ManageUsersVM", "Auth error: ${e.errorCode}, ${e.message}", e)
                _operationState.value = OperationState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("ManageUsersVM", "Failed to create Pimpinan account: ${e.message}", e)
                _operationState.value = OperationState.Error(e.message ?: "Gagal membuat akun Pimpinan")
            }
        }
    }

    fun createSupervisorAccount(
        email: String,
        password: String,
        username: String,
        userProfile: Map<String, Any>?
    ) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                if (username.isBlank()) throw IllegalArgumentException("Nama pengguna tidak boleh kosong")
                if (email.isBlank()) throw IllegalArgumentException("Email tidak boleh kosong")
                if (password.length < 6) throw IllegalArgumentException("Kata sandi harus minimal 6 karakter")

                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: throw IllegalStateException("ID pengguna tidak ditemukan")
                val userId = user.uid

                user.sendEmailVerification().await()

                try {
                    dbHelper.saveUserProfile(
                        userId = userId,
                        username = username,
                        email = email,
                        role = DatabaseHelper.UserRole.SUPERVISOR,
                        level = DatabaseHelper.UserLevel.SUPERVISOR
                    )
                } catch (e: Exception) {
                    user.delete().await()
                    throw e
                }

                _operationState.value = OperationState.Success("Supervisor berhasil ditambahkan. Verifikasi email terkirim. Silakan re-autentikasi.")
            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah digunakan oleh akun lain. Silakan gunakan email lain atau reset kata sandi."
                    "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                    "ERROR_WEAK_PASSWORD" -> "Kata sandi terlalu lemah."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                    else -> e.message ?: "Gagal menambah supervisor"
                }
                Log.e("ManageUsersVM", "Auth error: ${e.errorCode}, ${e.message}", e)
                _operationState.value = OperationState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("ManageUsersVM", "Failed to create Supervisor account: ${e.message}", e)
                _operationState.value = OperationState.Error(e.message ?: "Gagal menambah supervisor")
            }
        }
    }

    fun reAuthenticateAdmin(adminEmail: String?, adminPassword: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            try {
                val email = adminEmail ?: auth.currentUser?.email
                ?: throw IllegalStateException("Email admin tidak ditemukan")
                auth.signInWithEmailAndPassword(email, adminPassword).await()
                _operationState.value = OperationState.Success("Re-autentikasi berhasil")
            } catch (e: FirebaseAuthException) {
                val errorMessage = when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> "Kata sandi salah."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Kesalahan jaringan. Periksa koneksi internet Anda."
                    else -> e.message ?: "Re-autentikasi gagal"
                }
                Log.e("ManageUsersVM", "Re-auth error: ${e.errorCode}, ${e.message}", e)
                _operationState.value = OperationState.Error(errorMessage)
            } catch (e: Exception) {
                Log.e("ManageUsersVM", "Failed to re-authenticate: ${e.message}", e)
                _operationState.value = OperationState.Error(e.message ?: "Re-autentikasi gagal")
            }
        }
    }

    fun promoteUser(
        userId: String,
        newRole: String,
        newLevel: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                dbHelper.updateUserRole(userId, newRole, newLevel)
                onSuccess()
            } catch (e: Exception) {
                Log.e("ManageUsersVM", "Failed to promote user: ${e.message}", e)
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}