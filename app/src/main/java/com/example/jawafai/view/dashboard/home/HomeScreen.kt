package com.example.jawafai.view.dashboard.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.jawafai.R

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

// Data class for other services messages
data class ServiceMessage(
    val icon: ImageVector,
    val text: String
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
        isOnline = true
    ),
    ChatPreview(
        id = "3",
        userName = "Priya Patel",
        lastMessage = "The meeting is scheduled for tomorrow at 2 PM. Don't forget!",
        userImage = "https://randomuser.me/api/portraits/women/45.jpg",
        time = "Yesterday",
        unreadCount = 2,
        isOnline = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val darkTeal = Color(0xFF365A61) // Custom dark teal color

    Scaffold(
        containerColor = darkTeal,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "How can I help?",
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Open drawer */ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open search */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkTeal,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun HomeScreenContent(modifier: Modifier = Modifier) {
    val darkTeal = Color(0xFF365A61) // Custom dark teal color

    // Moving this here because vectorResource can only be used in @Composable functions
    val servicesList = listOf(
        ServiceMessage(
            icon = ImageVector.vectorResource(id = R.drawable.ic_instagram),
            text = "3+ messages"
        ),
        ServiceMessage(
            icon = Icons.Rounded.ChatBubble,
            text = "No new messages"
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(darkTeal),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // JAWAFAI Section Header (New AI Chatbot Section)
        item {
            SectionHeader(title = "JAWAFAI")
        }

        // JAWAFAI Chatbot Card
        item {
            JawafaiChatbotCard(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // CHATS Section Header (Changed from JAWAF)
        item {
            SectionHeader(title = "CHATS")
        }

        // JAWAF Chat items
        items(fakeChatList) { chat ->
            ChatItem(chat = chat)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // OTHERS Section Header
        item {
            SectionHeader(title = "OTHERS")
        }

        // OTHERS Service items
        items(servicesList) { service ->
            ServiceItem(service = service)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Extra space at the bottom for bottom navigation
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ChatItem(chat: ChatPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to chat detail */ }
            .padding(horizontal = 16.dp),
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
                        .border(2.dp, Color(0xFF365A61), CircleShape)
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
                    fontSize = 16.sp,
                    color = Color.White
                )

                Text(
                    text = chat.time,
                    color = Color.LightGray,
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
                    color = Color.LightGray,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
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

@Composable
fun ServiceItem(service: ServiceMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle service click */ }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Service icon with circular white background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = service.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Service message
        Text(
            text = service.text,
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}

@Composable
fun JawafaiChatbotCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { /* Handle chatbot click */ }
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Bot Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "JawafAI Bot",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Chatbot Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "JawafAI Assistant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ask me anything! I'm here to help.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Online indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
        }
    }
}
