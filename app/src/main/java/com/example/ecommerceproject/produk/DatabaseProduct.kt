package com.example.ecommerceproject

import android.net.Uri
import android.util.Log
import com.example.ecommerceproject.Customer.CartHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class DatabaseProduct {
    private val database = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    private val auth = FirebaseAuth.getInstance()
    private val cartHelper = CartHelper()

    suspend fun uploadProductImage(productId: String, uri: Uri?): String {
        return DatabaseHelper.uploadToCloudinary(uri, "product_images", "product_$productId", "product_images")
    }

    suspend fun addProduct(
        name: String,
        price: Double,
        description: String,
        imageUri: Uri?,
        category: String,
        stock: Int
    ): String {
        val dbHelper = DatabaseHelper()
        if (!dbHelper.isAdmin() && !dbHelper.isPengelola()) {
            Log.w("DatabaseProduct", "Non-admin atau non-pengelola mencoba menambah produk")
            throw IllegalStateException("Hanya admin dan pengelola yang dapat menambah produk")
        }
        require(name.isNotBlank()) { "Nama produk tidak boleh kosong" }
        require(price >= 0) { "Harga produk tidak boleh negatif" }
        require(description.isNotBlank()) { "Deskripsi produk tidak boleh kosong" }
        require(category.isNotBlank()) { "Kategori produk tidak boleh kosong" }
        require(stock >= 0) { "Stok produk tidak boleh negatif" }
        val productId = database.child("products").push().key
            ?: throw IllegalStateException("Gagal menghasilkan ID produk")
        val imageUrl = if (imageUri != null) uploadProductImage(productId, imageUri) else ""
        val product = mapOf(
            "name" to name,
            "price" to price,
            "description" to description,
            "imageUrl" to imageUrl,
            "category" to category,
            "stock" to stock,
            "createdAt" to System.currentTimeMillis(),
            "productId" to productId
        )
        try {
            database.child("products").child(productId).setValue(product).await()
            Log.d("DatabaseProduct", "Produk berhasil ditambahkan: id=$productId, name=$name")
            return productId
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal menambah produk: ${e.message}", e)
            throw DatabaseException("Gagal menambah produk: ${e.message}")
        }
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        price: Double,
        description: String,
        imageUri: Uri?,
        category: String,
        stock: Int
    ) {
        val dbHelper = DatabaseHelper()
        if (!dbHelper.isAdmin() && !dbHelper.isPengelola()) {
            Log.w("DatabaseProduct", "Non-admin atau non-pengelola mencoba memperbarui produk")
            throw IllegalStateException("Hanya admin dan pengelola yang dapat memperbarui produk")
        }
        require(name.isNotBlank()) { "Nama produk tidak boleh kosong" }
        require(price >= 0) { "Harga produk tidak boleh negatif" }
        require(description.isNotBlank()) { "Deskripsi produk tidak boleh kosong" }
        require(category.isNotBlank()) { "Kategori produk tidak boleh kosong" }
        require(stock >= 0) { "Stok produk tidak boleh negatif" }
        val existingProduct = database.child("products").child(productId).get().await()
        if (!existingProduct.exists()) {
            throw IllegalStateException("Produk tidak ditemukan: id=$productId")
        }
        val existingData = existingProduct.value as? Map<String, Any> ?: mapOf()
        val imageUrl = if (imageUri != null) {
            uploadProductImage(productId, imageUri)
        } else {
            existingData["imageUrl"] as? String ?: ""
        }
        val product = mapOf(
            "name" to name,
            "price" to price,
            "description" to description,
            "imageUrl" to imageUrl,
            "category" to category,
            "stock" to stock,
            "createdAt" to (existingData["createdAt"] ?: System.currentTimeMillis()),
            "productId" to productId
        )
        try {
            database.child("products").child(productId).setValue(product).await()
            Log.d("DatabaseProduct", "Produk berhasil diperbarui: id=$productId, name=$name")
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal memperbarui produk: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui produk: ${e.message}")
        }
    }

    suspend fun deleteProduct(productId: String) {
        val dbHelper = DatabaseHelper()
        if (!dbHelper.isAdmin() && !dbHelper.isPengelola()) {
            Log.w("DatabaseProduct", "Non-admin atau non-pengelola mencoba menghapus produk")
            throw IllegalStateException("Hanya admin dan pengelola yang dapat menghapus produk")
        }
        try {
            database.child("products").child(productId).removeValue().await()
            Log.d("DatabaseProduct", "Produk berhasil dihapus: id=$productId")
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal menghapus produk: ${e.message}", e)
            throw DatabaseException("Gagal menghapus produk: ${e.message}")
        }
    }

    suspend fun getAllProducts(
        category: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        searchQuery: String? = null
    ): List<Map<String, Any>> {
        try {
            val query = database.child("products")
            val snapshot = query.get().await()
            var products = snapshot.children.mapNotNull { child ->
                (child.value as? Map<String, Any>)?.plus("productId" to child.key!!)
            }
            if (category != null) {
                products = products.filter { it["category"] == category }
            }
            if (minPrice != null) {
                products = products.filter { (it["price"] as? Number)?.toDouble() ?: 0.0 >= minPrice }
            }
            if (maxPrice != null) {
                products = products.filter { (it["price"] as? Number)?.toDouble() ?: 0.0 <= maxPrice }
            }
            if (searchQuery != null) {
                products = products.filter {
                    (it["name"] as? String)?.contains(searchQuery, ignoreCase = true) == true
                }
            }
            Log.d("DatabaseProduct", "Berhasil mengambil produk: count=${products.size}")
            return products
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal mengambil produk: ${e.message}")
            throw DatabaseException("Gagal mengambil produk: ${e.message}")
        }
    }

    suspend fun getProductById(productId: String): Map<String, Any> {
        try {
            val snapshot = database.child("products").child(productId).get().await()
            if (!snapshot.exists()) {
                throw IllegalStateException("Produk tidak ditemukan: id=$productId")
            }
            val product = snapshot.value as? Map<String, Any> ?: throw IllegalStateException("Data produk tidak valid")
            Log.d("DatabaseProduct", "Berhasil mengambil produk: id=$productId")
            return product
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal mengambil produk: ${e.message}", e)
            throw DatabaseException("Gagal mengambil produk: ${e.message}")
        }
    }

    suspend fun addProductRating(productId: String, rating: Double, review: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalArgumentException("Pengguna tidak terautentikasi")
        require(rating in 0.0..5.0) { "Rating harus antara 0 dan 5" }
        require(review.isNotBlank()) { "Ulasan tidak boleh kosong" }
        val ratingData = mapOf(
            "rating" to rating,
            "review" to review,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            database.child("products").child(productId).child("ratings").child(userId).setValue(ratingData).await()
            Log.d("DatabaseProduct", "Rating produk berhasil ditambahkan: productId=$productId, userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal menambah rating produk: ${e.message}", e)
            throw DatabaseException("Gagal menambah rating produk: ${e.message}")
        }
    }

    suspend fun addProductToCart(productId: String, quantity: Int) {
        require(quantity > 0) { "Jumlah produk harus lebih dari 0" }
        val products = getAllProducts()
        val product = products.find { it["productId"] == productId }
            ?: throw IllegalStateException("Produk tidak ditemukan: id=$productId")
        val cartItem = product.plus("quantity" to quantity)
        try {
            cartHelper.addToCart(cartItem)
            Log.d("DatabaseProduct", "Produk berhasil ditambahkan ke keranjang: productId=$productId, quantity=$quantity")
        } catch (e: Exception) {
            Log.e("DatabaseProduct", "Gagal menambah produk ke keranjang: ${e.message}", e)
            throw DatabaseException("Gagal menambah produk ke keranjang: ${e.message}")
        }
    }
}

class DatabaseProductException(message: String) : Exception(message)