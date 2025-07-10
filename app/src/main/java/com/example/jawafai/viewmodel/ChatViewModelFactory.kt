package com.example.jawafai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.jawafai.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepository, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

