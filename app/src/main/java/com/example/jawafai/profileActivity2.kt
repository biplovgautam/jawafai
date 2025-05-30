package com.example.profilepage

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.R

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfileScreen()
        }
    }
}

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val profileImage = painterResource(id = R.drawable.profile)
    val background = painterResource(id = R.drawable.background)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = background,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        ProfileContent(
            profileImage = profileImage,
            userName = "Manisha",
            userEmail = "Manisha@example.com",
            onSave = { name, email ->
                Toast.makeText(context, "Saved: $name, $email", Toast.LENGTH_SHORT).show()
            },
            onChangePassword = {
                Toast.makeText(context, "Change password clicked", Toast.LENGTH_SHORT).show()
            },
            onImageChange = {
                Toast.makeText(context, "Open image picker", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ProfileContent(
    profileImage: Painter,
    userName: String,
    userEmail: String,
    onSave: (String, String) -> Unit,
    onChangePassword: () -> Unit,
    onImageChange: () -> Unit
) {
    var name by remember { mutableStateOf(userName) }
    var email by remember { mutableStateOf(userEmail) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Edit Profile",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D40)
            )
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            Box(modifier = Modifier.size(130.dp)) {
                Image(
                    painter = profileImage,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { onImageChange() },
                    contentScale = ContentScale.Crop
                )

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF004D40),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = { onSave(name, email) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp)) // Optional space at bottom
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileActivityPreview() {
    ProfileScreen()
}