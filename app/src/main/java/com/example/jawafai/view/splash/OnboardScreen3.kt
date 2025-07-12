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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.jawafai.R
import com.example.jawafai.view.ui.theme.JawafaiTheme

@Composable
fun Onboard3Screen(navController: NavController) {
    val KaiseiDecolFontFamily = FontFamily(Font(R.font.kaiseidecol_medium))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE7F6F2))
            .padding(24.dp)
    ) {
        // Skip Button
        TextButton(
            onClick = { navController.navigate("home") },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = "Skip",
                style = TextStyle(
                    fontFamily = KaiseiDecolFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF395B64)
                )
            )
        }

        // Main content
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
                style = TextStyle(
                    fontFamily = KaiseiDecolFontFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Start Exploring",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 100.dp),
                style = TextStyle(
                    fontFamily = KaiseiDecolFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Let’s get you into the app and begin your smart conversation journey!",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 1.dp),
                style = TextStyle(
                    fontFamily = KaiseiDecolFontFamily,
                    fontSize = 16.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("onboard4") },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF395B64)),
                modifier = Modifier
                    .height(48.dp)
                    .width(250.dp)
            ) {
                Text(
                    text = "Next",
                    style = TextStyle(
                        fontFamily = KaiseiDecolFontFamily,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                )
            }
        }

        // Dot Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { index ->
                val isSelected = index == 2
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
fun Onboard3ScreenPreview() {
    JawafaiTheme {
        val navController = rememberNavController()
        Onboard3Screen(navController)
    }
}
