package com.example.jawafai.view.dashboard.chat

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
    var isRefreshing by remember { mutableStateOf(false) }
    var showInitialLoading by remember { mutableStateOf(true) }
    var isSearchLoading by remember { mutableStateOf(false) }

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
        isRefreshing = true
        coroutineScope.launch {
            delay(1000)
            viewModel.refreshChatSummaries()
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
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isNewChatDialogVisible = true },
                containerColor = Color(0xFF395B64),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
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
                    viewModel.clearFoundUser()
                },
                isActive = isSearchActive,
                focusRequester = focusRequester
            )

            // Search Loading Indicator
            AnimatedVisibility(
                visible = isSearchLoading && isSearchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF395B64),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Loading indicator only for initial loading (removed duplicate animation)
            AnimatedVisibility(
                visible = showInitialLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
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

            // Chat List
            if (!showInitialLoading) {
                if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "No results",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF666666).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No chats found",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }
                        }
                    }
                } else if (searchResults.isEmpty() && searchQuery.isBlank()) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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
                                    imageVector = Icons.Rounded.Chat,
                                    contentDescription = "No chats",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF666666).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No conversations yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pull down to refresh or start a new chat",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666).copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val pullToRefreshState = rememberSwipeRefreshState(isRefreshing)

                    SwipeRefresh(
                        state = pullToRefreshState,
                        onRefresh = { handleRefresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { result ->
                                when (result) {
                                    is SearchResult.ExistingChat -> result.summary.chatId
                                    is SearchResult.NewUser -> result.user.userId
                                }
                            }) { result ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInHorizontally { it } + fadeIn(),
                                    exit = slideOutHorizontally { -it } + fadeOut()
                                ) {
                                    when (result) {
                                        is SearchResult.ExistingChat -> {
                                            ChatItemCard(
                                                summary = result.summary,
                                                onClick = {
                                                    onNavigateToChat(result.summary.chatId, result.summary.otherUserId)
                                                }
                                            )
                                        }
                                        is SearchResult.NewUser -> {
                                            UserSearchResultCard(
                                                user = result.user,
                                                onClick = {
                                                    selectedUser = result.user
                                                    showQuickMessages = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // New Chat Dialog
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

        // Quick Messages Dialog
        if (showQuickMessages && selectedUser != null) {
            QuickMessagesDialog(
                user = selectedUser!!,
                onDismiss = {
                    showQuickMessages = false
                    selectedUser = null
                },
                onMessageSelect = { message ->
                    coroutineScope.launch {
                        val chatId = viewModel.createChatWithUser(selectedUser!!.userId)
                        if (chatId != null) {
                            // Send the quick message
                            viewModel.sendMessage(selectedUser!!.userId, message)
                            showQuickMessages = false
                            selectedUser = null
                            onNavigateToChat(chatId, selectedUser!!.userId)
                        }
                    }
                },
                onCustomMessage = {
                    // Open chat without sending a message
                    coroutineScope.launch {
                        val chatId = viewModel.createChatWithUser(selectedUser!!.userId)
                        if (chatId != null) {
                            showQuickMessages = false
                            selectedUser = null
                            onNavigateToChat(chatId, selectedUser!!.userId)
                        }
                    }
                }
            )
        }
    }
}

// Search result sealed class
sealed class SearchResult {
    data class ExistingChat(val summary: ChatSummary) : SearchResult()
    data class NewUser(val user: com.example.jawafai.repository.UserProfile) : SearchResult()
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
                text = "Search chats or find users...",
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
        keyboardActions = KeyboardActions(
            onSearch = {
                // Handle search action if needed
            }
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
fun ChatItemCard(
    summary: ChatSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar with unread indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA5C9CA))
                ) {
                    if (summary.otherUserImageUrl != null) {
                        AsyncImage(
                            model = summary.otherUserImageUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.size(28.dp).align(Alignment.Center),
                            tint = Color.White
                        )
                    }
                }

                // Unread count badge
                if (summary.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF395B64))
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (summary.unreadCount > 9) "9+" else summary.unreadCount.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Chat Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = summary.otherUserName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF395B64)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatTimestamp(summary.lastMessageTimestamp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = summary.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun UserSearchResultCard(
    user: com.example.jawafai.repository.UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                        modifier = Modifier.size(28.dp).align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
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
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = Color(0xFF395B64).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Say hi! to start conversation",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF395B64),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickMessagesDialog(
    user: com.example.jawafai.repository.UserProfile,
    onDismiss: () -> Unit,
    onMessageSelect: (String) -> Unit,
    onCustomMessage: () -> Unit
) {
    val quickMessages = listOf(
        "Hi! 👋",
        "Hello! 😊",
        "Hey there! 🙂"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Start conversation with ${user.displayName.ifEmpty { user.username }}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF395B64)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choose a quick message or start typing:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick message options
                quickMessages.forEach { message ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMessageSelect(message) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFF395B64).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFF395B64)
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Custom message option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomMessage() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF395B64).copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF395B64).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Custom message",
                            tint = Color(0xFF395B64),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start typing your own message...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFF395B64)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NewChatDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onNavigateToChat: (String, String) -> Unit
) {
    val foundUser by viewModel.foundUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "New Chat",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF395B64)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Enter email or username",
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF395B64),
                        unfocusedTextColor = Color(0xFF395B64),
                        cursorColor = Color(0xFF395B64),
                        focusedBorderColor = Color(0xFF395B64),
                        unfocusedBorderColor = Color(0xFF666666).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = AppFonts.KarlaFontFamily,
                                color = Color(0xFF666666)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val user = viewModel.findUserByEmailOrUsername(searchQuery)
                                if (user != null) {
                                    val chatId = viewModel.createChatWithUser(user.userId)
                                    if (chatId != null) {
                                        onNavigateToChat(chatId, user.userId)
                                    }
                                }
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF395B64)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = AppFonts.KarlaFontFamily,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Show found user or error
                if (foundUser != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    UserSearchResultCard(
                        user = foundUser!!,
                        onClick = {
                            coroutineScope.launch {
                                val chatId = viewModel.createChatWithUser(foundUser!!.userId)
                                if (chatId != null) {
                                    onNavigateToChat(chatId, foundUser!!.userId)
                                }
                            }
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color.Red
                        )
                    )
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
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        diff < 48 * 60 * 60 * 1000 -> "yesterday"
        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
