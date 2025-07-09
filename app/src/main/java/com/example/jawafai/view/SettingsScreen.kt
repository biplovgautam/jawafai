package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth

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
    onProfileClicked: () -> Unit
) {
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
            onProfileClicked = onProfileClicked
        )
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onProfileClicked: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "User"
    val userName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: userEmail.substringBefore("@")
    val profileImage = currentUser?.photoUrl

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile Section
        item {
            UserProfileSection(
                name = userName,
                email = userEmail,
                profileImageUrl = profileImage.toString(),
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
                    icon = Icons.Outlined.DarkMode,
                    title = "Appearance",
                    subtitle = "Dark mode and theme settings",
                    onClick = { /* Navigate to appearance settings */ }
                )
            )
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
