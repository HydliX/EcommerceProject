package com.example.ecommerceproject.product

import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CartHelper {
    private val dbProduct = DatabaseHelper()

    suspend fun getCartItems(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            dbProduct.getCart()
        }
    }

    suspend fun addToCart(product: Map<String, Any>) {
        val productId = product["productId"] as? String
            ?: throw IllegalArgumentException("Product ID tidak ditemukan")
        val currentCart = getCartItems()
        val existingItem = currentCart.find { it["productId"] == productId }
        val quantity = if (existingItem != null) {
            (existingItem["quantity"] as? Number)?.toInt() ?: 1
        } else {
            1
        }
        dbProduct.updateCartItem(productId, quantity + 1)
    }

    suspend fun updateCartItemQuantity(productId: String, newQuantity: Int) {
        withContext(Dispatchers.IO) {
            dbProduct.updateCartItem(productId, newQuantity)
        }
    }

    suspend fun clearCart() {
        withContext(Dispatchers.IO) {
            val userId = dbProduct.CurrentUserId()
            if (userId != null) {
                dbProduct.clearCart(userId)
            }
        }
    }

    suspend fun removeFromCart(productId: String) {
        withContext(Dispatchers.IO) {
            dbProduct.updateCartItem(productId, 0)
        }
    }
}
