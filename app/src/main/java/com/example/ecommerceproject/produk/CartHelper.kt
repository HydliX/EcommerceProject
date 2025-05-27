package com.example.ecommerceproject.Customer

class CartHelper {
    private val cartItems = mutableListOf<Map<String, Any>>()

    fun addToCart(product: Map<String, Any>) {
        cartItems.add(product)
    }

    fun getCartItems(): List<Map<String, Any>> {
        return cartItems.toList()
    }

    fun clearCart() {
        cartItems.clear()
    }
}