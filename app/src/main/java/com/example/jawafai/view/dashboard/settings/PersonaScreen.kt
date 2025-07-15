package com.example.jawafai.view.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.ui.theme.AppFonts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Composable screen for the Persona Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // States for persona data
    val isLoading = remember { mutableStateOf(true) }
    val personaData = remember { mutableStateOf<Map<String, String>>(mapOf()) }
    val currentAnswers = remember { mutableStateOf<Map<String, String>>(mapOf()) }

    // Load persona data on first composition
    LaunchedEffect(Unit) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("persona")
                    .get()
                    .await()

                val data = mutableMapOf<String, String>()
                for (doc in snapshot.documents) {
                    doc.id.let { questionId ->
                        doc.getString("answer")?.let { answer ->
                            data[questionId] = answer
                        }
                    }
                }
                personaData.value = data
                currentAnswers.value = data.toMutableMap()
            }
        } catch (e: Exception) {
            // Handle error
            snackbarHostState.showSnackbar("Failed to load persona data: ${e.message}")
        } finally {
            isLoading.value = false
        }
    }

    // Define personality questions
    val questions = remember {
        listOf(
            PersonaQuestion(
                id = "communication_style",
                prompt = "How would you describe your communication style?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("Direct and brief", "Detailed and thorough", "Casual and friendly", "Formal and professional")
            ),
            PersonaQuestion(
                id = "decision_making",
                prompt = "How do you typically make decisions?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("Analytical and logical", "Based on feelings and intuition", "Considering others' opinions", "Quick and decisive")
            ),
            PersonaQuestion(
                id = "learning_preference",
                prompt = "What's your preferred learning style?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("Visual (seeing)", "Auditory (hearing)", "Reading/Writing", "Kinesthetic (doing)")
            ),
            PersonaQuestion(
                id = "interests",
                prompt = "What are your main interests or hobbies?",
                type = QuestionType.FREE_TEXT
            ),
            PersonaQuestion(
                id = "goals",
                prompt = "What are your current personal or professional goals?",
                type = QuestionType.FREE_TEXT
            ),
            PersonaQuestion(
                id = "challenges",
                prompt = "What challenges are you currently facing?",
                type = QuestionType.FREE_TEXT
            ),
            PersonaQuestion(
                id = "ideal_day",
                prompt = "Describe your ideal day in a few sentences.",
                type = QuestionType.FREE_TEXT
            ),
            PersonaQuestion(
                id = "stress_response",
                prompt = "How do you typically respond to stress?",
                type = QuestionType.SINGLE_CHOICE,
                options = listOf("Take action and solve problems", "Seek support from others", "Take time to reflect", "Distract myself with activities")
            )
        )
    }

    // Save function
    val savePersona = {
        coroutineScope.launch {
            try {
                isLoading.value = true
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                if (userId != null) {
                    val personaRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("persona")

                    // Delete existing answers (to clean up any that are no longer used)
                    val existingDocs = personaRef.get().await()
                    for (doc in existingDocs) {
                        personaRef.document(doc.id).delete().await()
                    }

                    // Add new answers
                    for ((questionId, answer) in currentAnswers.value) {
                        if (answer.isNotBlank()) {
                            personaRef.document(questionId).set(mapOf("answer" to answer)).await()
                        }
                    }

                    // Update user's personaCompleted field if they answered at least 5 questions
                    if (currentAnswers.value.count { it.value.isNotBlank() } >= 5) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .update("personaCompleted", true)
                            .await()
                    }

                    snackbarHostState.showSnackbar("Persona saved successfully!")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to save: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Persona",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF395B64)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { savePersona() },
                        enabled = !isLoading.value,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64)
                        )
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF395B64)
                                )
                                Text(
                                    text = "Loading your persona...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Introduction card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Personalize Your Experience",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = AppFonts.KarlaFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color(0xFF395B64)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tell us about yourself so we can personalize your AI responses to match your style and preferences.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Questions
                    items(questions.size) { index ->
                        val question = questions[index]
                        PersonaQuestionCard(
                            question = question,
                            answer = currentAnswers.value[question.id] ?: "",
                            onAnswerChanged = { answer ->
                                val updatedAnswers = currentAnswers.value.toMutableMap()
                                updatedAnswers[question.id] = answer
                                currentAnswers.value = updatedAnswers
                            }
                        )
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.navigationBarsPadding())
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PersonaQuestionCard(
    question: PersonaQuestion,
    answer: String,
    onAnswerChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Question prompt
            Text(
                text = question.prompt,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Answer input based on question type
            when (question.type) {
                QuestionType.SINGLE_CHOICE -> {
                    Column(
                        modifier = Modifier
                            .selectableGroup()
                            .fillMaxWidth()
                    ) {
                        question.options?.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (option == answer),
                                        onClick = { onAnswerChanged(option) },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (option == answer),
                                    onClick = null, // null because the parent is selectable
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF395B64),
                                        unselectedColor = Color(0xFF666666)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 14.sp,
                                        color = Color(0xFF395B64)
                                    )
                                )
                            }
                        }
                    }
                }
                QuestionType.FREE_TEXT -> {
                    OutlinedTextField(
                        value = answer,
                        onValueChange = onAnswerChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Your answer",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                                    color = Color(0xFF666666)
                                )
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF395B64),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedTextColor = Color(0xFF395B64),
                            unfocusedTextColor = Color(0xFF395B64),
                            cursorColor = Color(0xFF395B64)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

// Data models for persona questions
data class PersonaQuestion(
    val id: String,
    val prompt: String,
    val type: QuestionType,
    val options: List<String>? = null,
    val required: Boolean = true
)

enum class QuestionType {
    SINGLE_CHOICE, FREE_TEXT
}
