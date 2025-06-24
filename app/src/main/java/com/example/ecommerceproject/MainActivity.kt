package com.example.ecommerceproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ecommerceproject.admin.AdminDashboard
import com.example.ecommerceproject.customer.OrderStatusScreen
import com.example.ecommerceproject.customer.ComplaintScreen
import com.example.ecommerceproject.chat.ChatListScreen
import com.example.ecommerceproject.chat.ChatScreen
import com.example.ecommerceproject.customer.CustomerDashboard
import com.example.ecommerceproject.leader.LeaderDashboard
import com.example.ecommerceproject.pengelola.PengelolaDashboard
import com.example.ecommerceproject.supervisor.SupervisorDashboard
import com.example.ecommerceproject.product.EditProductScreen
import com.example.ecommerceproject.product.AllReviewScreen
import com.example.ecommerceproject.product.CartScreen
import com.example.ecommerceproject.product.CheckoutScreen
import com.example.ecommerceproject.product.OrderConfirmationScreen
import com.example.ecommerceproject.product.RatingReviewScreen
import com.example.ecommerceproject.product.ProductDetailScreen
import com.google.firebase.database.FirebaseDatabase
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    FirebaseDatabase.getInstance().setPersistenceEnabled(true)

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
        composable("adminDashboard") {
            AdminDashboard(
                navController = navController,
                userProfile = null,
                isLoading = false,
                message = "",
                snackbarHostState = snackbarHostState
            )
        }
        composable("supervisor_dashboard") {
            SupervisorDashboard(
                navController = navController,
                userProfile = null,
                isLoading = false,
                message = "",
                snackbarHostState = snackbarHostState
            )
        }
        composable("pengelola_dashboard") {
            PengelolaDashboard(
                navController = navController,
                userProfile = null,
                snackbarHostState = snackbarHostState
            )
        }
        composable("leader_dashboard") {
            LeaderDashboard(
                navController = navController,
                userProfile = null,
                isLoading = false,
                message = "",
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
        composable(
            "allReviews/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            AllReviewScreen(
                productId = productId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
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
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: "Chat"

            ChatScreen(
                navController = navController,
                chatRoomId = chatRoomId,
                receiverId = receiverId,
                receiverName = receiverName
            )
        }
        composable("complaint") {
            ComplaintScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
        composable("orderStatus") {
            val db = remember { DatabaseHelper() }
            val coroutineScope = rememberCoroutineScope()
            var orders by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                try {
                    orders = db.getOrders()
                    isLoading = false
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = e.message ?: "Failed to load orders"
                    snackbarHostState.showSnackbar(errorMessage)
                }
            }

            OrderStatusScreen(
                orders = orders,
                navController = navController,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                db = db
            )
        }
        composable(
            "ratingReview/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            RatingReviewScreen(
                orderId = orderId,
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }
    }
}
