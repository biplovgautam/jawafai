package com.example.jawafai.view.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

            // Title: "जवाफ.AI" - Using KaiseiDecol Bold for consistent font loading
            Text(
                text = "जवाफ.AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = Color(0xFF395B64)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle: "Your AI Wingman for Every Reply." - Using KaiseiDecol for consistency
            Text(
                text = "Your AI Wingman for Every Reply.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
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
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color.White
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
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = Color(0xFF395B64)
                    )
                )
            }
        }
    }
}
