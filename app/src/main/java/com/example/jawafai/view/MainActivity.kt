package com.example.jawafai.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.ui.theme.JawafaiTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat
import com.example.jawafai.view.auth.LoginActivity
import com.example.jawafai.view.auth.RegistrationActivity
import com.example.jawafai.view.dashboard.DashboardActivity
import com.example.jawafai.view.splash.Onboard1Screen
import com.example.jawafai.view.splash.Onboard2Screen
import com.example.jawafai.view.splash.Onboard3Screen
import com.example.jawafai.view.splash.Onboard4Screen
import com.example.jawafai.view.splash.SplashScreen
import com.example.jawafai.view.splash.WelcomeScreen

class MainActivity : ComponentActivity() {

    companion object {
        // Key for tracking if user has seen onboarding
        const val PREFS_NAME = "JawafaiPrefs"
        const val PREF_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Check if user has already seen onboarding screens
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenOnboarding = sharedPreferences.getBoolean(PREF_HAS_SEEN_ONBOARDING, false)

        // Check if user is logged in and force refresh from Firebase
        val auth = FirebaseAuth.getInstance()
        var isLoggedIn = auth.currentUser != null

        // Force reload user state from Firebase to handle deleted users
        auth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
            val refreshedUser = auth.currentUser
            isLoggedIn = refreshedUser != null

            setContent {
                JawafaiTheme {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "splash") {
                        // Splash screen - shown every time the app opens
                        composable("splash") {
                            SplashScreen(
                                onNavigate = { destination ->
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                checkUserState = {
                                    when {
                                        isLoggedIn -> "dashboard"
                                        hasSeenOnboarding -> "welcome"
                                        else -> "onboard1"
                                    }
                                }
                            )
                        }

                        // Onboarding screens - shown only first time after installing
                        composable("onboard1") { Onboard1Screen(navController) }
                        composable("onboard2") { Onboard2Screen(navController) }
                        composable("onboard3") { Onboard3Screen(navController) }
                        composable("onboard4") {
                            Onboard4Screen(
                                onFinishOnboarding = {
                                    // Mark that user has seen onboarding
                                    sharedPreferences.edit()
                                        .putBoolean(PREF_HAS_SEEN_ONBOARDING, true)
                                        .apply()
                                    navController.navigate("welcome") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Welcome screen - choice between login and registration
                        composable("welcome") { WelcomeScreen(navController) }

                        // Login screen - navigate to dashboard on successful login
                        composable("login") {
                            LoginNavigation(
                                navController = navController,
                                onLogin = {
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Registration screen - navigate to dashboard on successful registration
                        composable("registration") {
                            RegistrationNavigation(
                                navController = navController,
                                onRegister = {
                                    val intent = Intent(this@MainActivity, RegistrationActivity::class.java)
                                    startActivity(intent)
                                }
                            )
                        }

                        // Dashboard screen - only accessible for logged-in users
                        composable("dashboard") {
                            val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Close MainActivity once dashboard is opened
                        }
                    }
                }
            }
        } ?: run {
            // If no user, just proceed as not logged in
            setContent {
                JawafaiTheme {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(
                                onNavigate = { destination ->
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                checkUserState = {
                                    when {
                                        false -> "dashboard"
                                        hasSeenOnboarding -> "welcome"
                                        else -> "onboard1"
                                    }
                                }
                            )
                        }

                        // Onboarding screens - shown only first time after installing
                        composable("onboard1") { Onboard1Screen(navController) }
                        composable("onboard2") { Onboard2Screen(navController) }
                        composable("onboard3") { Onboard3Screen(navController) }
                        composable("onboard4") {
                            Onboard4Screen(
                                onFinishOnboarding = {
                                    // Mark that user has seen onboarding
                                    sharedPreferences.edit()
                                        .putBoolean(PREF_HAS_SEEN_ONBOARDING, true)
                                        .apply()
                                    navController.navigate("welcome") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Welcome screen - choice between login and registration
                        composable("welcome") { WelcomeScreen(navController) }

                        // Login screen - navigate to dashboard on successful login
                        composable("login") {
                            LoginNavigation(
                                navController = navController,
                                onLogin = {
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Registration screen - navigate to dashboard on successful registration
                        composable("registration") {
                            RegistrationNavigation(
                                navController = navController,
                                onRegister = {
                                    val intent = Intent(this@MainActivity, RegistrationActivity::class.java)
                                    startActivity(intent)
                                }
                            )
                        }

                        // Dashboard screen - only accessible for logged-in users
                        composable("dashboard") {
                            val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Close MainActivity once dashboard is opened
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun LoginNavigation(
    navController: androidx.navigation.NavController,
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.runtime.LaunchedEffect(key1 = true) {
            onLogin()
        }
    }
}

@androidx.compose.runtime.Composable
fun RegistrationNavigation(
    navController: androidx.navigation.NavController,
    onRegister: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.runtime.LaunchedEffect(key1 = true) {
            onRegister()
        }
    }
}
