package com.example.jawafai.view.dashboard.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.tasks.await
import android.provider.Settings
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

data class SettingsItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    val tint: Color = Color.Unspecified
)

data class PermissionStatus(val name: String, val granted: Boolean, val request: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit,
    onPersonaClicked: () -> Unit,
    onRequestPermission: (String) -> Unit,
    permissionsStatus: Map<String, Boolean>,
    viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(
            UserRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()),
            FirebaseAuth.getInstance()
        )
    )
) {
    val userProfile by viewModel.userProfile.observeAsState()
    val personaCompleted = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- Permission launcher for notifications ---
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onRequestPermission("POST_NOTIFICATIONS")
    }

    // --- Permission launcher for storage/media ---
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onRequestPermission("READ_MEDIA_IMAGES")
        } else {
            onRequestPermission("READ_EXTERNAL_STORAGE")
        }
    }

    // Fetch user profile when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()

        // Check if persona is completed
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                val personaData = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("persona")
                    .get()
                    .await()

                personaCompleted.value = !personaData.isEmpty && personaData.size() >= 5
            }
        } catch (e: Exception) {
            personaCompleted.value = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        SettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .background(Color.White),
            onLogout = onLogout,
            onProfileClicked = onProfileClicked,
            onPersonaClicked = onPersonaClicked,
            userModel = userProfile,
            personaCompleted = personaCompleted.value,
            permissionsStatus = permissionsStatus,
            onRequestPermission = { key ->
                if (key == "POST_NOTIFICATIONS" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context is android.app.Activity) {
                        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (!shouldShowRationale && permissionsStatus["POST_NOTIFICATIONS"] == false) {
                            // User denied with Don't Ask Again, open app settings
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } else {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                } else if (key == "READ_MEDIA_IMAGES") {
                    storageLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                } else if (key == "READ_EXTERNAL_STORAGE") {
                    storageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    onRequestPermission(key)
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit,
    onPersonaClicked: () -> Unit,
    userModel: UserModel?,
    personaCompleted: Boolean,
    permissionsStatus: Map<String, Boolean>,
    onRequestPermission: (String) -> Unit
) {
    val userEmail = userModel?.email ?: "User"
    val userName = userModel?.let {
        "${it.firstName} ${it.lastName}".trim().ifEmpty { it.username }
    } ?: userEmail.substringBefore("@")
    val profileImage = userModel?.imageUrl

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Profile Section
        item {
            UserProfileCard(
                name = userName,
                email = userEmail,
                profileImageUrl = profileImage ?: "",
                onClick = onProfileClicked
            )
        }

        // Persona Section
        item {
            Text(
                text = "Personalization",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF395B64)
                )
            )
        }

        item {
            PersonaCard(
                onClick = onPersonaClicked,
                completed = personaCompleted
            )
        }
        // Permissions Section
        item {
            val permissions = listOf(
                PermissionStatus(
                    name = "Notifications",
                    granted = permissionsStatus["POST_NOTIFICATIONS"] == true,
                    request = { onRequestPermission("POST_NOTIFICATIONS") }
                ),
                PermissionStatus(
                    name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) "Media Images" else "External Storage",
                    granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                        permissionsStatus["READ_MEDIA_IMAGES"] == true
                    else
                        permissionsStatus["READ_EXTERNAL_STORAGE"] == true,
                    request = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                            onRequestPermission("READ_MEDIA_IMAGES")
                        else
                            onRequestPermission("READ_EXTERNAL_STORAGE")
                    }
                ),
                PermissionStatus(
                    name = "Internet",
                    granted = permissionsStatus["INTERNET"] == true,
                    request = { onRequestPermission("INTERNET") }
                )
            )
            PermissionsSection(permissions = permissions)
        }

        // FCM Token Section
        item {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            var fcmToken by remember { mutableStateOf("") }
            var showCopiedToast by remember { mutableStateOf(false) }

            // Fetch FCM token once
            LaunchedEffect(Unit) {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    fcmToken = token
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "FCM Token",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF395B64)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (fcmToken.isNotBlank()) {
                    Text(
                        text = fcmToken,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            color = Color(0xFF666666)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(8.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    clipboardManager.setText(AnnotatedString(fcmToken))
                                    showCopiedToast = true
                                }
                            )
                    )
                    if (showCopiedToast) {
                        LaunchedEffect(Unit) {
                            Toast.makeText(context, "FCM token copied!", Toast.LENGTH_SHORT).show()
                            showCopiedToast = false
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Long press to copy",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF999999),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    Text(
                        text = "Fetching token...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF999999),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // App Settings Section
        item {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF395B64)
                )
            )
        }

        item {
            SettingsCard {
                SettingsItem(
                    item = SettingsItemData(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy",
                        subtitle = "Manage your privacy settings",
                        onClick = { /* Navigate to privacy settings */ }
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFE0E0E0)
                )

                SettingsItem(
                    item = SettingsItemData(
                        icon = Icons.Outlined.Notifications,
                        title = "Notifications",
                        subtitle = "Configure notification preferences",
                        onClick = { /* Navigate to notification settings */ }
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color(0xFFE0E0E0)
                )

                SettingsItem(
                    item = SettingsItemData(
                        icon = Icons.Outlined.Info,
                        title = "About",
                        subtitle = "App information and version",
                        onClick = { /* Show about info */ }
                    )
                )
            }
        }

        // Account Actions
        item {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF395B64)
                )
            )
        }

        item {
            LogoutCard(onLogout = onLogout)
        }

        // Add bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UserProfileCard(
    name: String,
    email: String,
    profileImageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5C9CA))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = profileImageUrl.ifEmpty { "https://ui-avatars.com/api/?name=$name&background=A5C9CA&color=ffffff" }
                    ),
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF395B64)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Edit Profile",
                tint = Color(0xFF395B64),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PersonaCard(
    onClick: () -> Unit,
    completed: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) Color(0xFFF0F8FF) else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (completed) Color(0xFF395B64) else Color(0xFFA5C9CA)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (completed) Icons.Filled.Check else Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Your Persona",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF395B64)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (completed) "Persona completed" else "Complete your persona for better responses",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = Color(0xFF395B64),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PermissionsSection(
    permissions: List<PermissionStatus>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Permissions",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = AppFonts.KarlaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF395B64)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        permissions.forEach { perm ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (perm.granted) Icons.Filled.Check else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (perm.granted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = perm.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color(0xFF395B64)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (!perm.granted) {
                        Button(
                            onClick = perm.request,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C9CA))
                        ) {
                            Text("Grant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun LogoutCard(onLogout: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLogout() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Logout",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
fun SettingsItem(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.tint != Color.Unspecified) item.tint else Color(0xFF395B64),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = if (item.tint != Color.Unspecified) item.tint else Color(0xFF395B64)
                )
            )

            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
    }
}
