package com.vishnu.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.assistant.core.speech.Speaker
import com.vishnu.assistant.core.speech.VoiceRecognitionEvent
import com.vishnu.assistant.core.speech.VoiceRecognizer
import com.vishnu.assistant.data.ChatRepository
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds assistant UI state, drives the voice recognizer, talks to
 * the backend, and speaks replies aloud.
 *
 * Full cycle:
 * LISTENING -> THINKING -> SPEAKING -> RECOGNIZED
 *
 * Language support:
 * English -> Android STT "en-IN" -> backend "en" -> TTS "en-IN"
 * Tamil   -> Android STT "ta-IN" -> backend "ta" -> TTS "ta-IN"
 *
 * Conversation support:
 * One conversation ID is created when the ViewModel is created
 * and reused for every message in the current conversation.
 */
class AssistantViewModel(
    private val voiceRecognizer: VoiceRecognizer,
    private val chatRepository: ChatRepository,
    private val speaker: Speaker
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val isRecognizerAvailable: Boolean
        get() = voiceRecognizer.isAvailable

    private var silenceWatchdog: Job? = null
    private var speechWatchdog: Job? = null

    /**
     * Unique ID for the current conversation.
     *
     * The same ID is sent with every message so the backend
     * can load the previous conversation history.
     */
    private var conversationId: String = UUID.randomUUID().toString()
    /**
     * Starts a new voice recognition session using the language
     * currently selected by the user.
     */
    fun startListening() {
        // Barge-in: a new question interrupts any ongoing speech.
        speaker.stop()
        speechWatchdog?.cancel()

        val language = _uiState.value.language

        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING,
                statusMessage = "Listening...",
                recognizedText = "",
                responseText = ""
            )
        }

        voiceRecognizer.startListening(
            speechLocale = language.speechLocale,
            onEvent = ::handleEvent
        )

        restartSilenceWatchdog()
    }

    /**
     * Changes the assistant conversation language.
     *
     * Language cannot be changed while the recognizer is actively
     * listening. This prevents an active recognition session from
     * switching language unexpectedly.
     */
    fun setLanguage(language: AssistantLanguage) {
        if (_uiState.value.status == AssistantStatus.LISTENING) {
            return
        }

        _uiState.update {
            it.copy(
                language = language,
                recognizedText = "",
                responseText = "",
                status = AssistantStatus.IDLE,
                statusMessage = "Tap the microphone to speak"
            )
        }
    }

    /**
     * Starts a completely new conversation.
     *
     * A new conversation ID is generated so the backend will not
     * use messages from the previous conversation.
     */
    fun reset() {
        silenceWatchdog?.cancel()
        speechWatchdog?.cancel()

        speaker.stop()

        conversationId = UUID.randomUUID().toString()

        _uiState.value = AssistantUiState()
    }

    override fun onCleared() {
        silenceWatchdog?.cancel()
        speechWatchdog?.cancel()
        voiceRecognizer.destroy()
        speaker.destroy()
        super.onCleared()
    }

    // ---- internals ----

    /**
     * The listening session self-terminates after this long
     * without speech activity.
     */
    private fun restartSilenceWatchdog() {
        silenceWatchdog?.cancel()

        silenceWatchdog = viewModelScope.launch {
            delay(SILENCE_TIMEOUT_MS)
            endSessionManually()
        }
    }

    private fun handleEvent(event: VoiceRecognitionEvent) {
        when (event) {

            VoiceRecognitionEvent.ListeningStarted,
            VoiceRecognitionEvent.SpeechEnded -> {
                restartSilenceWatchdog()
            }

            is VoiceRecognitionEvent.Partial -> {
                _uiState.update {
                    it.copy(
                        recognizedText = event.text
                    )
                }

                restartSilenceWatchdog()
            }

            is VoiceRecognitionEvent.Final -> {
                silenceWatchdog?.cancel()

                if (event.text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.ERROR,
                            statusMessage =
                                "I didn't catch any speech. Please try again."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.RECOGNIZED,
                            statusMessage = "Recognized.",
                            recognizedText = event.text
                        )
                    }

                    askBackend(event.text)
                }
            }

            is VoiceRecognitionEvent.Error -> {
                silenceWatchdog?.cancel()

                _uiState.update {
                    it.copy(
                        status = AssistantStatus.ERROR,
                        statusMessage = event.message
                    )
                }
            }
        }
    }

    /**
     * Sends recognized text to the backend together with the
     * selected language and current conversation ID.
     *
     * English -> "en"
     * Tamil   -> "ta"
     */
    private fun askBackend(text: String) {
        val language = _uiState.value.language

        _uiState.update {
            it.copy(
                status = AssistantStatus.THINKING,
                statusMessage = "Thinking..."
            )
        }

        viewModelScope.launch {
            when (
                val result = chatRepository.sendMessage(
                    text = text,
                    language = language.apiHint,
                    conversationId = conversationId
                )
            ) {
                is ChatRepository.ChatResult.Success -> {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.SPEAKING,
                            statusMessage = "Speaking...",
                            responseText = result.reply
                        )
                    }

                    // Pass the selected language's TTS locale.
                    speakReply(
                        reply = result.reply,
                        speechLocale = language.speechLocale
                    )
                }

                is ChatRepository.ChatResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.ERROR,
                            statusMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Speaks the reply aloud using the requested speech locale.
     *
     * English -> "en-IN"
     * Tamil   -> "ta-IN"
     *
     * AndroidSpeaker handles language availability and fallback.
     *
     * The speaking watchdog guarantees that the UI never remains
     * stuck in SPEAKING if the TTS engine does not call back.
     */
    private fun speakReply(
        reply: String,
        speechLocale: String
    ) {
        speechWatchdog?.cancel()

        speechWatchdog = viewModelScope.launch {
            delay(SPEAKING_TIMEOUT_MS)

            if (_uiState.value.status == AssistantStatus.SPEAKING) {
                _uiState.update {
                    it.copy(
                        status = AssistantStatus.RECOGNIZED,
                        statusMessage = "Reply received."
                    )
                }
            }
        }

        speaker.speak(
            text = reply,
            speechLocale = speechLocale
        ) {
            speechWatchdog?.cancel()

            _uiState.update { state ->
                if (state.status == AssistantStatus.SPEAKING) {
                    state.copy(
                        status = AssistantStatus.RECOGNIZED,
                        statusMessage = "Reply received."
                    )
                } else {
                    state
                }
            }
        }
    }

    /**
     * Fired by the silence watchdog when neither a Final result nor
     * an Error has arrived from the speech service.
     */
    private fun endSessionManually() {
        if (_uiState.value.status != AssistantStatus.LISTENING) {
            return
        }

        voiceRecognizer.destroy()

        val partial = _uiState.value.recognizedText

        if (partial.isNotBlank()) {
            _uiState.update {
                it.copy(
                    status = AssistantStatus.RECOGNIZED,
                    statusMessage = "Recognized."
                )
            }

            askBackend(partial)
        } else {
            _uiState.update {
                it.copy(
                    status = AssistantStatus.ERROR,
                    statusMessage =
                        "I didn't catch any speech. Please try again."
                )
            }
        }
    }

    companion object {
        private const val SILENCE_TIMEOUT_MS = 7_000L
        private const val SPEAKING_TIMEOUT_MS = 30_000L
    }
}