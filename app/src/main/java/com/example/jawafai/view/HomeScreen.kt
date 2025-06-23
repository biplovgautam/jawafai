package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// Data class to represent a chat
data class ChatPreview(
    val id: String,
    val userName: String,
    val lastMessage: String,
    val userImage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

// Sample data for chat list
private val fakeChatList = listOf(
    ChatPreview(
        id = "1",
        userName = "Aisha Khan",
        lastMessage = "Hey, how's it going? Did you check out the new AI features?",
        userImage = "https://randomuser.me/api/portraits/women/12.jpg",
        time = "10:30 AM",
        unreadCount = 3,
        isOnline = true
    ),
    ChatPreview(
        id = "2",
        userName = "Rahul Sharma",
        lastMessage = "I sent you the files you requested. Let me know if you need anything else.",
        userImage = "https://randomuser.me/api/portraits/men/32.jpg",
        time = "Yesterday",
        unreadCount = 0,
        isOnline = false
    ),
    ChatPreview(
        id = "3",
        userName = "Priya Patel",
        lastMessage = "The meeting is scheduled for tomorrow at 2 PM. Don't forget!",
        userImage = "https://randomuser.me/api/portraits/women/45.jpg",
        time = "Yesterday",
        unreadCount = 2,
        isOnline = true
    ),
    ChatPreview(
        id = "4",
        userName = "Arjun Desai",
        lastMessage = "That's great! Let's catch up soon.",
        userImage = "https://randomuser.me/api/portraits/men/55.jpg",
        time = "Tuesday",
        unreadCount = 0,
        isOnline = false
    ),
    ChatPreview(
        id = "5",
        userName = "Neha Gupta",
        lastMessage = "Did you see the latest episode? It was amazing!",
        userImage = "https://randomuser.me/api/portraits/women/29.jpg",
        time = "Monday",
        unreadCount = 0,
        isOnline = true
    ),
    ChatPreview(
        id = "6",
        userName = "Vikram Singh",
        lastMessage = "I'll send you the presentation slides soon.",
        userImage = "https://randomuser.me/api/portraits/men/42.jpg",
        time = "Sunday",
        unreadCount = 1,
        isOnline = false
    ),
    ChatPreview(
        id = "7",
        userName = "Ananya Reddy",
        lastMessage = "Thanks for your help with the project!",
        userImage = "https://randomuser.me/api/portraits/women/22.jpg",
        time = "Last week",
        unreadCount = 0,
        isOnline = true
    ),
    ChatPreview(
        id = "8",
        userName = "Karan Malhotra",
        lastMessage = "Let's meet at the coffee shop at 5.",
        userImage = "https://randomuser.me/api/portraits/men/36.jpg",
        time = "Last week",
        unreadCount = 0,
        isOnline = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "जवाफ.AI",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { /* Create new chat */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        ChatListContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun ChatListContent(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(fakeChatList) { chat ->
            ChatItem(chat = chat)
            Divider(
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ChatItem(chat: ChatPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to chat detail */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.BottomEnd
        ) {
            // User profile image
            Image(
                painter = rememberAsyncImagePainter(model = chat.userImage),
                contentDescription = "Profile picture of ${chat.userName}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Online status indicator
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = chat.time,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
