package com.example.jawafai.view.dashboard.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jawafai.model.ChatBotMessageModel
import com.example.jawafai.ui.theme.AppFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotConversationScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    // Local state for this conversation
    var messages by remember { mutableStateOf<List<ChatBotMessageModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var conversationTitle by remember { mutableStateOf("Chat") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Load conversation data
    LaunchedEffect(conversationId) {
        isLoading = true
        // Simulate loading
        delay(500)

        // Set conversation title based on ID (in real implementation, fetch from database)
        conversationTitle = when (conversationId) {
            "1" -> "Weather and Travel Tips"
            "2" -> "Recipe Suggestions"
            "3" -> "Programming Help"
            else -> "Chat"
        }

        // Sample messages for this conversation
        messages = listOf(
            ChatBotMessageModel(
                id = "1",
                conversationId = conversationId,
                message = "Hello! How can I help you today?",
                isFromUser = false,
                timestamp = System.currentTimeMillis() - 120000
            ),
            ChatBotMessageModel(
                id = "2",
                conversationId = conversationId,
                message = "Can you help me with $conversationTitle?",
                isFromUser = true,
                timestamp = System.currentTimeMillis() - 60000
            ),
            ChatBotMessageModel(
                id = "3",
                conversationId = conversationId,
                message = "Of course! I'd be happy to help you with that. What specific aspect would you like to know more about?",
                isFromUser = false,
                timestamp = System.currentTimeMillis() - 30000
            )
        )
        isLoading = false
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Function to send messages
    fun sendMessage(message: String) {
        // Add user message
        val userMessage = ChatBotMessageModel(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = conversationId,
            message = message,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        messages = messages + userMessage
        isTyping = true

        // Simulate AI response
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            delay(2000)
            val aiMessage = ChatBotMessageModel(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                message = "This is a response to: \"$message\" in conversation: $conversationTitle",
                isFromUser = false,
                timestamp = System.currentTimeMillis()
            )
            messages = messages + aiMessage
            isTyping = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversationTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF395B64)
                            )
                        )

                        AnimatedVisibility(visible = isTyping) {
                            Text(
                                text = "AI is typing...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            )
                        }
                    }
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
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Chat History",
                            tint = Color(0xFF395B64)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            MessageInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        sendMessage(messageText)
                        messageText = ""
                        keyboardController?.hide()
                    }
                },
                isLoading = isTyping
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF395B64))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(messages) { message ->
                    MessageItem(message = message)
                }

                // Show typing indicator
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
