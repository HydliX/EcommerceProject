package com.example.ecommerceproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Import yang benar untuk semua layar chat
import com.example.ecommerceproject.chat.ChatListScreen
import com.example.ecommerceproject.chat.ChatScreen
import com.example.ecommerceproject.customer.CustomerDashboard
import com.example.ecommerceproject.pengelola.EditProductScreen
import com.example.ecommerceproject.product.CartScreen
import com.example.ecommerceproject.product.CheckoutScreen
import com.example.ecommerceproject.product.OrderConfirmationScreen
import com.example.ecommerceproject.product.ProductDetailScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi Cloudinary sekali di awal
        DatabaseHelper.initCloudinary(this)
        setContent {
            ECommerceProjectTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("register") {
            RegisterScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("dashboard") {
            Dashboard(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("customerDashboard") {
            CustomerDashboard(
                navController = navController,
                userProfile = null,
                snackbarHostState = snackbarHostState
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("settings") {
            SettingsScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            "productDetail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("cart") {
            CartScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("checkout") {
            CheckoutScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            "orderConfirmation/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderConfirmationScreen(
                orderId = orderId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable(
            "editProduct/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            EditProductScreen(
                productId = productId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }

        // ==========================================================
        // PENAMBAHAN RUTE UNTUK FITUR CHAT DI SINI
        // ==========================================================
        composable("chatList") {
            ChatListScreen(navController = navController)
        }
        composable(
            "chat/{chatRoomId}/{receiverId}/{receiverName}",
            arguments = listOf(
                navArgument("chatRoomId") { type = NavType.StringType },
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: "" // Ambil receiverId
            val receiverName = backStackEntry.arguments?.getString("receiverName")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: "Chat"

            // Panggil ChatScreen dengan semua parameter yang diperlukan
            ChatScreen(
                navController = navController,
                chatRoomId = chatRoomId,
                receiverId = receiverId, // Kirim receiverId ke ChatScreen
                receiverName = receiverName
            )
        }
    }
}