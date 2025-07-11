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
import coil.compose.rememberAsyncImagePainter
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.tasks.await
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
    onPersonaClicked: () -> Unit,
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
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                val personaData = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("persona")
                    .get()
                    .await()

                // Consider persona completed if there are at least 5 answers
                personaCompleted.value = !personaData.isEmpty && personaData.size() >= 5
            }
        } catch (e: Exception) {
            // If we can't fetch persona data, assume it's not completed
            personaCompleted.value = false
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
            onPersonaClicked = onPersonaClicked,
            userModel = userProfile,
            personaCompleted = personaCompleted.value // Pass the direct value
        )
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit,
    onPersonaClicked: () -> Unit,
    userModel: UserModel?,
    personaCompleted: Boolean // Receive the direct value
) {
    val userEmail = userModel?.email ?: "User"
    val userName = userModel?.let {
        "${it.firstName} ${it.lastName}".trim().ifEmpty { it.username }
    } ?: userEmail.substringBefore("@")
    val profileImage = userModel?.imageUrl

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile Section
        item {
            UserProfileSection(
                name = userName,
                email = userEmail,
                profileImageUrl = profileImage ?: "",
                onClick = onProfileClicked // Navigate to profile screen
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
        }

        // Persona Section - Moved to be right after Profile section
        item {
            SettingsSectionTitle(title = "Persona")
        }

        item {
            PersonaSettingsItem(
                title = "Your Persona",
                subtitle = if (personaCompleted) "Persona completed" else "Complete your persona",
                onClick = onPersonaClicked,
                completed = personaCompleted
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) }

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
    name: String,
    email: String,
    profileImageUrl: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image
        Image(
            painter = rememberAsyncImagePainter(
                model = profileImageUrl.ifEmpty { "https://ui-avatars.com/api/?name=$name" }
            ),
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // User info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
    }
}

@Composable
fun PersonaSettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    completed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon - Person icon in normal state, check icon when completed
        Icon(
            imageVector = if (completed) Icons.Filled.Check else Icons.Outlined.Person,
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Arrow icon for navigation
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
