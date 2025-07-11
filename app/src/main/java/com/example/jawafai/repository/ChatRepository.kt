package com.example.jawafai.repository

import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(senderId: String, receiverId: String, message: String)
    fun getMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun markMessagesAsSeen(chatId: String, receiverId: String)
    fun getChatSummaries(userId: String): Flow<List<ChatSummary>>
}

