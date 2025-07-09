package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val onClick: () -> Unit,
    val tint: Color = Color.Black
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        SettingsContent(
            modifier = Modifier.padding(paddingValues),
            onLogout = onLogout
        )
    }
}

@Composable
fun SettingsContent(modifier: Modifier = Modifier, onLogout: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "User"
    val userName = currentUser?.displayName ?: userEmail.substringBefore("@")
    val profileImage = currentUser?.photoUrl ?: "https://ui-avatars.com/api/?name=${userName.replace(" ", "+")}"

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile Section
        item {
            UserProfileSection(
                name = userName,
                email = userEmail,
                profileImageUrl = profileImage.toString()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Account Settings
        item {
            SettingsSectionTitle(title = "Account")
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.AccountCircle,
                    title = "Profile",
                    subtitle = "Edit your profile information",
                    onClick = { /* Navigate to profile edit */ }
                )
            )
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy",
                    subtitle = "Manage your privacy settings",
                    onClick = { /* Navigate to privacy settings */ }
                )
            )
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Configure notification preferences",
                    onClick = { /* Navigate to notification settings */ }
                )
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // App Settings
        item {
            SettingsSectionTitle(title = "App Settings")
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.DarkMode, // Changed from Contrast to DarkMode
                    title = "Appearance",
                    subtitle = "Dark mode and theme settings",
                    onClick = { /* Navigate to appearance settings */ }
                )
            )
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.Translate, // Changed from Language to Translate
                    title = "Language",
                    subtitle = "Change app language",
                    onClick = { /* Navigate to language settings */ }
                )
            )
        }

        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "About",
                    subtitle = "App information and version",
                    onClick = { /* Show about info */ }
                )
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // Logout
        item {
            SettingsItem(
                item = SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp, // Replace Logout with ExitToApp
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = onLogout,
                    tint = Color.Red
                )
            )
        }

        // Extra space at the bottom
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun UserProfileSection(
    name: String,
    email: String,
    profileImageUrl: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile image
        Image(
            painter = rememberAsyncImagePainter(model = profileImageUrl),
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // User info
        Column {
            Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = item.tint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                color = item.tint
            )

            if (item.subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Arrow icon
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, // Replace ChevronRight with KeyboardArrowRight
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
