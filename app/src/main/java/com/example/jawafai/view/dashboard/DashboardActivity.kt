package com.example.jawafai.view.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.jawafai.view.dashboard.chat.ChatBotConversationScreen
import com.example.jawafai.view.dashboard.chat.ChatBotScreen
import com.example.jawafai.view.dashboard.chat.ChatBotHistoryScreen
import com.example.jawafai.view.dashboard.chat.ChatDetailScreen
import com.example.jawafai.view.dashboard.chat.ChatScreen
import com.example.jawafai.view.dashboard.home.HomeScreen
import com.example.jawafai.view.dashboard.notifications.NotificationScreen
import com.example.jawafai.view.dashboard.settings.PersonaScreen
import com.example.jawafai.view.dashboard.settings.ProfileScreen
import com.example.jawafai.view.dashboard.settings.SettingsScreen
import com.example.jawafai.utils.WithNetworkMonitoring
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable full screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContent {
            JawafaiTheme {
                // Wrap dashboard with network monitoring
                WithNetworkMonitoring {
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
fun DashboardScreen(onLogout: () -> Unit) {
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

    // State for swipe gestures
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 300f

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

    // Helper function to navigate to tab by index
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
        val unused = paddingValues // To avoid unused variable warning
        // Main content area with enhanced swipe gesture support
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(isInMainScreen) {
                    if (isInMainScreen) {
                        detectDragGestures(
                            onDragStart = {
                                swipeOffset = 0f
                            },
                            onDragEnd = {
                                val currentIndex = getCurrentTabIndex()

                                when {
                                    swipeOffset > swipeThreshold -> {
                                        // Swipe right - go to previous tab
                                        if (currentIndex > 0) {
                                            navigateToTab(currentIndex - 1)
                                        }
                                    }
                                    swipeOffset < -swipeThreshold -> {
                                        // Swipe left - go to next tab
                                        if (currentIndex < items.size - 1) {
                                            navigateToTab(currentIndex + 1)
                                        }
                                    }
                                }
                                swipeOffset = 0f
                            }
                        ) { change, dragAmount ->
                            swipeOffset += dragAmount.x
                            change.consume()
                        }
                    }
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    BottomNavItem.Home.route,
                    enterTransition = {
                        val targetIndex = 0
                        val initialIndex = items.indexOfFirst { it.route == initialState.destination.route }

                        if (initialIndex > targetIndex) {
                            // Coming from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        } else {
                            // Coming from left or initial
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        }
                    },
                    exitTransition = {
                        val currentIndex = 0
                        val targetIndex = items.indexOfFirst { it.route == targetState.destination.route }

                        if (targetIndex > currentIndex) {
                            // Going to right
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        } else {
                            // Going to left
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
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
                            // Navigate to Chat tab instead of just chat route
                            navigateToTab(1) // Chat is at index 1
                        }
                    )
                }

                composable(
                    BottomNavItem.Chat.route,
                    enterTransition = {
                        val targetIndex = 1
                        val initialIndex = items.indexOfFirst { it.route == initialState.destination.route }

                        if (initialIndex > targetIndex) {
                            // Coming from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        } else {
                            // Coming from left
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        }
                    },
                    exitTransition = {
                        val currentIndex = 1
                        val targetIndex = items.indexOfFirst { it.route == targetState.destination.route }

                        if (targetIndex > currentIndex) {
                            // Going to right
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        } else {
                            // Going to left
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    }
                ) {
                    ChatScreen(
                        onNavigateToChat = { chatId, otherUserId ->
                            navController.navigate("chat_detail/$chatId/$otherUserId")
                        }
                    )
                }

                composable(
                    route = "chat_detail/{chatId}/{otherUserId}",
                    arguments = listOf(
                        navArgument("chatId") { type = NavType.StringType },
                        navArgument("otherUserId") { type = NavType.StringType }
                    ),
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(400)
                        ) + fadeIn(animationSpec = tween(400))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(400)
                        ) + fadeOut(animationSpec = tween(400))
                    }
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

                composable(
                    BottomNavItem.Notifications.route,
                    enterTransition = {
                        val targetIndex = 2
                        val initialIndex = items.indexOfFirst { it.route == initialState.destination.route }

                        if (initialIndex > targetIndex) {
                            // Coming from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        } else {
                            // Coming from left
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        }
                    },
                    exitTransition = {
                        val currentIndex = 2
                        val targetIndex = items.indexOfFirst { it.route == targetState.destination.route }

                        if (targetIndex > currentIndex) {
                            // Going to right
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        } else {
                            // Going to left
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    }
                ) {
                    NotificationScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    BottomNavItem.Settings.route,
                    enterTransition = {
                        val targetIndex = 3
                        val initialIndex = items.indexOfFirst { it.route == initialState.destination.route }

                        if (initialIndex > targetIndex) {
                            // Coming from right (shouldn't happen as this is the rightmost)
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        } else {
                            // Coming from left
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        }
                    },
                    exitTransition = {
                        val currentIndex = 3
                        val targetIndex = items.indexOfFirst { it.route == targetState.destination.route }

                        if (targetIndex > currentIndex) {
                            // Going to right (shouldn't happen as this is the rightmost)
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        } else {
                            // Going to left
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
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
                        }
                    )
                }

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
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHistory = { navController.navigate("chatbot_history") }
                    )
                }

                composable("chatbot_history") {
                    ChatBotHistoryScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onConversationClick = { conversationId ->
                            navController.navigate("chatbot_conversation/$conversationId")
                        },
                        onNewChatClick = { navController.navigate("chatbot") }
                    )
                }

                composable(
                    route = "chatbot_conversation/{conversationId}",
                    arguments = listOf(
                        navArgument("conversationId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    ChatBotConversationScreen(
                        conversationId = conversationId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHistory = { navController.navigate("chatbot_history") }
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
    DashboardScreen(onLogout = {})
}
