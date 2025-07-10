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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    checkUserState: () -> String
) {
    LaunchedEffect(Unit) {
        delay(3000) // 3-second delay for splash screen
        val destination = checkUserState()
        onNavigate(destination)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Icon",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(200.dp))

            Text(
                text = "जवाफ.AI",
                fontSize = 30.sp,
                fontFamily = AppFonts.KaiseiRegularFontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = "Your AI Wingman for Every Reply.",
                fontSize = 18.sp,
                fontFamily = AppFonts.KaiseiRegularFontFamily,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}