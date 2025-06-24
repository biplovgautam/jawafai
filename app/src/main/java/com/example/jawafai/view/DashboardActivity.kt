package com.example.jawafai.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.ui.theme.JawafaiTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle back press to move app to background instead of navigating back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Move task to back which sends app to background (like pressing home button)
                moveTaskToBack(true)
            }
        })

        setContent {
            JawafaiTheme {
                DashboardScreen(
                    onLogout = {
                        // Sign out from Firebase
                        FirebaseAuth.getInstance().signOut()

                        // Navigate directly to login screen
                        val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // Close dashboard activity
                    }
                )
            }
        }
    }

    // Override the default back button behavior as a fallback for older Android versions
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // First call super (required to satisfy the Android lint warning)
        super.onBackPressed()

        // The OnBackPressedDispatcher should handle this on newer Android versions,
        // but in case it doesn't (on older versions), also move task to back
        moveTaskToBack(true)
    }
}

// Define navigation items for bottom navigation
sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String
) {
    object Home : BottomNavItem(
        route = "home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescription = "Home"
    )

    object Search : BottomNavItem(
        route = "search",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat,
        contentDescription = "Chat"
    )

    object Notifications : BottomNavItem(
        route = "notifications",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        contentDescription = "Notifications"
    )

    object Settings : BottomNavItem(
        route = "settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        contentDescription = "Settings"
    )

    object Profile : BottomNavItem(
        route = "profile",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        contentDescription = "Profile"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Profile,
        BottomNavItem.Search,
        BottomNavItem.Home,
        BottomNavItem.Notifications,
        BottomNavItem.Settings
    )

    // Dark teal color for the bottom navigation bar (#365A61)
    val bottomNavBackgroundColor = Color(0xFF365A61)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = bottomNavBackgroundColor,
                contentColor = Color.White
            ) {5
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationBarItem(
                        icon = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.contentDescription,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp) // Increased from default to 32.dp
                                )

                                // Small indicator dot below the icon when selected
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                } else {
                                    // Empty spacer with the same height to maintain layout consistency
                                    Spacer(modifier = Modifier.height(7.dp))
                                }
                            }
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to avoid
                                // building up a large stack of destinations
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        // Custom colors with transparent indicator (since we're using our own)
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor = Color.Transparent // Make the default indicator transparent
                        ),
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
            composable(BottomNavItem.Search.route) {
                ChatScreen()
            }
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Notifications.route) {
                NotificationsScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(onLogout)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(onLogout = {})
}
