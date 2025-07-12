package com.example.jawafai.view.dashboard.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jawafai.model.ChatMessage
import com.example.jawafai.repository.ChatRepositoryImpl
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.viewmodel.ChatViewModel
import com.example.jawafai.viewmodel.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// UI constants
private val darkTeal = Color(0xFF365A61)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    otherUserId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            ChatRepositoryImpl(),
            UserRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()),
            FirebaseAuth.getInstance()
        )
    )
) {
    val messages by viewModel.messages.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()
    var newMessageText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Get user info for the other user
    var otherUserName by remember { mutableStateOf("Chat") }
    var isLoadingUserInfo by remember { mutableStateOf(true) }

    // Load messages when screen opens
    LaunchedEffect(chatId, currentUserId) {
        if (currentUserId != null) {
            try {
                // Use the new method signature: getMessagesForChat(senderId, receiverId)
                viewModel.getMessagesForChat(currentUserId, otherUserId)
                viewModel.markMessagesAsSeen(currentUserId, otherUserId)
            } catch (e: Exception) {
                println("🔍 DEBUG: ChatDetailScreen - Error loading messages: ${e.message}")
            }
        }
    }

    // Load user info for the other user
    LaunchedEffect(otherUserId) {
        try {
            println("🔍 DEBUG: ChatDetailScreen - Looking up user with ID: $otherUserId")
            val userProfile = viewModel.findUserById(otherUserId)
            if (userProfile != null) {
                otherUserName = userProfile.displayName.ifEmpty {
                    userProfile.username.ifEmpty { userProfile.email }
                }
                println("🔍 DEBUG: ChatDetailScreen - Set user name to: $otherUserName")
            } else {
                println("🔍 DEBUG: ChatDetailScreen - Could not find user with ID: $otherUserId")
                otherUserName = "Unknown User"
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: ChatDetailScreen - Error loading user info: ${e.message}")
            otherUserName = "Unknown User"
        } finally {
            isLoadingUserInfo = false
        }
    }

    Scaffold(
        containerColor = darkTeal,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = otherUserName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        // Show typing indicator in the title area
                        if (typingStatus?.isTyping == true) {
                            Text(
                                text = "typing...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkTeal
                )
            )
        },
        bottomBar = {
            MessageInput(
                value = newMessageText,
                onValueChange = { newText ->
                    newMessageText = newText
                    // Trigger typing indicator
                    viewModel.onTextChanged(otherUserId, newText)
                },
                onSendClick = {
                    if (newMessageText.isNotBlank()) {
                        viewModel.sendMessage(otherUserId, newMessageText)
                        newMessageText = ""
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(darkTeal)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Add typing indicator as a message bubble at the bottom
            if (typingStatus?.isTyping == true) {
                item {
                    TypingIndicatorBubble()
                }
            }

            items(messages.sortedByDescending { it.timestamp }) { message ->
                MessageBubble(
                    message = message,
                    isFromCurrentUser = message.senderId == currentUserId
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 20.dp
            ),
            color = Color.White.copy(alpha = 0.15f),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated dots for typing indicator
                repeat(3) { index ->
                    val alpha by rememberInfiniteTransition(label = "typing").animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ), label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = alpha))
                    )

                    if (index < 2) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isFromCurrentUser: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isFromCurrentUser) 20.dp else 4.dp,
                bottomEnd = if (isFromCurrentUser) 4.dp else 20.dp
            ),
            color = if (isFromCurrentUser) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.White.copy(alpha = 0.15f)
            },
            tonalElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isFromCurrentUser) {
                        Color.White
                    } else {
                        Color.White
                    },
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        color = if (isFromCurrentUser) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            Color.White.copy(alpha = 0.6f)
                        },
                        fontSize = 12.sp
                    )

                    // Show "seen" indicator for sent messages
                    if (isFromCurrentUser && message.seen) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "✓✓",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = darkTeal,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onSendClick,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                color = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
