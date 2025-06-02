package com.example.ecommerceproject.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch

@Composable
fun CartScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Fetch cart items when the screen is composed
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Initialize DatabaseProduct with Cloudinary (if not already done)
                DatabaseHelper.initCloudinary(context)
                cartItems = DatabaseHelper().getCart()
            } catch (e: Exception) {
                // Handle error (e.g., show snackbar)
                snackbarHostState.showSnackbar("Failed to load cart: ${e.message}")
            }
        }
    }

    Column {
        LazyColumn {
            items(cartItems) { item ->
                val name = item["name"] as? String ?: "Unknown Product"
                val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                val category = item["category"] as? String ?: ""
                val quantity = (item["quantity"] as? Number)?.toInt() ?: 1

                Text(text = "Name: $name")
                Text(text = "Price: $price")
                Text(text = "Category: $category")
                Text(text = "Quantity: $quantity")
                Text(text = "Subtotal: ${price * quantity}")
            }
        }
    }
}
