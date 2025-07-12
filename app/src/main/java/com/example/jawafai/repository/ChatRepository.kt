package com.example.jawafai.repository

import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import kotlinx.coroutines.flow.Flow

data class UserProfile(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String? = null
)

interface ChatRepository {
    suspend fun sendMessage(senderId: String, receiverId: String, message: String)
    fun getMessages(chatId: String): Flow<List<ChatMessage>>
    suspend fun markMessagesAsSeen(chatId: String, receiverId: String)
    fun getChatSummaries(userId: String): Flow<List<ChatSummary>>
    suspend fun findUserByEmailOrUsername(query: String): UserProfile?
    suspend fun createChatWithUser(currentUserId: String, otherUserId: String): String
}
