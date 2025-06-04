package com.example.ecommerceproject.product

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import kotlinx.coroutines.launch

@Composable
fun CartScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cartItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var cartProducts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Fetch cart items and corresponding product details
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                DatabaseHelper.initCloudinary(context)
                cartItems = DatabaseHelper().getCart()
                cartProducts = cartItems.mapNotNull { item ->
                    val productId = item["productId"] as? String
                    if (productId != null) {
                        DatabaseHelper().getProductById(productId)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to load cart: ${e.message}")
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Keranjang Belanja", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(cartProducts) { product ->
                val productId = product["productId"] as? String ?: ""
                val cartItem = cartItems.find { it["productId"] == productId }
                val quantity = (cartItem?.get("quantity") as? Number)?.toInt() ?: 0
                val price = (product["price"] as? Number)?.toDouble() ?: 0.0
                val name = product["name"] as? String ?: "Unknown Product"
                val category = product["category"] as? String ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Name: $name")
                            Text(text = "Price: Rp${String.format("%.2f", price)}")
                            Text(text = "Category: $category")
                            Text(text = "Quantity: $quantity")
                            Text(text = "Subtotal: Rp${String.format("%.2f", price * quantity)}")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            DatabaseHelper().updateCartItem(productId, quantity + 1)
                                            cartItems = DatabaseHelper().getCart()
                                            cartProducts = cartItems.mapNotNull { item ->
                                                val pid = item["productId"] as? String
                                                if (pid != null) DatabaseHelper().getProductById(pid) else null
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Failed to update quantity: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Text("+")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val newQuantity = if (quantity > 1) quantity - 1 else 0
                                            DatabaseHelper().updateCartItem(productId, newQuantity)
                                            cartItems = DatabaseHelper().getCart()
                                            cartProducts = cartItems.mapNotNull { item ->
                                                val pid = item["productId"] as? String
                                                if (pid != null) DatabaseHelper().getProductById(pid) else null
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Failed to update quantity: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Text("-")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            DatabaseHelper().updateCartItem(productId, 0)
                                            cartItems = DatabaseHelper().getCart()
                                            cartProducts = cartItems.mapNotNull { item ->
                                                val pid = item["productId"] as? String
                                                if (pid != null) DatabaseHelper().getProductById(pid) else null
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Failed to remove item: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Text("Hapus")
                            }
                        }
                    }
                }
            }
        }

        // Tambahkan tombol untuk checkout
        Button(
            onClick = { navController.navigate("checkout") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = cartProducts.isNotEmpty()
        ) {
            Text("Lanjut ke Checkout")
        }
    }
}