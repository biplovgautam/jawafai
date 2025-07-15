package com.example.jawafai.view.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jawafai.ui.theme.JawafaiTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat
import com.example.jawafai.view.auth.LoginActivity
import com.example.jawafai.view.dashboard.chat.ChatBotScreen
import com.example.jawafai.view.dashboard.chat.ChatDetailScreen
import com.example.jawafai.view.dashboard.chat.ChatScreen
import com.example.jawafai.view.dashboard.home.HomeScreen
import com.example.jawafai.view.dashboard.notifications.NotificationScreen
import com.example.jawafai.view.dashboard.settings.PersonaScreen
import com.example.jawafai.view.dashboard.settings.ProfileScreen
import com.example.jawafai.view.dashboard.settings.SettingsScreen
import com.example.jawafai.utils.WithNetworkMonitoring
import kotlinx.coroutines.launch
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog

class DashboardActivity : ComponentActivity() {
    // Permission request codes
    private val PERMISSION_REQUEST_CODE = 2002
    private var permissionToRequest: String? = null

    // Helper to map permission keys to Android permission strings
    private fun getAndroidPermission(key: String): String? = when (key) {
        "POST_NOTIFICATIONS" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        "READ_MEDIA_IMAGES" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else null
        "READ_EXTERNAL_STORAGE" -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_EXTERNAL_STORAGE else null
        "INTERNET" -> Manifest.permission.INTERNET
        else -> null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request notification permission if not granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }
        // Enable full screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            JawafaiTheme {
                // Wrap dashboard with network monitoring
                WithNetworkMonitoring {
                    // --- Permission state management ---
                    var permissionsStatus by remember { mutableStateOf(checkAllPermissions()) }
                    val activity = this@DashboardActivity

                    fun requestPermission(key: String) {
                        val perm = getAndroidPermission(key)
                        if (perm != null && ActivityCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                            permissionToRequest = key
                            ActivityCompat.requestPermissions(activity, arrayOf(perm), PERMISSION_REQUEST_CODE)
                        }
                    }

                    // Update permissionsStatus when activity resumes
                    DisposableEffect(Unit) {
                        onResumePermissionsCheck = {
                            permissionsStatus = checkAllPermissions()
                        }
                        onDispose { onResumePermissionsCheck = null }
                    }

                    DashboardScreen(
                        onLogout = {
                            // Sign out from Firebase
                            FirebaseAuth.getInstance().signOut()

                            // Navigate directly to login screen
                            val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Close dashboard activity
                        },
                        permissionsStatus = permissionsStatus,
                        onRequestPermission = ::requestPermission
                    )
                }
            }
        }
    }

    // Helper to check all permissions
    private fun checkAllPermissions(): Map<String, Boolean> {
        val map = mutableMapOf<String, Boolean>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            map["POST_NOTIFICATIONS"] = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            map["READ_MEDIA_IMAGES"] = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            map["READ_EXTERNAL_STORAGE"] = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        map["INTERNET"] = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        return map
    }

    // Used to trigger permission re-check on resume
    private var onResumePermissionsCheck: (() -> Unit)? = null
    override fun onResume() {
        super.onResume()
        onResumePermissionsCheck?.invoke()
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            onResumePermissionsCheck?.invoke()
        } else if (requestCode == 2001) {
            val denied = permissions.zip(grantResults.toTypedArray()).filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Notification Permission Required")
                    .setMessage("Notification permission is required to receive chat notifications. Please enable it in settings.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
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


    object Chat : BottomNavItem(
        route = "chat",
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

    // Profile is no longer a bottom nav item, but a destination from Settings
    object Profile : BottomNavItem(
        route = "profile",
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle,
        contentDescription = "Profile"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    permissionsStatus: Map<String, Boolean>,
    onRequestPermission: (String) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Chat,
        BottomNavItem.Notifications,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Determine if we should show bottom bar and handle back press differently
    val showBottomBar = items.any { it.route == currentRoute }
    val isInMainScreen = items.any { it.route == currentRoute }

    // Global back press handling
    BackHandler(
        enabled = !isInMainScreen,
        onBack = {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }
        }
    )

    // Handle back press for main screens (exit app)
    BackHandler(
        enabled = isInMainScreen,
        onBack = {
            // For main screens, move app to background instead of closing
            (navController.context as? ComponentActivity)?.moveTaskToBack(true)
        }
    )

    // Helper function to get current tab index
    fun getCurrentTabIndex(): Int {
        return items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    }

    // Helper function to navigate to tab by index with smooth animation
    fun navigateToTab(index: Int) {
        if (index in items.indices) {
            val targetRoute = items[index].route
            if (currentRoute != targetRoute) {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFFE7F6F2),
                    contentColor = Color(0xFF395B64),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Remove the bottom padding to attach directly to bottom
//                        .navigationBarsPadding()
                ) {
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
                                        modifier = Modifier.size(28.dp)
                                    )
                                    AnimatedVisibility(visible = selected) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .width(16.dp)
                                                .height(2.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF395B64))
                                        )
                                    }
                                }
                            },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navigateToTab(items.indexOf(item))
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF395B64),
                                unselectedIconColor = Color(0xFF666666),
                                indicatorColor = Color.Transparent
                            ),
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val unused = paddingValues // Use paddingValues to avoid unused variable warning
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home Screen (Index 0 - Leftmost)
                composable(
                    BottomNavItem.Home.route,
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetIndex = 0
                        val initialIndex = items.indexOfFirst { it.route == initialRoute }

                        when {
                            initialIndex > targetIndex -> {
                                // Coming from right (Chat/Notifications/Settings to Home)
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            else -> {
                                // Initial load or other transitions
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                        }
                    },
                    exitTransition = {
                        val targetRoute = targetState.destination.route
                        val currentIndex = 0
                        val targetIndex = items.indexOfFirst { it.route == targetRoute }

                        when {
                            targetIndex > currentIndex -> {
                                // Going to right (Home to Chat/Notifications/Settings)
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            else -> {
                                // Other transitions
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                        }
                    }
                ) {
                    HomeScreen(
                        onProfileClick = { navController.navigate(BottomNavItem.Profile.route) },
                        onChatBotClick = { navController.navigate("chatbot") },
                        onCompletePersonaClick = { navController.navigate("settings/persona") },
                        onRecentChatClick = { chatId, otherUserId ->
                            navController.navigate("chat_detail/$chatId/$otherUserId")
                        },
                        onNotificationClick = { navController.navigate(BottomNavItem.Notifications.route) },
                        onSeeAllChatsClick = {
                            navigateToTab(1) // Chat is at index 1
                        }
                    )
                }

                // Chat Screen (Index 1)
                composable(
                    BottomNavItem.Chat.route,
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetIndex = 1
                        val initialIndex = items.indexOfFirst { it.route == initialRoute }

                        when {
                            initialIndex < targetIndex -> {
                                // Coming from left (Home to Chat)
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            initialIndex > targetIndex -> {
                                // Coming from right (Notifications/Settings to Chat)
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            else -> {
                                fadeIn(animationSpec = tween(500))
                            }
                        }
                    },
                    exitTransition = {
                        val targetRoute = targetState.destination.route
                        val currentIndex = 1
                        val targetIndex = items.indexOfFirst { it.route == targetRoute }

                        when {
                            targetIndex < currentIndex -> {
                                // Going to left (Chat to Home)
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            targetIndex > currentIndex -> {
                                // Going to right (Chat to Notifications/Settings)
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            else -> {
                                fadeOut(animationSpec = tween(500))
                            }
                        }
                    }
                ) {
                    ChatScreen(
                        onNavigateToChat = { chatId, otherUserId ->
                            navController.navigate("chat_detail/$chatId/$otherUserId")
                        },
                        onNavigateToChatBot = { navController.navigate("chatbot") }
                    )
                }

                // Notifications Screen (Index 2)
                composable(
                    BottomNavItem.Notifications.route,
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetIndex = 2
                        val initialIndex = items.indexOfFirst { it.route == initialRoute }

                        when {
                            initialIndex < targetIndex -> {
                                // Coming from left (Home/Chat to Notifications)
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            initialIndex > targetIndex -> {
                                // Coming from right (Settings to Notifications)
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            else -> {
                                fadeIn(animationSpec = tween(500))
                            }
                        }
                    },
                    exitTransition = {
                        val targetRoute = targetState.destination.route
                        val currentIndex = 2
                        val targetIndex = items.indexOfFirst { it.route == targetRoute }

                        when {
                            targetIndex < currentIndex -> {
                                // Going to left (Notifications to Home/Chat)
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            targetIndex > currentIndex -> {
                                // Going to right (Notifications to Settings)
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            else -> {
                                fadeOut(animationSpec = tween(500))
                            }
                        }
                    }
                ) {
                    NotificationScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Settings Screen (Index 3 - Rightmost)
                composable(
                    BottomNavItem.Settings.route,
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetIndex = 3
                        val initialIndex = items.indexOfFirst { it.route == initialRoute }

                        when {
                            initialIndex < targetIndex -> {
                                // Coming from left (Home/Chat/Notifications to Settings)
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                            else -> {
                                // Other transitions
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(500))
                            }
                        }
                    },
                    exitTransition = {
                        val targetRoute = targetState.destination.route
                        val currentIndex = 3
                        val targetIndex = items.indexOfFirst { it.route == targetRoute }

                        when {
                            targetIndex < currentIndex -> {
                                // Going to left (Settings to Home/Chat/Notifications)
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                            else -> {
                                // Other transitions
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(500))
                            }
                        }
                    }
                ) {
                    SettingsScreen(
                        onLogout = onLogout,
                        onProfileClicked = {
                            navController.navigate(BottomNavItem.Profile.route)
                        },
                        onPersonaClicked = {
                            navController.navigate("settings/persona")
                        },
                        permissionsStatus = permissionsStatus,
                        onRequestPermission = onRequestPermission
                    )
                }

                // Non-main screens with standard transitions
                composable(
                    route = "chat_detail/{chatId}/{otherUserId}",
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("otherUserId") { type = NavType.StringType }
                    ),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(400))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(400))
                    },
                    ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""

                    ChatDetailScreen(
                        chatId = chatId,
                        otherUserId = otherUserId,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Other screens with similar smooth transitions
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLogout = onLogout
                    )
                }

                composable("settings/persona") {
                    PersonaScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("chatbot") {
                    ChatBotScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// Custom BackHandler composable for better UX
@Composable
fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }

    SideEffect {
        backCallback.isEnabled = enabled
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(backDispatcher, backCallback) {
        backDispatcher?.addCallback(backCallback)
        onDispose {
            backCallback.remove()
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(onLogout = {}, permissionsStatus = emptyMap(), onRequestPermission = {})
}
