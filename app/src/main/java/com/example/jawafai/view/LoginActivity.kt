package com.example.jawafai.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import com.example.jawafai.R
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.JawafaiTheme
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Define the font family directly in this file
val KaiseiFontFamily = FontFamily(
    Font(R.font.kaiseidecol_regular)
)

class LoginActivity : ComponentActivity() {
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase components
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repository = UserRepositoryImpl(auth, firestore)

        // Initialize ViewModel
        viewModel = UserViewModelFactory(repository, auth).create(UserViewModel::class.java)

        setContent {
            JawafaiTheme {
                LoginScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: UserViewModel) {
    val context = LocalContext.current

    // State variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Observe ViewModel state
    val userOperationState = viewModel.userState.observeAsState(initial = UserViewModel.UserOperationResult.Initial)

    // Handle state changes
    LaunchedEffect(userOperationState.value) {
        when (val state = userOperationState.value) {
            is UserViewModel.UserOperationResult.Initial -> {
                // Initial state, do nothing
            }
            is UserViewModel.UserOperationResult.Loading -> {
                isLoading = true
            }
            is UserViewModel.UserOperationResult.Success -> {
                isLoading = false
                Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                // Navigate to Dashboard/Profile/Main screen
                val intent = Intent(context, DashboardActivity::class.java)
                context.startActivity(intent)
                if (context is ComponentActivity) {
                    context.finish() // Close login activity
                }
            }
            is UserViewModel.UserOperationResult.Error -> {
                isLoading = false
                Toast.makeText(context, "Login failed: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is UserViewModel.UserOperationResult.PasswordResetSent -> {
                isLoading = false
                showResetDialog = false
                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
            }
        }
    }

    // UI elements
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF006064)
            )
        }

        // Login form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.profile),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Title
            Text(
                "Login",
                fontSize = 32.sp,
                color = Color(0xFF004D40),
                fontFamily = KaiseiFontFamily
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", fontFamily = KaiseiFontFamily) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field with visibility toggle
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontFamily = KaiseiFontFamily) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val icon = if (passwordVisible)
                        painterResource(id = R.drawable.baseline_visibility_off_24)
                    else
                        painterResource(id = R.drawable.baseline_visibility_24)

                    Icon(
                        painter = icon,
                        contentDescription = "Toggle password visibility",
                        modifier = Modifier.clickable {
                            passwordVisible = !passwordVisible
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Forgot Password?",
                    color = Color.Blue,
                    fontFamily = KaiseiFontFamily,
                    modifier = Modifier.clickable {
                        showResetDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = {
                    // Validate fields
                    when {
                        email.isBlank() -> Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                        !email.contains('@') -> Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                        password.isBlank() -> Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
                        else -> {
                            // Trigger login in ViewModel
                            viewModel.login(email, password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064))
            ) {
                Text("Login", color = Color.White, fontSize = 18.sp, fontFamily = KaiseiFontFamily)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Register link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Don't have an account? ",
                    color = Color.DarkGray,
                    fontFamily = KaiseiFontFamily
                )
                Text(
                    "Register",
                    color = Color.Blue,
                    fontFamily = KaiseiFontFamily,
                    modifier = Modifier.clickable {
                        // Navigate to registration
                        val intent = Intent(context, RegistrationActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Password Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Password", fontFamily = KaiseiFontFamily) },
                text = {
                    Column {
                        Text("Enter your email to receive a password reset link.", fontFamily = KaiseiFontFamily)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email", fontFamily = KaiseiFontFamily) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (resetEmail.isBlank() || !resetEmail.contains("@")) {
                            Toast.makeText(context, "Enter a valid email", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.resetPassword(resetEmail)
                        }
                    }) {
                        Text("Send Reset Link", fontFamily = KaiseiFontFamily)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", fontFamily = KaiseiFontFamily)
                    }
                }
            )
        }
    }
}
