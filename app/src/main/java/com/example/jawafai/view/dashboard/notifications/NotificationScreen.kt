package com.example.jawafai.view.dashboard.notifications

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jawafai.R
import com.example.jawafai.service.NotificationMemoryStore
import com.example.jawafai.service.NotificationAIReplyManager
import com.example.jawafai.service.SmartReplyAIModule
import com.example.jawafai.ui.theme.AppFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data classes for chat notifications
data class ChatNotification(
    val id: String,
    val platform: ChatPlatform,
    val senderName: String,
    val senderAvatar: String?,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val hasGeneratedReply: Boolean = false,
    val generatedReply: String = "",
    val hasReplyAction: Boolean = false,
    val isSent: Boolean = false,
    val conversationId: String = "",
    val notificationHash: String = ""
)

enum class ChatPlatform(
    val displayName: String,
    val color: Color,
    val iconRes: Int? = null
) {
    WHATSAPP("WhatsApp", Color(0xFF25D366)),
    INSTAGRAM("Instagram", Color(0xFFE4405F)),
    MESSENGER("Messenger", Color(0xFF0084FF)),
    TELEGRAM("Telegram", Color(0xFF0088CC)),
    SMS("SMS", Color(0xFF34C759)),
    GENERAL("General", Color(0xFF395B64))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // State management
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<ChatPlatform?>(null) }
    var generatingReplyFor by remember { mutableStateOf<String?>(null) }

    // Observe external notifications from NotificationMemoryStore
    val externalNotifications by remember {
        derivedStateOf {
            NotificationMemoryStore.getAllNotifications()
        }
    }

    // Map external notifications to ChatNotification for display
    val liveNotifications = remember(externalNotifications) {
        externalNotifications.map { notification ->
            ChatNotification(
                id = notification.hash,
                platform = when {
                    notification.packageName.contains("whatsapp", true) -> ChatPlatform.WHATSAPP
                    notification.packageName.contains("instagram", true) -> ChatPlatform.INSTAGRAM
                    notification.packageName.contains("messenger", true) ||
                    notification.packageName.contains("facebook.orca", true) -> ChatPlatform.MESSENGER
                    notification.packageName.contains("telegram", true) -> ChatPlatform.TELEGRAM
                    notification.packageName.contains("sms", true) -> ChatPlatform.SMS
                    else -> ChatPlatform.GENERAL
                },
                senderName = notification.sender?.takeIf { it.isNotBlank() } ?: notification.title.ifBlank { notification.packageName },
                senderAvatar = null,
                message = notification.text,
                timestamp = notification.time,
                isRead = false,
                hasGeneratedReply = notification.ai_reply.isNotBlank(),
                generatedReply = notification.ai_reply,
                hasReplyAction = notification.hasReplyAction,
                isSent = notification.is_sent,
                conversationId = notification.conversationId,
                notificationHash = notification.hash
            )
        }
    }

    // Filter notifications based on search and platform filter
    val filteredNotifications = remember(searchQuery, selectedFilter, liveNotifications) {
        liveNotifications.filter { notification ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                notification.senderName.contains(searchQuery, ignoreCase = true) ||
                notification.message.contains(searchQuery, ignoreCase = true) ||
                notification.platform.displayName.contains(searchQuery, ignoreCase = true)
            }

            val matchesFilter = selectedFilter?.let { filter ->
                notification.platform == filter
            } ?: true

            matchesSearch && matchesFilter
        }
    }

    // Function to generate AI reply
    fun generateAIReply(notificationHash: String) {
        coroutineScope.launch {
            try {
                generatingReplyFor = notificationHash

                // Get the notification from memory store
                val notification = NotificationMemoryStore.getAllNotifications()
                    .find { it.hash == notificationHash }

                if (notification != null) {
                    // Generate AI reply
                    val result = NotificationAIReplyManager.generateAIReply(
                        notification = notification,
                        userPersona = null, // You can pass user persona here
                        context = context
                    )

                    if (result.success && result.reply != null) {
                        // Reply is already stored in NotificationMemoryStore by the manager
                        // Just update the UI state
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                generatingReplyFor = null
            }
        }
    }

    // Function to send reply
    fun sendReply(conversationId: String, replyText: String) {
        coroutineScope.launch {
            try {
                val notifications = NotificationMemoryStore.getNotificationsByConversation(conversationId)
                val latestNotification = notifications.firstOrNull { it.hasReplyAction }

                if (latestNotification != null) {
                    // Update the notification as sent
                    NotificationMemoryStore.markAsSent(latestNotification.hash)
                } else {
                    // Show error toast if no remote input available
                    Toast.makeText(context, "Can't send message: This notification doesn't support direct replies.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Pull to refresh simulation
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1000) // Simulate refresh
            isRefreshing = false
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
                    Text(
                        text = "Smart Notifications",
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF395B64)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Mark all as read functionality
                        }
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Mark all as read",
                            tint = Color(0xFF395B64)
                        )
                    }
                    IconButton(
                        onClick = {
                            isRefreshing = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF395B64)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            SearchBarContent(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    isSearchActive = it.isNotEmpty()
                },
                onClear = {
                    searchQuery = ""
                    isSearchActive = false
                    keyboardController?.hide()
                },
                isActive = isSearchActive,
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Platform Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { selectedFilter = null },
                        label = { Text("All") },
                        selected = selectedFilter == null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF395B64),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                items(ChatPlatform.values().toList()) { platform ->
                    FilterChip(
                        onClick = {
                            selectedFilter = if (selectedFilter == platform) null else platform
                        },
                        label = { Text(platform.displayName) },
                        selected = selectedFilter == platform,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = platform.color,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Loading indicator
            AnimatedVisibility(visible = isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF395B64),
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Notifications List
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "No notifications",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF666666).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No notifications found" else "No notifications yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotifications) { notification ->
                        EnhancedNotificationCard(
                            notification = notification,
                            onGenerateReply = { generateAIReply(notification.notificationHash) },
                            onSendReply = { sendReply(notification.conversationId, notification.generatedReply) },
                            onMarkAsRead = { /* Handle mark as read */ },
                            isGeneratingReply = generatingReplyFor == notification.notificationHash
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isActive: Boolean,
    focusRequester: FocusRequester
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search notifications...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    color = Color(0xFF666666)
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF666666)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color(0xFF666666)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF395B64),
            unfocusedTextColor = Color(0xFF395B64),
            cursorColor = Color(0xFF395B64),
            focusedBorderColor = Color(0xFF395B64),
            unfocusedBorderColor = Color(0xFF666666).copy(alpha = 0.3f),
            focusedPlaceholderColor = Color(0xFF666666),
            unfocusedPlaceholderColor = Color(0xFF666666).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun EnhancedNotificationCard(
    notification: ChatNotification,
    onGenerateReply: () -> Unit,
    onSendReply: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    isGeneratingReply: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F8FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with platform and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(notification.platform.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notification.platform.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = notification.platform.color
                        )
                    )

                    // Reply action indicator
                    if (notification.hasReplyAction) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Reply Available",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sender and message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA5C9CA))
                ) {
                    if (notification.senderAvatar != null) {
                        AsyncImage(
                            model = notification.senderAvatar,
                            contentDescription = "Sender Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Message content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.senderName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF395B64)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF395B64))
                    )
                }
            }

            // AI Generated Reply Section
            if (notification.hasGeneratedReply && notification.generatedReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F8FF).copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Generated",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF395B64)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Generated Reply",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFonts.KarlaFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF395B64)
                                )
                            )

                            if (notification.isSent) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = notification.generatedReply,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 14.sp,
                                color = Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // Action buttons
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Generate Reply Button
                if (!notification.hasGeneratedReply && !isGeneratingReply) {
                    Button(
                        onClick = onGenerateReply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate Reply",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Generate Reply",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Loading state for reply generation
                if (isGeneratingReply) {
                    Button(
                        onClick = { /* Do nothing while generating */ },
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64).copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Generating...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Send Reply Button (only if reply is generated, has reply action, and not sent)
                if (notification.hasGeneratedReply &&
                    notification.hasReplyAction &&
                    !notification.isSent &&
                    !isGeneratingReply) {
                    Button(
                        onClick = onSendReply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(40.dp) // Make the button square
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reply",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                }

                // Regenerate Reply Button (if reply exists but want to regenerate)
                if (notification.hasGeneratedReply && !isGeneratingReply) {
                    OutlinedButton(
                        onClick = onGenerateReply,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF395B64)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Regenerate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Mark as Read Button
                if (!notification.isRead) {
                    OutlinedButton(
                        onClick = { onMarkAsRead(notification.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF666666)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Mark as Read",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark Read",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format timestamps
@Composable
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 48 * 60 * 60 * 1000 -> "yesterday"
        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
