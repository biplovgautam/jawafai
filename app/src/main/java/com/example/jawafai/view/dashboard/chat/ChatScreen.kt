package com.example.jawafai.view.dashboard.chat

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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

    // Auto-migrate current user to database when screen loads
    LaunchedEffect(Unit) {
        UserMigrationUtils.saveCurrentUserToDatabase()
        UserMigrationUtils.showAllUsersInDatabase()
    }

    val chatSummaries by viewModel.chatSummaries.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isNewChatDialogVisible by remember { mutableStateOf(false) }

    val filteredChats = remember(searchQuery, chatSummaries) {
        chatSummaries.filter { summary ->
            if (searchQuery.isBlank()) {
                true
            } else {
                summary.otherUserName.contains(searchQuery, ignoreCase = true) ||
                summary.lastMessage.contains(searchQuery, ignoreCase = true)
            }
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
                actions = {
                    IconButton(onClick = { isNewChatDialogVisible = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = Color(0xFF395B64)
                        )
                    }
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
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            SearchBarContent(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = {},
                onClear = { searchQuery = "" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chat List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredChats) { summary ->
                    ChatItemCard(
                        summary = summary,
                        onClick = { onNavigateToChat(summary.chatId, summary.otherUserId) }
                    )
                }
            }
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
            onSearch = { onSearch() }
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF395B64),
            unfocusedTextColor = Color(0xFF395B64),
            cursorColor = Color(0xFF395B64),
            focusedBorderColor = Color(0xFF395B64),
            unfocusedBorderColor = Color(0xFF666666).copy(alpha = 0.5f),
            focusedPlaceholderColor = Color(0xFF666666),
            unfocusedPlaceholderColor = Color(0xFF666666).copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = formatTimestamp(summary.lastMessageTimestamp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        )

                        if (summary.unreadCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF395B64),
                                        CircleShape
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = summary.unreadCount.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = AppFonts.KarlaFontFamily,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.lastMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            fontWeight = if (summary.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
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
            shape = RoundedCornerShape(16.dp),
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
                        unfocusedBorderColor = Color(0xFF666666).copy(alpha = 0.5f)
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
                        shape = RoundedCornerShape(8.dp)
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    val chatId = viewModel.createChatWithUser(foundUser!!.userId)
                                    if (chatId != null) {
                                        onNavigateToChat(chatId, foundUser!!.userId)
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA5C9CA))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.align(Alignment.Center),
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = foundUser!!.displayName.ifEmpty { foundUser!!.username },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = AppFonts.KarlaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF395B64)
                                    )
                                )
                                Text(
                                    text = foundUser!!.email,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }
                        }
                    }
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
