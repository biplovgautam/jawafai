package com.example.jawafai.view

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.jawafai.R
import com.example.jawafai.model.UserModel
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.JawafaiTheme
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.text.SimpleDateFormat
import kotlinx.coroutines.delay

class RegistrationActivity : ComponentActivity() {
    private lateinit var viewModel: UserViewModel
    private val TAG = "RegistrationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Activity created")

        // Initialize Firebase components
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val repository = UserRepositoryImpl(auth, firestore)

        // Initialize ViewModel
        viewModel = UserViewModelFactory(repository, auth).create(UserViewModel::class.java)
        Log.d(TAG, "onCreate: ViewModel initialized")

        setContent {
            val navController = rememberNavController()
            JawafaiTheme {
                RegistrationScreen(
                    navController = navController,
                    viewModel = viewModel,
                    onSuccessfulRegistration = {
                        Log.d(TAG, "onSuccessfulRegistration: Navigating to login screen")
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // Close registration activity
                    }
                )
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    navController: NavController,
    viewModel: UserViewModel = viewModel(),
    onSuccessfulRegistration: () -> Unit = {}
) {
    val TAG = "RegistrationScreen"
    val context = LocalContext.current

    // State variables for form fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var acceptTerms by remember { mutableStateOf(false) }

    // State for showing loading indicator
    var isLoading by remember { mutableStateOf(false) }

    // Observe ViewModel state using observeAsState from runtime-livedata
    val userOperationState = viewModel.userState.observeAsState(initial = UserViewModel.UserOperationResult.Initial)

    // Display current state for debugging
    val currentState = userOperationState.value.toString()
    Log.d(TAG, "Current state: $currentState")

    // Remove registrationSuccess logic and handle navigation and spinner in LaunchedEffect
    LaunchedEffect(userOperationState.value) {
        when (val state = userOperationState.value) {
            is UserViewModel.UserOperationResult.Success -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                delay(1500)
                onSuccessfulRegistration()
            }
            is UserViewModel.UserOperationResult.Error -> {
                isLoading = false
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is UserViewModel.UserOperationResult.Loading -> {
                isLoading = true
            }
            else -> {}
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Date picker setup with improved handling
    val calendar = remember { Calendar.getInstance() }
    // Default to 18 years ago
    calendar.add(Calendar.YEAR, -18)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Calculated states for date picker
    val year = remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    val month = remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    val day = remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    // Dialog control
    var showDatePicker by remember { mutableStateOf(false) }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                year.value = selectedYear
                month.value = selectedMonth
                day.value = selectedDay

                // Update the calendar with selected date
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                // Format the date and update dob
                dob = dateFormatter.format(selectedCalendar.time)

                // Close dialog
                showDatePicker = false
            },
            year.value,
            month.value,
            day.value
        ).apply {
            // Set date constraints - minimum age 13, maximum age 100
            val minAgeCalendar = Calendar.getInstance()
            minAgeCalendar.add(Calendar.YEAR, -13)
            datePicker.maxDate = minAgeCalendar.timeInMillis

            val maxAgeCalendar = Calendar.getInstance()
            maxAgeCalendar.add(Calendar.YEAR, -100)
            datePicker.minDate = maxAgeCalendar.timeInMillis

            show()
        }
    }

    // Main UI
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF006064)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.clickable { permissionLauncher.launch(permission) }
                ) {
                    AsyncImage(
                        model = imageUri ?: R.drawable.profile,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Add Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(6.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Register",
                    fontSize = 32.sp,
                    color = Color(0xFF004D40),
                    fontFamily = AppFonts.KaiseiRegularFontFamily // Use the centralized font
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            item {
                OutlinedTextField(
                    value = dob,
                    onValueChange = { /* Read only, handled by dialog */ },
                    label = { Text("Date of Birth", fontFamily = AppFonts.KaiseiRegularFontFamily) },
                    placeholder = { Text("Select your birth date") },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                            contentDescription = "Select date",
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                if (!isLoading) showDatePicker = true
                            }
                        )
                    },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) {
                            if (!isLoading) showDatePicker = true
                        },
                    supportingText = { Text("Age must be at least 13 years") },
                    enabled = !isLoading
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { if (!isLoading) acceptTerms = it },
                        enabled = !isLoading
                    )
                    Text("Accept Terms & Conditions", fontFamily = AppFonts.KaiseiRegularFontFamily)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isLoading) return@Button

                        if (!acceptTerms) {
                            Toast.makeText(context, "Accept terms to proceed", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Field validation
                        when {
                            firstName.isBlank() -> Toast.makeText(context, "First name is required", Toast.LENGTH_SHORT).show()
                            lastName.isBlank() -> Toast.makeText(context, "Last name is required", Toast.LENGTH_SHORT).show()
                            username.isBlank() -> Toast.makeText(context, "Username is required", Toast.LENGTH_SHORT).show()
                            email.isBlank() -> Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
                            !email.contains('@') -> Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                            password.length < 6 -> Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            dob.isBlank() -> Toast.makeText(context, "Date of birth is required", Toast.LENGTH_SHORT).show()
                            else -> {
                                isLoading = true
                                val registerUser = { imageUrl: String? ->
                                    val userModel = UserModel(
                                        firstName = firstName,
                                        lastName = lastName,
                                        username = username,
                                        email = email,
                                        dateOfBirth = dob,
                                        bio = bio,
                                        imageUrl = imageUrl
                                    )
                                    viewModel.register(email, password, userModel)
                                }

                                if (imageUri != null) {
                                    Log.d("RegistrationActivity", "Starting image upload: $imageUri")
                                    viewModel.uploadProfileImage(context, imageUri!!) { imageUrl ->
                                        Log.d("RegistrationActivity", "Upload callback received, result: ${imageUrl != null}")
                                        if (imageUrl != null) {
                                            Log.d("RegistrationActivity", "Upload successful, URL: $imageUrl")
                                            registerUser(imageUrl)
                                        } else {
                                            Log.e("RegistrationActivity", "Image upload returned null URL")
                                            isLoading = false
                                            Toast.makeText(context, "Image upload failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Log.d("RegistrationActivity", "No image to upload, proceeding with registration")
                                    registerUser(null)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006064)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Register", color = Color.White, fontSize = 18.sp, fontFamily = AppFonts.KaiseiRegularFontFamily)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Already have an account? Sign In",
                    color = Color.Blue,
                    fontFamily = AppFonts.KaiseiRegularFontFamily,
                    modifier = Modifier.clickable(enabled = !isLoading) {
                        if (!isLoading) {
                            val intent = Intent(context, LoginActivity::class.java)
                            context.startActivity(intent)
                            if (context is ComponentActivity) {
                                context.finish() // Close registration activity
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegistrationScreenPreview() {
    val navController = rememberNavController()
    JawafaiTheme {
        RegistrationScreen(navController = navController)
    }
}
