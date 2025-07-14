package com.example.jawafai.view.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.utils.rememberNetworkConnectivity
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    checkUserState: () -> String
) {
    // Lottie animation composition
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.live_chatbot))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    // Network connectivity state
    val isConnected = rememberNetworkConnectivity()
    var showNoInternetDialog by remember { mutableStateOf(false) }
    var hasCheckedInternet by remember { mutableStateOf(false) }
    var isRetrying by remember { mutableStateOf(false) }
    var connectionEstablished by remember { mutableStateOf(false) }
    var startFinalCountdown by remember { mutableStateOf(false) }

    // Check internet connection after initial delay
    LaunchedEffect(Unit) {
        delay(2000) // Wait 2 seconds for animation to show
        hasCheckedInternet = true
        if (!isConnected) {
            showNoInternetDialog = true
        } else {
            connectionEstablished = true
            startFinalCountdown = true
        }
    }

    // Handle connection establishment
    LaunchedEffect(isConnected, hasCheckedInternet) {
        if (isConnected && hasCheckedInternet && !connectionEstablished) {
            // Connection just established
            connectionEstablished = true
            showNoInternetDialog = false
            startFinalCountdown = true
        }
    }

    // Final countdown after connection is established
    LaunchedEffect(startFinalCountdown) {
        if (startFinalCountdown) {
            delay(1000) // Show splash for 3 seconds after connection (reduced from 5 seconds)
            val destination = checkUserState()
            onNavigate(destination)
        }
    }

    // Show no internet dialog when connection is lost
    LaunchedEffect(isConnected) {
        if (hasCheckedInternet && !isConnected && connectionEstablished) {
            // Connection lost after being established
            showNoInternetDialog = true
            startFinalCountdown = false
            connectionEstablished = false
        } else if (hasCheckedInternet && !isConnected && !connectionEstablished) {
            // No connection from the start
            showNoInternetDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Changed to white background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lottie Animation
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Name - Using Kadwa Bold font as per Figma specifications
            Text(
                text = "जवाफ.AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = AppFonts.KadwaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 32.sp, // 100% line height
                    letterSpacing = 0.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline - Using Karla Bold font as per Figma specifications
            Text(
                text = "Your AI Wingman for Every Reply.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 15.sp, // 100% line height
                    letterSpacing = 0.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Connection Status
            if (hasCheckedInternet) {
                if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_dialog_info),
                            contentDescription = "Connected",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                color = Color(0xFF4CAF50)
                            )
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_dialog_alert),
                            contentDescription = "No Connection",
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No Internet Connection",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                color = Color(0xFFFF5722)
                            )
                        )
                    }
                }
            }
        }

        // No Internet Dialog
        if (showNoInternetDialog) {
            AlertDialog(
                onDismissRequest = { /* Don't allow dismissal */ },
                containerColor = Color(0xFFE7F6F2), // Updated background color to #E7F6F2
                title = {
                    Text(
                        text = "No Internet Connection",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            color = Color(0xFF395B64)
                        )
                    )
                },
                text = {
                    Text(
                        text = "Please check your internet connection and try again. जवाफ.AI requires an active internet connection to work properly.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            color = Color(0xFF666666)
                        ),
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!isRetrying) {
                                isRetrying = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRetrying) {
                            // Rotating animation for the refresh icon
                            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                            val rotationAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Checking",
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer(rotationZ = rotationAngle)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isRetrying) "Checking..." else "Retry",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily
                            )
                        )
                    }
                }
            )
        }
    }

    // Reset retry state when connection is restored or after timeout
    LaunchedEffect(isConnected) {
        if (isConnected) {
            isRetrying = false
        }
    }

    // Timeout mechanism for retry button
    LaunchedEffect(isRetrying) {
        if (isRetrying) {
            delay(2000) // Wait 3 seconds
            if (!isConnected) {
                isRetrying = false // Reset retry state if still no connection
            }
        }
    }
}