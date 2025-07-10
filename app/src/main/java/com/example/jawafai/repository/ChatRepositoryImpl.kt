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
                val summaries = snapshot.children.mapNotNull { chatSnapshot ->
                    val otherUserId = chatSnapshot.key
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastMessageTimestamp = chatSnapshot.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0
                    val isSeen = chatSnapshot.child("isSeen").getValue(Boolean::class.java) ?: false

                    if (otherUserId != null) {
                        ChatSummary(
                            chatId = getChatId(userId, otherUserId),
                            otherUserId = otherUserId,
                            lastMessage = lastMessage,
                            lastMessageTimestamp = lastMessageTimestamp,
                            isLastMessageSeen = isSeen
                        )
                    } else {
                        null
                    }
                }.sortedByDescending { it.lastMessageTimestamp }
                trySend(summaries).isSuccess
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        chatsRef.child(userId).addValueEventListener(listener)
        awaitClose { chatsRef.child(userId).removeEventListener(listener) }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }
}
