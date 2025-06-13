package com.example.ecommerceproject.chat

import android.util.Log
import com.example.ecommerceproject.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val database = FirebaseDatabase.getInstance("https://ecommerceproject-82a0e-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    private val auth = FirebaseAuth.getInstance()
    private val userDb = DatabaseHelper()

    fun getChatRoomId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    suspend fun startChatSession(receiverId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        val currentUserProfile = userDb.getUserProfile() ?: throw IllegalStateException("Current user profile not found")
        val receiverProfileSnapshot = database.child("users").child(receiverId).get().await()
        val receiverProfile = receiverProfileSnapshot.value as? Map<String, Any> ?: throw IllegalStateException("Receiver profile not found")

        val currentUserName = currentUserProfile["username"] as? String ?: "User"
        val receiverName = receiverProfile["username"] as? String ?: "User"

        val chatRoomId = getChatRoomId(currentUserId, receiverId)

        // Membuat/memperbarui entri di /user-chats untuk kedua pengguna
        database.child("user-chats").child(currentUserId).child(chatRoomId)
            .updateChildren(mapOf("receiverId" to receiverId, "receiverName" to receiverName)).await()
        database.child("user-chats").child(receiverId).child(chatRoomId)
            .updateChildren(mapOf("receiverId" to currentUserId, "receiverName" to currentUserName)).await()

        // Membuat metadata awal untuk ruang chat HANYA JIKA BELUM ADA
        val metadataRef = database.child("chats").child(chatRoomId).child("metadata")
        val snapshot = metadataRef.get().await()
        if (!snapshot.exists()) {
            val initialMetadata = mapOf(
                "participantIds" to mapOf(currentUserId to true, receiverId to true),
                "lastMessage" to "Chat dimulai...",
                "lastTimestamp" to ServerValue.TIMESTAMP
            )
            metadataRef.setValue(initialMetadata).await()
        }

        return chatRoomId
    }

    suspend fun sendMessage(chatRoomId: String, text: String, receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val messageData = mapOf(
            "senderId" to senderId,
            "text" to text,
            "timestamp" to ServerValue.TIMESTAMP
        )
        val metadataUpdate = mapOf(
            "lastMessage" to text,
            "lastTimestamp" to ServerValue.TIMESTAMP
        )

        // Kirim pesan
        database.child("chats").child(chatRoomId).child("messages").push().setValue(messageData).await()

        // Update metadata
        database.child("chats").child(chatRoomId).child("metadata").updateChildren(metadataUpdate).await()
        database.child("user-chats").child(senderId).child(chatRoomId).updateChildren(metadataUpdate).await()
        // Kita tidak bisa update punya receiver karena permission denied, ini sudah benar.
    }


    fun getUserChatRooms(userId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        val ref = database.child("user-chats").child(userId).orderByChild("lastTimestamp")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRooms = snapshot.children.mapNotNull {
                    (it.value as? Map<String, Any>)?.plus("chatRoomId" to it.key!!)
                }
                onResult(chatRooms.reversed()) // reversed() agar yang terbaru di atas
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Failed to get chat rooms: ${error.message}")
            }
        })
    }

    fun getChatMessages(chatRoomId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        val ref = database.child("chats").child(chatRoomId).child("messages").orderByChild("timestamp")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.value as? Map<String, Any>
                }
                onResult(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Failed to get messages: ${error.message}")
            }
        })
    }
}