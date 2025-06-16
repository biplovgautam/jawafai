package com.example.jawafai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.ui.theme.JawafaiTheme
import com.example.jawafai.view.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JawafaiTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") { SplashScreen(navController) }
                        composable("onboard1") { Onboard1Screen(navController) }
                        composable("onboard2") { Onboard2Screen(navController) }
                        composable("onboard3") { Onboard3Screen(navController) }
                        composable("onboard4") { Onboard4Screen(navController) } // Replace with your actual HomeScreen
                        // Add SignUp / Login screens here as needed
                    }
                }
            }
        }
    }
}
