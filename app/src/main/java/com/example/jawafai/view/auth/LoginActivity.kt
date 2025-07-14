package com.example.jawafai.view.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.example.jawafai.JawafaiApplication
import com.example.jawafai.R
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.JawafaiTheme
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.view.dashboard.DashboardActivity
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

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

        // Check for auto-logout when app starts
        (application as JawafaiApplication).checkAutoLogout()

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
        // The Application class will handle the auto-logout logic
        // No need for manual logout here as it's handled globally
    }

    override fun onDestroy() {
        super.onDestroy()
        // Application class handles the session management
        // Individual activity destruction doesn't trigger logout
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

    // Focus requesters for keyboard navigation
    val passwordFocusRequester = remember { FocusRequester() }

    // Lottie animation composition
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.live_chatbot))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    // Observe ViewModel states
    val userOperationState = viewModel.userState.observeAsState(initial = UserViewModel.UserOperationResult.Initial)
    val emailValidationState = viewModel.emailValidationState.observeAsState(initial = UserViewModel.EmailValidationState.Initial)

    // Email validation with debounce
    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            delay(750) // 0.75 second delay
            if (email.isNotBlank()) { // Check again after delay
                viewModel.validateEmail(email)
            }
        } else {
            viewModel.resetEmailValidation()
        }
    }

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

                // Save remember me preference using Application class
                if (context is ComponentActivity) {
                    (context.application as JawafaiApplication).saveRememberMePreference(rememberMe)
                }

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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Add IME padding for keyboard handling
        containerColor = Color.White
    ) { paddingValues ->
        val unused = paddingValues
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Lottie Animation
            item {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(180.dp)
                )
            }

            // App Title - Using Kadwa font (only exception)
            item {
                Text(
                    text = "जवाफ.AI",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = AppFonts.KadwaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        color = Color(0xFF395B64)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Subtitle - Using Karla font
            item {
                Text(
                    text = "signin to continue using ai powered जवाफ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    ),
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Email TextField - Fixed height and validation indicator
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = {
                            Text(
                                "Enter your email",
                                fontFamily = AppFonts.KarlaFontFamily,
                                color = Color(0xFF666666)
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = AppFonts.KarlaFontFamily,
                            color = Color.Black,
                            fontSize = 16.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                passwordFocusRequester.requestFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp), // Increased height for proper text visibility
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF395B64),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF395B64),
                            unfocusedLabelColor = Color(0xFF666666),
                            cursorColor = Color(0xFF395B64),
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    // Email validation indicator
                    if (email.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(24.dp)
                                .background(
                                    Color(0xFFA5C9CA),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when (emailValidationState.value) {
                                is UserViewModel.EmailValidationState.Checking -> {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                is UserViewModel.EmailValidationState.Valid -> {
                                    // Use a proper tick/check mark icon
                                    Text(
                                        text = "✓",
                                        fontFamily = AppFonts.KarlaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                is UserViewModel.EmailValidationState.Invalid -> {
                                    Icon(
                                        painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                                        contentDescription = "Email doesn't exist",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                else -> {
                                    // Show nothing for initial state
                                }
                            }
                        }
                    }
                }
            }

            // Password TextField - Fixed height
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            "Enter your password",
                            fontFamily = AppFonts.KarlaFontFamily,
                            color = Color(0xFF666666)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = AppFonts.KarlaFontFamily,
                        color = Color.Black,
                        fontSize = 16.sp
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            // Trigger login when Done is pressed
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = Color(0xFF395B64)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp) // Increased height for proper text visibility
                        .focusRequester(passwordFocusRequester),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF395B64),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = Color(0xFF395B64),
                        unfocusedLabelColor = Color(0xFF666666),
                        cursorColor = Color(0xFF395B64),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
            }

            // Remember Me Checkbox and Forgot Password
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF395B64),
                                uncheckedColor = Color(0xFF666666)
                            )
                        )
                        Text(
                            text = "Remember Me",
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }

                    // Forgot Password Text Button
                    Text(
                        text = "Forgot Password?",
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF395B64),
                        modifier = Modifier.clickable {
                            showResetDialog = true
                        }
                    )
                }
            }

            // Sign In Button - Round cornered
            item {
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email, password)
                        } else {
                            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF395B64)
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Create Account Text
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Don't have an account? ",
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFFA5C9CA)
                    )
                    Text(
                        text = "Create an account",
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF395B64),
                        modifier = Modifier.clickable {
                            // Navigate to sign up screen
                            Toast.makeText(context, "Navigate to Sign Up", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Add some bottom padding for keyboard handling
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Password Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    "Reset Password",
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your email address to receive a password reset link.",
                        fontFamily = AppFonts.KarlaFontFamily
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = {
                            Text(
                                "Email",
                                fontFamily = AppFonts.KarlaFontFamily
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = AppFonts.KarlaFontFamily,
                            color = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            viewModel.resetPassword(resetEmail)
                        }
                    }
                ) {
                    Text(
                        "Send Reset Link",
                        fontFamily = AppFonts.KarlaFontFamily,
                        color = Color(0xFF395B64)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        "Cancel",
                        fontFamily = AppFonts.KarlaFontFamily,
                        color = Color(0xFF666666)
                    )
                }
            }
        )
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
