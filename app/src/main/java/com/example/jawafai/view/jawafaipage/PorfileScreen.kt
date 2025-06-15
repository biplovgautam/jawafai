package com.example.jawafai.view.jawafaipage

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.R


@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val profileImage = painterResource(id = R.drawable.logo)
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
            userEmail = "manisha@example.com",
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

    Column (
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Profile",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF004D40)
        )

        Spacer(modifier = Modifier.height(40.dp))

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

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button (
            onClick = { onSave(name, email) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton (
            onClick = onChangePassword,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Password")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileActivityPreview() {
    // In preview mode, use dummy resources
    val dummyPainter = if (LocalInspectionMode.current) {
        painterResource(android.R.drawable.sym_def_app_icon)
    } else {
        painterResource(id = R.drawable.logo)
    }

    ProfileContent(
        profileImage = dummyPainter,
        userName = "Manisha",
        userEmail = "manisha@example.com",
        onSave = { _, _ -> },
        onChangePassword = {},
        onImageChange = {}
    )
}
