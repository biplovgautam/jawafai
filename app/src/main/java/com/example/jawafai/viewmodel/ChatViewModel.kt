package com.example.jawafai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.model.TypingStatus
import com.example.jawafai.repository.ChatRepository
import com.example.jawafai.repository.UserProfile
import com.example.jawafai.repository.UserRepository // Import UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository, // Add UserRepository for Firestore search
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private val _chatSummaries = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatSummaries: StateFlow<List<ChatSummary>> = _chatSummaries.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _foundUser = MutableStateFlow<UserProfile?>(null)
    val foundUser: StateFlow<UserProfile?> = _foundUser.asStateFlow()

    // Typing indicator states
    private val _typingStatus = MutableStateFlow<TypingStatus?>(null)
    val typingStatus: StateFlow<TypingStatus?> = _typingStatus.asStateFlow()

    private val _isUserTyping = MutableStateFlow(false)
    val isUserTyping: StateFlow<Boolean> = _isUserTyping.asStateFlow()

    // Current chat state
    private var currentChatUserId: String? = null
    private var typingIndicatorJob: Job? = null

    init {
        fetchChatSummaries()
    }

    private fun fetchChatSummaries() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                chatRepository.getChatSummaries(userId).collect { summaries ->
                    _chatSummaries.value = summaries
                }
            }
        }
    }

    fun getMessagesForChat(senderId: String, receiverId: String) {
        currentChatUserId = receiverId
        viewModelScope.launch {
            // Get messages using the new method signature
            chatRepository.getMessages(senderId, receiverId).collect { messages ->
                _messages.value = messages
            }
        }

        // Start listening for typing status from the other user
        startTypingStatusListener(receiverId)
    }

    fun sendMessage(receiverId: String, message: String) {
        currentUserId?.let { senderId ->
            viewModelScope.launch {
                // Stop typing indicator when sending message
                stopTyping(receiverId)
                chatRepository.sendMessage(senderId, receiverId, message)
            }
        }
    }

    fun markMessagesAsSeen(senderId: String, receiverId: String) {
        currentUserId?.let { currentId ->
            viewModelScope.launch {
                chatRepository.markMessagesAsSeen(senderId, receiverId, currentId)
            }
        }
    }

    // Typing indicator methods
    fun startTyping(receiverId: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isUserTyping.value = true
                chatRepository.updateTypingStatus(userId, receiverId, true)

                // Cancel previous job
                typingIndicatorJob?.cancel()

                // Auto-stop typing after 3 seconds of inactivity
                typingIndicatorJob = viewModelScope.launch {
                    delay(3000)
                    stopTyping(receiverId)
                }
            }
        }
    }

    fun stopTyping(receiverId: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _isUserTyping.value = false
                chatRepository.updateTypingStatus(userId, receiverId, false)
                typingIndicatorJob?.cancel()
            }
        }
    }

    private fun startTypingStatusListener(otherUserId: String) {
        viewModelScope.launch {
            chatRepository.getTypingStatus(otherUserId).collect { typingStatus ->
                // Only show typing indicator if the other user is typing to current user
                val shouldShowTyping = typingStatus?.isTyping == true &&
                                     typingStatus.typingTo == currentUserId

                _typingStatus.value = if (shouldShowTyping) typingStatus else null
            }
        }
    }

    fun onTextChanged(receiverId: String, text: String) {
        if (text.isNotEmpty()) {
            startTyping(receiverId)
        } else {
            stopTyping(receiverId)
        }
    }

    suspend fun findUserByEmailOrUsername(query: String): UserProfile? {
        println("🔍 DEBUG: ChatViewModel - findUserByEmailOrUsername called with query: '$query'")
        _isLoading.value = true
        _errorMessage.value = null
        _foundUser.value = null

        return try {
            if (query.isBlank()) {
                println("🔍 DEBUG: ChatViewModel - Query is blank")
                _errorMessage.value = "Please enter a username or email"
                return null
            }

            println("🔍 DEBUG: ChatViewModel - Searching in Firestore first...")

            // Try Firestore search first (better search capabilities)
            val userModel = userRepository.findUserByEmailOrUsername(query)

            if (userModel != null) {
                println("🔍 DEBUG: ChatViewModel - User found in Firestore: ${userModel.username}")

                // Convert UserModel to UserProfile for compatibility
                val userProfile = UserProfile(
                    userId = userModel.id,
                    username = userModel.username,
                    email = userModel.email,
                    displayName = "${userModel.firstName} ${userModel.lastName}".trim().ifEmpty { userModel.username },
                    profileImageUrl = userModel.imageUrl
                )

                _foundUser.value = userProfile
                return userProfile
            }

            // Fallback to Realtime Database search if not found in Firestore
            println("🔍 DEBUG: ChatViewModel - User not found in Firestore, trying Realtime Database...")
            val realtimeUser = chatRepository.findUserByEmailOrUsername(query)

            if (realtimeUser != null) {
                println("🔍 DEBUG: ChatViewModel - User found in Realtime Database: ${realtimeUser.username}")
                _foundUser.value = realtimeUser
                return realtimeUser
            }

            println("🔍 DEBUG: ChatViewModel - No user found in either database")
            _errorMessage.value = "User not found. Please check the email or username."
            null

        } catch (e: Exception) {
            println("🔍 DEBUG: ChatViewModel - Error: ${e.message}")
            e.printStackTrace()
            _errorMessage.value = "Error searching for user: ${e.localizedMessage}"
            null
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createChatWithUser(otherUserId: String): String? {
        println("🔍 DEBUG: ChatViewModel - createChatWithUser called with otherUserId: '$otherUserId'")

        return currentUserId?.let { currentId ->
            try {
                println("🔍 DEBUG: ChatViewModel - Current user ID: $currentId")
                val chatId = chatRepository.createChatWithUser(currentId, otherUserId)
                println("🔍 DEBUG: ChatViewModel - Chat created with ID: $chatId")
                chatId
            } catch (e: Exception) {
                println("🔍 DEBUG: ChatViewModel - Error creating chat: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Error creating chat: ${e.localizedMessage}"
                null
            }
        } ?: run {
            println("🔍 DEBUG: ChatViewModel - Current user ID is null")
            _errorMessage.value = "User not logged in"
            null
        }
    }

    suspend fun findUserById(userId: String): UserProfile? {
        println("🔍 DEBUG: ChatViewModel - findUserById called with userId: '$userId'")
        return try {
            val user = chatRepository.findUserById(userId)
            if (user != null) {
                println("🔍 DEBUG: ChatViewModel - User found by ID: ${user.displayName} (${user.email})")
            } else {
                println("🔍 DEBUG: ChatViewModel - No user found with ID: $userId")
            }
            user
        } catch (e: Exception) {
            println("🔍 DEBUG: ChatViewModel - Error finding user by ID: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearFoundUser() {
        _foundUser.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up typing indicator when leaving chat
        currentChatUserId?.let { receiverId ->
            stopTyping(receiverId)
        }
    }
}
