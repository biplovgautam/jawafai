package com.example.jawafai.repository

import com.example.jawafai.model.ChatMessage
import com.example.jawafai.model.ChatSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ChatRepository {

    private val usersRef = database.getReference("users")
    private val chatsRef = database.getReference("chats")
    private val messagesRef = database.getReference("messages")

    override suspend fun sendMessage(senderId: String, receiverId: String, message: String) {
        val chatId = getChatId(senderId, receiverId)
        val messageId = messagesRef.child(chatId).push().key ?: return
        val timestamp = System.currentTimeMillis()

        val chatMessage = ChatMessage(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            messageText = message,
            timestamp = timestamp,
            isSeen = false
        )

        messagesRef.child(chatId).child(messageId).setValue(chatMessage).await()

        val lastMessageMeta = mapOf(
            "lastMessage" to message,
            "lastMessageTimestamp" to timestamp,
            "lastMessageSenderId" to senderId,
            "isSeen" to false
        )

        chatsRef.child(senderId).child(receiverId).updateChildren(lastMessageMeta)
        chatsRef.child(receiverId).child(senderId).updateChildren(lastMessageMeta)
    }

    override fun getMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        messagesRef.child(chatId).addValueEventListener(listener)
        awaitClose { messagesRef.child(chatId).removeEventListener(listener) }
    }

    override suspend fun markMessagesAsSeen(chatId: String, receiverId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messagesQuery = messagesRef.child(chatId).orderByChild("receiverId").equalTo(currentUserId)

        messagesQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    if (messageSnapshot.child("seen").getValue(Boolean::class.java) == false) {
                        messageSnapshot.ref.child("seen").setValue(true)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        chatsRef.child(currentUserId).child(receiverId).child("isSeen").setValue(true)
        chatsRef.child(receiverId).child(currentUserId).child("isSeen").setValue(true)
    }

    override fun getChatSummaries(userId: String): Flow<List<ChatSummary>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val summaries = mutableListOf<ChatSummary>()
                val summariesData = snapshot.children.mapNotNull { chatSnapshot ->
                    val otherUserId = chatSnapshot.key
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastMessageTimestamp = chatSnapshot.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0
                    val isSeen = chatSnapshot.child("isSeen").getValue(Boolean::class.java) ?: false

                    if (otherUserId != null) {
                        Triple(otherUserId, lastMessage to lastMessageTimestamp, isSeen)
                    } else {
                        null
                    }
                }

                // Fetch user names for each chat
                var processedCount = 0
                val totalCount = summariesData.size

                if (totalCount == 0) {
                    trySend(emptyList()).isSuccess
                    return
                }

                summariesData.forEach { (otherUserId, messageData, isSeen) ->
                    usersRef.child(otherUserId).get().addOnSuccessListener { userSnapshot ->
                        val otherUserName = userSnapshot.child("displayName").getValue(String::class.java)
                            ?: userSnapshot.child("username").getValue(String::class.java)
                            ?: userSnapshot.child("email").getValue(String::class.java)
                            ?: "Unknown User"
                        val otherUserImageUrl = userSnapshot.child("profileImageUrl").getValue(String::class.java)

                        summaries.add(
                            ChatSummary(
                                chatId = getChatId(userId, otherUserId),
                                otherUserId = otherUserId,
                                otherUserName = otherUserName,
                                otherUserImageUrl = otherUserImageUrl,
                                lastMessage = messageData.first,
                                lastMessageTimestamp = messageData.second,
                                isLastMessageSeen = isSeen
                            )
                        )

                        processedCount++
                        if (processedCount == totalCount) {
                            val sortedSummaries = summaries.sortedByDescending { it.lastMessageTimestamp }
                            trySend(sortedSummaries).isSuccess
                        }
                    }.addOnFailureListener {
                        summaries.add(
                            ChatSummary(
                                chatId = getChatId(userId, otherUserId),
                                otherUserId = otherUserId,
                                otherUserName = "Unknown User",
                                otherUserImageUrl = null,
                                lastMessage = messageData.first,
                                lastMessageTimestamp = messageData.second,
                                isLastMessageSeen = isSeen
                            )
                        )

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
        chatsRef.child(userId).addValueEventListener(listener)
        awaitClose { chatsRef.child(userId).removeEventListener(listener) }
    }

    override suspend fun findUserByEmailOrUsername(query: String): UserProfile? {
        return try {
            val userSnapshot = usersRef.orderByChild("email").equalTo(query).get().await()
            if (userSnapshot.exists()) {
                val userData = userSnapshot.children.first()
                UserProfile(
                    userId = userData.key ?: "",
                    username = userData.child("username").getValue(String::class.java) ?: "",
                    email = userData.child("email").getValue(String::class.java) ?: "",
                    displayName = userData.child("displayName").getValue(String::class.java) ?: "",
                    profileImageUrl = userData.child("profileImageUrl").getValue(String::class.java)
                )
            } else {
                // Try searching by username
                val usernameSnapshot = usersRef.orderByChild("username").equalTo(query).get().await()
                if (usernameSnapshot.exists()) {
                    val userData = usernameSnapshot.children.first()
                    UserProfile(
                        userId = userData.key ?: "",
                        username = userData.child("username").getValue(String::class.java) ?: "",
                        email = userData.child("email").getValue(String::class.java) ?: "",
                        displayName = userData.child("displayName").getValue(String::class.java) ?: "",
                        profileImageUrl = userData.child("profileImageUrl").getValue(String::class.java)
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createChatWithUser(currentUserId: String, otherUserId: String): String {
        val chatId = getChatId(currentUserId, otherUserId)
        val timestamp = System.currentTimeMillis()

        // Initialize chat metadata for both users
        val chatMeta = mapOf(
            "lastMessage" to "",
            "lastMessageTimestamp" to timestamp,
            "lastMessageSenderId" to "",
            "isSeen" to true
        )

        chatsRef.child(currentUserId).child(otherUserId).updateChildren(chatMeta).await()
        chatsRef.child(otherUserId).child(currentUserId).updateChildren(chatMeta).await()

        return chatId
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }
}
