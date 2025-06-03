package com.example.ecommerceproject

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController, snackbarHostState: SnackbarHostState) {
    val auth = FirebaseAuth.getInstance()
    val dbHelper = DatabaseHelper()
    var userProfile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var username by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var profilePhotoUrl by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showEditUsernameDialog by remember { mutableStateOf(false) }
    var showEditAddressDialog by remember { mutableStateOf(false) }
    var showEditHobbyDialog by remember { mutableStateOf(false) }
    var currentEditingHobbyIndex by remember { mutableStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Data class for hobby information including title and description
    data class HobbyInfo(
        val imageUrl: String = "",
        val title: String = "",
        val description: String = ""
    )

    // Store hobbies as HobbyInfo objects
    var hobbies by remember { mutableStateOf<List<HobbyInfo>>(emptyList()) }

    // Image picker launcher for profile photo
    val profileImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    Log.d("ProfileScreen", "Uploading profile photo to Cloudinary: $uri")
                    val downloadUrl = dbHelper.uploadProfilePhoto(auth.currentUser!!.uid, it)
                    profilePhotoUrl = downloadUrl
                    Log.d("ProfileScreen", "Profile photo uploaded: $downloadUrl")
                    dbHelper.updateUserProfile(
                        username,
                        address,
                        profilePhotoUrl,
                        role,
                        level,
                        hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                    )
                    message = "Profile photo updated successfully"
                    userProfile = dbHelper.getUserProfile()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to upload profile photo: ${e.message}", e)
                    message = "Failed to upload profile photo: ${e.message}"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Image picker launcher for hobby photos
    val hobbyImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    Log.d("ProfileScreen", "Uploading hobby photo to Cloudinary: $uri")
                    val hobbyIndex = currentEditingHobbyIndex.takeIf { it >= 0 }
                        ?: hobbies.size.coerceAtMost(2) // Next available slot (0, 1, or 2)
                    val downloadUrl = dbHelper.uploadHobbyPhoto(auth.currentUser!!.uid, it, hobbyIndex)
                    Log.d("ProfileScreen", "Hobby photo uploaded: $downloadUrl")

                    val updatedHobbies = hobbies.toMutableList()

                    // If we're editing an existing hobby, update it
                    if (hobbyIndex < updatedHobbies.size) {
                        val currentHobby = updatedHobbies[hobbyIndex]
                        updatedHobbies[hobbyIndex] = currentHobby.copy(imageUrl = downloadUrl)
                    } else {
                        // Otherwise add a new hobby
                        updatedHobbies.add(HobbyInfo(imageUrl = downloadUrl, title = "", description = ""))
                    }

                    hobbies = updatedHobbies

                    // Save to database
                    dbHelper.updateUserProfile(
                        username,
                        address,
                        profilePhotoUrl,
                        role,
                        level,
                        hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                    )

                    message = "Hobby photo updated successfully"
                    userProfile = dbHelper.getUserProfile()

                    // Open the edit dialog to allow adding title and description
                    currentEditingHobbyIndex = hobbyIndex
                    showEditHobbyDialog = true

                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Failed to upload hobby photo: ${e.message}", e)
                    message = "Failed to upload hobby photo: ${e.message}"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    // Edit hobby dialog (for title and description)
    if (showEditHobbyDialog && currentEditingHobbyIndex >= 0 && currentEditingHobbyIndex < hobbies.size) {
        val currentHobby = hobbies[currentEditingHobbyIndex]
        var tempTitle by remember { mutableStateOf(currentHobby.title) }
        var tempDescription by remember { mutableStateOf(currentHobby.description) }

        AlertDialog(
            onDismissRequest = { showEditHobbyDialog = false },
            title = { Text("Edit Hobby Details") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Hobby Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempDescription,
                        onValueChange = { tempDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // Update the hobby with new title and description
                                val updatedHobbies = hobbies.toMutableList()
                                updatedHobbies[currentEditingHobbyIndex] = updatedHobbies[currentEditingHobbyIndex].copy(
                                    title = tempTitle,
                                    description = tempDescription
                                )
                                hobbies = updatedHobbies

                                // Save to database
                                dbHelper.updateUserProfile(
                                    username,
                                    address,
                                    profilePhotoUrl,
                                    role,
                                    level,
                                    hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                                )

                                message = "Hobby details updated successfully"
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                showEditHobbyDialog = false
                            } catch (e: Exception) {
                                Log.e("ProfileScreen", "Failed to update hobby details: ${e.message}", e)
                                message = "Failed to update hobby details: ${e.message}"
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHobbyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit username dialog
    if (showEditUsernameDialog) {
        var tempUsername by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showEditUsernameDialog = false },
            title = { Text("Edit Username") },
            text = {
                OutlinedTextField(
                    value = tempUsername,
                    onValueChange = { tempUsername = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempUsername.isBlank()) {
                            message = "Username cannot be empty"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = message,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            coroutineScope.launch {
                                try {
                                    username = tempUsername
                                    dbHelper.updateUserProfile(
                                        username,
                                        address,
                                        profilePhotoUrl,
                                        role,
                                        level,
                                        hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                                    )
                                    message = "Username updated successfully"
                                    userProfile = dbHelper.getUserProfile()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    showEditUsernameDialog = false
                                } catch (e: Exception) {
                                    Log.e("ProfileScreen", "Failed to update username: ${e.message}", e)
                                    message = "Failed to update username: ${e.message}"
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUsernameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit address dialog
    if (showEditAddressDialog) {
        var tempAddress by remember { mutableStateOf(address) }
        AlertDialog(
            onDismissRequest = { showEditAddressDialog = false },
            title = { Text("Edit Address") },
            text = {
                OutlinedTextField(
                    value = tempAddress,
                    onValueChange = { tempAddress = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                address = tempAddress
                                dbHelper.updateUserProfile(
                                    username,
                                    address,
                                    profilePhotoUrl,
                                    role,
                                    level,
                                    hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                                )
                                message = "Address updated successfully"
                                userProfile = dbHelper.getUserProfile()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                showEditAddressDialog = false
                            } catch (e: Exception) {
                                Log.e("ProfileScreen", "Failed to update address: ${e.message}", e)
                                message = "Failed to update address: ${e.message}"
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAddressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Check if user is authenticated and email is verified
    if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
        LaunchedEffect(Unit) {
            Log.w("ProfileScreen", "User not authenticated or email not verified, redirecting to login")
            message = if (auth.currentUser == null) {
                "Please login to access your profile."
            } else {
                "Please verify your email to access your profile."
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
        return
    }

    // Load user profile and pre-fill fields
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            Log.d("ProfileScreen", "Loading user profile for userId=${auth.currentUser?.uid}")
            userProfile = dbHelper.getUserProfile()
            userProfile?.let { profile ->
                username = profile["username"] as? String ?: ""
                address = profile["address"] as? String ?: ""
                profilePhotoUrl = profile["profilePhotoUrl"] as? String ?: ""
                role = profile["role"] as? String ?: DatabaseHelper.UserRole.CUSTOMER
                level = profile["level"] as? String ?: DatabaseHelper.UserLevel.USER

                Log.d("ProfileScreen", "Raw hobbies data: ${profile["hobbies"]}, type=${profile["hobbies"]?.javaClass}")

                // Initialize hobbies as HobbyInfo objects
                hobbies = when (val hobbiesData = profile["hobbies"]) {
                    is List<*> -> hobbiesData
                        .take(3)
                        .mapNotNull { item ->
                            when (item) {
                                is Map<*, *> -> {
                                    val imageUrl = item["imageUrl"] as? String ?: ""
                                    val title = item["title"] as? String ?: ""
                                    val description = item["description"] as? String ?: ""
                                    HobbyInfo(imageUrl = imageUrl, title = title, description = description)
                                }
                                is String -> {
                                    // Handle legacy data where hobbies were just URLs
                                    HobbyInfo(imageUrl = item, title = "", description = "")
                                }
                                else -> {
                                    Log.w("ProfileScreen", "Invalid hobby item: $item")
                                    null
                                }
                            }
                        }
                    else -> emptyList()
                }

                Log.d("ProfileScreen", "Profile loaded: username=$username, address=$address, profilePhotoUrl=$profilePhotoUrl, role=$role, level=$level, hobbies=$hobbies")
            } ?: run {
                Log.w("ProfileScreen", "No profile data found")
                message = "No profile data found. Please update your profile."
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Failed to load profile: ${e.message}", e)
            message = "Failed to load profile: ${e.message}"
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = navController.currentDestination?.route == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = navController.currentDestination?.route == "profile",
                    onClick = {
                        navController.navigate("profile") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = navController.currentDestination?.route == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Loading profile...",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (userProfile != null) {
                userProfile?.let { profile ->
                    // Profile photo
                    Image(
                        painter = if (profilePhotoUrl.isNotEmpty()) {
                            rememberAsyncImagePainter(
                                model = profilePhotoUrl,
                                placeholder = painterResource(R.drawable.ic_placeholder),
                                error = painterResource(R.drawable.ic_error),
                                onLoading = { Log.d("ProfileScreen", "Loading profile photo: $profilePhotoUrl") },
                                onSuccess = { Log.d("ProfileScreen", "Successfully loaded profile photo") },
                                onError = { error ->
                                    Log.e("ProfileScreen", "Failed to load profile photo: ${error.result.throwable.message}")
                                }
                            )
                        } else {
                            painterResource(R.drawable.ic_placeholder)
                        },
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Change Photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(
                                enabled = !isLoading,
                                onClick = { profileImagePickerLauncher.launch("image/*") }
                            )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Profile details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Email: ${profile["email"] as? String ?: "Unknown"}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Username: $username",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { showEditUsernameDialog = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Username",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Address: ${address.ifBlank { "Not set" }}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { showEditAddressDialog = true }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Address",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Role: ${role.ifBlank { "Not set" }}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Level: ${level.ifBlank { "Not set" }}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Hobbies section
                    Text(
                        "Hobbies",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hobby cards layout
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(3) { index ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(250.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Hobby image
                                    Image(
                                        painter = if (index < hobbies.size && hobbies[index].imageUrl.isNotEmpty()) {
                                            rememberAsyncImagePainter(
                                                model = hobbies[index].imageUrl,
                                                placeholder = painterResource(R.drawable.ic_placeholder),
                                                error = painterResource(R.drawable.ic_error)
                                            )
                                        } else {
                                            painterResource(R.drawable.ic_placeholder)
                                        },
                                        contentDescription = "Hobby Photo ${index + 1}",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable(
                                                enabled = !isLoading,
                                                onClick = {
                                                    currentEditingHobbyIndex = index
                                                    hobbyImagePickerLauncher.launch("image/*")
                                                }
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Hobby title
                                    Text(
                                        text = if (index < hobbies.size && hobbies[index].title.isNotEmpty())
                                            hobbies[index].title else "Add title",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Hobby description
                                    Text(
                                        text = if (index < hobbies.size && hobbies[index].description.isNotEmpty())
                                            hobbies[index].description else "Add a description",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Edit button
                                    Button(
                                        onClick = {
                                            if (index < hobbies.size) {
                                                currentEditingHobbyIndex = index
                                                showEditHobbyDialog = true
                                            } else {
                                                currentEditingHobbyIndex = index
                                                hobbyImagePickerLauncher.launch("image/*")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(if (index < hobbies.size) "Edit" else "Add")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Admin controls
                    if (role == DatabaseHelper.UserRole.ADMIN) {
                        val manageUsersInteractionSource = remember { MutableInteractionSource() }
                        val manageUsersIsPressed by manageUsersInteractionSource.collectIsPressedAsState()
                        val manageUsersScale by animateFloatAsState(if (manageUsersIsPressed) 0.95f else 1f)

                        Button(
                            onClick = {
                                Log.d("ProfileScreen", "Admin navigating to dashboard")
                                navController.navigate("dashboard") {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(manageUsersScale),
                            interactionSource = manageUsersInteractionSource,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            elevation = ButtonDefaults.buttonElevation(4.dp)
                        ) {
                            Text("Manage Users")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Interaction source for Save Hobbies button
                    val saveHobbiesInteractionSource = remember { MutableInteractionSource() }
                    val saveHobbiesIsPressed by saveHobbiesInteractionSource.collectIsPressedAsState()
                    val saveHobbiesScale by animateFloatAsState(if (saveHobbiesIsPressed) 0.95f else 1f)

                    // Save hobbies button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    Log.d(
                                        "ProfileScreen",
                                        "Saving profile changes: username=$username, address=$address, photo=$profilePhotoUrl, role=$role, level=$level, hobbies=$hobbies"
                                    )
                                    dbHelper.updateUserProfile(
                                        username,
                                        address,
                                        profilePhotoUrl,
                                        role,
                                        level,
                                        hobbies.map { mapOf("imageUrl" to it.imageUrl, "title" to it.title, "description" to it.description) }
                                    )
                                    message = "Hobbies updated successfully"
                                    userProfile = dbHelper.getUserProfile()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("ProfileScreen", "Failed to update hobbies: ${e.message}", e)
                                    message = "Failed to update hobbies: ${e.message}"
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(saveHobbiesScale),
                        enabled = !isLoading,
                        interactionSource = saveHobbiesInteractionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("Save Hobbies")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Interaction source for Logout button
                    val logoutInteractionSource = remember { MutableInteractionSource() }
                    val logoutIsPressed by logoutInteractionSource.collectIsPressedAsState()
                    val logoutScale by animateFloatAsState(if (logoutIsPressed) 0.95f else 1f)

                    // Logout button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    Log.d("ProfileScreen", "Logging out user: ${auth.currentUser?.uid}")
                                    auth.signOut()
                                    message = "Logged out successfully"
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                    navController.navigate("login") {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("ProfileScreen", "Failed to log out: ${e.message}", e)
                                    message = "Failed to log out: ${e.message}"
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(logoutScale),
                        enabled = !isLoading,
                        interactionSource = logoutInteractionSource,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        Text("Logout")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}