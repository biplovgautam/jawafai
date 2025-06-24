package com.example.jawafai.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.jawafai.model.ChatPreview

// Sample data for chat list
enum class ChatFilter(val label: String, val icon: ImageVector?) {
    All("All", null),
    Unread("Unread", Icons.Filled.MarkEmailUnread),
    Favorites("Favorites", Icons.Filled.Star),
    Groups("Groups", Icons.Filled.Group)
}

private val chatList = listOf(
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
        userName = "Study Group",
        lastMessage = "Priya: Let's meet at 5 PM!",
        userImage = "",
        time = "09:15 AM",
        unreadCount = 0,
        isOnline = false
    ),
    ChatPreview(
        id = "3",
        userName = "Rahul Sharma",
        lastMessage = "I'll send the notes soon.",
        userImage = "https://randomuser.me/api/portraits/men/32.jpg",
        time = "Yesterday",
        unreadCount = 1,
        isOnline = false
    ),
    ChatPreview(
        id = "4",
        userName = "Favorites Group",
        lastMessage = "You: See you all tomorrow!",
        userImage = "",
        time = "Yesterday",
        unreadCount = 0,
        isOnline = false
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val darkTeal = Color(0xFF365A61)
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.All) }

    val filteredChats = remember(searchQuery, selectedFilter) {
        chatList.filter { chat ->
            val matchesQuery = chat.userName.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                ChatFilter.All -> true
                ChatFilter.Unread -> chat.unreadCount > 0
                ChatFilter.Favorites -> chat.userName.contains("Favorites", ignoreCase = true)
                ChatFilter.Groups -> chat.userName.contains("Group", ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = darkTeal,
        topBar = {
            Column(
                Modifier.background(darkTeal)
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Chats", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
                SearchBarContent(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    onClear = { searchQuery = "" }
                )
                ChatFilterSelector(selected = selectedFilter, onSelect = { selectedFilter = it })
            }
        }
    ) { paddingValues ->
        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chats found",
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredChats) { chat ->
                    ChatListItem(chat = chat)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }
    }
}

@Composable
fun ChatFilterSelector(selected: ChatFilter, onSelect: (ChatFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatFilter.values().forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        filter.icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = filter.label,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(filter.label)
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /* TODO: Add new chat/group */ }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Chat/Group",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search chats...",
                modifier = Modifier.alpha(0.6f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ChatListItem(chat: ChatPreview) {
    val isGroup = chat.userName.contains("Group", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable { /* TODO: Open chat */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isGroup) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D3A3F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "Group",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Image(
                painter = if (chat.userImage.isNotBlank()) rememberAsyncImagePainter(model = chat.userImage) else rememberAsyncImagePainter(model = "https://ui-avatars.com/api/?name=${chat.userName.replace(" ", "+")}"),
                contentDescription = "Profile picture of ${chat.userName}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
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
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
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
