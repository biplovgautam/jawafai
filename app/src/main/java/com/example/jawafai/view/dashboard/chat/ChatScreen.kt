package com.example.jawafai.view.dashboard.chat

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.repository.ChatRepositoryImpl
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.utils.UserMigrationUtils
import com.example.jawafai.viewmodel.ChatViewModel
import com.example.jawafai.viewmodel.ChatViewModelFactory
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Auto-migrate current user to database when screen loads
    LaunchedEffect(Unit) {
        UserMigrationUtils.saveCurrentUserToDatabase()
        UserMigrationUtils.showAllUsersInDatabase()
    }

    val chatSummaries by viewModel.chatSummaries.collectAsState()
    val foundUser by viewModel.foundUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isNewChatDialogVisible by remember { mutableStateOf(false) }
    var showQuickMessages by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<com.example.jawafai.repository.UserProfile?>(null) }
    var showInitialLoading by remember { mutableStateOf(true) }
    var isSearchLoading by remember { mutableStateOf(false) }

    // Pull-to-refresh state
    val isRefreshing by remember { derivedStateOf { isLoading } }
    val pullToRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Handle back press for search
    DisposableEffect(isSearchActive) {
        val callback = object : OnBackPressedCallback(isSearchActive) {
            override fun handleOnBackPressed() {
                if (isSearchActive) {
                    searchQuery = ""
                    isSearchActive = false
                    keyboardController?.hide()
                    viewModel.clearFoundUser()
                }
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    // Combined search results: existing chats + found users
    val searchResults = remember(searchQuery, chatSummaries, foundUser) {
        if (searchQuery.isBlank()) {
            chatSummaries.map { SearchResult.ExistingChat(it) }
        } else {
            val filteredChats = chatSummaries.filter { summary ->
                summary.otherUserName.contains(searchQuery, ignoreCase = true) ||
                summary.lastMessage.contains(searchQuery, ignoreCase = true)
            }.map { SearchResult.ExistingChat(it) }

            val userResults = if (foundUser != null) {
                listOf(SearchResult.NewUser(foundUser!!))
            } else {
                emptyList()
            }

            filteredChats + userResults
        }
    }

    // Debounced search for users when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearchLoading = true
            delay(500) // Wait for user to finish typing
            delay(100) // Show loading animation for 0.1s
            viewModel.findUserByEmailOrUsername(searchQuery)
            isSearchLoading = false
        } else {
            viewModel.clearFoundUser()
            isSearchLoading = false
        }
    }

    // Handle initial loading
    LaunchedEffect(chatSummaries) {
        if (chatSummaries.isNotEmpty()) {
            showInitialLoading = false
        } else {
            delay(1000)
            showInitialLoading = false
        }
    }

    // Handle refresh
    fun handleRefresh() {
        coroutineScope.launch {
            viewModel.refreshChatSummaries()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Disable all system window insets to take full screen
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding() // Add status bar padding to top bar
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isNewChatDialogVisible = true },
                containerColor = Color(0xFF395B64),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .navigationBarsPadding() // Add navigation bar padding to FAB
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            SwipeRefresh(
                state = pullToRefreshState,
                onRefresh = { handleRefresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Search bar
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { isSearchActive = true },
                        active = isSearchActive,
                        onActiveChange = { isSearchActive = it },
                        placeholder = {
                            Text(
                                "Search chats or find users...",
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
                                tint = Color(0xFF395B64)
                            )
                        },
                        trailingIcon = {
                            if (isSearchActive) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                    keyboardController?.hide()
                                    viewModel.clearFoundUser()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF666666)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(focusRequester),
                        colors = SearchBarDefaults.colors(
                            containerColor = Color(0xFFF5F5F5),
                            dividerColor = Color.Transparent
                        )
                    ) {
                        // Search results
                        SearchResults(
                            searchResults = searchResults,
                            isSearchLoading = isSearchLoading,
                            onChatClick = { chatId, otherUserId ->
                                onNavigateToChat(chatId, otherUserId)
                                isSearchActive = false
                                keyboardController?.hide()
                            },
                            onUserClick = { user ->
                                selectedUser = user
                                isSearchActive = false
                                keyboardController?.hide()
                            },
                            searchQuery = searchQuery
                        )
                    }

                    // Chat list
                    if (showInitialLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF395B64)
                                )
                                Text(
                                    text = "Loading chats...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }
                        }
                    } else {
                        ChatList(
                            chatSummaries = chatSummaries,
                            onChatClick = onNavigateToChat,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding() // Add bottom padding for navigation bar
                        )
                    }
                }
            }
        }

        // New chat dialog
        if (isNewChatDialogVisible) {
            NewChatDialog(
                onDismiss = { isNewChatDialogVisible = false },
                onStartChat = { user ->
                    selectedUser = user
                    isNewChatDialogVisible = false
                },
                viewModel = viewModel
            )
        }

        // Selected user actions
        selectedUser?.let { user ->
            LaunchedEffect(user) {
                coroutineScope.launch {
                    val chatId = viewModel.createChatWithUser(user.userId)
                    if (chatId != null) {
                        onNavigateToChat(chatId, user.userId)
                    }
                    selectedUser = null
                }
            }
        }
    }
}

