package com.vishnu.assistant.data

import com.vishnu.assistant.core.network.ChatRequest
import com.vishnu.assistant.core.network.EnodaApi
import com.vishnu.assistant.core.network.NetworkModule

/**
 * Single source of truth for conversations with the EnodaAI
 * backend. The ViewModel depends on this - never on Retrofit.
 */
class ChatRepository(
    private val api: EnodaApi = NetworkModule.enodaApi
) {

    /** Outcome of one request: either the reply, or a friendly error. */
    sealed class ChatResult {
        data class Success(val reply: String) : ChatResult()
        data class Failure(val message: String) : ChatResult()
    }

    /**
     * Sends the user's message to the backend together with
     * the selected language and conversation ID.
     *
     * English -> "en"
     * Tamil   -> "ta"
     */
    suspend fun sendMessage(
        text: String,
        language: String,
        conversationId: String
    ): ChatResult {

        return try {

            val response = api.chat(
                ChatRequest(
                    text = text,
                    language = language,
                    conversationId = conversationId
                )
            )

            ChatResult.Success(response.reply)

        } catch (e: Exception) {

            ChatResult.Failure(
                "Could not reach EnodaAI's server. Is the backend running?"
            )
        }
    }
}