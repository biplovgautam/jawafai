package com.example.jawafai.service

import android.content.Context
import android.util.Log
import com.example.jawafai.managers.GroqApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * AI Reply Manager for generating contextually aware replies to notifications
 * Uses GroqApiManager for LLM integration with conversation context and user persona
 */
object NotificationAIReplyManager {

    private const val TAG = "NotificationAIReply"
    private const val MAX_CONTEXT_MESSAGES = 10

    /**
     * Generate AI reply for a notification with conversation context
     */
    suspend fun generateAIReply(
        notification: NotificationMemoryStore.ExternalNotification,
        userPersona: Map<String, Any>? = null,
        context: Context
    ): AIReplyResult = withContext(Dispatchers.IO) {

        try {
            Log.d(TAG, "🤖 Generating AI reply for: ${notification.conversationId}")
            Log.d(TAG, "📱 App: ${notification.packageName}")
            Log.d(TAG, "💬 Message: ${notification.text}")

            // Get conversation context (last 10 messages from same conversation)
            val conversationHistory = NotificationMemoryStore.getConversationContext(
                notification.conversationId,
                MAX_CONTEXT_MESSAGES
            )

            Log.d(TAG, "📚 Conversation history: ${conversationHistory.size} messages")

            // Build context-aware prompt
            val contextPrompt = buildContextPrompt(notification, conversationHistory, userPersona)

            // Convert notification history to chat messages for GroqApiManager
            val chatHistory = convertToChatMessages(conversationHistory)

            // Generate reply using GroqApiManager
            val groqResponse = GroqApiManager.getChatResponse(
                userMessage = contextPrompt,
                conversationHistory = chatHistory,
                userPersona = userPersona
            )

            if (groqResponse.success && groqResponse.message != null) {
                Log.d(TAG, "✅ AI reply generated successfully")
                Log.d(TAG, "🎯 Reply: ${groqResponse.message.take(100)}...")

                // Update notification with AI reply
                NotificationMemoryStore.updateAIReply(notification.hash, groqResponse.message)

                return@withContext AIReplyResult(
                    success = true,
                    reply = groqResponse.message,
                    error = null,
                    conversationId = notification.conversationId
                )
            } else {
                Log.e(TAG, "❌ AI reply generation failed: ${groqResponse.error}")
                return@withContext AIReplyResult(
                    success = false,
                    reply = null,
                    error = groqResponse.error ?: "Unknown error",
                    conversationId = notification.conversationId
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in AI reply generation: ${e.message}", e)
            return@withContext AIReplyResult(
                success = false,
                reply = null,
                error = e.message ?: "Unknown error",
                conversationId = notification.conversationId
            )
        }
    }

    /**
     * Build context-aware prompt for AI reply generation
     */
    private fun buildContextPrompt(
        currentNotification: NotificationMemoryStore.ExternalNotification,
        conversationHistory: List<NotificationMemoryStore.ExternalNotification>,
        userPersona: Map<String, Any>?
    ): String {

        val appName = getAppName(currentNotification.packageName)
        val isGroupChat = !currentNotification.conversationTitle.isNullOrBlank()
        val conversationType = if (isGroupChat) "group chat" else "individual conversation"

        return buildString {
            append("🤖 SMART REPLY GENERATION REQUEST 🤖\n\n")

            // Context Information
            append("📱 CONTEXT:\n")
            append("- Platform: $appName\n")
            append("- Type: $conversationType\n")
            if (isGroupChat) {
                append("- Group: ${currentNotification.conversationTitle}\n")
            }
            append("- Sender: ${currentNotification.sender ?: currentNotification.title}\n")
            append("- Time: ${formatTimestamp(currentNotification.time)}\n\n")

            // Conversation History
            if (conversationHistory.isNotEmpty()) {
                append("📚 CONVERSATION HISTORY (Last ${conversationHistory.size} messages):\n")
                conversationHistory.forEach { msg ->
                    val sender = msg.sender ?: msg.title
                    val time = formatTimestamp(msg.time)
                    append("[$time] $sender: ${msg.text}\n")
                }
                append("\n")
            }

            // Current Message
            append("💬 CURRENT MESSAGE TO REPLY TO:\n")
            append("From: ${currentNotification.sender ?: currentNotification.title}\n")
            append("Message: \"${currentNotification.text}\"\n\n")

            // Instructions
            append("🎯 REPLY INSTRUCTIONS:\n")
            append("Generate a contextually appropriate reply that:\n")
            append("1. Considers the conversation history and tone\n")
            append("2. Matches the communication style of previous messages\n")
            append("3. Is relevant to the current message\n")
            append("4. Feels natural and human-like\n")
            append("5. Is concise but meaningful\n")

            if (isGroupChat) {
                append("6. Is appropriate for group chat context\n")
            }

            // App-specific instructions
            when (currentNotification.packageName) {
                "com.whatsapp", "com.whatsapp.w4b" -> {
                    append("7. Use WhatsApp-appropriate casual tone\n")
                }
                "com.instagram.android" -> {
                    append("7. Use Instagram-appropriate casual/trendy tone\n")
                }
                "com.facebook.orca" -> {
                    append("7. Use Messenger-appropriate friendly tone\n")
                }
                "com.telegram.messenger" -> {
                    append("7. Use Telegram-appropriate direct tone\n")
                }
            }

            append("\n⚡ Generate ONLY the reply text, no explanations or metadata.\n")
            append("Keep it natural, contextual, and conversational!")
        }
    }

    /**
     * Convert notification history to chat messages format
     */
    private fun convertToChatMessages(
        notifications: List<NotificationMemoryStore.ExternalNotification>
    ): List<GroqApiManager.ChatMessage> {

        val chatMessages = mutableListOf<GroqApiManager.ChatMessage>()

        // Add context as system message
        if (notifications.isNotEmpty()) {
            chatMessages.add(
                GroqApiManager.ChatMessage(
                    role = "system",
                    content = "You are generating contextual replies based on a messaging conversation. Maintain consistency with the conversation tone and style."
                )
            )
        }

        // Add conversation history
        notifications.forEach { notification ->
            // Add original message as user message
            chatMessages.add(
                GroqApiManager.ChatMessage(
                    role = "user",
                    content = "${notification.sender ?: notification.title}: ${notification.text}"
                )
            )

            // Add AI reply if available as assistant message
            if (notification.ai_reply.isNotBlank()) {
                chatMessages.add(
                    GroqApiManager.ChatMessage(
                        role = "assistant",
                        content = notification.ai_reply
                    )
                )
            }
        }

        return chatMessages
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "com.whatsapp.w4b" -> "WhatsApp Business"
            "com.instagram.android" -> "Instagram"
            "com.facebook.orca" -> "Facebook Messenger"
            "com.facebook.katana" -> "Facebook"
            "com.telegram.messenger" -> "Telegram"
            "com.snapchat.android" -> "Snapchat"
            "com.twitter.android" -> "Twitter"
            else -> packageName
        }
    }

    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Analyze conversation tone from history
     */
    fun analyzeConversationTone(
        conversationHistory: List<NotificationMemoryStore.ExternalNotification>
    ): ConversationTone {

        if (conversationHistory.isEmpty()) {
            return ConversationTone.NEUTRAL
        }

        val recentMessages = conversationHistory.take(5)
        val combinedText = recentMessages.joinToString(" ") { it.text.lowercase() }

        return when {
            combinedText.contains(Regex("(haha|lol|😂|😄|😊|funny|joke)")) -> ConversationTone.CASUAL_FUNNY
            combinedText.contains(Regex("(thanks|thank you|appreciate|please|sorry)")) -> ConversationTone.POLITE_FORMAL
            combinedText.contains(Regex("(love|miss|care|heart|❤️|💕)")) -> ConversationTone.AFFECTIONATE
            combinedText.contains(Regex("(urgent|asap|important|quick|now)")) -> ConversationTone.URGENT
            combinedText.contains(Regex("(meeting|work|business|project|task)")) -> ConversationTone.PROFESSIONAL
            else -> ConversationTone.NEUTRAL
        }
    }

    /**
     * Get conversation statistics
     */
    fun getConversationStats(conversationId: String): ConversationStats {
        val notifications = NotificationMemoryStore.getNotificationsByConversation(conversationId)
        val withReplies = notifications.count { it.ai_reply.isNotBlank() }
        val sent = notifications.count { it.is_sent }

        return ConversationStats(
            totalMessages = notifications.size,
            messagesWithAIReplies = withReplies,
            sentReplies = sent,
            tone = analyzeConversationTone(notifications)
        )
    }

    /**
     * Data classes for results
     */
    data class AIReplyResult(
        val success: Boolean,
        val reply: String?,
        val error: String?,
        val conversationId: String
    )

    data class ConversationStats(
        val totalMessages: Int,
        val messagesWithAIReplies: Int,
        val sentReplies: Int,
        val tone: ConversationTone
    )

    enum class ConversationTone {
        CASUAL_FUNNY,
        POLITE_FORMAL,
        AFFECTIONATE,
        URGENT,
        PROFESSIONAL,
        NEUTRAL
    }
}
