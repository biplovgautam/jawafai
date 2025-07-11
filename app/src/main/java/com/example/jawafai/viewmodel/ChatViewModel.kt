package com.example.jawafai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.repository.ChatRepository
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
}

