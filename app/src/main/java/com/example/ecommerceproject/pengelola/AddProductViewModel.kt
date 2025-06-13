package com.example.ecommerceproject.pengelola

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

// sealed class untuk merepresentasikan state UI
sealed class AddProductUiState {
    data object Idle : AddProductUiState()
    data object Loading : AddProductUiState()
    data class Success(val message: String) : AddProductUiState()
    data class Error(val message: String) : AddProductUiState()
}

class AddProductViewModel : ViewModel() {
    private val dbHelper = DatabaseHelper()

    private val _uiState = MutableStateFlow<AddProductUiState>(AddProductUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun addProduct(
        productName: String,
        productPrice: String,
        productCategory: String,
        productDescription: String,
        productStock: String,
        productImageUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = AddProductUiState.Loading
            try {
                // Validasi input di dalam ViewModel
                val price = productPrice.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Harga harus berupa angka")
                val stock = productStock.toIntOrNull()
                    ?: throw IllegalArgumentException("Stok harus berupa angka")

                if (productName.isBlank() || productCategory.isBlank() || productDescription.isBlank()) {
                    throw IllegalArgumentException("Semua kolom harus diisi")
                }
                if (price < 0 || stock < 0) {
                    throw IllegalArgumentException("Harga dan stok tidak boleh negatif")
                }
                if (productImageUri == null) {
                    throw IllegalArgumentException("Gambar produk harus dipilih")
                }

                // Panggil fungsi database dari viewModelScope
                dbHelper.addProduct(
                    name = productName,
                    price = price,
                    description = productDescription,
                    imageUri = productImageUri,
                    category = productCategory,
                    stock = stock
                )

                // Jika berhasil
                _uiState.value = AddProductUiState.Success("Produk berhasil ditambahkan")

            } catch (e: CancellationException) {
                Log.w("AddProductViewModel", "Operasi tambah produk dibatalkan.")
                _uiState.value = AddProductUiState.Error("Operasi dibatalkan")
            } catch (e: Exception) {
                // Jika gagal karena alasan lain
                Log.e("AddProductViewModel", "Gagal menambah produk", e)
                _uiState.value = AddProductUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    // Fungsi untuk mereset state setelah pesan ditampilkan
    fun resetState() {
        _uiState.value = AddProductUiState.Idle
    }
}