package com.example.ecommerceproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecommerceproject.customer.CustomerDashboard
import com.example.ecommerceproject.pengelola.EditProductScreen
import com.example.ecommerceproject.product.CartScreen
import com.example.ecommerceproject.product.CheckoutScreen
import com.example.ecommerceproject.product.OrderConfirmationScreen
import com.example.ecommerceproject.product.ProductDetailScreen
import com.example.ecommerceproject.product.CartScreen // Tambahan

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
                isLoading = false,
                message = "",
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
            arguments = listOf(navArgument("productId") { type = androidx.navigation.NavType.StringType })
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
            arguments = listOf(navArgument("orderId") { type = androidx.navigation.NavType.StringType })
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
            arguments = listOf(navArgument("productId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            EditProductScreen(
                productId = productId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("chart") { // Tambahan rute untuk ChartScreen
            CartScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}