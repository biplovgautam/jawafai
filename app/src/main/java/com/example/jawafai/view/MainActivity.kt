package com.example.jawafai.view

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.ui.theme.JawafaiTheme
import com.example.jawafai.utils.WithNetworkMonitoring
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat
import com.example.jawafai.view.auth.LoginActivity
import com.example.jawafai.view.auth.RegistrationActivity
import com.example.jawafai.view.dashboard.DashboardActivity
import com.example.jawafai.view.splash.OnboardingScreen
import com.example.jawafai.view.splash.SplashScreen
import com.example.jawafai.view.splash.WelcomeScreen
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    companion object {
        // Key for tracking if user has seen onboarding
        const val PREFS_NAME = "JawafaiPrefs"
        const val PREF_HAS_SEEN_ONBOARDING = "hasSeenOnboarding"
        const val PREF_REMEMBER_ME = "rememberMe"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable full screen immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Request all required permissions at launch
        requestAllPermissions()

        // Check SharedPreferences
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenOnboarding = sharedPreferences.getBoolean(PREF_HAS_SEEN_ONBOARDING, false)
        val rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false)

        // Check if user is logged in
        val auth = FirebaseAuth.getInstance()
        var isLoggedIn = auth.currentUser != null

        // Auto-logout if Remember Me is not checked
        if (!rememberMe && isLoggedIn) {
            auth.signOut()
            isLoggedIn = false
        }

        // Force reload user state from Firebase to handle deleted users
        auth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
            val refreshedUser = auth.currentUser
            isLoggedIn = refreshedUser != null && rememberMe

            setContent {
                JawafaiTheme {
                    // Wrap entire app with network monitoring
                    WithNetworkMonitoring {
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
                                            else -> "onboarding"
                                        }
                                    }
                                )
                            }

                            // Merged onboarding screens - shown only first time after installing
                            composable("onboarding") {
                                OnboardingScreen(
                                    navController = navController,
                                    onFinishOnboarding = {
                                        // Mark that user has seen onboarding
                                        sharedPreferences.edit()
                                            .putBoolean(PREF_HAS_SEEN_ONBOARDING, true)
                                            .apply()
                                        navController.navigate("welcome") {
                                            popUpTo("onboarding") { inclusive = true }
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
        } ?: run {
            // If no user, just proceed as not logged in
            setContent {
                JawafaiTheme {
                    // Wrap entire app with network monitoring
                    WithNetworkMonitoring {
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
                                            else -> "onboarding"
                                        }
                                    }
                                )
                            }

                            // Merged onboarding screens - shown only first time after installing
                            composable("onboarding") {
                                OnboardingScreen(
                                    navController = navController,
                                    onFinishOnboarding = {
                                        // Mark that user has seen onboarding
                                        sharedPreferences.edit()
                                            .putBoolean(PREF_HAS_SEEN_ONBOARDING, true)
                                            .apply()
                                        navController.navigate("welcome") {
                                            popUpTo("onboarding") { inclusive = true }
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

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        val notGranted = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1001)
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val denied = permissions.zip(grantResults.toTypedArray()).filter { it.second != PackageManager.PERMISSION_GRANTED }
            if (denied.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("Some permissions are required for the app to function properly. Please enable them in settings.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check auto-logout when app is resumed
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false)

        if (!rememberMe && FirebaseAuth.getInstance().currentUser != null) {
            // Auto logout if Remember Me is not checked
            FirebaseAuth.getInstance().signOut()
            // Restart the app to go back to splash screen
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
