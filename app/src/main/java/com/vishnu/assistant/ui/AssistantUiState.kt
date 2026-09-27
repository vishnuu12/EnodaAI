package com.vishnu.assistant.ui

import com.vishnu.assistant.data.ChatMessage

enum class AssistantLanguage(
    val displayName: String,
    val speechLocale: String,
    val apiHint: String
) {
    ENGLISH(
        displayName = "English",
        speechLocale = "en-IN",
        apiHint = "en"
    ),
    TAMIL(
        displayName = "Tamil",
        speechLocale = "ta-IN",
        apiHint = "ta"
    )
}

enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    RECOGNIZED,
    ERROR
}

data class AssistantUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val statusMessage: String = "Tap the microphone to speak",
    val recognizedText: String = "",
    val responseText: String = "",
    val language: AssistantLanguage = AssistantLanguage.ENGLISH,

    val contactsPermissionRequired: Boolean = false,
    val callLogPermissionRequired: Boolean = false,
    val callPermissionRequired: Boolean = false,

    val messages: List<ChatMessage> = emptyList()
)