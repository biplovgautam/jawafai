package com.example.jawafai.view

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.jawafai.model.ChatPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults

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
    ),
    ChatPreview(
        id = "5",
        userName = "Maya Patel",
        lastMessage = "The presentation looks great! I've made some minor edits.",
        userImage = "https://randomuser.me/api/portraits/women/22.jpg",
        time = "2 days ago",
        unreadCount = 0,
        isOnline = true
    ),
    ChatPreview(
        id = "6",
        userName = "Dev Team",
        lastMessage = "Sam: I've pushed the latest changes to the repository.",
        userImage = "",
        time = "3 days ago",
        unreadCount = 5,
        isOnline = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val coroutineScope = rememberCoroutineScope()

    val darkTeal = Color(0xFF365A61)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.All) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    var selectedChat: ChatPreview? by remember { mutableStateOf(null) }
    var isNewChatDialogVisible by remember { mutableStateOf(false) }

    val filteredChats = remember(searchQuery, selectedFilter) {
        chatList.filter { chat ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                chat.userName.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (selectedFilter) {
                ChatFilter.All -> true
                ChatFilter.Unread -> chat.unreadCount > 0
                ChatFilter.Favorites -> chat.userName.contains("Favorites", ignoreCase = true)
                ChatFilter.Groups -> chat.userName.contains("Group", ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Use a solid background color instead of the animated gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkTeal)
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            topBar = {
                Column {
                    AnimatedVisibility(
                        visible = !isSearchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        TopAppBarContent(
                            onSearchClick = {
                                isSearchExpanded = true
                                coroutineScope.launch {
                                    delay(100)
                                    searchFocusRequester.requestFocus()
                                }
                            },
                            onNewChatClick = { isNewChatDialogVisible = true }
                        )
                    }

                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        SearchBarContent(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {},
                            onClear = { searchQuery = "" },
                            onBack = {
                                isSearchExpanded = false
                                searchQuery = ""
                            },
                            focusRequester = searchFocusRequester
                        )
                    }

                    ChatFilterChips(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isNewChatDialogVisible = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "New Chat"
                    )
                }
            }
        ) { paddingValues ->
            ChatListContent(
                chats = filteredChats,
                paddingValues = paddingValues,
                onChatClick = { selectedChat = it }
            )
        }

        // New Chat Dialog
        if (isNewChatDialogVisible) {
            NewChatDialog(
                onDismiss = { isNewChatDialogVisible = false },
                onCreateChat = { /* Create new chat */ }
            )
        }

        // Show selected chat in overlay (would normally navigate)
        selectedChat?.let { chat ->
            ChatDetailOverlay(
                chat = chat,
                onClose = { selectedChat = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarContent(
    onSearchClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Jawafai",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 22.sp
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            IconButton(onClick = onNewChatClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search chats...",
                    modifier = Modifier.alpha(0.7f),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White
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
                .weight(1f)
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatFilterChips(
    selected: ChatFilter,
    onSelect: (ChatFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
        maxItemsInEachRow = 4
    ) {
        ChatFilter.entries.forEach { filter ->
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White.copy(alpha = 0.2f),
                    containerColor = Color.Transparent,
                    labelColor = Color.White,
                    selectedLabelColor = Color.White,
                    iconColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun ChatListContent(
    chats: List<ChatPreview>,
    paddingValues: PaddingValues,
    onChatClick: (ChatPreview) -> Unit
) {
    if (chats.isEmpty()) {
        EmptyChatState(paddingValues)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(chats, key = { it.id }) { chat ->
                ChatListItem(
                    chat = chat,
                    onClick = { onChatClick(chat) }
                )
            }
        }
    }
}

@Composable
fun EmptyChatState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No chats found",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start a new conversation or try a different search",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatPreview,
    onClick: () -> Unit
) {
    val isGroup = chat.userName.contains("Group", ignoreCase = true)

    // Animation for pressed state
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressAnimation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        tonalElevation = if (chat.unreadCount > 0) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2D3A3F),
                        modifier = Modifier
                            .size(56.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = "Group",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    AsyncImage(
                        model = if (chat.userImage.isNotBlank()) chat.userImage
                            else "https://ui-avatars.com/api/?name=${chat.userName.replace(" ", "+")}&background=4A7A85&color=fff&size=200",
                        contentDescription = "Profile picture of ${chat.userName}",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (chat.isOnline) 2.dp else 0.dp,
                                color = Color(0xFF4CAF50),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )

                    // Online indicator
                    if (chat.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .border(1.5.dp, Color(0xFF2D3A3F), CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false)
                    )

                    Text(
                        text = chat.time,
                        color = if (chat.unreadCount > 0) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
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
                        color = if (chat.unreadCount > 0) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (String) -> Unit
) {
    var contactName by remember { mutableStateOf("") }
    var isCreatingGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf(setOf<String>()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2D3A3F),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = if (isCreatingGroup) "New Group" else "New Chat",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isCreatingGroup) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Select Contacts",
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mock contact selection
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(5) { index ->
                            val contactId = "contact_$index"
                            val isSelected = selectedContacts.contains(contactId)

                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable {
                                        selectedContacts = if (isSelected) {
                                            selectedContacts - contactId
                                        } else {
                                            selectedContacts + contactId
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "C${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Contact Name or Number", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            isCreatingGroup = !isCreatingGroup
                        }
                    ) {
                        Text(
                            text = if (isCreatingGroup) "Create Chat Instead" else "Create Group Instead",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = {
                            if (isCreatingGroup) {
                                onCreateChat("group_${groupName}")
                            } else {
                                onCreateChat(contactName)
                            }
                            onDismiss()
                        },
                        enabled = if (isCreatingGroup) {
                            groupName.isNotBlank() && selectedContacts.isNotEmpty()
                        } else {
                            contactName.isNotBlank()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailOverlay(
    chat: ChatPreview,
    onClose: () -> Unit
) {
    val isGroup = chat.userName.contains("Group", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top app bar
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Chat detail content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                if (isGroup) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D3A3F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = "Group",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = if (chat.userImage.isNotBlank()) chat.userImage
                            else "https://ui-avatars.com/api/?name=${chat.userName.replace(" ", "+")}&background=4A7A85&color=fff&size=200",
                        contentDescription = "Profile picture of ${chat.userName}",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = chat.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )

                if (chat.isOnline) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "Online",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "Chat",
                        onClick = onClose
                    )

                    ActionButton(
                        icon = Icons.Default.Call,
                        label = "Call",
                        onClick = { /* Handle call action */ }
                    )

                    ActionButton(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        onClick = { /* Handle video call action */ }
                    )

                    ActionButton(
                        icon = Icons.Default.Info,
                        label = "Info",
                        onClick = { /* Handle info action */ }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Last message preview
                if (chat.lastMessage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Last message",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = chat.lastMessage,
                                fontSize = 16.sp,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = chat.time,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}
