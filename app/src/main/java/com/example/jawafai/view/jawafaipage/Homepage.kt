package com.example.profileactivity.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.R


@Composable
fun Homepage() {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val textColor = colorScheme.onBackground
    val cardColor = colorScheme.surface.copy(alpha = 0.1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background Image with dimmed overlay
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f } // fix for blur
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.0f), Color.Black.copy(alpha = 0.1f))
                        )
                    )
                }
        )

        // Foreground content that scrolls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(230.dp))

            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("जवाफ.AI", fontSize = 40.sp, color = textColor)
            //Text("Main Dashboard", fontSize = 22.sp, color = textColor)
            //Text("AI Reply Assistant", fontSize = 16.sp, color = textColor.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("How can I assist you today?", fontSize = 18.sp, color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = textColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("New Message", fontSize = 16.sp, color = colorScheme.background)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // padding for scroll
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomepagePreview() {
    MaterialTheme(
        colorScheme = lightColorScheme() // or use your custom theme
    ) {
        Homepage()
    }
}