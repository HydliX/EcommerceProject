package com.example.ecommerceproject.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(navController: NavController) {
    val chatRepository = remember { ChatRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var chatRooms by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            chatRepository.getUserChatRooms(currentUserId) { rooms ->
                chatRooms = rooms
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(chatRooms) { room ->
                val receiverName = room["receiverName"] as? String ?: "User"
                val chatRoomId = room["chatRoomId"] as? String ?: ""
                val receiverId = room["receiverId"] as? String ?: ""
                val lastMessage = room["lastMessage"] as? String ?: "..."

                // URL Encode nama agar aman dilewatkan sebagai argumen navigasi
                val encodedReceiverName = URLEncoder.encode(receiverName, StandardCharsets.UTF_8.toString())

                ListItem(
                    headlineContent = { Text(receiverName) },
                    supportingContent = { Text(lastMessage) },
                    modifier = Modifier.clickable {
                        navController.navigate("chat/$chatRoomId/$receiverId/$encodedReceiverName")
                    }
                )
                Divider()
            }
        }
    }
}