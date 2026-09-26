package com.vishnu.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * The main EnodaAI screen. A pure function of AssistantUiState:
 * permission handling, the mic button, the conversation (You /
 * EnodaAI), the Thinking spinner, the Speaking indicator, and
 * error states.
 */
@Composable
fun AssistantScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        permissionDenied = !granted
        if (granted) viewModel.startListening()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("EnodaAI", fontSize = 28.sp)

            Spacer(Modifier.height(8.dp))

            Text(
                text = uiState.statusMessage,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // ---- the conversation ----

            if (uiState.recognizedText.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u201C${uiState.recognizedText}\u201D",
                    fontSize = 20.sp
                )
            }

            if (uiState.responseText.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "EnodaAI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.responseText,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            // ---- state-driven controls ----

            when {
                !viewModel.isRecognizerAvailable -> Text(
                    text = "Speech recognition is not available on this device. " +
                        "Install or enable the Google app, then retry.",
                    color = MaterialTheme.colorScheme.error
                )

                !hasMicPermission -> {
                    ExtendedFloatingActionButton(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        text = { Text("Grant microphone access") }
                    )
                    if (permissionDenied) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Voice input needs microphone access. Nothing is " +
                                "recorded beyond processing your current request.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { openAppSettings(context) }) {
                            Text("Open app settings")
                        }
                    }
                }

                uiState.status == AssistantStatus.LISTENING -> Surface(
                    onClick = {},
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp),
                    enabled = false
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Listening",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                uiState.status == AssistantStatus.THINKING -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Thinking...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                uiState.status == AssistantStatus.SPEAKING -> Surface(
                    onClick = {},
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(96.dp),
                    enabled = false
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speaking",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                else -> ExtendedFloatingActionButton(
                    onClick = viewModel::startListening,
                    icon = { Icon(Icons.Filled.Mic, contentDescription = "Start listening") },
                    text = { Text("Speak") }
                )
            }
        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}
