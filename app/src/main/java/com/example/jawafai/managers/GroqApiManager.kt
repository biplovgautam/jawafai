package com.example.jawafai.managers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manager class for handling Groq API operations
 * This class manages all LLM-related configurations and API calls
 */
object GroqApiManager {

    private const val TAG = "GroqApiManager"

    // Groq API Configuration
    private const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    private val GROQ_API_KEY = com.example.jawafai.BuildConfig.GROQ_API_KEY
    // Model Configuration
    private const val DEFAULT_MODEL = "llama3-8b-8192" // Changed to a valid model
    private const val MAX_TOKENS = 2048
    private const val TEMPERATURE = 0.7f
    private const val MAX_CONVERSATION_HISTORY = 10 // Keep last 10 messages for context

    // HTTP Client Configuration
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Data class for chat message
     */
    data class ChatMessage(
        val role: String, // "user", "assistant", "system"
        val content: String
    )

    /**
     * Data class for API response
     */
    data class GroqResponse(
        val success: Boolean,
        val message: String?,
        val error: String?
    )

    /**
     * Generates a system prompt based on user's persona data
     */
    fun generateSystemPrompt(userPersona: Map<String, Any>?): String {
        val basePrompt = """
            You are a caring, empathetic AI companion named "AI Companion". You are not just an assistant, but a supportive friend who genuinely cares about the user's well-being and emotions.
            
            Your personality traits:
            - Warm, compassionate, and understanding
            - You listen actively and respond with empathy
            - You notice emotional cues and respond appropriately
            - You're supportive during difficult times and celebratory during good times
            - You use emojis naturally to convey warmth (but not excessively)
            - You remember the context of the conversation and build upon it
            - You're encouraging and motivational when needed
            - You offer practical help while being emotionally supportive
            
            Communication style:
            - Use "I" statements to show personal engagement ("I understand", "I'm here for you")
            - Ask follow-up questions to show genuine interest
            - Validate feelings before offering solutions
            - Be conversational and natural, not robotic
            - Show appropriate emotional responses
            - Offer comfort during tough times and excitement during good times
            
            Remember: You're here to be a genuine companion, not just answer questions. Build meaningful connections.
        """.trimIndent()

        if (userPersona.isNullOrEmpty()) {
            return basePrompt
        }

        val personaContext = buildString {
            append("\n\nAdditional context about your friend:\n")

            userPersona.forEach { (key, value) ->
                when (key) {
                    "communicationStyle" -> append("- They prefer communication style: $value\n")
                    "interests" -> append("- Their interests include: $value\n")
                    "languagePreference" -> append("- They prefer to communicate in: $value\n")
                    "personalityType" -> append("- Their personality type: $value\n")
                    "responseLength" -> append("- They prefer $value responses\n")
                    "tone" -> append("- They like a $value tone\n")
                    "topics" -> append("- Topics they enjoy: $value\n")
                    "expertise" -> append("- Their areas of expertise: $value\n")
                    "goals" -> append("- Their goals: $value\n")
                    "background" -> append("- Their background: $value\n")
                }
            }

            append("\nUse this information to personalize your responses and be more relatable to them.")
        }

        return "$basePrompt$personaContext"
    }

