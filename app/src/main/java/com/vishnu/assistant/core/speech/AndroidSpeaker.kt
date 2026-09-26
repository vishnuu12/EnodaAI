package com.vishnu.assistant.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Wraps Android's on-device TextToSpeech engine.
 *
 * - No permissions required; works offline once voice data is
 *   present on the device.
 * - Initialization is asynchronous: anything spoken before it
 *   completes is buffered and spoken when ready.
 * - Completion callbacks arrive on a binder thread; the ViewModel's
 *   StateFlow handles updates from any thread safely.
 */
class AndroidSpeaker(context: Context) : Speaker {

    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)
    private val utteranceCounter = AtomicInteger(0)
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()

    // Text that arrived before the engine finished initializing.
    private var pendingText: String? = null
    private var pendingOnDone: (() -> Unit)? = null

    override val isAvailable: Boolean
        get() = ready.get()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine != null) {
                    configureEngine(engine)
                    ready.set(true)

                    // Flush anything spoken while we were initializing.
                    val text = pendingText
                    val onDone = pendingOnDone
                    pendingText = null
                    pendingOnDone = null
                    if (text != null) {
                        speak(text, onDone ?: {})
                    }
                }
            }
        }
    }

    private fun configureEngine(engine: TextToSpeech) {
        // Indian-English voice when available; fall back to US.
        // Phase 9 adds Tamil voice selection based on reply language.
        val result = engine.setLanguage(Locale("en", "IN"))
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            engine.setLanguage(Locale.US)
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { callbacks.remove(it) }?.invoke()
            }

            override fun onError(utteranceId: String?) {
                // Treat failure like completion so the caller's
                // state machine always settles.
                utteranceId?.let { callbacks.remove(it) }?.invoke()
            }
        })
    }

    override fun speak(text: String, onDone: () -> Unit) {
        if (!ready.get()) {
            pendingText = text
            pendingOnDone = onDone
            return
        }

        val engine = tts
        if (engine == null) {
            onDone()
            return
        }

        val id = "enodaai-${utteranceCounter.incrementAndGet()}"
        callbacks[id] = onDone
        // QUEUE_FLUSH: a new utterance replaces any in-progress one.
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    override fun stop() {
        pendingText = null
        pendingOnDone = null
        tts?.stop()
    }

    override fun destroy() {
        stop()
        callbacks.clear()
        tts?.shutdown()
        tts = null
        ready.set(false)
    }
}
