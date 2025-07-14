package com.example.jawafai.repository

import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import com.example.jawafai.model.LastMessage
import com.example.jawafai.model.TypingStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://jawafai-d2c23-default-rtdb.firebaseio.com/"),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChatRepository {

    private val usersRef = database.getReference("users")
    private val chatsRef = database.getReference("chats")
    private val lastMessagesRef = database.getReference("lastMessages")
    private val typingStatusRef = database.getReference("typingStatus")

    override suspend fun sendMessage(senderId: String, receiverId: String, message: String) = withContext(Dispatchers.IO) {
        val chatId = getChatId(senderId, receiverId)
        val messageId = "msg_${System.currentTimeMillis()}"
        val timestamp = System.currentTimeMillis()

        val chatMessage = ChatMessage(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            text = message,
            timestamp = timestamp,
            seen = false
        )

        try {
            // Save message to chats/user1_user2/msg_001
            chatsRef.child(chatId).child(messageId).setValue(chatMessage).await()

            // Update last messages for both users
            val lastMessage = LastMessage(
                text = message,
                timestamp = timestamp,
                seen = false
            )

            // lastMessages/senderId/receiverId
            lastMessagesRef.child(senderId).child(receiverId).setValue(lastMessage).await()
            // lastMessages/receiverId/senderId (mark as unseen for receiver)
            lastMessagesRef.child(receiverId).child(senderId).setValue(lastMessage).await()

            println("✅ Message sent successfully: $messageId")
        } catch (e: Exception) {
            println("❌ Error sending message: ${e.message}")
            throw e
        }
    }

    override fun getMessages(senderId: String, receiverId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = getChatId(senderId, receiverId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull {
                    it.getValue(ChatMessage::class.java)
                }.sortedBy { it.timestamp }

                println("📨 Received ${messages.size} messages for chat: $chatId")
                trySend(messages).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ Error getting messages: ${error.message}")
                close(error.toException())
            }
        }

        chatsRef.child(chatId).addValueEventListener(listener)
        awaitClose { chatsRef.child(chatId).removeEventListener(listener) }
    }

    override suspend fun markMessagesAsSeen(senderId: String, receiverId: String, currentUserId: String) = withContext(Dispatchers.IO) {
        val chatId = getChatId(senderId, receiverId)

        try {
            // Mark all messages from the other user as seen
            val messagesSnapshot = chatsRef.child(chatId).get().await()

            messagesSnapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(ChatMessage::class.java)
                if (message != null && message.receiverId == currentUserId && !message.seen) {
                    // Mark this specific message as seen
                    chatsRef.child(chatId).child(message.messageId).child("seen").setValue(true)
                }
            }

            // Update last message as seen for the current user
            lastMessagesRef.child(currentUserId).child(if (currentUserId == senderId) receiverId else senderId).child("seen").setValue(true)

            println("✅ Messages marked as seen for chat: $chatId")
        } catch (e: Exception) {
            println("❌ Error marking messages as seen: ${e.message}")
        }
    }

    override fun getChatSummaries(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val summaries = mutableListOf<ChatSummary>()
                val lastMessages = snapshot.children.mapNotNull { chatSnapshot ->
                    val otherUserId = chatSnapshot.key
                    val lastMessage = chatSnapshot.getValue(LastMessage::class.java)

                    if (otherUserId != null && lastMessage != null) {
                        Pair(otherUserId, lastMessage)
                    } else null
                }

                var processedCount = 0
                val totalCount = lastMessages.size

                if (totalCount == 0) {
                    trySend(emptyList()).isSuccess
                    return
                }

                lastMessages.forEach { (otherUserId, lastMessage) ->
                    // Fetch user info for display name
                    usersRef.child(otherUserId).get().addOnSuccessListener { userSnapshot ->
                        val otherUserName = userSnapshot.child("displayName").getValue(String::class.java)
                            ?: userSnapshot.child("username").getValue(String::class.java)
                            ?: userSnapshot.child("email").getValue(String::class.java)
                            ?: "Unknown User"
                        val otherUserImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)

                        // Count unread messages for this chat
                        val chatId = getChatId(userId, otherUserId)
                        chatsRef.child(chatId).orderByChild("receiverId").equalTo(userId).get().addOnSuccessListener { messagesSnapshot ->
                            var unreadCount = 0
                            messagesSnapshot.children.forEach { messageSnapshot ->
                                val message = messageSnapshot.getValue(ChatMessage::class.java)
                                if (message != null && !message.seen && message.receiverId == userId) {
                                    unreadCount++
                                }
                            }

                            summaries.add(
                                ChatSummary(
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    otherUserName = otherUserName,
                                    otherUserImageUrl = otherUserImageUrl,
                                    lastMessage = lastMessage.text,
                                    lastMessageTimestamp = lastMessage.timestamp,
                                    isLastMessageSeen = lastMessage.seen,
                                    unreadCount = unreadCount
                                )
                            )

                            processedCount++
                            if (processedCount == totalCount) {
                                val sortedSummaries = summaries.sortedByDescending { it.lastMessageTimestamp }
                                trySend(sortedSummaries).isSuccess
                            }
                        }.addOnFailureListener {
                            // If counting fails, still add the summary without unread count
                            summaries.add(
                                ChatSummary(
                                    chatId = chatId,
                                    otherUserId = otherUserId,
                                    otherUserName = otherUserName,
                                    otherUserImageUrl = otherUserImageUrl,
                                    lastMessage = lastMessage.text,
                                    lastMessageTimestamp = lastMessage.timestamp,
                                    isLastMessageSeen = lastMessage.seen,
                                    unreadCount = 0
                                )
                            )

                            processedCount++
                            if (processedCount == totalCount) {
                                val sortedSummaries = summaries.sortedByDescending { it.lastMessageTimestamp }
                                trySend(sortedSummaries).isSuccess
                            }
                        }
                    }.addOnFailureListener {
                        processedCount++
                        if (processedCount == totalCount) {
                            val sortedSummaries = summaries.sortedByDescending { it.lastMessageTimestamp }
                            trySend(sortedSummaries).isSuccess
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        lastMessagesRef.child(userId).addValueEventListener(listener)
        awaitClose { lastMessagesRef.child(userId).removeEventListener(listener) }
    }

    override suspend fun updateTypingStatus(userId: String, typingTo: String, isTyping: Boolean) = withContext(Dispatchers.IO) {
        val typingStatus = TypingStatus(
            typingTo = typingTo,
            isTyping = isTyping,
            lastUpdate = System.currentTimeMillis()
        )

        try {
            if (isTyping) {
                typingStatusRef.child(userId).setValue(typingStatus).await()
            } else {
                typingStatusRef.child(userId).removeValue().await()
            }
            println("✅ Typing status updated: $userId -> $typingTo, typing: $isTyping")
        } catch (e: Exception) {
            println("❌ Error updating typing status: ${e.message}")
        }
    }

    override fun getTypingStatus(userId: String): Flow<TypingStatus?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingStatus = snapshot.getValue(TypingStatus::class.java)
                trySend(typingStatus).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        typingStatusRef.child(userId).addValueEventListener(listener)
        awaitClose { typingStatusRef.child(userId).removeEventListener(listener) }
    }

    override fun getLastMessages(userId: String): Flow<Map<String, LastMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessages = snapshot.children.associate { childSnapshot ->
                    val otherUserId = childSnapshot.key ?: ""
                    val lastMessage = childSnapshot.getValue(LastMessage::class.java) ?: LastMessage()
                    otherUserId to lastMessage
                }
                trySend(lastMessages).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        lastMessagesRef.child(userId).addValueEventListener(listener)
        awaitClose { lastMessagesRef.child(userId).removeEventListener(listener) }
    }

    override suspend fun findUserByEmailOrUsername(query: String): UserProfile? = withContext(Dispatchers.IO) {
        return@withContext try {
            println("🔍 DEBUG: Starting user search for query: '$query'")

            // Clean the query - remove @ if present and trim whitespace
            val cleanQuery = query.trim().removePrefix("@").lowercase()
            println("🔍 DEBUG: Cleaned query: '$cleanQuery'")

            // First, ensure we have the current user in the database
            val currentUser = auth.currentUser
            if (currentUser != null) {
                println("🔍 DEBUG: Ensuring current user is in database...")
                val currentUserProfile = mapOf(
                    "email" to (currentUser.email ?: ""),
                    "username" to extractUsernameFromEmail(currentUser.email ?: ""),
                    "displayName" to (currentUser.displayName ?: extractUsernameFromEmail(currentUser.email ?: "")),
                    "profileImageUrl" to currentUser.photoUrl?.toString(),
                    "createdAt" to System.currentTimeMillis(),
                    "lastLoginAt" to System.currentTimeMillis()
                )
                usersRef.child(currentUser.uid).setValue(currentUserProfile).await()
                println("🔍 DEBUG: Current user saved to database")
            }

            // Debug: First let's see all users in database
            println("🔍 DEBUG: Checking all users in database...")
            val allUsersSnapshot = usersRef.get().await()
            println("🔍 DEBUG: Total users in database: ${allUsersSnapshot.childrenCount}")

            if (allUsersSnapshot.childrenCount == 0L) {
                println("🔍 DEBUG: No users found in database! Creating test users...")
                // Create some test users for debugging
                createTestUsers()

                // Try again after creating test users
                val retrySnapshot = usersRef.get().await()
                println("🔍 DEBUG: After creating test users, total: ${retrySnapshot.childrenCount}")
            }

            // List all users for debugging
            allUsersSnapshot.children.forEach { userSnapshot ->
                val email = userSnapshot.child("email").getValue(String::class.java)
                val username = userSnapshot.child("username").getValue(String::class.java)
                val displayName = userSnapshot.child("displayName").getValue(String::class.java)
                println("🔍 DEBUG: User - ID: ${userSnapshot.key}, Email: $email, Username: $username, DisplayName: $displayName")
            }

            // Try searching by email (case-insensitive)
            println("🔍 DEBUG: Searching by email...")
            var foundUser: UserProfile? = null

            // Since Firebase doesn't support case-insensitive queries, we need to search through all users
            allUsersSnapshot.children.forEach { userSnapshot ->
                val userEmail = userSnapshot.child("email").getValue(String::class.java)?.lowercase()
                val userName = userSnapshot.child("username").getValue(String::class.java)?.lowercase()

                if (userEmail == cleanQuery || userName == cleanQuery) {
                    println("🔍 DEBUG: Found matching user!")
                    foundUser = UserProfile(
                        userId = userSnapshot.key ?: "",
                        username = userSnapshot.child("username").getValue(String::class.java) ?: "",
                        email = userSnapshot.child("email").getValue(String::class.java) ?: "",
                        displayName = userSnapshot.child("displayName").getValue(String::class.java) ?: "",
                        profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                    )
                    return@forEach
                }
            }

            if (foundUser != null) {
                println("🔍 DEBUG: Found user profile: $foundUser")
                foundUser
            } else {
                println("🔍 DEBUG: No user found with email/username: $cleanQuery")
                null
            }

        } catch (e: Exception) {
            println("🔍 DEBUG: Error searching for user: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private suspend fun createTestUsers() {
        try {
            val testUsers = listOf(
                mapOf(
                    "email" to "test@example.com",
                    "username" to "testuser",
                    "displayName" to "Test User",
                    "profileImageUrl" to null,
                    "createdAt" to System.currentTimeMillis()
                ),
                mapOf(
                    "email" to "john@example.com",
                    "username" to "johndoe",
                    "displayName" to "John Doe",
                    "profileImageUrl" to null,
                    "createdAt" to System.currentTimeMillis()
                ),
                mapOf(
                    "email" to "jane@example.com",
                    "username" to "janedoe",
                    "displayName" to "Jane Doe",
                    "profileImageUrl" to null,
                    "createdAt" to System.currentTimeMillis()
                )
            )

            testUsers.forEach { userData ->
                val userId = usersRef.push().key!!
                usersRef.child(userId).setValue(userData).await()
                println("🔍 DEBUG: Created test user: ${userData["email"]}")
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: Error creating test users: ${e.message}")
        }
    }

    private fun extractUsernameFromEmail(email: String): String {
        return email.substringBefore("@").lowercase()
    }

    override suspend fun findUserById(userId: String): UserProfile? {
        return try {
            println("🔍 DEBUG: Finding user by ID: '$userId'")
            val userSnapshot = usersRef.child(userId).get().await()

            if (userSnapshot.exists()) {
                val userProfile = UserProfile(
                    userId = userSnapshot.key ?: "",
                    username = userSnapshot.child("username").getValue(String::class.java) ?: "",
                    email = userSnapshot.child("email").getValue(String::class.java) ?: "",
                    displayName = userSnapshot.child("displayName").getValue(String::class.java) ?: "",
                    profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)
                )
                println("🔍 DEBUG: Found user by ID: $userProfile")
                userProfile
            } else {
                println("🔍 DEBUG: No user found with ID: $userId")
                null
            }
        } catch (e: Exception) {
            println("🔍 DEBUG: Error finding user by ID: ${e.message}")
            null
        }
    }

    override suspend fun createChatWithUser(currentUserId: String, otherUserId: String): String {
        val chatId = getChatId(currentUserId, otherUserId)
        val timestamp = System.currentTimeMillis()

        val initialLastMessage = LastMessage(
            text = "",
            timestamp = timestamp,
            seen = true
        )

        try {
            lastMessagesRef.child(currentUserId).child(otherUserId).setValue(initialLastMessage).await()
            lastMessagesRef.child(otherUserId).child(currentUserId).setValue(initialLastMessage).await()
        } catch (e: Exception) {
            println("❌ Error creating chat: ${e.message}")
        }

        return chatId
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    override suspend fun deleteMessage(messageId: String, senderId: String, receiverId: String) = withContext(Dispatchers.IO) {
        val chatId = getChatId(senderId, receiverId)

        try {
            // Delete the message from the chat
            chatsRef.child(chatId).child(messageId).removeValue().await()

            // Update last message if this was the last message
            val remainingMessagesSnapshot = chatsRef.child(chatId).orderByChild("timestamp").limitToLast(1).get().await()

            if (remainingMessagesSnapshot.exists()) {
                // There are still messages, update last message to the most recent one
                val lastMessage = remainingMessagesSnapshot.children.firstOrNull()?.getValue(ChatMessage::class.java)
                if (lastMessage != null) {
                    val newLastMessage = LastMessage(
                        text = lastMessage.text,
                        timestamp = lastMessage.timestamp,
                        seen = lastMessage.seen
                    )

                    lastMessagesRef.child(senderId).child(receiverId).setValue(newLastMessage).await()
                    lastMessagesRef.child(receiverId).child(senderId).setValue(newLastMessage).await()
                }
            } else {
                // No messages left, remove the chat from last messages
                lastMessagesRef.child(senderId).child(receiverId).removeValue().await()
                lastMessagesRef.child(receiverId).child(senderId).removeValue().await()
            }

            println("✅ Message deleted successfully: $messageId")
        } catch (e: Exception) {
            println("❌ Error deleting message: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteChat(currentUserId: String, otherUserId: String) = withContext(Dispatchers.IO) {
        val chatId = getChatId(currentUserId, otherUserId)

        try {
            // Delete all messages in the chat
            chatsRef.child(chatId).removeValue().await()

            // Remove last message entries for both users
            lastMessagesRef.child(currentUserId).child(otherUserId).removeValue().await()
            lastMessagesRef.child(otherUserId).child(currentUserId).removeValue().await()

            // Remove typing status entries
            typingStatusRef.child(currentUserId).removeValue().await()
            typingStatusRef.child(otherUserId).removeValue().await()

            println("✅ Chat deleted successfully: $chatId")
        } catch (e: Exception) {
            println("❌ Error deleting chat: ${e.message}")
            throw e
        }
    }
}
