package com.example.ecommerceproject.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatRoomId: String,
    receiverId: String,
    receiverName: String
) {
    val chatRepository = remember { ChatRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var messages by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var newMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(chatRoomId) {
        chatRepository.getChatMessages(chatRoomId) { updatedMessages ->
            messages = updatedMessages
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(index = messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(receiverName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                value = newMessage,
                onValueChange = { newMessage = it },
                onSendClick = {
                    if (newMessage.isNotBlank()) {
                        scope.launch {
                            // --- PENAMBAHAN TRY-CATCH UNTUK KEAMANAN ---
                            try {
                                val messageToSend = newMessage
                                newMessage = "" // Langsung kosongkan UI
                                chatRepository.sendMessage(
                                    chatRoomId = chatRoomId,
                                    text = messageToSend,
                                    receiverId = receiverId
                                )
                            } catch (e: Exception) {
                                Log.e("ChatScreen", "Gagal mengirim pesan", e)
                                snackbarHostState.showSnackbar(
                                    message = "Gagal mengirim pesan: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                                newMessage = "" // Kembalikan teks jika gagal
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { message ->
                val isSentByMe = message["senderId"] == currentUserId
                ChatMessageItem(message, isSentByMe)
            }
        }
    }
}


@Composable
fun ChatMessageItem(message: Map<String, Any>, isSentByMe: Boolean) {
    val text = message["text"] as? String ?: ""
    val timestamp = message["timestamp"] as? Long ?: 0
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isSentByMe) 16.dp else 0.dp,
                        bottomEnd = if (isSentByMe) 0.dp else 16.dp
                    )
                )
                .background(if (isSentByMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = text)
        }
        Text(text = timeFormat.format(Date(timestamp)), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun MessageInput(value: String, onValueChange: (String) -> Unit, onSendClick: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ketik pesan...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = onSendClick) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}