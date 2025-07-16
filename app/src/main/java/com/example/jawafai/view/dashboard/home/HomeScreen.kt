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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.jawafai.R
import com.example.jawafai.repository.ChatRepositoryImpl
import com.example.jawafai.repository.UserRepositoryImpl
import com.example.jawafai.ui.theme.AppFonts
import com.example.jawafai.viewmodel.ChatViewModel
import com.example.jawafai.viewmodel.ChatViewModelFactory
import com.example.jawafai.viewmodel.UserViewModel
import com.example.jawafai.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Data classes
data class ChatPreview(
    val id: String,
    val userName: String,
    val userImageUrl: String?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit = {},
    onChatBotClick: () -> Unit = {},
    onCompletePersonaClick: () -> Unit = {},
    onRecentChatClick: (String, String) -> Unit = { _, _ -> },
    onNotificationClick: (String) -> Unit = {},
    onSeeAllChatsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Initialize ViewModels
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val repository = UserRepositoryImpl(auth, firestore)
    val userViewModel = remember { UserViewModelFactory(repository, auth).create(UserViewModel::class.java) }

    // Initialize ChatViewModel for recent chats
    val chatRepository = remember { ChatRepositoryImpl() }
    val chatViewModel = remember { ChatViewModelFactory(chatRepository, repository, auth).create(ChatViewModel::class.java) }

    // Observe user profile and chat summaries
    val userProfile by userViewModel.userProfile.observeAsState()
    val chatSummaries by chatViewModel.chatSummaries.collectAsState()

    // Store profile data in local variables to avoid smart cast issues
    val currentUserProfile = userProfile
    val userImageUrl = currentUserProfile?.imageUrl
    val userUsername = currentUserProfile?.username
    val userFirstName = currentUserProfile?.firstName

    // Check persona completion status based on new questions
    var isPersonaCompleted by remember { mutableStateOf(false) }

    // Check persona completion when user profile loads
    LaunchedEffect(currentUserProfile) {
        if (currentUserProfile != null) {
            try {
                val personaRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserProfile.id)
                    .collection("persona")

                val personaData = personaRef.get().await()

                // Check if we have valid answers for the new questions
                val validAnswers = personaData.documents.filter { doc ->
                    val questionId = doc.id
                    val answer = doc.getString("answer")
                    // Check if this question ID exists in our new questions and has a valid answer
                    com.example.jawafai.model.PersonaQuestions.questions.any { it.id == questionId } &&
                            !answer.isNullOrBlank()
                }

                // Need at least 8 valid answers to be considered complete
                isPersonaCompleted = validAnswers.size >= 8
            } catch (e: Exception) {
                isPersonaCompleted = false
            }
        }
    }

    // Fetch user profile when screen loads
    LaunchedEffect(Unit) {
        userViewModel.fetchUserProfile()
    }

    // Get recent chats (only take first 3-4 for home screen)
    val recentChats = remember(chatSummaries) {
        chatSummaries.filter { it.lastMessage.isNotBlank() }.take(3)
    }

    // Sample notifications data - Replace with real data from your repository
    val notifications = remember {
        listOf(
            Notification("1", "Persona Update", "Complete your persona for better responses", System.currentTimeMillis() - 7200000, true),
            Notification("2", "New Feature", "Smart replies now available in Hindi", System.currentTimeMillis() - 86400000, true),
            Notification("3", "Tips", "Use voice messages for faster replies", System.currentTimeMillis() - 172800000, true)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Disable all system window insets to take full screen
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App Name
                        Text(
                            text = "जवाफ.AI",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = AppFonts.KadwaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFF395B64)
                            )
                        )

                        // Username and Profile
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentUserProfile?.let { profile ->
                                Text(
                                    text = profile.username,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = AppFonts.KarlaFontFamily,
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }

                            // Profile Picture
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onProfileClick() }
                                    .background(Color(0xFFA5C9CA))
                            ) {
                                if (userImageUrl != null) {
                                    AsyncImage(
                                        model = userImageUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Profile",
                                        modifier = Modifier.align(Alignment.Center),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding() // Add status bar padding to top bar
            )
        }
    ) { paddingValues ->
        // Use padding values only for the top bar, handle bottom manually
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Welcome Section
            item {
                Text(
                    text = "Welcome back, ${userFirstName ?: "User"}!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF395B64)
                    )
                )
            }

            // Complete Persona Section
            item {
                CompletePersonaSection(
                    onCompletePersonaClick = onCompletePersonaClick,
                    isPersonaCompleted = isPersonaCompleted
                )
            }

            // Chat Bot Section
            item {
                ChatBotSection(onChatBotClick = onChatBotClick)
            }

            // Recent Chats Section
            item {
                RecentChatsSection(
                    chats = recentChats,
                    onChatClick = onRecentChatClick,
                    onSeeAllClick = onSeeAllChatsClick
                )
            }

            // Notifications Section
            item {
                NotificationsSection(
                    notifications = notifications,
                    onNotificationClick = onNotificationClick
                )
            }

            // Add bottom padding for navigation bar
            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CompletePersonaSection(
    onCompletePersonaClick: () -> Unit,
    isPersonaCompleted: Boolean
) {
    // Lottie animation for persona
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.persona))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    if (!isPersonaCompleted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCompletePersonaClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text Section
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Complete Your Persona",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF395B64)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Help us understand your style for better responses",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }

                // Lottie Animation
                composition?.let {
                    LottieAnimation(
                        composition = it,
                        progress = { progress },
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBotSection(onChatBotClick: () -> Unit) {
    // Lottie animation for live_chatbot
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.live_chatbot))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatBotClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF395B64)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "AI Chat Companion",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get instant AI-powered responses as per your personality",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Start Chat",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KarlaFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Start Chat",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Lottie Animation
            composition?.let {
                LottieAnimation(
                    composition = it,
                    progress = { progress },
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}

@Composable
fun RecentChatsSection(
    chats: List<com.example.jawafai.model.ChatSummary>,
    onChatClick: (String, String) -> Unit,
    onSeeAllClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Chats",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = AppFonts.KarlaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF395B64)
                )
            )
            TextButton(
                onClick = onSeeAllClick
            ) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF395B64)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (chats.isEmpty()) {
            // Show empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent chats",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFonts.KaiseiDecolFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    )
                }
            }
        } else {
            chats.forEach { chat ->
                RecentChatItem(
                    chat = chat,
                    onClick = { onChatClick(chat.chatId, chat.otherUserId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun RecentChatItem(
    chat: com.example.jawafai.model.ChatSummary,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5C9CA))
            ) {
                if (chat.otherUserImageUrl != null) {
                    AsyncImage(
                        model = chat.otherUserImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        text = chat.otherUserName,
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
                            text = formatTimestamp(chat.lastMessageTimestamp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = AppFonts.KaiseiDecolFontFamily,
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        )

                        if (chat.unreadCount > 0) {
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
                                    text = chat.unreadCount.toString(),
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

                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NotificationsSection(
    notifications: List<Notification>,
    onNotificationClick: (String) -> Unit
) {
    Column {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = AppFonts.KarlaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF395B64)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        notifications.take(3).forEach { notification ->
            NotificationItem(
                notification = notification,
                onClick = { onNotificationClick(notification.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F8FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = AppFonts.KarlaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF395B64)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFonts.KaiseiDecolFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = AppFonts.KaiseiDecolFontFamily,
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Helper function to format timestamps
@Composable
fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 24 * 60 * 60 * 1000 -> { // Today
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        diff < 48 * 60 * 60 * 1000 -> { // Yesterday
            "Yesterday"
        }
        else -> { // Older
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
