package com.example.ecommerceproject

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DatabaseHelper {
    private val database = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    private val auth = FirebaseAuth.getInstance()

    object UserRole {
        const val ADMIN = "admin"
        const val PENGELOLA = "com/example/ecommerceproject/pengelola"
        const val SUPERVISOR = "com/example/ecommerceproject/supervisor"
        const val CUSTOMER = "customer"
    }

    object UserLevel {
        const val ADMIN = "admin"
        const val PENGELOLA = "com/example/ecommerceproject/pengelola"
        const val SUPERVISOR = "com/example/ecommerceproject/supervisor"
        const val USER = "user"
    }

    companion object {
        fun initCloudinary(context: Context) {
            val config = mapOf(
                "cloud_name" to "djwfibc4t",
                "api_key" to "461576188761489",
                "api_secret" to "P5pvHP3RSurX_jTut1Or6wIYAUU"
            )
            try {
                MediaManager.init(context, config)
                Log.d("DatabaseHelper", "Cloudinary berhasil diinisialisasi dengan cloud_name: djwfibc4t")
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Gagal menginisialisasi Cloudinary: ${e.message}", e)
                throw IllegalStateException("Inisialisasi Cloudinary gagal")
            }
        }

        internal suspend fun uploadToCloudinary(uri: Uri?, folder: String, publicId: String, preset: String): String {
            if (uri == null) {
                Log.e("DatabaseHelper", "Gagal mengunggah: URI kosong")
                throw IllegalArgumentException("URI gambar tidak boleh kosong")
            }
            Log.d("DatabaseHelper", "Mengunggah ke Cloudinary: folder=$folder, publicId=$publicId, uri=$uri")
            return suspendCancellableCoroutine { continuation ->
                MediaManager.get().upload(uri)
                    .option("folder", folder)
                    .option("upload_preset", preset)
                    .option("public_id", publicId)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d("DatabaseHelper", "Upload Cloudinary dimulai: requestId=$requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            Log.d("DatabaseHelper", "Progres upload Cloudinary: $bytes/$totalBytes")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = resultData["secure_url"] as? String
                            if (url != null) {
                                Log.d("DatabaseHelper", "Upload Cloudinary berhasil: url=$url")
                                continuation.resume(url)
                            } else {
                                Log.e("DatabaseHelper", "Upload Cloudinary gagal: Tidak ada URL")
                                continuation.resumeWithException(Exception("Tidak ada URL dari Cloudinary"))
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e("DatabaseHelper", "Upload Cloudinary gagal: ${error.description}")
                            continuation.resumeWithException(Exception("Upload Cloudinary gagal: ${error.description}"))
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w("DatabaseHelper", "Upload Cloudinary dijadwalkan ulang: ${error.description}")
                        }
                    })
                    .dispatch()
            }
        }
    }

    suspend fun uploadProfilePhoto(userId: String, uri: Uri?): String {
        return uploadToCloudinary(uri, "profile_photos", "profile_$userId", "profile_photos")
    }

    suspend fun uploadHobbyPhoto(userId: String, uri: Uri?, hobbyIndex: Int): String {
        return uploadToCloudinary(uri, "hobby_photos", "hobby_${userId}_$hobbyIndex", "hobby_photos")
    }

    suspend fun isAdmin(): Boolean {
        try {
            val profile = getUserProfile()
            if (profile == null) {
                Log.w("DatabaseHelper", "Profil pengguna kosong untuk userId=${auth.currentUser?.uid}")
                return false
            }
            val isAdmin = profile["role"] == UserRole.ADMIN
            Log.d("DatabaseHelper", "Cek isAdmin: profile=$profile, isAdmin=$isAdmin")
            return isAdmin
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memeriksa isAdmin: ${e.message}", e)
            return false
        }
    }

    suspend fun isPengelola(): Boolean {
        try {
            val profile = getUserProfile()
            if (profile == null) {
                Log.w("DatabaseHelper", "Profil pengguna kosong untuk userId=${auth.currentUser?.uid}")
                return false
            }
            val isPengelola = profile["role"] == UserRole.PENGELOLA
            Log.d("DatabaseHelper", "Cek isPengelola: profile=$profile, isPengelola=$isPengelola")
            return isPengelola
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memeriksa isPengelola: ${e.message}", e)
            return false
        }
    }

    suspend fun createAdminAccount(email: String, password: String, username: String) {
        Log.d("DatabaseHelper", "Mencoba membuat akun admin: email=$email")
        require(username.isNotBlank()) { "Username tidak boleh kosong" }
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(password.length >= 6) { "Kata sandi harus minimal 6 karakter" }
        val users = getAllUsers(UserRole.ADMIN)
        if (users.isNotEmpty()) {
            Log.w("DatabaseHelper", "Akun admin sudah ada: $users")
            throw IllegalStateException("Akun admin sudah ada")
        }
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw IllegalStateException("Gagal membuat pengguna admin")
            val userId = user.uid
            Log.d("DatabaseHelper", "Autentikasi admin berhasil dibuat: userId=$userId")
            user.sendEmailVerification().await()
            Log.d("DatabaseHelper", "Email verifikasi dikirim untuk admin: userId=$userId")
            saveUserProfile(
                userId = userId,
                username = username,
                email = email,
                role = UserRole.ADMIN,
                level = UserLevel.ADMIN
            )
            Log.d("DatabaseHelper", "Profil admin berhasil disimpan")
        } catch (e: FirebaseAuthException) {
            Log.e("DatabaseHelper", "Kesalahan autentikasi saat membuat admin: ${e.errorCode}, ${e.message}", e)
            throw when (e.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE" -> IllegalStateException("Email sudah digunakan")
                "ERROR_INVALID_EMAIL" -> IllegalArgumentException("Format email tidak valid")
                "ERROR_WEAK_PASSWORD" -> IllegalArgumentException("Kata sandi harus minimal 6 karakter")
                else -> DatabaseException("Gagal membuat admin: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Kesalahan tak terduga saat membuat admin: ${e.message}", e)
            throw DatabaseException("Gagal membuat admin: ${e.message}")
        }
    }

    suspend fun saveUserProfile(
        userId: String,
        username: String,
        email: String
    ) {
        saveUserProfile(userId, username, email, UserRole.CUSTOMER, UserLevel.USER, emptyList())
    }

    suspend fun saveUserProfile(
        userId: String,
        username: String,
        email: String,
        role: String,
        level: String,
        hobbies: List<Map<String, String>> = emptyList()
    ) {
        Log.d("DatabaseHelper", "Mencoba menyimpan profil pengguna: userId=$userId, role=$role, email=$email, hobbies=$hobbies")
        require(username.isNotBlank()) { "Username tidak boleh kosong" }
        require(email.isNotBlank()) { "Email tidak boleh kosong" }
        require(role in listOf(UserRole.ADMIN, UserRole.PENGELOLA, UserRole.SUPERVISOR, UserRole.CUSTOMER)) { "Role tidak valid: $role" }
        require(level in listOf(UserLevel.ADMIN, UserLevel.PENGELOLA, UserLevel.SUPERVISOR, UserLevel.USER)) { "Level tidak valid: $level" }
        hobbies.forEachIndexed { index, hobby ->
            require(hobby is Map<*, *>) { "Hobi $index harus Map, bukan ${hobby.javaClass}" }
            require(hobby.containsKey("imageUrl")) { "Hobi $index harus memiliki imageUrl" }
            require(hobby.containsKey("title")) { "Hobi $index harus memiliki title" }
            require(hobby.containsKey("description")) { "Hobi $index harus memiliki description" }
        }
        val isAdmin = isAdmin()
        if (role != UserRole.CUSTOMER && !isAdmin) {
            Log.w("DatabaseHelper", "Non-admin mencoba menetapkan role=$role")
            throw IllegalStateException("Hanya admin yang dapat menetapkan role non-customer")
        }
        val createdAt = System.currentTimeMillis()
        val user = mapOf(
            "username" to username,
            "email" to email,
            "address" to "",
            "profilePhotoUrl" to "",
            "createdAt" to createdAt,
            "role" to role,
            "level" to level,
            "userId" to userId,
            "hobbies" to hobbies
        )
        try {
            database.child("users").child(userId).setValue(user).await()
            Log.d("DatabaseHelper", "Profil pengguna berhasil disimpan: userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menyimpan profil pengguna: ${e.message}", e)
            throw DatabaseException("Gagal menyimpan profil pengguna: ${e.message}")
        }
    }

    suspend fun ensureUserProfile() {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        val profile = getUserProfile()
        if (profile == null) {
            val email = auth.currentUser?.email ?: ""
            saveUserProfile(
                userId = userId,
                username = email.substringBefore("@"),
                email = email,
                role = UserRole.CUSTOMER,
                level = UserLevel.USER
            )
        }
    }

    suspend fun getUserProfile(): Map<String, Any>? {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            val snapshot = database.child("users").child(userId).get().await()
            if (!snapshot.exists()) {
                ensureUserProfile()
                return getUserProfile()
            }
            val profile = snapshot.value as? Map<String, Any>
            Log.d("DatabaseHelper", "Berhasil mengambil profil pengguna: userId=$userId, profile=$profile")
            return profile
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil profil pengguna: ${e.message}", e)
            throw DatabaseException("Gagal mengambil profil pengguna: ${e.message}")
        }
    }

    suspend fun updateUserProfile(
        username: String,
        address: String,
        profilePhotoUrl: String,
        role: String = UserRole.CUSTOMER,
        level: String = UserLevel.USER,
        hobbies: List<Map<String, String>> = emptyList()
    ) {
        Log.d("DatabaseHelper", "Mencoba memperbarui profil pengguna: role=$role, photoUrl=$profilePhotoUrl, hobbies=$hobbies")
        require(username.isNotBlank()) { "Username tidak boleh kosong" }
        require(role in listOf(UserRole.ADMIN, UserRole.PENGELOLA, UserRole.SUPERVISOR, UserRole.CUSTOMER)) { "Role tidak valid: $role" }
        require(level in listOf(UserLevel.ADMIN, UserLevel.PENGELOLA, UserLevel.SUPERVISOR, UserLevel.USER)) { "Level tidak valid: $level" }
        hobbies.forEachIndexed { index, hobby ->
            require(hobby is Map<*, *>) { "Hobi $index harus Map, bukan ${hobby.javaClass}" }
            require(hobby.containsKey("imageUrl")) { "Hobi $index harus memiliki imageUrl" }
            require(hobby.containsKey("title")) { "Hobi $index harus memiliki title" }
            require(hobby.containsKey("description")) { "Hobi $index harus memiliki description" }
        }
        if (role != UserRole.CUSTOMER || level != UserLevel.USER) {
            if (!isAdmin()) {
                Log.w("DatabaseHelper", "Non-admin mencoba memperbarui role=$role, level=$level")
                throw IllegalStateException("Hanya admin yang dapat memperbarui role atau level")
            }
        }
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        val existingProfile = getUserProfile()
        val user = mapOf(
            "username" to username,
            "email" to (auth.currentUser?.email ?: ""),
            "address" to address,
            "profilePhotoUrl" to profilePhotoUrl,
            "createdAt" to (existingProfile?.get("createdAt") ?: System.currentTimeMillis()),
            "role" to role,
            "level" to level,
            "userId" to userId,
            "hobbies" to hobbies
        )
        try {
            database.child("users").child(userId).setValue(user).await()
            Log.d("DatabaseHelper", "Profil pengguna berhasil diperbarui: userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memperbarui profil pengguna: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui profil pengguna: ${e.message}")
        }
    }

    suspend fun getAllUsers(roleFilter: String? = null): List<Map<String, Any>> {
        try {
            if (roleFilter == UserRole.CUSTOMER && !isAdmin()) {
                val snapshot = database.child("users")
                    .orderByChild("role")
                    .equalTo(UserRole.CUSTOMER)
                    .get()
                    .await()
                val users = snapshot.children.mapNotNull { child ->
                    (child.value as? Map<String, Any>)?.plus("userId" to child.key!!)
                }
                Log.d("DatabaseHelper", "Berhasil mengambil pengguna: filter=$roleFilter, users=$users")
                return users
            } else {
                val snapshot = database.child("users").get().await()
                val users = snapshot.children.mapNotNull { child ->
                    (child.value as? Map<String, Any>)?.plus("userId" to child.key!!)
                }.filter { roleFilter == null || it["role"] == roleFilter }
                Log.d("DatabaseHelper", "Berhasil mengambil pengguna: filter=$roleFilter, users=$users")
                return users
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil pengguna: ${e.message}", e)
            throw DatabaseException("Gagal mengambil pengguna: ${e.message}")
        }
    }

    suspend fun deleteUser(userId: String) {
        Log.d("DatabaseHelper", "Mencoba menghapus pengguna: userId=$userId")
        if (!isAdmin()) {
            Log.w("DatabaseHelper", "Non-admin mencoba menghapus pengguna")
            throw IllegalStateException("Hanya admin yang dapat menghapus pengguna")
        }
        if (userId == auth.currentUser?.uid) {
            Log.w("DatabaseHelper", "Mencoba menghapus akun sendiri")
            throw IllegalStateException("Tidak dapat menghapus akun sendiri")
        }
        try {
            database.child("users").child(userId).removeValue().await()
            Log.d("DatabaseHelper", "Pengguna berhasil dihapus: userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menghapus pengguna: ${e.message}", e)
            throw DatabaseException("Gagal menghapus pengguna: ${e.message}")
        }
    }

    suspend fun updateUserRole(userId: String, role: String, level: String) {
        Log.d("DatabaseHelper", "Mencoba memperbarui role pengguna: userId=$userId, role=$role, level=$level")
        require(role in listOf(UserRole.ADMIN, UserRole.PENGELOLA, UserRole.SUPERVISOR, UserRole.CUSTOMER)) { "Role tidak valid: $role" }
        require(level in listOf(UserLevel.ADMIN, UserLevel.PENGELOLA, UserLevel.SUPERVISOR, UserLevel.USER)) { "Level tidak valid: $level" }
        if (!isAdmin()) {
            Log.w("DatabaseHelper", "Non-admin mencoba memperbarui role pengguna")
            throw IllegalStateException("Hanya admin yang dapat memperbarui role pengguna")
        }
        val snapshot = database.child("users").child(userId).get().await()
        if (!snapshot.exists()) {
            Log.w("DatabaseHelper", "Pengguna tidak ditemukan: userId=$userId")
            throw IllegalStateException("Pengguna tidak ditemukan")
        }
        val existingProfile = snapshot.value as? Map<String, Any> ?: throw IllegalStateException("Data pengguna tidak valid")
        val user = mapOf(
            "username" to (existingProfile["username"] ?: ""),
            "email" to (existingProfile["email"] ?: ""),
            "address" to (existingProfile["address"] ?: ""),
            "profilePhotoUrl" to (existingProfile["profilePhotoUrl"] ?: ""),
            "createdAt" to (existingProfile["createdAt"] ?: System.currentTimeMillis()),
            "role" to role,
            "level" to level,
            "userId" to userId,
            "hobbies" to (existingProfile["hobbies"] ?: emptyList<Map<String, String>>())
        )
        try {
            database.child("users").child(userId).setValue(user).await()
            Log.d("DatabaseHelper", "Role pengguna berhasil diperbarui: userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memperbarui role pengguna: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui role pengguna: ${e.message}")
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        username: String,
        address: String,
        contactPhone: String,
        profilePhotoUrl: String,
        role: String = UserRole.CUSTOMER,
        level: String = UserLevel.USER,
        hobbies: List<Map<String, String>> = emptyList()
    ) {
        Log.d("DatabaseHelper", "Mencoba memperbarui profil pengguna: userId=$userId, username=$username, address=$address, contactPhone=$contactPhone")
        require(username.isNotBlank()) { "Username tidak boleh kosong" }
        require(role in listOf(UserRole.ADMIN, UserRole.PENGELOLA, UserRole.SUPERVISOR, UserRole.CUSTOMER)) { "Role tidak valid: $role" }
        require(level in listOf(UserLevel.ADMIN, UserLevel.PENGELOLA, UserLevel.SUPERVISOR, UserLevel.USER)) { "Level tidak valid: $level" }
        hobbies.forEachIndexed { index, hobby ->
            require(hobby is Map<*, *>) { "Hobi $index harus Map, bukan ${hobby.javaClass}" }
            require(hobby.containsKey("imageUrl")) { "Hobi $index harus memiliki imageUrl" }
            require(hobby.containsKey("title")) { "Hobi $index harus memiliki title" }
            require(hobby.containsKey("description")) { "Hobi $index harus memiliki description" }
        }
        if (userId != auth.currentUser?.uid && !isAdmin()) {
            Log.w("DatabaseHelper", "Non-admin mencoba memperbarui profil pengguna lain: userId=$userId")
            throw IllegalStateException("Hanya admin yang dapat memperbarui profil pengguna lain")
        }
        val snapshot = database.child("users").child(userId).get().await()
        if (!snapshot.exists()) {
            Log.w("DatabaseHelper", "Profil pengguna tidak ditemukan: userId=$userId")
            throw IllegalStateException("Pengguna tidak ditemukan")
        }
        val existingProfile = snapshot.value as? Map<String, Any> ?: throw IllegalStateException("Data pengguna tidak valid")
        val user = mapOf(
            "username" to username,
            "email" to (existingProfile["email"] ?: ""),
            "address" to address,
            "contactPhone" to contactPhone,
            "profilePhotoUrl" to profilePhotoUrl,
            "createdAt" to (existingProfile["createdAt"] ?: System.currentTimeMillis()),
            "role" to role,
            "level" to level,
            "userId" to userId,
            "hobbies" to hobbies
        )
        try {
            database.child("users").child(userId).setValue(user).await()
            Log.d("DatabaseHelper", "Profil pengguna berhasil diperbarui: userId=$userId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memperbarui profil pengguna: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui profil pengguna: ${e.message}")
        }
    }

    suspend fun addToWishlist(productId: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            database.child("wishlist").child(userId).child(productId).setValue(true).await()
            Log.d("DatabaseHelper", "Produk berhasil ditambahkan ke wishlist: userId=$userId, productId=$productId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menambah produk ke wishlist: ${e.message}", e)
            throw DatabaseException("Gagal menambah produk ke wishlist: ${e.message}")
        }
    }

    suspend fun removeFromWishlist(productId: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            database.child("wishlist").child(userId).child(productId).removeValue().await()
            Log.d("DatabaseHelper", "Produk berhasil dihapus dari wishlist: userId=$userId, productId=$productId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menghapus produk dari wishlist: ${e.message}", e)
            throw DatabaseException("Gagal menghapus produk dari wishlist: ${e.message}")
        }
    }

    suspend fun getWishlist(): List<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            val snapshot = database.child("wishlist").child(userId).get().await()
            val productIds = snapshot.children.map { it.key!! }
            val dbProduct = DatabaseProduct()
            val products = dbProduct.getAllProducts().filter { it["productId"] in productIds }
            Log.d("DatabaseHelper", "Berhasil mengambil wishlist: userId=$userId, count=${products.size}")
            return products
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil wishlist: ${e.message}", e)
            throw DatabaseException("Gagal mengambil wishlist: ${e.message}")
        }
    }

    suspend fun addToCart(productId: String, quantity: Int) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        require(quantity > 0) { "Jumlah produk harus lebih dari 0" }
        val cartItem = mapOf(
            "quantity" to quantity,
            "addedAt" to System.currentTimeMillis()
        )
        try {
            database.child("cart").child(userId).child(productId).setValue(cartItem).await()
            Log.d("DatabaseHelper", "Produk berhasil ditambahkan ke keranjang: userId=$userId, productId=$productId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menambah produk ke keranjang: ${e.message}", e)
            throw DatabaseException("Gagal menambah produk ke keranjang: ${e.message}")
        }
    }

    suspend fun updateCartItem(productId: String, quantity: Int) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        require(quantity >= 0) { "Jumlah produk tidak boleh negatif" }
        try {
            if (quantity == 0) {
                database.child("cart").child(userId).child(productId).removeValue().await()
                Log.d("DatabaseHelper", "Produk dihapus dari keranjang: userId=$userId, productId=$productId")
            } else {
                val cartItem = mapOf(
                    "quantity" to quantity,
                    "addedAt" to System.currentTimeMillis()
                )
                database.child("cart").child(userId).child(productId).setValue(cartItem).await()
                Log.d("DatabaseHelper", "Jumlah produk di keranjang diperbarui: userId=$userId, productId=$productId, quantity=$quantity")
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memperbarui keranjang: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui keranjang: ${e.message}")
        }
    }

    suspend fun getCart(): List<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            val snapshot = database.child("cart").child(userId).get().await()
            val cartItems = snapshot.children.mapNotNull { child ->
                (child.value as? Map<String, Any>)?.plus("productId" to child.key!!)
            }
            Log.d("DatabaseHelper", "Berhasil mengambil keranjang: userId=$userId, count=${cartItems.size}")
            return cartItems
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil keranjang: ${e.message}", e)
            throw DatabaseException("Gagal mengambil keranjang: ${e.message}")
        }
    }

    suspend fun createOrder(
        items: Map<String, Map<String, Any>>,
        totalPrice: Double,
        shippingAddress: String,
        paymentMethod: String
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        require(totalPrice >= 0) { "Total harga tidak boleh negatif" }
        require(shippingAddress.isNotBlank()) { "Alamat pengiriman tidak boleh kosong" }
        require(paymentMethod.isNotBlank()) { "Metode pembayaran tidak boleh kosong" }
        require(items.isNotEmpty()) { "Pesanan harus berisi setidaknya satu item" }
        val orderId = database.child("orders").push().key
            ?: throw IllegalStateException("Gagal menghasilkan ID pesanan")
        val order = mapOf(
            "userId" to userId,
            "status" to "PENDING",
            "totalPrice" to totalPrice,
            "createdAt" to System.currentTimeMillis(),
            "shippingAddress" to shippingAddress,
            "paymentMethod" to paymentMethod,
            "items" to items
        )
        try {
            database.child("orders").child(orderId).setValue(order).await()
            database.child("cart").child(userId).removeValue().await()
            Log.d("DatabaseHelper", "Pesanan berhasil dibuat: orderId=$orderId, userId=$userId")
            return orderId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal membuat pesanan: ${e.message}", e)
            throw DatabaseException("Gagal membuat pesanan: ${e.message}")
        }
    }

    suspend fun getOrders(): List<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            val snapshot = database.child("orders").orderByChild("userId").equalTo(userId).get().await()
            val orders = snapshot.children.mapNotNull { child ->
                (child.value as? Map<String, Any>)?.plus("orderId" to child.key!!)
            }
            Log.d("DatabaseHelper", "Berhasil mengambil pesanan: userId=$userId, count=${orders.size}")
            return orders
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil pesanan: ${e.message}", e)
            throw DatabaseException("Gagal mengambil pesanan: ${e.message}")
        }
    }

    suspend fun addPromotion(
        title: String,
        description: String,
        startDate: Long,
        endDate: Long,
        category: String,
        discountPercentage: Double
    ): String {
        if (!isAdmin()) {
            Log.w("DatabaseHelper", "Non-admin mencoba menambah promosi")
            throw IllegalStateException("Hanya admin yang dapat menambah promosi")
        }
        require(title.isNotBlank()) { "Judul promosi tidak boleh kosong" }
        require(description.isNotBlank()) { "Deskripsi promosi tidak boleh kosong" }
        require(startDate <= endDate) { "Tanggal mulai harus sebelum tanggal berakhir" }
        require(category.isNotBlank()) { "Kategori promosi tidak boleh kosong" }
        require(discountPercentage in 0.0..100.0) { "Persentase diskon harus antara 0 dan 100" }
        val promoId = database.child("promotions").push().key
            ?: throw IllegalStateException("Gagal menghasilkan ID promosi")
        val promotion = mapOf(
            "title" to title,
            "description" to description,
            "startDate" to startDate,
            "endDate" to endDate,
            "category" to category,
            "discountPercentage" to discountPercentage,
            "createdAt" to System.currentTimeMillis()
        )
        try {
            database.child("promotions").child(promoId).setValue(promotion).await()
            Log.d("DatabaseHelper", "Promosi berhasil ditambahkan: promoId=$promoId, title=$title")
            return promoId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menambah promosi: ${e.message}", e)
            throw DatabaseException("Gagal menambah promosi: ${e.message}")
        }
    }

    suspend fun getPromotions(category: String? = null): List<Map<String, Any>> {
        try {
            val snapshot = database.child("promotions").get().await()
            var promotions = snapshot.children.mapNotNull { child ->
                (child.value as? Map<String, Any>)?.plus("promoId" to child.key!!)
            }
            if (category != null) {
                promotions = promotions.filter { it["category"] == category }
            }
            Log.d("DatabaseHelper", "Berhasil mengambil promosi: count=${promotions.size}")
            return promotions
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil promosi: ${e.message}", e)
            throw DatabaseException("Gagal mengambil promosi: ${e.message}")
        }
    }

    suspend fun createSupportTicket(subject: String, message: String): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        require(subject.isNotBlank()) { "Subjek tiket tidak boleh kosong" }
        require(message.isNotBlank()) { "Pesan tiket tidak boleh kosong" }
        val ticketId = database.child("support").push().key
            ?: throw IllegalStateException("Gagal menghasilkan ID tiket")
        val ticket = mapOf(
            "userId" to userId,
            "subject" to subject,
            "message" to message,
            "status" to "OPEN",
            "createdAt" to System.currentTimeMillis()
        )
        try {
            database.child("support").child(ticketId).setValue(ticket).await()
            Log.d("DatabaseHelper", "Tiket dukungan berhasil dibuat: ticketId=$ticketId, userId=$userId")
            return ticketId
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal membuat tiket dukungan: ${e.message}", e)
            throw DatabaseException("Gagal membuat tiket dukungan: ${e.message}")
        }
    }

    suspend fun addSupportResponse(ticketId: String, message: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        require(message.isNotBlank()) { "Pesan respons tidak boleh kosong" }
        val responseId = database.child("support").child(ticketId).child("responses").push().key
            ?: throw IllegalStateException("Gagal menghasilkan ID respons")
        val response = mapOf(
            "message" to message,
            "sender" to userId,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            database.child("support").child(ticketId).child("responses").child(responseId).setValue(response).await()
            Log.d("DatabaseHelper", "Respons dukungan berhasil ditambahkan: ticketId=$ticketId, responseId=$responseId")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal menambah respons dukungan: ${e.message}", e)
            throw DatabaseException("Gagal menambah respons dukungan: ${e.message}")
        }
    }

    suspend fun getSupportTickets(): List<Map<String, Any>> {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Pengguna tidak terautentikasi")
        try {
            val isAdmin = isAdmin()
            val snapshot = if (isAdmin) {
                database.child("support").get().await()
            } else {
                database.child("support").orderByChild("userId").equalTo(userId).get().await()
            }
            val tickets = snapshot.children.mapNotNull { child ->
                (child.value as? Map<String, Any>)?.plus("ticketId" to child.key!!)
            }
            Log.d("DatabaseHelper", "Berhasil mengambil tiket dukungan: userId=$userId, count=${tickets.size}")
            return tickets
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil tiket dukungan: ${e.message}", e)
            throw DatabaseException("Gagal mengambil tiket dukungan: ${e.message}")
        }
    }

    suspend fun updateInfo(
        contactEmail: String,
        contactPhone: String,
        returnPolicy: String,
        faq: List<Map<String, String>>
    ) {
        if (!isAdmin()) {
            Log.w("DatabaseHelper", "Non-admin mencoba memperbarui info")
            throw IllegalStateException("Hanya admin yang dapat memperbarui info")
        }
        require(contactEmail.isNotBlank()) { "Email kontak tidak boleh kosong" }
        require(contactPhone.isNotBlank()) { "Nomor telepon kontak tidak boleh kosong" }
        require(returnPolicy.isNotBlank()) { "Kebijakan pengembalian tidak boleh kosong" }
        require(faq.isNotEmpty()) { "FAQ tidak boleh kosong" }
        faq.forEach { item ->
            require(item.containsKey("question") && item.containsKey("answer")) { "Setiap FAQ harus memiliki pertanyaan dan jawaban" }
        }
        val info = mapOf(
            "contact" to mapOf(
                "email" to contactEmail,
                "phone" to contactPhone
            ),
            "returnPolicy" to returnPolicy,
            "faq" to faq
        )
        try {
            database.child("info").setValue(info).await()
            Log.d("DatabaseHelper", "Informasi statis berhasil diperbarui")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal memperbarui informasi statis: ${e.message}", e)
            throw DatabaseException("Gagal memperbarui informasi statis: ${e.message}")
        }
    }

    suspend fun getInfo(): Map<String, Any> {
        try {
            val snapshot = database.child("info").get().await()
            val info = snapshot.value as? Map<String, Any> ?: emptyMap()
            Log.d("DatabaseHelper", "Berhasil mengambil informasi statis: $info")
            return info
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Gagal mengambil informasi statis: ${e.message}", e)
            throw DatabaseException("Gagal mengambil informasi statis: ${e.message}")
        }
    }
}

class DatabaseException(message: String) : Exception(message)