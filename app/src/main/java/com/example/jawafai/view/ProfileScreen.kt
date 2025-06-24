package com.example.jawafai.view

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.example.jawafai.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.example.jawafai.model.UserModel
import androidx.compose.runtime.livedata.observeAsState
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val repository = remember { UserRepositoryImpl(FirebaseAuth.getInstance(), com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    val auth = remember { FirebaseAuth.getInstance() }
    val viewModel: UserViewModel = viewModel(factory = UserViewModelFactory(repository, auth))
    val userProfile by viewModel.userProfile.observeAsState()
    val userState by viewModel.userState.observeAsState(UserViewModel.UserOperationResult.Initial)
    var isEditing by remember { mutableStateOf(false) }

    // Local state for editing fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf("") }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfileImage(it) { url ->
                if (url != null && userProfile != null) {
                    val updatedUser = userProfile!!.copy(imageUrl = url)
                    viewModel.updateUser(updatedUser)
                    imageUrl = url // Update local state so UI reflects new image immediately
                    snackbarMessage = "Profile image updated!"
                } else {
                    snackbarMessage = "Failed to upload image."
                }
            }
        }
    }

    // Permission launcher for runtime permissions
    val context = LocalContext.current
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            // Optionally show a message to the user
        }
    }

    // Fetch user profile on first composition ONLY (never write default data)
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }

    // Update local state when userProfile changes
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            firstName = profile.firstName
            lastName = profile.lastName
            username = profile.username
            email = profile.email
            dateOfBirth = profile.dateOfBirth
            imageUrl = profile.imageUrl ?: ""
        }
    }

    // Optionally, also fetch profile when returning to this screen or after updates
    LaunchedEffect(userState) {
        if (userState is UserViewModel.UserOperationResult.Success) {
            viewModel.fetchUserProfile()
        }
    }

    // Show Snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage.isNotBlank()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            snackbarMessage = ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(contentAlignment = Alignment.BottomEnd) {
                val profileImageUrl = if (imageUrl.isNotBlank()) imageUrl else userProfile?.imageUrl?.takeIf { !it.isNullOrBlank() } ?: "https://ui-avatars.com/api/?name=${userProfile?.username ?: "User"}"
                Image(
                    painter = rememberAsyncImagePainter(profileImageUrl),
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 34) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = Color(0xFF365A61)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { if (isEditing) firstName = it },
                label = { Text("First Name") },
                enabled = isEditing,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { if (isEditing) lastName = it },
                label = { Text("Last Name") },
                enabled = isEditing,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = { if (isEditing) username = it },
                label = { Text("Username") },
                enabled = isEditing,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { if (isEditing) email = it },
                label = { Text("Email") },
                enabled = false, // Email is not editable
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Date of Birth
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { if (isEditing) dateOfBirth = it },
                label = { Text("Date of Birth") },
                enabled = isEditing,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                Button(
                    onClick = {
                        userProfile?.let { profile ->
                            val updatedUser = profile.copy(
                                firstName = firstName,
                                lastName = lastName,
                                username = username,
                                dateOfBirth = dateOfBirth,
                                imageUrl = imageUrl
                            )
                            viewModel.updateUser(updatedUser)
                        }
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userState !is UserViewModel.UserOperationResult.Loading
                ) {
                    Text("Save")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { isEditing = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    userProfile?.id?.let { id -> viewModel.deleteUser(id) }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete Account")
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (userState) {
                is UserViewModel.UserOperationResult.Loading -> {
                    CircularProgressIndicator()
                }
                is UserViewModel.UserOperationResult.Success -> {
                    Text((userState as UserViewModel.UserOperationResult.Success).message, color = Color.Green)
                }
                is UserViewModel.UserOperationResult.Error -> {
                    Text((userState as UserViewModel.UserOperationResult.Error).message, color = Color.Red)
                }
                else -> {}
            }
        }
    }
}
