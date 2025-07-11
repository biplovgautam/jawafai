package com.example.jawafai.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jawafai.model.PersonaQuestions
import com.example.jawafai.model.QuestionType
import com.example.jawafai.viewmodel.PersonaViewModel
import com.example.jawafai.viewmodel.PersonaViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

/**
 * Composable screen for the Persona Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    onNavigateBack: () -> Unit,
    viewModel: PersonaViewModel = viewModel(factory = PersonaViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentAnswers by viewModel.currentAnswers.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personality Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.savePersona()
                            result.fold(
                                onSuccess = { message ->
                                    snackbarHostState.showSnackbar(message)
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(
                                        error.message ?: "Failed to save persona"
                                    )
                                }
                            )
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Save")
                }
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is PersonaViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is PersonaViewModel.UiState.Success -> {
                PersonaQuestionsContent(
                    modifier = Modifier.padding(paddingValues),
                    currentAnswers = currentAnswers,
                    onAnswerChanged = viewModel::updateAnswer
                )
            }

            is PersonaViewModel.UiState.Error -> {
                val errorMessage = (uiState as PersonaViewModel.UiState.Error).message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $errorMessage")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadPersona() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonaQuestionsContent(
    modifier: Modifier = Modifier,
    currentAnswers: Map<String, String>,
    onAnswerChanged: (String, String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(PersonaQuestions.questions) { question ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                when (question.type) {
                    QuestionType.SINGLE_CHOICE -> {
                        val selectedOption = currentAnswers[question.id] ?: ""

                        Column(
                            modifier = Modifier
                                .selectableGroup()
                                .fillMaxWidth()
                        ) {
                            question.options?.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .selectable(
                                            selected = option == selectedOption,
                                            onClick = { onAnswerChanged(question.id, option) },
                                            role = Role.RadioButton
                                        )
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = option == selectedOption,
                                        onClick = null // null because the parent is handling the click
                                    )
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    QuestionType.FREE_TEXT -> {
                        val answer = currentAnswers[question.id] ?: ""

                        OutlinedTextField(
                            value = answer,
                            onValueChange = { onAnswerChanged(question.id, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Your answer") },
                            minLines = 3
                        )
                    }
                }

                if (question.required) {
                    Text(
                        text = "* Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Add some padding at the bottom to ensure content isn't hidden by the bottom bar
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
