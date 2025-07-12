package com.example.jawafai.view.auth

 import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
 import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.example.jawafai.R
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.JawafaiTheme
 import com.example.jawafai.view.dashboard.DashboardActivity
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

    companion object {
        const val PREFS_NAME = "JawafaiPrefs"
        const val PREF_REMEMBER_ME = "rememberMe"
    }

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

    override fun onStop() {
        super.onStop()
        // Check if "Remember Me" is disabled and user is closing the app
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false)

        if (!rememberMe) {
            // Auto logout if Remember Me is not checked
            FirebaseAuth.getInstance().signOut()
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
    var rememberMe by remember { mutableStateOf(false) }

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
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Login form
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                // App Logo
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(100.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                // Login Title
                Text(
                    "Login",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = KaiseiFontFamily
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontFamily = KaiseiFontFamily) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
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
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
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
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                // Remember Me checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        "Remember Me",
                        fontFamily = KaiseiFontFamily,
                        modifier = Modifier.clickable {
                            rememberMe = !rememberMe
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
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

                                // Save or clear Remember Me preference
                                val sharedPreferences = context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                                with(sharedPreferences.edit()) {
                                    putBoolean(LoginActivity.PREF_REMEMBER_ME, rememberMe)
                                    apply()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Login", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontFamily = KaiseiFontFamily)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                // Register link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Don't have an account? ",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontFamily = KaiseiFontFamily
                    )
                    Text(
                        "Register",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontFamily = KaiseiFontFamily,
                        modifier = Modifier.clickable {
                            // Navigate to registration
                            val intent = Intent(context, RegistrationActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    JawafaiTheme {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repository = UserRepositoryImpl(auth, firestore)
        val viewModel = UserViewModelFactory(repository, auth).create(UserViewModel::class.java)
        LoginScreen(viewModel = viewModel)
    }
}