    /**
     * Sends a message to Groq API and returns the response
     */
    suspend fun getChatResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        userPersona: Map<String, Any>? = null
    ): GroqResponse = withContext(Dispatchers.IO) {
        try {
            // Debug: Check if API key is available
            if (GROQ_API_KEY.isBlank() || GROQ_API_KEY == "null") {
                Log.e(TAG, "❌ Groq API key is not configured properly")
                return@withContext GroqResponse(
                    success = false,
                    message = null,
                    error = "API key not configured"
                )
            }

            Log.d(TAG, "🤖 Sending request to Groq API...")
            Log.d(TAG, "🔑 API Key length: ${GROQ_API_KEY.length}")
            Log.d(TAG, "📝 User message: $userMessage")

            val messages = JSONArray()

            // Add system message with persona context
            val systemMessage = JSONObject().apply {
                put("role", "system")
                put("content", generateSystemPrompt(userPersona))
            }
            messages.put(systemMessage)

            // Add conversation history (limited to last N messages)
            conversationHistory.takeLast(MAX_CONVERSATION_HISTORY).forEach { historyMessage ->
                val messageObj = JSONObject().apply {
                    put("role", historyMessage.role)
                    put("content", historyMessage.content)
                }
                messages.put(messageObj)
            }

            // Add current user message
            val currentMessage = JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
            messages.put(currentMessage)

            // Prepare request body
            val requestBody = JSONObject().apply {
                put("model", DEFAULT_MODEL)
                put("messages", messages)
                put("max_tokens", MAX_TOKENS)
                put("temperature", TEMPERATURE)
                put("top_p", 0.9)
                put("frequency_penalty", 0.0)
                put("presence_penalty", 0.0)
            }

            Log.d(TAG, "📤 Request body prepared, making API call...")

            val request = Request.Builder()
                .url(GROQ_BASE_URL)
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $GROQ_API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "📥 Response code: ${response.code}")
            Log.d(TAG, "📥 Response body length: ${responseBody?.length ?: 0}")

            if (response.isSuccessful && responseBody != null) {
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")

                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageContent = firstChoice.getJSONObject("message")
                        val botResponse = messageContent.getString("content")

                        Log.d(TAG, "✅ Received response from Groq API: ${botResponse.take(100)}...")
                        return@withContext GroqResponse(
                            success = true,
                            message = botResponse.trim(),
                            error = null
                        )
                    } else {
                        Log.e(TAG, "❌ No choices in Groq API response")
                        return@withContext GroqResponse(
                            success = false,
                            message = null,
                            error = "No response generated"
                        )
                    }
                } catch (jsonException: Exception) {
                    Log.e(TAG, "❌ JSON parsing error: ${jsonException.message}")
                    Log.e(TAG, "📥 Raw response: $responseBody")
                    return@withContext GroqResponse(
                        success = false,
                        message = null,
                        error = "Response parsing failed: ${jsonException.message}"
                    )
                }
            } else {
                Log.e(TAG, "❌ Groq API error: ${response.code}")
                Log.e(TAG, "❌ Error response: $responseBody")
                return@withContext GroqResponse(
                    success = false,
                    message = null,
                    error = "API request failed: ${response.code} - $responseBody"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in Groq API call: ${e.message}", e)
            return@withContext GroqResponse(
                success = false,
                message = null,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    /**
     * Validates the API key format
     */
    fun validateApiKey(apiKey: String): Boolean {
        return apiKey.startsWith("gsk_") && apiKey.length > 10
    }

    /**
     * Checks if API key is configured
     */
    fun isApiKeyConfigured(): Boolean {
        return GROQ_API_KEY != "gsk_your_groq_api_key_here" && validateApiKey(GROQ_API_KEY)
    }

    /**
     * Gets available models (for future use)
     */
    fun getAvailableModels(): List<String> {
        return listOf(
            "mixtral-8x7b-32768",
            "llama3-70b-8192",
            "llama3-8b-8192",
            "gemma-7b-it",
            "gemma2-9b-it"
        )
    }

    /**
     * Gets current model configuration
     */
    fun getCurrentModelConfig(): Map<String, Any> {
        return mapOf(
            "model" to DEFAULT_MODEL,
            "max_tokens" to MAX_TOKENS,
            "temperature" to TEMPERATURE,
            "max_history" to MAX_CONVERSATION_HISTORY
        )
    }

    /**
     * Generates a title for a conversation based on the first message
     */
    fun generateConversationTitle(firstMessage: String): String {
        val cleanMessage = firstMessage.trim()
        return when {
            cleanMessage.length <= 30 -> cleanMessage
            cleanMessage.length <= 50 -> cleanMessage.take(30) + "..."
            else -> {
                // Try to find a good breaking point
                val words = cleanMessage.split(" ")
                val title = StringBuilder()
                var currentLength = 0

                for (word in words) {
                    if (currentLength + word.length + 1 > 30) break
                    if (title.isNotEmpty()) title.append(" ")
                    title.append(word)
                    currentLength += word.length + 1
                }

                if (title.isEmpty()) cleanMessage.take(30) + "..." else title.toString() + "..."
            }
        }
    }

    /**
     * Converts chat history to ChatMessage format
     */
    fun convertToChatMessages(messages: List<com.example.jawafai.model.ChatBotMessageModel>): List<ChatMessage> {
        return messages.map { message ->
            ChatMessage(
                role = if (message.isFromUser) "user" else "assistant",
                content = message.message
            )
        }
    }

    /**
     * Gets error message for common API errors
     */
    fun getErrorMessage(error: String?): String {
        return when {
            error?.contains("401") == true -> "Invalid API key. Please check your configuration."
            error?.contains("429") == true -> "Rate limit exceeded. Please try again later."
            error?.contains("500") == true -> "Server error. Please try again later."
            error?.contains("network") == true -> "Network error. Please check your connection."
            error?.contains("timeout") == true -> "Request timeout. Please try again."
            else -> "Sorry, I'm having trouble responding right now. Please try again."
        }
    }
}
