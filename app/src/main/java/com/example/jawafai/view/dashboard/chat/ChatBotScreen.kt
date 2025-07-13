package com.example.jawafai.view.dashboard.chat

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.jawafai.R
import com.example.jawafai.model.ChatBotMessageModel
import com.example.jawafai.model.ChatBotMessageType
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initialize ViewModel using existing pattern from HomeScreen
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val repository = UserRepositoryImpl(auth, firestore)
    val userViewModel = remember { UserViewModelFactory(repository, auth).create(UserViewModel::class.java) }

    // State for chatbot functionality
    var messages by remember { mutableStateOf<List<ChatBotMessageModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Function to send messages with real Groq API integration
    fun sendMessage(message: String) {
        // Add user message
        val userMessage = ChatBotMessageModel(
            id = java.util.UUID.randomUUID().toString(),
            conversationId = "current_conversation",
            message = message,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        messages = messages + userMessage
        isTyping = true

        // Use GroqApiManager for real AI responses
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // Convert existing messages to GroqApiManager format
                val chatHistory = com.example.jawafai.managers.GroqApiManager.convertToChatMessages(messages.dropLast(1))

                // Get response from Groq API
                val response = com.example.jawafai.managers.GroqApiManager.getChatResponse(
                    userMessage = message,
                    conversationHistory = chatHistory,
                    userPersona = null // Could fetch user persona here
                )

                val aiMessage = if (response.success && response.message != null) {
                    ChatBotMessageModel(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = "current_conversation",
                        message = response.message,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                } else {
                    // Show detailed error for debugging
                    val errorMsg = response.error ?: "Unknown error occurred"
                    Log.e("ChatBotScreen", "API Error: $errorMsg")

                    ChatBotMessageModel(
                        id = java.util.UUID.randomUUID().toString(),
                        conversationId = "current_conversation",
                        message = "Debug: API Error - $errorMsg. Please check logs for details.",
                        isFromUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                }

                messages = messages + aiMessage
                isTyping = false
            } catch (e: Exception) {
                Log.e("ChatBotScreen", "Exception in sendMessage: ${e.message}", e)
                val errorMessage = ChatBotMessageModel(
                    id = java.util.UUID.randomUUID().toString(),
                    conversationId = "current_conversation",
                    message = "Debug: Exception - ${e.message}. Please check logs for details.",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                messages = messages + errorMessage
                isTyping = false
            }
        }
    }

    // Function to create new conversation
    fun createNewConversation() {
        messages = emptyList()
        messageText = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Fixed Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Bot Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF395B64))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "AI Bot",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }

                    Column {
                        Text(
                            text = "AI Companion",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF395B64)
                            )
                        )

                        AnimatedVisibility(visible = isTyping) {
                            Text(
                                text = "Typing...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF395B64)
                    )
                }
            },
            actions = {
                // History button
                IconButton(
                    onClick = { onNavigateToHistory() }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Chat History",
                        tint = Color(0xFF395B64)
                    )
                }

                // New chat button
                IconButton(
                    onClick = {
                        createNewConversation()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Color(0xFF395B64)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // Chat Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isLoading) {
                EmptyStateView(
                    onSuggestionClick = { suggestion ->
                        sendMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
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

        // Fixed Message Input Bar
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
}

@Composable
fun MessageItem(message: ChatBotMessageModel) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) Color(0xFF395B64) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = if (isUser) Color.White else Color(0xFF333333)
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Text(
                text = formatChatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFF999999)
                ),
                textAlign = if (isUser) TextAlign.End else TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (!isUser) {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "typing_dot_$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF395B64).copy(alpha = alpha))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(48.dp))
    }
}

@Composable
fun MessageInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Type your message...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { onSendMessage() }
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (messageText.isBlank() || isLoading) Color(0xFFCCCCCC) else Color(0xFF395B64)
                    )
                    .clickable(enabled = messageText.isNotBlank() && !isLoading) {
                        onSendMessage()
                    }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(onSuggestionClick: (String) -> Unit) {
    // Lottie animation for chatbot
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.live_chatbot))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hello! I'm Your AI Companion 💙",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = AppFonts.KarlaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF395B64)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "I'm here to listen, support, and help you through whatever you're going through. Let's have a meaningful conversation!",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = AppFonts.KaiseiDecolFontFamily,
                fontSize = 15.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Emotional Support Suggestions
        SuggestionCard(
            title = "💭 Let's Talk",
            suggestions = listOf(
                "I'm feeling low today",
                "Tell me a joke to cheer me up",
                "I need some motivation"
            ),
            onSuggestionClick = onSuggestionClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Productivity Suggestions
        SuggestionCard(
            title = "📅 Daily Support",
            suggestions = listOf(
                "Help me plan my day",
                "I'm feeling overwhelmed with tasks",
                "Give me some productivity tips"
            ),
            onSuggestionClick = onSuggestionClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Learning & Fun Suggestions
        SuggestionCard(
            title = "🌟 Learn & Explore",
            suggestions = listOf(
                "Tell me an interesting scientific fact",
                "Explain something fascinating",
                "Share some life wisdom"
            ),
            onSuggestionClick = onSuggestionClick
        )
    }
}

@Composable
fun SuggestionCard(
    title: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF395B64)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            onSuggestionClick(suggestion)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF555555)
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun formatChatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
