package com.example.ecommerceproject.Customer

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val cartHelper = CartHelper()
    val cartItems = cartHelper.getCartItems()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ringkasan Pesanan:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (cartItems.isEmpty()) {
                Text(
                    text = "Tidak ada item di pesanan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            } else {
                cartItems.forEach { item ->
                    Text(
                        text = "${item["name"]} - Rp${item["price"]}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pilih Metode Pembayaran:",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Pembayaran dengan Kartu Kredit belum diimplementasikan",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kartu Kredit")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Pembayaran dengan Transfer Bank belum diimplementasikan",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer Bank")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            cartHelper.clearCart() // Kosongkan keranjang
                            navController.navigate("orderConfirmation")
                            snackbarHostState.showSnackbar(
                                message = "Pesanan berhasil diselesaikan",
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            Log.e("CheckoutScreen", "Navigasi ke orderConfirmation gagal: ${e.message}", e)
                            snackbarHostState.showSnackbar(
                                message = "Gagal menyelesaikan pesanan",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Selesaikan Pesanan")
            }
        }
    }
}