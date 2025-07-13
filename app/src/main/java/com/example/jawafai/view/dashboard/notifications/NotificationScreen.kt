package com.example.jawafai.view.dashboard.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jawafai.R
import com.example.jawafai.ui.theme.AppFonts
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
    val iconRes: Int? = null // Make iconRes optional since we don't have the drawable files
) {
    WHATSAPP("WhatsApp", Color(0xFF25D366)),
    INSTAGRAM("Instagram", Color(0xFFE4405F)),
    MESSENGER("Messenger", Color(0xFF0084FF)),
    TELEGRAM("Telegram", Color(0xFF0088CC)),
    DISCORD("Discord", Color(0xFF5865F2)),
    SNAPCHAT("Snapchat", Color(0xFFFFFC00)),
    OTHER("Other", Color(0xFF666666))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    // Sample chat notifications - Replace with real data from your notification service
    val chatNotifications = remember {
        listOf(
            ChatNotification(
                "1", ChatPlatform.WHATSAPP, "Priya Sharma", null,
                "Hey! Are you free for coffee tomorrow?", System.currentTimeMillis() - 300000, false
            ),
            ChatNotification(
                "2", ChatPlatform.INSTAGRAM, "Rajesh Kumar", null,
                "Loved your latest post! 🔥", System.currentTimeMillis() - 900000, true, true
            ),
            ChatNotification(
                "3", ChatPlatform.MESSENGER, "Anita Singh", null,
                "Can you send me the project files?", System.currentTimeMillis() - 1800000, false
            ),
            ChatNotification(
                "4", ChatPlatform.TELEGRAM, "Vikram Gupta", null,
                "The meeting has been rescheduled to 3 PM", System.currentTimeMillis() - 3600000, true
            ),
            ChatNotification(
                "5", ChatPlatform.WHATSAPP, "Mom", null,
                "Don't forget to call me tonight", System.currentTimeMillis() - 7200000, false
            ),
            ChatNotification(
                "6", ChatPlatform.DISCORD, "Gaming Squad", null,
                "Who's up for a game tonight?", System.currentTimeMillis() - 10800000, true
            ),
            ChatNotification(
                "7", ChatPlatform.INSTAGRAM, "Sarah Wilson", null,
                "Happy Birthday! 🎉🎂", System.currentTimeMillis() - 86400000, true, true
            ),
            ChatNotification(
                "8", ChatPlatform.SNAPCHAT, "Best Friend", null,
                "Check out this cute cat! 🐱", System.currentTimeMillis() - 172800000, true
            )
        )
    }

    // Group notifications: unread first, then read
    val unreadNotifications = chatNotifications.filter { !it.isRead }
    val readNotifications = chatNotifications.filter { it.isRead }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chat Notifications",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF395B64)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (unreadNotifications.isNotEmpty()) {
                            Badge(
                                containerColor = Color(0xFF395B64)
                            ) {
                                Text(
                                    text = unreadNotifications.size.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = AppFonts.KarlaFontFamily
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show platforms filter chips
            item {
                PlatformFilterChips()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Unread notifications section
            if (unreadNotifications.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Messages",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF395B64)
                            )
                        )
                        Text(
                            text = "${unreadNotifications.size} unread",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(unreadNotifications) { notification ->
                    ChatNotificationCard(
                        notification = notification,
                        onGenerateReply = { /* Handle reply generation */ },
                        onMarkAsRead = { /* Handle mark as read */ }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Read notifications section
            if (readNotifications.isNotEmpty()) {
                item {
                    Text(
                        text = "Earlier",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(readNotifications) { notification ->
                    ChatNotificationCard(
                        notification = notification,
                        onGenerateReply = { /* Handle reply generation */ },
                        onMarkAsRead = { /* Already read */ }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PlatformFilterChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All platforms chip
        FilterChip(
            onClick = { /* Filter all */ },
            label = {
                Text(
                    text = "All",
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontSize = 12.sp
                )
            },
            selected = true,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF395B64),
                selectedLabelColor = Color.White
            )
        )

        // WhatsApp chip
        FilterChip(
            onClick = { /* Filter WhatsApp */ },
            label = {
                Text(
                    text = "WhatsApp",
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontSize = 12.sp
                )
            },
            selected = false,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(ChatPlatform.WHATSAPP.color, CircleShape)
                )
            }
        )

        // Instagram chip
        FilterChip(
            onClick = { /* Filter Instagram */ },
            label = {
                Text(
                    text = "Instagram",
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontSize = 12.sp
                )
            },
            selected = false,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(ChatPlatform.INSTAGRAM.color, CircleShape)
                )
            }
        )
    }
}

@Composable
fun ChatNotificationCard(
    notification: ChatNotification,
    onGenerateReply: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!notification.isRead) {
                    onMarkAsRead()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F8FF)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 2.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with platform, sender, and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Platform and sender info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Platform indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(notification.platform.color, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = notification.platform.displayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = notification.platform.color
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF666666)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = notification.senderName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                }

                // Timestamp and status
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatChatTime(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                    )

                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF395B64), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message content
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender avatar or initial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA5C9CA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (notification.senderAvatar != null) {
                        AsyncImage(
                            model = notification.senderAvatar,
                            contentDescription = "Sender Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = notification.senderName.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Message text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generate Reply button
                Button(
                    onClick = onGenerateReply,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (notification.hasGeneratedReply) Color(0xFF4CAF50) else Color(0xFF395B64)
                    )
                ) {
                    Icon(
                        imageVector = if (notification.hasGeneratedReply) Icons.Default.Reply else Icons.Default.AutoAwesome,
                        contentDescription = if (notification.hasGeneratedReply) "View Reply" else "Generate Reply",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (notification.hasGeneratedReply) "View Reply" else "Generate Reply",
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontSize = 12.sp
                    )
                }

                // Mark as read button (only for unread)
                if (!notification.isRead) {
                    OutlinedButton(
                        onClick = onMarkAsRead,
                        shape = RoundedCornerShape(20.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF395B64)
                        )
                    ) {
                        Text(
                            text = "Mark Read",
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun formatChatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
