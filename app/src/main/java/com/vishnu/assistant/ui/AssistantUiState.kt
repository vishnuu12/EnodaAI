package com.vishnu.assistant.ui

/** Supported assistant conversation languages. */
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

/** What the assistant is visibly doing right now. */
enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    RECOGNIZED,
    ERROR
}

/**
 * Everything the main screen needs to render.
 *
 * Full state machine:
 * IDLE -> LISTENING -> THINKING -> SPEAKING -> RECOGNIZED
 * (ERROR reachable from any stage).
 */
data class AssistantUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val statusMessage: String = "Tap the microphone to speak",
    val recognizedText: String = "",
    val responseText: String = "",
    val language: AssistantLanguage = AssistantLanguage.ENGLISH
)