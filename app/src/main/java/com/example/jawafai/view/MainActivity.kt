package com.example.jawafai.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.ui.theme.JawafaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JawafaiTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") { SplashScreen(navController) }
                    composable("onboard1") { Onboard1Screen(navController) }
                    composable("onboard2") { Onboard2Screen(navController) }
                    composable("onboard3") { Onboard3Screen(navController) }
                    composable("onboard4") { Onboard4Screen(navController) }
                    composable("welcome") { WelcomeScreen(navController) }

                    // Updated login navigation to launch LoginActivity
                    composable("login") {
                        LoginNavigation(navController = navController, onLogin = {
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                        })
                    }

                    composable("registration") {
                        RegistrationNavigation(navController = navController, onRegister = {
                            val intent = Intent(this@MainActivity, RegistrationActivity::class.java)
                            startActivity(intent)
                        })
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun LoginNavigation(navController: androidx.navigation.NavController, onLogin: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.runtime.LaunchedEffect(key1 = true) {
            onLogin()
        }
    }
}

@androidx.compose.runtime.Composable
fun RegistrationNavigation(navController: androidx.navigation.NavController, onRegister: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.runtime.LaunchedEffect(key1 = true) {
            onRegister()
        }
    }
}
