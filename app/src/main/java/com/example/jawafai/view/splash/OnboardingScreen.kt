package com.example.jawafai.view.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    navController: NavController,
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFA5C9CA)) // Background color #A5C9CA for all screens
    ) {
        // Skip Button
        TextButton(
            onClick = { onFinishOnboarding() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Text(
                text = "Skip",
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    color = Color(0xFF395B64)
                )
            )
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> OnboardingPage1()
                1 -> OnboardingPage2()
                2 -> OnboardingPage3()
                3 -> OnboardingPage4()
            }
        }

        // Next Button
        Button(
            onClick = {
                if (pagerState.currentPage < 3) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinishOnboarding()
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF395B64)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(48.dp)
                .width(120.dp)
        ) {
            Text(
                text = if (pagerState.currentPage < 3) "Next" else "Start",
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily
                )
            )
        }

        // Page Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (index == pagerState.currentPage) Color(0xFF395B64) else Color.Gray,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
fun OnboardingPage1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "Smart Replies",
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
                color = Color(0xFF395B64)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Smart Replies",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 100.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64),
                fontSize = 20.sp
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Replies that sound just like you - only faster.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64)
            )
        )
    }
}

@Composable
fun OnboardingPage2() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo2),
            contentDescription = "AI Assistant",
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
                color = Color(0xFF395B64),
                fontSize = 30.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Cross-Platform",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 100.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64),
                fontSize = 19.sp
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "From DMs to emails — Jawaf's got it.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64)
            )
        )
    }
}

@Composable
fun OnboardingPage3() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "Personalized Experience",
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
                color = Color(0xFF395B64),
                fontSize = 30.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Personalized",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 50.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64),
                fontSize = 19.sp
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Learn your style and preferences for better responses.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64)
            )
        )
    }
}

@Composable
fun OnboardingPage4() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "Get Started",
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
                color = Color(0xFF395B64),
                fontSize = 30.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Ready to Start",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 50.dp),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64),
                fontSize = 19.sp
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Let's get you set up and ready to go!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                color = Color(0xFF395B64)
            )
        )
    }
}
