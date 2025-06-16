package com.example.jawafai.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jawafai.R
import com.example.jawafai.view.ui.theme.JawafaiTheme
import kotlinx.coroutines.delay

// UI content only (no nav logic) — reusable for preview or composable calls
@Composable
fun SplashScreenContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF395B64))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f)) // Push content downward

            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Icon",
                tint = Color.White,
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(200.dp)) // Gap between logo and text

            Text(
                text = "जवाफ.AI",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = "Your AI Wingman for Every Reply.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f)) // Push text closer to bottom
        }
    }
}

// Actual Splash Screen composable with navigation logic
@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        println("SplashScreen LaunchedEffect triggered")  // debug log
        delay(5000)
        navController.navigate("onboard1") {
            popUpTo("splash") { inclusive = true }
        }
    }
    SplashScreenContent()
}

// Preview only for UI in Android Studio
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    JawafaiTheme {
        SplashScreenContent()
    }
}