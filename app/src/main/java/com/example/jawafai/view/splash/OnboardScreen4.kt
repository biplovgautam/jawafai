package com.example.jawafai.view.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.R
import com.example.jawafai.view.ui.theme.JawafaiTheme
import com.example.jawafai.ui.theme.AppFonts

@Composable
fun Onboard4Screen(
    navController: NavController? = null,
    onFinishOnboarding: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F6F2))
            .padding(24.dp)
    ) {
        // Skip Button
        TextButton(
            onClick = {
                onFinishOnboarding?.invoke() ?: navController?.navigate("welcome")
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF395B64)
                )
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "Onboarding Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "जवाफ.AI",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Secured Chats",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 100.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Privacy that listens only to you.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 1.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontSize = 16.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    onFinishOnboarding?.invoke() ?: navController?.navigate("welcome")
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF395B64)),
                modifier = Modifier
                    .height(48.dp)
                    .width(250.dp)
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        color = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                val isSelected = index == 3
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(10.dp)
                        .padding(horizontal = 4.dp)
                        .background(
                            color = if (isSelected) Color(0xFF395B64) else Color(0xFFA5C9CA),
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Onboard4ScreenPreview() {
    JawafaiTheme {
        Onboard4Screen()
    }
}
