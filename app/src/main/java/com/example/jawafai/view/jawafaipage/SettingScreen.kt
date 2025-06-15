package com.example.jawafai.view.jawafaipage

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.R


@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val background = painterResource(id = R.drawable.background)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = background,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        SettingsContent(
            onLogout = {
                Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
            },
            onToggleNotification = {
                Toast.makeText(context, "Notification Toggled: $it", Toast.LENGTH_SHORT).show()
            },
            onToggleDarkMode = {
                Toast.makeText(context, "Dark Mode Toggled: $it", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SettingsContent(
    onLogout: () -> Unit,
    onToggleNotification: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Settings",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF004D40),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingToggle(
            title = "Enable Notifications",
            isChecked = notificationsEnabled,
            onCheckedChange = {
                notificationsEnabled = it
                onToggleNotification(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingToggle(
            title = "Enable Dark Mode",
            isChecked = darkModeEnabled,
            onCheckedChange = {
                darkModeEnabled = it
                onToggleDarkMode(it)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingItem("Language") {
            Toast.makeText(context, "Change Language clicked", Toast.LENGTH_SHORT).show()
        }

        SettingItem("Privacy Policy") {
            Toast.makeText(context, "Open Privacy Policy", Toast.LENGTH_SHORT).show()
        }

        SettingItem("Help & Support") {
            Toast.makeText(context, "Open Help & Support", Toast.LENGTH_SHORT).show()
        }

        SettingItem("About") {
            Toast.makeText(context, "About App", Toast.LENGTH_SHORT).show()
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Log Out", color = Color.White)
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            color = Color(0xFF004D40)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF004D40)
            )
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            color = Color(0xFF004D40)
        )

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Go",
            tint = Color(0xFF004D40),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
