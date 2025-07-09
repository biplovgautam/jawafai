package com.example.jawafai.view

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.example.jawafai.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.livedata.observeAsState
import android.Manifest
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val repository = remember { UserRepositoryImpl(FirebaseAuth.getInstance(), com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    val auth = remember { FirebaseAuth.getInstance() }
    val viewModel: UserViewModel = viewModel(factory = UserViewModelFactory(repository, auth))
    val userProfile by viewModel.userProfile.observeAsState()
    val userState by viewModel.userState.observeAsState(UserViewModel.UserOperationResult.Initial)

    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    // Form state
    var firstName by remember { mutableStateOf(TextFieldValue("")) }
    var lastName by remember { mutableStateOf(TextFieldValue("")) }
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var dateOfBirth by remember { mutableStateOf(TextFieldValue("")) }
    var bio by remember { mutableStateOf(TextFieldValue("")) }
    var imageUrl by remember { mutableStateOf("") }

    // Validation state
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf("") }
    var dateOfBirthError by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Theme colors
    val darkTeal = Color(0xFF365A61)
    val lightTeal = Color(0xFF4A7A85)
    val accentGreen = Color(0xFF4CAF50)
    val errorRed = Color(0xFFE53E3E)
    val warningOrange = Color(0xFFED8936)

    // Helper function for date validation
    fun isValidDate(dateString: String): Boolean {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.isLenient = false
            format.parse(dateString)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Image picker launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            viewModel.uploadProfileImage(it) { url ->
                if (url != null) {
                    imageUrl = url
                    userProfile?.let { profile ->
                        viewModel.updateUser(profile.copy(imageUrl = url))
                    }
                }
            }
        }
        showImagePickerDialog = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        }
    }

    // Validation functions
    fun validateForm(): Boolean {
        var isValid = true

        firstNameError = when {
            firstName.text.isBlank() -> { isValid = false; "First name is required" }
            firstName.text.length < 2 -> { isValid = false; "First name must be at least 2 characters" }
            else -> ""
        }

        lastNameError = when {
            lastName.text.isBlank() -> { isValid = false; "Last name is required" }
            lastName.text.length < 2 -> { isValid = false; "Last name must be at least 2 characters" }
            else -> ""
        }

        usernameError = when {
            username.text.isBlank() -> { isValid = false; "Username is required" }
            username.text.length < 3 -> { isValid = false; "Username must be at least 3 characters" }
            !username.text.matches(Regex("^[a-zA-Z0-9_]+$")) -> { isValid = false; "Username can only contain letters, numbers, and underscores" }
            else -> ""
        }

        dateOfBirthError = when {
            dateOfBirth.text.isNotBlank() && !isValidDate(dateOfBirth.text) -> { isValid = false; "Invalid date format (DD/MM/YYYY)" }
            else -> ""
        }

        return isValid
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
            email = TextFieldValue(profile.email)
            dateOfBirth = TextFieldValue(profile.dateOfBirth)
            bio = TextFieldValue("") // Add bio field to UserModel if needed
            imageUrl = profile.imageUrl ?: ""
        }
    }

    // Handle state changes
    LaunchedEffect(userState) {
        when (val currentState = userState) {
            is UserViewModel.UserOperationResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = currentState.message.ifEmpty { "Profile updated successfully" },
                    actionLabel = "OK"
                )
                if (isEditing) {
                    isEditing = false
                    focusManager.clearFocus()
                }
            }
            is UserViewModel.UserOperationResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = currentState.message,
                    actionLabel = "Retry"
                )
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = darkTeal,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isEditing) {
                                if (validateForm()) {
                                    userProfile?.let { profile ->
                                        val updatedUser = profile.copy(
                                            firstName = firstName.text.trim(),
                                            lastName = lastName.text.trim(),
                                            username = username.text.trim(),
                                            dateOfBirth = dateOfBirth.text.trim(),
                                            imageUrl = imageUrl
                                        )
                                        viewModel.updateUser(updatedUser)
                                    }
                                }
                            } else {
                                isEditing = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkTeal
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = when (userState) {
                            is UserViewModel.UserOperationResult.Success -> accentGreen
                            is UserViewModel.UserOperationResult.Error -> errorRed
                            else -> MaterialTheme.colorScheme.inverseSurface
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(darkTeal, lightTeal),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image Section
                ProfileImageSection(
                    imageUrl = imageUrl.ifEmpty { userProfile?.imageUrl },
                    userName = "${userProfile?.firstName} ${userProfile?.lastName}".trim(),
                    onImageClick = { showImagePickerDialog = true },
                    isEditing = isEditing
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Form
                ProfileForm(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    email = email,
                    dateOfBirth = dateOfBirth,
                    bio = bio,
                    isEditing = isEditing,
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    usernameError = usernameError,
                    dateOfBirthError = dateOfBirthError,
                    onFirstNameChange = { firstName = it; firstNameError = "" },
                    onLastNameChange = { lastName = it; lastNameError = "" },
                    onUsernameChange = { username = it; usernameError = "" },
                    onDateOfBirthChange = { dateOfBirth = it; dateOfBirthError = "" },
                    onBioChange = { bio = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                ActionButtons(
                    isEditing = isEditing,
                    isLoading = userState is UserViewModel.UserOperationResult.Loading,
                    onEditToggle = {
                        if (isEditing) {
                            isEditing = false
                            focusManager.clearFocus()
                        } else {
                            isEditing = true
                        }
                    },
                    onSave = {
                        if (validateForm()) {
                            userProfile?.let { profile ->
                                val updatedUser = profile.copy(
                                    firstName = firstName.text.trim(),
                                    lastName = lastName.text.trim(),
                                    username = username.text.trim(),
                                    dateOfBirth = dateOfBirth.text.trim(),
                                    imageUrl = imageUrl
                                )
                                viewModel.updateUser(updatedUser)
                            }
                        }
                    },
                    onLogout = {
                        viewModel.logout()
                        onLogout()
                    },
                    onDeleteAccount = { showDeleteDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Loading indicator
            if (userState is UserViewModel.UserOperationResult.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = darkTeal)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Updating profile...")
                        }
                    }
                }
            }
        }
    }

    // Image Picker Dialog
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onGalleryClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                userProfile?.id?.let { id ->
                    viewModel.deleteUser(id)
                }
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun ProfileImageSection(
    imageUrl: String?,
    userName: String,
    onImageClick: () -> Unit,
    isEditing: Boolean
) {
    val darkTeal = Color(0xFF365A61)

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Profile image with animation
        val animatedSize by animateDpAsState(
            targetValue = if (isEditing) 110.dp else 100.dp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )

        AsyncImage(
            model = imageUrl?.takeIf { it.isNotBlank() }
                ?: "https://ui-avatars.com/api/?name=${userName}&background=4A7A85&color=fff&size=200",
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(animatedSize)
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .clickable(enabled = isEditing) { onImageClick() },
            contentScale = ContentScale.Crop
        )

        // Edit icon overlay
        AnimatedVisibility(
            visible = isEditing,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(darkTeal)
                    .clickable { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileForm(
    firstName: TextFieldValue,
    lastName: TextFieldValue,
    username: TextFieldValue,
    email: TextFieldValue,
    dateOfBirth: TextFieldValue,
    bio: TextFieldValue,
    isEditing: Boolean,
    firstNameError: String,
    lastNameError: String,
    usernameError: String,
    dateOfBirthError: String,
    onFirstNameChange: (TextFieldValue) -> Unit,
    onLastNameChange: (TextFieldValue) -> Unit,
    onUsernameChange: (TextFieldValue) -> Unit,
    onDateOfBirthChange: (TextFieldValue) -> Unit,
    onBioChange: (TextFieldValue) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val dateOfBirthFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // First Name
        ProfileTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = "First Name",
            isError = firstNameError.isNotEmpty(),
            errorMessage = firstNameError,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { lastNameFocusRequester.requestFocus() }
            ),
            modifier = Modifier.focusRequester(firstNameFocusRequester)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name
        ProfileTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = "Last Name",
            isError = lastNameError.isNotEmpty(),
            errorMessage = lastNameError,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { usernameFocusRequester.requestFocus() }
            ),
            modifier = Modifier.focusRequester(lastNameFocusRequester)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Username
        ProfileTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = "Username",
            isError = usernameError.isNotEmpty(),
            errorMessage = usernameError,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { dateOfBirthFocusRequester.requestFocus() }
            ),
            modifier = Modifier.focusRequester(usernameFocusRequester)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email (Read-only)
        ProfileTextField(
            value = email,
            onValueChange = { },
            label = "Email",
            enabled = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date of Birth
        ProfileTextField(
            value = dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = "Date of Birth (DD/MM/YYYY)",
            isError = dateOfBirthError.isNotEmpty(),
            errorMessage = dateOfBirthError,
            enabled = isEditing,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.focusRequester(dateOfBirthFocusRequester)
        )
    }
}

@Composable
fun ProfileTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                disabledTextColor = Color.White.copy(alpha = 0.6f),
                disabledBorderColor = Color.White.copy(alpha = 0.5f),
                disabledLabelColor = Color.White.copy(alpha = 0.5f),
                errorBorderColor = Color(0xFFE53E3E),
                errorLabelColor = Color(0xFFE53E3E)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(0xFFE53E3E),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun ActionButtons(
    isEditing: Boolean,
    isLoading: Boolean,
    onEditToggle: () -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = isEditing,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column {
                // Save Button
                Button(
                    onClick = onSave,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onEditToggle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }

        AnimatedVisibility(
            visible = !isEditing,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column {
                // Edit Button
                Button(
                    onClick = onEditToggle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF365A61),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", color = Color(0xFF365A61))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logout Button
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Delete Account Button
                OutlinedButton(
                    onClick = onDeleteAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53E3E)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE53E3E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }
}

@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Change Profile Picture",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Gallery Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGalleryClick() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF365A61)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Choose from Gallery")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF365A61)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF365A61))
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to delete your account? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF365A61)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF365A61))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53E3E)
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
