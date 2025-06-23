package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

// Data class to represent a notification
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val imageUrl: String?, // Profile picture or notification image
    val time: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    MESSAGE, MENTION, SYSTEM, REMINDER
}

// Sample data for notification list
private val fakeNotifications = listOf(
    NotificationItem(
        id = "1",
        title = "New Message",
        message = "Aisha Khan sent you a message",
        imageUrl = "https://randomuser.me/api/portraits/women/12.jpg",
        time = "10 min ago",
        type = NotificationType.MESSAGE,
        isRead = false
    ),
    NotificationItem(
        id = "2",
        title = "Mention",
        message = "Rahul Sharma mentioned you in a group chat",
        imageUrl = "https://randomuser.me/api/portraits/men/32.jpg",
        time = "2 hours ago",
        type = NotificationType.MENTION,
        isRead = true
    ),
    NotificationItem(
        id = "3",
        title = "System Update",
        message = "New AI features are now available",
        imageUrl = null,
        time = "Yesterday",
        type = NotificationType.SYSTEM,
        isRead = false
    ),
    NotificationItem(
        id = "4",
        title = "Reminder",
        message = "You have a scheduled meeting at 2 PM",
        imageUrl = null,
        time = "Yesterday",
        type = NotificationType.REMINDER,
        isRead = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        if (fakeNotifications.isEmpty()) {
            EmptyNotificationsState(Modifier.padding(paddingValues))
        } else {
            NotificationsList(
                notifications = fakeNotifications,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun NotificationsList(
    notifications: List<NotificationItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(notifications) { notification ->
            NotificationItem(notification = notification)
            Divider(
                modifier = Modifier.padding(start = if (notification.imageUrl != null) 80.dp else 16.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationItem) {
    val backgroundColor = if (!notification.isRead)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    else
        Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification icon or profile image
        if (notification.imageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(model = notification.imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // System notification icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Title and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp
                )

                Text(
                    text = notification.time,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message
            Text(
                text = notification.message,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (!notification.isRead) Color.Black else Color.Gray,
                fontSize = 14.sp
            )
        }

        // Unread indicator
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun EmptyNotificationsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .alpha(0.5f),
                tint = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No notifications",
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You'll be notified when something arrives",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
