package com.example.jawafai.view.dashboard.chat

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jawafai.model.ChatMessage
import com.example.jawafai.repository.ChatRepositoryImpl
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.viewmodel.ChatViewModel
import com.example.jawafai.viewmodel.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

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
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    // Handle back press properly
    DisposableEffect(backDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onNavigateBack()
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    val messages by viewModel.messages.collectAsState()
    val typingStatus by viewModel.typingStatus.collectAsState()
    var newMessageText by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Get user info for the other user
    var otherUserName by remember { mutableStateOf("Chat") }
    var otherUserImageUrl by remember { mutableStateOf<String?>(null) }

    // Load messages when screen opens
    LaunchedEffect(chatId, currentUserId) {
        if (currentUserId != null) {
            try {
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
                otherUserImageUrl = userProfile.profileImageUrl
                println("🔍 DEBUG: ChatDetailScreen - Set user name to: $otherUserName")
            } else {
                println("🔍 DEBUG: ChatDetailScreen - Could not find user with ID: $otherUserId")
                otherUserName = "Unknown User"
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: ChatDetailScreen - Error loading user info: ${e.message}")
            otherUserName = "Unknown User"
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Profile Picture
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA5C9CA))
                        ) {
                            if (otherUserImageUrl != null) {
                                AsyncImage(
                                    model = otherUserImageUrl,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Avatar",
                                    modifier = Modifier.size(20.dp).align(Alignment.Center),
                                    tint = Color.White
                                )
                            }
                        }

                        // User name and typing indicator
                        Column {
                            Text(
                                text = otherUserName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = AppFonts.KarlaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF395B64)
                                )
                            )

                            // Show typing indicator in the title area
                            if (typingStatus?.isTyping == true) {
                                Text(
                                    text = "typing...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFF4CAF50),
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
            color = Color(0xFFF0F0F0),
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated dots for typing indicator
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ), label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF666666).copy(alpha = alpha))
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
                Color(0xFF395B64)
            } else {
                Color(0xFFF0F0F0)
            },
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        color = if (isFromCurrentUser) {
                            Color.White
                        } else {
                            Color(0xFF333333)
                        }
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 12.sp,
                            color = if (isFromCurrentUser) {
                                Color.White.copy(alpha = 0.7f)
                        } else {
                            Color(0xFF666666)
                        }
                        )
                    )

                    // Show "seen" indicator for sent messages
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (message.seen) "✓✓" else "✓",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 10.sp,
                                color = if (message.seen) {
                                    Color(0xFF4CAF50) // Green for seen
                                } else {
                                    Color.White.copy(alpha = 0.7f)
                                }
                            )
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
        color = Color.White,
        shadowElevation = 8.dp
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            color = Color(0xFF666666)
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF395B64),
                    unfocusedTextColor = Color(0xFF395B64),
                    focusedBorderColor = Color(0xFF395B64),
                    unfocusedBorderColor = Color(0xFF666666).copy(alpha = 0.3f),
                    cursorColor = Color(0xFF395B64),
                    focusedPlaceholderColor = Color(0xFF666666),
                    unfocusedPlaceholderColor = Color(0xFF666666).copy(alpha = 0.7f)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                onClick = onSendClick,
                enabled = value.isNotBlank(),
                shape = CircleShape,
                color = if (value.isNotBlank()) {
                    Color(0xFF395B64)
                } else {
                    Color(0xFF666666).copy(alpha = 0.3f)
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
