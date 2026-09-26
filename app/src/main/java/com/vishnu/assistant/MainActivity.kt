package com.vishnu.assistant

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vishnu.assistant.core.speech.AndroidSpeaker
import com.vishnu.assistant.core.speech.AndroidSpeechRecognizer
import com.vishnu.assistant.data.ChatRepository
import com.vishnu.assistant.ui.AssistantScreen
import com.vishnu.assistant.ui.AssistantViewModel

class MainActivity : ComponentActivity() {

    // by viewModels { factory } = lazy, survives configuration changes
    // (rotation), recreated only when the activity is truly destroyed.
    private val viewModel: AssistantViewModel by viewModels {
        AssistantViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AssistantScreen(viewModel)
            }
        }
    }
}

/** Hand-rolled DI: builds the ViewModel with its dependencies. */
class AssistantViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AssistantViewModel(
            voiceRecognizer = AndroidSpeechRecognizer(appContext),
            chatRepository = ChatRepository(),
            speaker = AndroidSpeaker(appContext)
        ) as T
}
