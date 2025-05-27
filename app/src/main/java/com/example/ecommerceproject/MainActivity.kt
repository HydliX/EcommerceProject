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
import com.example.ecommerceproject.Customer.CartScreen
import com.example.ecommerceproject.Customer.CheckoutScreen
import com.example.ecommerceproject.Customer.OrderConfirmationScreen
import com.example.ecommerceproject.Customer.ProductDetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
        composable("dashboard") {
            Dashboard(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("productDetail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                navController = navController
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
        composable("orderConfirmation") {
            OrderConfirmationScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}