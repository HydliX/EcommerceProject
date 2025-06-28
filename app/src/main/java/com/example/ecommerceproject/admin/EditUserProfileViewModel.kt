package com.example.ecommerceproject

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUserProfileViewModel : ViewModel() {
    val isLoading = mutableStateOf(false)
    val message = mutableStateOf("")
    val snackbarMessage = mutableStateOf<String?>(null)

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
            isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    val dbHelper = DatabaseHelper()
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
                onUsersUpdated()
                snackbarMessage.value = "Profil pengguna berhasil diperbarui"
            } catch (e: Exception) {
                message.value = e.message ?: "Gagal memperbarui profil"
                snackbarMessage.value = message.value
            } finally {
                isLoading.value = false
            }
        }
    }
}