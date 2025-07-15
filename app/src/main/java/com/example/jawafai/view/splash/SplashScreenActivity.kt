package com.example.jawafai.view.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.utils.rememberNetworkConnectivity
import com.example.jawafai.utils.NotificationPermissionUtils
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.*

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    checkUserState: () -> String
) {
    val context = LocalContext.current

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

    // Notification permission states
    var notificationPermissionRequested by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var showNotificationListenerDialog by remember { mutableStateOf(false) }
    var notificationListenerEnabled by remember { mutableStateOf(false) }
    var permissionsCheckComplete by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            notificationPermissionGranted = granted
            // After basic notification permission, check notification listener
            if (granted) {
                notificationListenerEnabled = NotificationPermissionUtils.isNotificationListenerEnabled(context)
                if (!notificationListenerEnabled) {
                    showNotificationListenerDialog = true
                } else {
                    permissionsCheckComplete = true
                }
            } else {
                // If basic notification permission is denied, still check notification listener
                notificationListenerEnabled = NotificationPermissionUtils.isNotificationListenerEnabled(context)
                if (!notificationListenerEnabled) {
                    showNotificationListenerDialog = true
                } else {
                    permissionsCheckComplete = true
                }
            }
        }
    )

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

    // Request notification permissions at the start
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionRequested) {
            notificationPermissionRequested = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationPermissionGranted = true // Not needed on older versions
            // Check notification listener permission directly
            notificationListenerEnabled = NotificationPermissionUtils.isNotificationListenerEnabled(context)
            if (!notificationListenerEnabled) {
                showNotificationListenerDialog = true
            } else {
                permissionsCheckComplete = true
            }
        }
    }

    // Check notification listener permission when returning from settings
    LaunchedEffect(showNotificationListenerDialog) {
        if (!showNotificationListenerDialog) {
            // User returned from settings, check again
            notificationListenerEnabled = NotificationPermissionUtils.isNotificationListenerEnabled(context)
            if (notificationListenerEnabled) {
                permissionsCheckComplete = true
            }
        }
    }

    // Add lifecycle awareness to detect when user returns from settings
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && showNotificationListenerDialog) {
                // User returned from settings, check permission again
                notificationListenerEnabled = NotificationPermissionUtils.isNotificationListenerEnabled(context)
                if (notificationListenerEnabled) {
                    showNotificationListenerDialog = false
                    permissionsCheckComplete = true
                }
            }
        }

        activity?.lifecycle?.addObserver(lifecycleObserver)

        onDispose {
            activity?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }

    // Wait for all permissions and connection before starting final countdown
    LaunchedEffect(permissionsCheckComplete, startFinalCountdown) {
        if (permissionsCheckComplete && startFinalCountdown) {
            delay(1000)
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
            .background(Color.White),
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
                    lineHeight = 32.sp,
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
                    lineHeight = 15.sp,
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
                containerColor = Color(0xFFE7F6F2),
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

        // Notification Listener Permission Dialog
        if (showNotificationListenerDialog) {
            AlertDialog(
                onDismissRequest = { /* Don't allow dismissal */ },
                containerColor = Color(0xFFE7F6F2),
                title = {
                    Text(
                        text = "Notification Access Required",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            color = Color(0xFF395B64)
                        )
                    )
                },
                text = {
                    Text(
                        text = "जवाफ.AI needs access to read notifications from other apps to provide AI-powered reply suggestions. Please enable notification access in the settings.",
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
                            NotificationPermissionUtils.openNotificationAccessSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Settings",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotificationListenerDialog = false
                            permissionsCheckComplete = true // Continue without notification access
                        }
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                color = Color(0xFF666666)
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
            delay(3000) // Wait 3 seconds before enabling retry button again
            isRetrying = false
        }
    }
}