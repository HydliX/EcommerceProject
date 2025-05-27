package com.example.ecommerceproject

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUserProfileViewModel : ViewModel() {
    private val dbHelper = DatabaseHelper()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun updateUserProfile(
        userId: String,
        username: String,
        address: String,
        contactPhone: String,
        profilePhotoUrl: String,
        role: String,
        level: String,
        hobbies: List<Map<String, String>>,
        onUsersUpdated: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("EditUserProfileVM", "Memulai update profil untuk userId: $userId, username: '$username'")

                if (username.isBlank()) {
                    _message.value = "Gagal memperbarui profil: Username tidak boleh kosong"
                    _snackbarMessage.value = "Gagal memperbarui profil: Username tidak boleh kosong"
                    Log.e("EditUserProfileVM", "Validasi gagal: Username tidak boleh kosong, value: '$username'")
                    _isLoading.value = false
                    return@launch
                }

                Log.d("EditUserProfileVM", "Mengirim ke DatabaseHelper: userId=$userId, username='$username'")
                withContext(Dispatchers.IO) {
                    dbHelper.updateUserProfile(
                        userId = userId,
                        username = username,
                        address = address,
                        contactPhone = contactPhone,
                        profilePhotoUrl = profilePhotoUrl,
                        role = role,
                        level = level,
                        hobbies = hobbies
                    )
                }
                Log.d("EditUserProfileVM", "Berhasil update profil untuk userId: $userId")
                _message.value = "Profil berhasil diperbarui"
                _snackbarMessage.value = "Profil berhasil diperbarui"
                onUsersUpdated()
            } catch (e: java.net.UnknownHostException) {
                _message.value = "Gagal terhubung ke server. Periksa koneksi internet Anda."
                _snackbarMessage.value = "Gagal terhubung ke server. Periksa koneksi internet Anda."
            } catch (e: SecurityException) {
                _message.value = "Anda tidak memiliki izin untuk mengedit profil ini."
                _snackbarMessage.value = "Tidak memiliki izin untuk mengedit profil ini"
            } catch (e: Exception) {
                _message.value = "Gagal memperbarui profil: ${e.message ?: "Kesalahan tidak diketahui"}"
                _snackbarMessage.value = "Gagal memperbarui profil: ${e.message ?: "Kesalahan tidak diketahui"}"
                Log.e("EditUserProfileVM", "Error memperbarui profil: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}