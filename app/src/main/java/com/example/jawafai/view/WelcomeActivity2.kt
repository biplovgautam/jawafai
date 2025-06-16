package com.example.jawafai.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.view.LoginActivity
import com.example.jawafai.R

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WelcomeScreen()
        }
    }
}

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "Bot Icon",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Welcome",
                fontSize = 75.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D40)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your smart assistant is ready to help.",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Get Started", color = Color.White, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Don't have an account? Signup",
                color = Color.Blue,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(context, RegistrationActivity::class.java))
                    activity?.finish()
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen()
}