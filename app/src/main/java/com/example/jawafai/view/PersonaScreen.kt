package com.example.jawafai.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

                    snackbarHostState.showSnackbar("Persona saved!")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to save: ${e.message}")
            } finally {
                isLoading.value = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Persona") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Save button in top bar
                    Button(
                        onClick = { savePersona() },
                        enabled = !isLoading.value
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Introduction text
                    item {
                        Text(
                            "Tell us about yourself so we can personalize your experience",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Questions
                    items(questions.size) { index ->
                        val question = questions[index]
                        PersonaQuestionItem(
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
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun PersonaQuestionItem(
    question: PersonaQuestion,
    answer: String,
    onAnswerChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Question prompt
        Text(
            text = question.prompt,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                                .height(48.dp)
                                .selectable(
                                    selected = (option == answer),
                                    onClick = { onAnswerChanged(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == answer),
                                onClick = null // null because the parent is selectable
                            )
                            Text(
                                text = option,
                                modifier = Modifier.padding(start = 16.dp)
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
                    placeholder = { Text("Your answer") },
                    minLines = 3
                )
            }
        }

        // Show "required" text if question is required
        if (question.required) {
            Text(
                text = "* Required",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
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