@Composable
private fun ChatList(
    chatSummaries: List<ChatSummary>,
    onChatClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter out chats with empty messages
    val filteredChats = chatSummaries.filter { it.lastMessage.isNotBlank() }

    if (filteredChats.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF395B64).copy(alpha = 0.3f)
                )
                Text(
                    text = "No chats yet",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        color = Color(0xFF666666)
                    )
                )
                Text(
                    text = "Start a new conversation",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        color = Color(0xFF999999)
                    )
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(filteredChats, key = { it.chatId }) { chatSummary ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { 300 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -300 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                ) {
                    ChatItem(
                        chatSummary = chatSummary,
                        onClick = { onChatClick(chatSummary.chatId, chatSummary.otherUserId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatItem(
    chatSummary: ChatSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5C9CA))
            ) {
                if (chatSummary.otherUserImageUrl != null) {
                    AsyncImage(
                        model = chatSummary.otherUserImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Chat info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chatSummary.otherUserName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF395B64)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatTimestamp(chatSummary.lastMessageTimestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chatSummary.lastMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread count badge
                    if (chatSummary.unreadCount > 0) {
                        Badge(
                            modifier = Modifier.size(20.dp),
                            containerColor = Color(0xFF395B64)
                        ) {
                            Text(
                                text = chatSummary.unreadCount.toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: List<SearchResult>,
    isSearchLoading: Boolean,
    onChatClick: (String, String) -> Unit,
    onUserClick: (com.example.jawafai.repository.UserProfile) -> Unit,
    searchQuery: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSearchLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF395B64),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            items(searchResults) { result ->
                when (result) {
                    is SearchResult.ExistingChat -> {
                        ChatItem(
                            chatSummary = result.chatSummary,
                            onClick = { onChatClick(result.chatSummary.chatId, result.chatSummary.otherUserId) }
                        )
                    }
                    is SearchResult.NewUser -> {
                        UserItem(
                            user = result.user,
                            onClick = { onUserClick(result.user) }
                        )
                    }
                }
            }

            if (searchResults.isEmpty() && searchQuery.isNotBlank() && !isSearchLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserItem(
    user: com.example.jawafai.repository.UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5C9CA))
            ) {
                if (user.profileImageUrl != null) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.displayName.ifEmpty { user.username },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF395B64)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Start chat",
                tint = Color(0xFF395B64),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NewChatDialog(
    onDismiss: () -> Unit,
    onStartChat: (com.example.jawafai.repository.UserProfile) -> Unit,
    viewModel: ChatViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val foundUser by viewModel.foundUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Start New Chat",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF395B64)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Enter username or email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            coroutineScope.launch {
                                viewModel.findUserByEmailOrUsername(searchQuery)
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF395B64))
                    }
                } else if (foundUser != null) {
                    UserItem(
                        user = foundUser!!,
                        onClick = { onStartChat(foundUser!!) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.findUserByEmailOrUsername(searchQuery)
                            }
                        },
                        enabled = searchQuery.isNotBlank()
                    ) {
                        Text("Search")
                    }
                }
            }
        }
    }
}

// Sealed class for search results
sealed class SearchResult {
    data class ExistingChat(val chatSummary: ChatSummary) : SearchResult()
    data class NewUser(val user: com.example.jawafai.repository.UserProfile) : SearchResult()
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    return when {
        diff < 60 * 1000 -> "now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
