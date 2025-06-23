package com.example.jawafai.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.ui.theme.JawafaiTheme
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle back press to move app to background instead of navigating back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Move task to back which sends app to background (like pressing home button)
                moveTaskToBack(true)
            }
        })

        setContent {
            JawafaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        onLogout = {
                            // Sign out from Firebase
                            FirebaseAuth.getInstance().signOut()

                            // Navigate directly to login screen instead of MainActivity/splash
                            val intent = Intent(this@DashboardActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish() // Close dashboard activity
                        }
                    )
                }
            }
        }
    }

    // Override the default back button behavior as a fallback for older Android versions
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // First call super (required to satisfy the Android lint warning)
        super.onBackPressed()

        // The OnBackPressedDispatcher should handle this on newer Android versions,
        // but in case it doesn't (on older versions), also move task to back
        moveTaskToBack(true)
    }
}

@Composable
fun DashboardScreen(onLogout: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Dashboard!",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        currentUser?.let { user ->
            Text(
                text = "Logged in as: ${user.email}",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}
