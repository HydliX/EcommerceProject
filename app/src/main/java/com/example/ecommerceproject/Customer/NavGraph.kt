package com.example.ecommerceproject

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecommerceproject.customer.CustomerDashboard
import com.example.ecommerceproject.product.CartScreen
import com.example.ecommerceproject.product.CheckoutScreen
import com.example.ecommerceproject.product.OrderConfirmationScreen
import com.example.ecommerceproject.product.ProductDetailScreen

@Composable
fun SetupNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("customerDashboard") {
            CustomerDashboard(
                navController = navController,
                userProfile = null, // Replace with actual userProfile if available
                isLoading = false,
                message = "",
                snackbarHostState = remember { SnackbarHostState() }
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
                snackbarHostState = remember { SnackbarHostState() }
            )
        }
        composable("cart") {
            CartScreen(
                navController = navController,
                snackbarHostState = remember { SnackbarHostState() }
            )
        }
        composable("checkout") {
            CheckoutScreen(
                navController = navController,
                snackbarHostState = remember { SnackbarHostState() }
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
                snackbarHostState = remember { SnackbarHostState() }
            )
        }
    }
}