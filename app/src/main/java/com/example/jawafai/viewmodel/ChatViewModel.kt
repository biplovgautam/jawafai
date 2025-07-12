package com.example.jawafai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.repository.ChatRepository
import com.example.jawafai.repository.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
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

    fun getMessagesForChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun sendMessage(receiverId: String, message: String) {
        currentUserId?.let { senderId ->
            viewModelScope.launch {
                chatRepository.sendMessage(senderId, receiverId, message)
            }
        }
    }

    fun markMessagesAsSeen(chatId: String, receiverId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsSeen(chatId, receiverId)
        }
    }

    suspend fun findUserByEmailOrUsername(query: String): UserProfile? {
        _isLoading.value = true
        _errorMessage.value = null
        return try {
            val user = chatRepository.findUserByEmailOrUsername(query)
            _foundUser.value = user
            if (user == null) {
                _errorMessage.value = "User not found"
            }
            user
        } catch (e: Exception) {
            _errorMessage.value = "Error searching for user: ${e.message}"
            null
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createChatWithUser(otherUserId: String): String? {
        return currentUserId?.let { currentId ->
            try {
                chatRepository.createChatWithUser(currentId, otherUserId)
            } catch (e: Exception) {
                _errorMessage.value = "Error creating chat: ${e.message}"
                null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearFoundUser() {
        _foundUser.value = null
    }
}
