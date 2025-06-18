package com.example.ecommerceproject.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageUsersViewModel : ViewModel() {

    private val dbHelper = DatabaseHelper()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

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
}