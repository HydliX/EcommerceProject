package com.example.ecommerceproject.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ecommerceproject.DatabaseHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val dbHelper = DatabaseHelper()
    val auth = FirebaseAuth.getInstance()
    var feedbackText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf("Criticism") } // Default ke Criticism, bisa Criticism atau Suggestion
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = Color(0xFF6200EE)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Submit Feedback",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Share your Criticism or Suggestion",
                style = MaterialTheme.typography.titleLarge,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dropdown untuk memilih jenis feedback
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = feedbackType,
                    onValueChange = { /* Tidak perlu, hanya untuk tampilan */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    label = { Text("Feedback Type") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = primaryColor,
                        unfocusedIndicatorColor = Color.Gray,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = { Text("Criticism") },
                        onClick = {
                            feedbackType = "Criticism"
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Suggestion") },
                        onClick = {
                            feedbackType = "Suggestion"
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // TextField untuk input feedback
            TextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                label = { Text("Your Feedback") },
                placeholder = { Text("Write your criticism or suggestion here...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = primaryColor,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (feedbackText.isBlank()) {
                        errorMessage = "Feedback cannot be empty"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = ""
                    coroutineScope.launch {
                        try {
                            val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
                            val feedback = mapOf(
                                "userId" to userId,
                                "text" to feedbackText.trim(),
                                "createdAt" to System.currentTimeMillis(),
                                "status" to "Pending",
                                "type" to feedbackType // Menambahkan field type untuk membedakan kritik atau saran
                            )
                            dbHelper.submitComplaint(feedback)
                            snackbarHostState.showSnackbar("Feedback submitted successfully")
                            feedbackText = "" // Reset input setelah berhasil
                            navController.navigate("dashboard") { // Kembali ke dashboard
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            errorMessage = "Failed to submit feedback: ${e.message}"
                            snackbarHostState.showSnackbar(errorMessage)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Submit Feedback")
                }
            }
        }
    }
}
