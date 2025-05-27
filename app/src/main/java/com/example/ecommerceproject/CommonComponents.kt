package com.example.ecommerceproject

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserCard(
    user: Map<String, Any>,
    isLoading: Boolean,
    onPromote: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = if (user["profilePhotoUrl"]?.toString()?.isNotEmpty() == true) {
                    rememberAsyncImagePainter(
                        model = user["profilePhotoUrl"],
                        placeholder = painterResource(R.drawable.ic_placeholder),
                        error = painterResource(R.drawable.ic_error),
                        onLoading = { Log.d("UserCard", "Memuat foto pengguna: ${user["profilePhotoUrl"]}") },
                        onSuccess = { Log.d("UserCard", "Berhasil memuat foto pengguna") },
                        onError = { error ->
                            Log.e("UserCard", "Gagal memuat foto pengguna: ${error.result.throwable.message}")
                        }
                    )
                } else {
                    painterResource(R.drawable.ic_placeholder)
                },
                contentDescription = "Foto Pengguna",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Nama Pengguna: ${user["username"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Email: ${user["email"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "Role: ${user["role"] as? String ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                val createdAt = user["createdAt"] as? Long
                if (createdAt != null) {
                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(createdAt))
                    Text(
                        text = "Dibuat: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
            if (onPromote != null || onDelete != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    if (onPromote != null) {
                        IconButton(
                            onClick = { onPromote() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Promosikan Pengguna",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onDelete != null) {
                        IconButton(
                            onClick = { onDelete() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}