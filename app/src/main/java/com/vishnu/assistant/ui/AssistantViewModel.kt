package com.vishnu.assistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vishnu.assistant.core.speech.Speaker
import com.vishnu.assistant.core.speech.VoiceRecognitionEvent
import com.vishnu.assistant.core.speech.VoiceRecognizer
import com.vishnu.assistant.data.ChatRepository
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
 * Full cycle: LISTENING (hands-free) -> THINKING -> SPEAKING ->
 * RECOGNIZED. All external dependencies are injected as
 * interfaces so tests can supply fakes.
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

    fun startListening() {
        // Barge-in: a new question interrupts any ongoing speech.
        speaker.stop()
        speechWatchdog?.cancel()

        _uiState.update {
            it.copy(
                status = AssistantStatus.LISTENING,
                statusMessage = "Listening...",
                recognizedText = "",
                responseText = ""
            )
        }

        voiceRecognizer.startListening(::handleEvent)
        restartSilenceWatchdog()
    }

    fun reset() {
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
     * without speech activity (new partials reset it).
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
            VoiceRecognitionEvent.SpeechEnded -> restartSilenceWatchdog()

            is VoiceRecognitionEvent.Partial -> {
                _uiState.update { it.copy(recognizedText = event.text) }
                restartSilenceWatchdog()
            }

            is VoiceRecognitionEvent.Final -> {
                silenceWatchdog?.cancel()
                if (event.text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.ERROR,
                            statusMessage = "I didn't catch any speech. Please try again."
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

    /** Send the recognized text to the backend, then speak the reply. */
    private fun askBackend(text: String) {
        _uiState.update {
            it.copy(
                status = AssistantStatus.THINKING,
                statusMessage = "Thinking..."
            )
        }

        viewModelScope.launch {
            when (val result = chatRepository.sendMessage(text)) {
                is ChatRepository.ChatResult.Success -> {
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.SPEAKING,
                            statusMessage = "Speaking...",
                            responseText = result.reply
                        )
                    }
                    speakReply(result.reply)
                }

                is ChatRepository.ChatResult.Failure ->
                    _uiState.update {
                        it.copy(
                            status = AssistantStatus.ERROR,
                            statusMessage = result.message
                        )
                    }
            }
        }
    }

    /**
     * Speak the reply aloud; when done (or on error), settle in
     * RECOGNIZED. The watchdog guarantees we never freeze in
     * SPEAKING even if the engine never calls back.
     */
    private fun speakReply(reply: String) {
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

        speaker.speak(reply) {
            // This callback may arrive on a background thread -
            // StateFlow updates are thread-safe.
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
        if (_uiState.value.status != AssistantStatus.LISTENING) return

        voiceRecognizer.destroy() // hard-stop this session

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
                    statusMessage = "I didn't catch any speech. Please try again."
                )
            }
        }
    }

    companion object {
        private const val SILENCE_TIMEOUT_MS = 7_000L
        private const val SPEAKING_TIMEOUT_MS = 30_000L
    }
}
