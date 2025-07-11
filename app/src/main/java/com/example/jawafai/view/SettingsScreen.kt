package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.PersonaRepository
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.livedata.observeAsState
import kotlin.math.abs

data class SettingsItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    val tint: Color = Color.Unspecified
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit,
    onPersonaClicked: () -> Unit,  // New parameter for persona navigation
    viewModel: UserViewModel = viewModel(
        factory = UserViewModelFactory(
            UserRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()),
            FirebaseAuth.getInstance()
        )
    )
) {
    val userProfile by viewModel.userProfile.observeAsState()

    // Track if persona is completed
    val personaCompleted = remember { mutableStateOf(false) }

    // Fetch user profile when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()

        // Check if persona is completed
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val personaRepo = PersonaRepository.getInstance()
            val personaData = personaRepo.getPersonaMap(currentUserId)
            // Consider persona completed if there are at least 5 answers
            personaCompleted.value = personaData.size >= 5
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                // This makes the top bar transparent to blend with the content
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        // Apply system bar padding to the Scaffold content
        modifier = Modifier
    ) { paddingValues ->
        SettingsContent(
            modifier = Modifier.padding(paddingValues),
            onLogout = onLogout,
            onProfileClicked = onProfileClicked,
            onPersonaClicked = onPersonaClicked, // Pass the new click handler
            userModel = userProfile,
            personaCompleted = personaCompleted.value // Pass the completion status
        )
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit,
    onPersonaClicked: () -> Unit, // New parameter for persona navigation
    userModel: UserModel?,
    personaCompleted: Boolean // New parameter for persona completion status
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 1.dp)
    ) {
        // Profile Section
        item {
            UserProfileSection(
                userModel = userModel,
                onClick = onProfileClicked // Navigate to profile screen
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
        }

        // Account Settings
        item {
            SettingsSectionTitle(title = "Account")
        }

        item {
            SettingsItem(
                item = SettingsItemData(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy",
                    subtitle = "Manage your privacy settings",
                    onClick = { /* Navigate to privacy settings */ }
                )
            )
        }

        item {
            SettingsItem(
                item = SettingsItemData(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Configure notification preferences",
                    onClick = { /* Navigate to notification settings */ }
                )
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) }

        // App Settings
        item {
            SettingsSectionTitle(title = "App Settings")
        }

        item {
            SettingsItem(
                item = SettingsItemData(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = "App information and version",
                    onClick = { /* Show about info */ }
                )
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) }

        // Persona Section
        item {
            SettingsSectionTitle(title = "Persona")
        }

        item {
            SettingsItem(
                item = SettingsItemData(
                    icon = Icons.Outlined.Person,
                    title = "Your Persona",
                    subtitle = if (personaCompleted) "Persona completed" else "Complete your persona",
                    onClick = onPersonaClicked,
                    tint = if (personaCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) }

        // Logout
        item {
            SettingsItem(
                item = SettingsItemData(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Logout",
                    onClick = onLogout,
                    tint = MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
fun UserProfileSection(
    userModel: UserModel?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Profile Image Avatar ---
        val imageSize = 64.dp
        val placeholderText = (userModel?.firstName?.firstOrNull()?.toString() ?: userModel?.username?.firstOrNull()?.toString() ?: "A").uppercase()
        val seedForColor = userModel?.username ?: userModel?.id ?: "default"

        val avatarColors = remember {
            listOf(
                Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
                Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
                Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548)
            )
        }
        val backgroundColor = remember(seedForColor) {
            avatarColors[abs(seedForColor.hashCode()) % avatarColors.size]
        }

        Box(
            modifier = Modifier
                .size(imageSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (userModel?.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                AsyncImage(
                    model = userModel.imageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // --- End Profile Image Avatar ---

        Spacer(modifier = Modifier.width(16.dp))

        // --- User Info ---
        Column(modifier = Modifier.weight(1f)) {
            val fullName = userModel?.let { "${it.firstName} ${it.lastName}".trim() }?.ifEmpty { userModel.username } ?: "Anonymous"
            val username = userModel?.username ?: ""
            val email = userModel?.email ?: ""

            Text(
                text = fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (email.isNotEmpty()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (username.isNotEmpty()) {
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        // --- End User Info ---

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Edit Profile",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.tint != Color.Unspecified) item.tint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.tint != Color.Unspecified) item.tint else MaterialTheme.colorScheme.onSurface
            )

            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Checkmark icon for completed persona
        if (item.title == "Your Persona" && item.subtitle == "Complete your persona") {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Persona completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
