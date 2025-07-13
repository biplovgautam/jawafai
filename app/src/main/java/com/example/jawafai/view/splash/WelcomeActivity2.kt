package com.example.jawafai.view.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
import com.airbnb.lottie.compose.*

@Composable
fun WelcomeScreen(navController: NavController) {
    // Lottie animation composition - same as splash screen
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.live_chatbot))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Plain white background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie Animation at the top
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Title: "जवाफ.AI"
            Text(
                text = "जवाफ.AI",
                fontSize = 42.sp,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    color = Color(0xFF395B64)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle: "Your AI Wingman for Every Reply."
            Text(
                text = "Your AI Wingman for Every Reply.",
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    color = Color(0xFF395B64)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sign In Button
            Button(
                onClick = {
                    navController.navigate("login") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C9CA)),
                modifier = Modifier
                    .height(50.dp)
            ) {
                Text(
                    text = "Sign In",
                    color = Color.White,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create an account button (text-only)
            TextButton(
                onClick = {
                    navController.navigate("registration") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "Create an account",
                    color = Color(0xFF395B64),
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily
                    )
                )
            }
        }
    }
}


