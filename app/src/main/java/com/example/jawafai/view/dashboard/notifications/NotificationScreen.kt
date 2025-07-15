package com.example.jawafai.view.dashboard.notifications

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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jawafai.R
import com.example.jawafai.service.NotificationMemoryStore
import com.example.jawafai.ui.theme.AppFonts
import kotlinx.coroutines.delay
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
    val hasGeneratedReply: Boolean = false
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
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // State management
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<ChatPlatform?>(null) }

    // Observe external notifications from NotificationMemoryStore
    val externalNotifications = NotificationMemoryStore.notifications

    // Map external notifications to ChatNotification for display
    val liveNotifications = externalNotifications.map {
        ChatNotification(
            id = it.time.toString(),
            platform = when {
                it.packageName.contains("whatsapp", true) -> ChatPlatform.WHATSAPP
                it.packageName.contains("instagram", true) -> ChatPlatform.INSTAGRAM
                it.packageName.contains("messenger", true) -> ChatPlatform.MESSENGER
                it.packageName.contains("telegram", true) -> ChatPlatform.TELEGRAM
                it.packageName.contains("sms", true) -> ChatPlatform.SMS
                else -> ChatPlatform.GENERAL
            },
            senderName = it.title.ifBlank { it.packageName },
            senderAvatar = null,
            message = it.text,
            timestamp = it.time,
            isRead = false
        )
    }

    // Combine live notifications with sample notifications for now
    val notifications = remember(liveNotifications) {
        // Only show live notifications, comment out sample notifications
        liveNotifications
        /*
        if (liveNotifications.isNotEmpty()) liveNotifications else listOf(
            ChatNotification(
                id = "1",
                platform = ChatPlatform.WHATSAPP,
                senderName = "Priya Sharma",
                senderAvatar = null,
                message = "Hey! How are you doing?",
                timestamp = System.currentTimeMillis() - 3600000,
                isRead = false,
                hasGeneratedReply = true
            ),
            ChatNotification(
                id = "2",
                platform = ChatPlatform.INSTAGRAM,
                senderName = "john_doe_official",
                senderAvatar = null,
                message = "Loved your latest post! 🔥",
                timestamp = System.currentTimeMillis() - 7200000,
                isRead = false
            ),
            ChatNotification(
                id = "3",
                platform = ChatPlatform.MESSENGER,
                senderName = "Anita Singh",
                senderAvatar = null,
                message = "Can we reschedule our meeting?",
                timestamp = System.currentTimeMillis() - 86400000,
                isRead = true,
                hasGeneratedReply = true
            ),
            ChatNotification(
                id = "4",
                platform = ChatPlatform.TELEGRAM,
                senderName = "Tech Updates",
                senderAvatar = null,
                message = "New Android 15 features released!",
                timestamp = System.currentTimeMillis() - 172800000,
                isRead = true
            ),
            ChatNotification(
                id = "5",
                platform = ChatPlatform.SMS,
                senderName = "Bank Alert",
                senderAvatar = null,
                message = "Your account balance is Rs. 25,000",
                timestamp = System.currentTimeMillis() - 259200000,
                isRead = true
            )
        )
        */
    }

    // Filter notifications based on search and platform filter
    val filteredNotifications = remember(searchQuery, selectedFilter, notifications) {
        notifications.filter { notification ->
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
                        text = "Notifications",
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
                        NotificationCard(
                            notification = notification,
                            onReplyClick = { /* Handle reply generation */ },
                            onMarkAsRead = { /* Handle mark as read */ }
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
fun NotificationCard(
    notification: ChatNotification,
    onReplyClick: (String) -> Unit,
    onMarkAsRead: (String) -> Unit
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

            // Action buttons
            if (!notification.isRead || notification.hasGeneratedReply) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (notification.hasGeneratedReply) {
                        OutlinedButton(
                            onClick = { onReplyClick(notification.id) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF395B64)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Generated Reply",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View Reply",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFonts.KarlaFontFamily,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

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
