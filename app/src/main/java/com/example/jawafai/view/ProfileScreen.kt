package com.example.jawafai.view

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { UserRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
    val auth = remember { FirebaseAuth.getInstance() }
    val viewModel: UserViewModel = viewModel(factory = UserViewModelFactory(repository, auth))

    val userProfile by viewModel.userProfile.observeAsState()
    val userState by viewModel.userState.observeAsState(UserViewModel.UserOperationResult.Initial)

    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Form state
    var firstName by remember { mutableStateOf(TextFieldValue("")) }
    var lastName by remember { mutableStateOf(TextFieldValue("")) }
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var bio by remember { mutableStateOf(TextFieldValue("")) }
    var imageUrl by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Image picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfileImage(it) { url ->
                if (url != null) {
                    imageUrl = url
                }
            }
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        }
    }

    // Fetch user profile on first composition
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }

    // Update local state when userProfile changes
    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            firstName = TextFieldValue(profile.firstName)
            lastName = TextFieldValue(profile.lastName)
            username = TextFieldValue(profile.username)
            bio = TextFieldValue(profile.bio)
            imageUrl = profile.imageUrl ?: ""
        }
    }

    // Handle state changes from ViewModel
    LaunchedEffect(userState) {
        when (val result = userState) {
            is UserViewModel.UserOperationResult.Success -> {
                if (result.message.isNotEmpty()) {
                    snackbarHostState.showSnackbar(result.message)
                }
                if (isEditing) {
                    isEditing = false
                    focusManager.clearFocus()
                }
            }
            is UserViewModel.UserOperationResult.Error -> snackbarHostState.showSnackbar(result.message)
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (isEditing) {
                                userProfile?.let {
                                    val updatedUser = it.copy(
                                        firstName = firstName.text,
                                        lastName = lastName.text,
                                        username = username.text,
                                        bio = bio.text,
                                        imageUrl = imageUrl
                                    )
                                    viewModel.updateUser(updatedUser)
                                }
                            }
                            isEditing = !isEditing
                        },
                        enabled = userState !is UserViewModel.UserOperationResult.Loading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(if (isEditing) "Save" else "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(MaterialTheme.colorScheme.primary)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Profile Image
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable(enabled = isEditing) {
                        permissionLauncher.launch(permission)
                    }
                ) {
                    AsyncImage(
                        model = imageUrl.ifEmpty { "https://ui-avatars.com/api/?name=${firstName.text}+${lastName.text}&background=random" },
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    if (isEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Image",
                            tint = Color.White,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // User Name and Email
                userProfile?.let {
                    Text(
                        text = "${it.firstName} ${it.lastName}".trim().ifEmpty { it.username },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = it.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Form Fields
                ProfileTextField(label = "First Name", value = firstName, onValueChange = { firstName = it }, enabled = isEditing)
                ProfileTextField(label = "Last Name", value = lastName, onValueChange = { lastName = it }, enabled = isEditing)
                ProfileTextField(label = "Username", value = username, onValueChange = { username = it }, enabled = isEditing)
                ProfileTextField(label = "Bio", value = bio, onValueChange = { bio = it }, enabled = isEditing, singleLine = false, minLines = 3)

                Spacer(Modifier.height(32.dp))

                // Account Actions
                AccountActions(
                    onLogout = {
                        viewModel.logout()
                        onLogout()
                    },
                    onDeleteAccount = { showDeleteDialog = true }
                )

                Spacer(Modifier.height(32.dp))
            }

            if (userState is UserViewModel.UserOperationResult.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            if (showDeleteDialog) {
                DeleteAccountDialog(
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteUser(userProfile?.id ?: "")
                        showDeleteDialog = false
                        onLogout()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    enabled: Boolean,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = if (enabled) Color.White else Color.Gray) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.LightGray,
            disabledBorderColor = Color.Transparent,
            disabledTextColor = Color.Black,
            disabledLabelColor = Color.Gray
        ),
        readOnly = !enabled,
        textStyle = LocalTextStyle.current.copy(color = if (enabled) Color.White else Color.DarkGray)
    )
}

@Composable
private fun AccountActions(onLogout: () -> Unit, onDeleteAccount: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Account Actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Logout")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onDeleteAccount,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Account")
        }
    }
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = { Text("Are you sure? This action is permanent and cannot be undone.") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
