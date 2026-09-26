package com.vishnu.assistant.ui

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
 * Full state machine:
 * IDLE -> LISTENING -> THINKING -> SPEAKING -> RECOGNIZED
 * (ERROR reachable from any stage).
 */
data class AssistantUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val statusMessage: String = "Tap the microphone to speak",
    val recognizedText: String = "",
    val responseText: String = ""
)
