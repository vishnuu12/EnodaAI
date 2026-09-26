package com.vishnu.assistant.core.speech

/**
 * Abstraction over text-to-speech so a different engine
 * (cloud TTS, recorded prompts) can replace the Android one later.
 */
interface Speaker {

    /** True once the engine finished initializing. */
    val isAvailable: Boolean

    /**
     * Speak the given text aloud. [onDone] fires exactly once,
     * when the utterance finishes OR fails. May be called from a
     * background thread.
     */
    fun speak(text: String, onDone: () -> Unit = {})

    /** Stop any current speech immediately. Safe if not speaking. */
    fun stop()

    /** Release the engine. */
    fun destroy()
}
