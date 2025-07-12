package com.example.jawafai.view.dashboard.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.repository.ChatRepositoryImpl
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.utils.UserMigrationUtils
import com.example.jawafai.viewmodel.ChatViewModel
import com.example.jawafai.viewmodel.ChatViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// Common UI constants
private val darkTeal = Color(0xFF365A61)
private val standardPadding = 16.dp
private val itemSpacing = 16.dp

// Sample data for chat list
enum class ChatFilter(val label: String, val icon: ImageVector?) {
    All("All", null),
    Unread("Unread", Icons.Filled.MarkEmailUnread),
    Groups("Groups", Icons.Filled.Group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToChat: (chatId: String, otherUserId: String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val chatRepository = remember { ChatRepositoryImpl() }
    val userRepository = remember { UserRepositoryImpl(auth, FirebaseFirestore.getInstance()) }
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(chatRepository, userRepository, auth)
    )

    val coroutineScope = rememberCoroutineScope()

    // Auto-migrate current user to database when screen loads
    LaunchedEffect(Unit) {
        UserMigrationUtils.saveCurrentUserToDatabase()
        UserMigrationUtils.showAllUsersInDatabase()
    }

    val chatSummaries by viewModel.chatSummaries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.All) }
    val searchFocusRequester = remember { FocusRequester() }
    var isNewChatDialogVisible by remember { mutableStateOf(false) }

    val filteredChats = remember(searchQuery, selectedFilter, chatSummaries) {
        chatSummaries.filter { summary ->
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                summary.otherUserName.contains(searchQuery, ignoreCase = true) ||
                summary.lastMessage.contains(searchQuery, ignoreCase = true)
            }

            val matchesFilter = when (selectedFilter) {
                ChatFilter.All -> true
                ChatFilter.Unread -> !summary.isLastMessageSeen
                ChatFilter.Groups -> false // TODO: Implement group logic
            }
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = darkTeal,
        contentColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { isNewChatDialogVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkTeal
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isNewChatDialogVisible = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "New Chat"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkTeal)
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBarContent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                onClear = { searchQuery = "" },
                focusRequester = searchFocusRequester
            )

            // Filter Chips
            ChatFilterChips(
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )

            // Chat List
            ChatListContent(
                chats = filteredChats,
                paddingValues = PaddingValues(0.dp),
                onChatClick = { summary ->
                    onNavigateToChat(summary.chatId, summary.otherUserId)
                }
            )
        }

        if (isNewChatDialogVisible) {
            NewChatDialog(
                viewModel = viewModel,
                onDismiss = { isNewChatDialogVisible = false },
                onNavigateToChat = { chatId, otherUserId ->
                    isNewChatDialogVisible = false
                    onNavigateToChat(chatId, otherUserId)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarContent(
    onNewChatClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Chats",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 22.sp
            )
        },
//        actions = {
//            IconButton(onClick = onNewChatClick) {
//                Icon(
//                    imageVector = Icons.Default.Add,
//                    contentDescription = "New Chat",
//                    tint = Color.White
//                )
//            }
//        },
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
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Ask jawafai or search",
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
    chats: List<ChatSummary>,
    paddingValues: PaddingValues,
    onChatClick: (ChatSummary) -> Unit
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
            items(chats, key = { it.chatId }) { chat ->
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
    chat: ChatSummary,
    onClick: () -> Unit
) {
    val isUnread = !chat.isLastMessageSeen

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
        tonalElevation = if (isUnread) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                imageUrl = chat.otherUserImageUrl,
                userName = chat.otherUserName,
                size = 56.dp
            )

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
                        text = chat.otherUserName,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, false)
                    )

                    Text(
                        text = formatTimestamp(chat.lastMessageTimestamp),
                        color = if (isUnread) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
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
                        color = if (isUnread) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    imageUrl: String?,
    userName: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val placeholderText = (userName.firstOrNull()?.toString() ?: "A").uppercase()
    val seedForColor = userName
    val avatarColors = remember {
        listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
            Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548)
        )
    }
    val backgroundColor = remember(seedForColor) {
        avatarColors[abs(seedForColor.hashCode()) % avatarColors.size]
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = (size.value / 2).sp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile picture of $userName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val foundUser by viewModel.foundUser.collectAsState()

    // Move rememberCoroutineScope to the composable function level
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.clearError()
        viewModel.clearFoundUser()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = darkTeal,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Chat",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 24.sp
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = {
                        Text(
                            "Enter username or email",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g., john@example.com or @johndoe",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                isSearching = true
                            }
                        }
                    ),
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        isSearching = true
                                    }
                                },
                                enabled = searchQuery.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (searchQuery.isNotBlank()) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message
                errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Found User Display
                foundUser?.let { user ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Avatar
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    if (user.profileImageUrl != null) {
                                        AsyncImage(
                                            model = user.profileImageUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = user.displayName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // User Info
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = user.displayName.ifEmpty { user.username },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = user.email,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                    if (user.username.isNotEmpty()) {
                                        Text(
                                            text = "@${user.username}",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Start Chat Button
                            Button(
                                onClick = {
                                    // Use the coroutineScope declared at the composable level
                                    coroutineScope.launch {
                                        val chatId = viewModel.createChatWithUser(user.userId)
                                        if (chatId != null) {
                                            onNavigateToChat(chatId, user.userId)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Start Chat",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Start Chat",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Search Instructions
                if (foundUser == null && !isLoading && errorMessage == null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "How to find users:",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Enter the exact email address\n• Enter the username (with or without @)",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Handle search trigger
    LaunchedEffect(isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            println("🔍 DEBUG: NewChatDialog - Triggering search for: '$searchQuery'")
            viewModel.findUserByEmailOrUsername(searchQuery.trim())
            isSearching = false
        }
    }

    // Debug: Log state changes
    LaunchedEffect(isLoading) {
        println("🔍 DEBUG: NewChatDialog - Loading state changed to: $isLoading")
    }

    LaunchedEffect(errorMessage) {
        println("🔍 DEBUG: NewChatDialog - Error message changed to: $errorMessage")
    }

    LaunchedEffect(foundUser) {
        println("🔍 DEBUG: NewChatDialog - Found user changed to: $foundUser")
    }
}
