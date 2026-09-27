package com.vishnu.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vishnu.assistant.R
import com.vishnu.assistant.data.ChatMessage
import com.vishnu.assistant.data.ChatRole

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel
) {

    val context =
        LocalContext.current

    val uiState by
        viewModel.uiState.collectAsState()

    var hasMicPermission by
        remember {

            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }

    var hasContactsPermission by
        remember {

            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }

    var hasCallPermission by
        remember {

            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }

    var hasCallLogPermission by
        remember {

            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALL_LOG
                ) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }

    var permissionDenied by
        remember {
            mutableStateOf(false)
        }

    /*
     * Microphone permission.
     */
    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasMicPermission =
                granted

            permissionDenied =
                !granted

            if (granted) {
                viewModel.startListening()
            }
        }

    /*
     * Contacts permission.
     */
    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasContactsPermission =
                granted

            viewModel.onContactsPermissionResult(
                granted
            )
        }

    /*
     * Call log permission.
     */
    val callLogPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCallLogPermission =
                granted

            viewModel.onCallLogPermissionResult(
                granted
            )
        }

    /*
     * Phone call permission.
     */
    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasCallPermission =
                granted

            viewModel.onCallPermissionResult(
                granted
            )
        }

    /*
     * Automatically show Contacts permission
     * when EnodaAI needs to search a contact.
     */
    LaunchedEffect(
        uiState.contactsPermissionRequired
    ) {

        if (
            uiState.contactsPermissionRequired &&
            !hasContactsPermission
        ) {

            contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS
            )
        }
    }

    /*
     * Automatically show Call Log permission
     * when EnodaAI needs to find the
     * recently contacted number.
     */
    LaunchedEffect(
        uiState.callLogPermissionRequired
    ) {

        if (
            uiState.callLogPermissionRequired &&
            !hasCallLogPermission
        ) {

            callLogPermissionLauncher.launch(
                Manifest.permission.READ_CALL_LOG
            )
        }
    }

    /*
     * Automatically show phone permission
     * when EnodaAI is ready to make the call.
     */
    LaunchedEffect(
        uiState.callPermissionRequired
    ) {

        if (
            uiState.callPermissionRequired &&
            !hasCallPermission
        ) {

            callPermissionLauncher.launch(
                Manifest.permission.CALL_PHONE
            )
        }
    }

    val conversationListState =
        rememberLazyListState()

    LaunchedEffect(
        uiState.messages.size
    ) {

        if (
            uiState.messages.isNotEmpty()
        ) {

            conversationListState.animateScrollToItem(
                uiState.messages.lastIndex
            )
        }
    }

    Surface(
        modifier =
            Modifier.fillMaxSize(),

        color =
            MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /*
             * EnodaAI Logo
             */
            Image(
                painter =
                    painterResource(
                        id = R.drawable.logo
                    ),

                contentDescription =
                    "EnodaAI Logo",

                modifier =
                    Modifier.size(96.dp)
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    "EnodaAI",

                fontSize =
                    28.sp
            )

            Spacer(
                Modifier.height(16.dp)
            )

            SingleChoiceSegmentedButtonRow {

                AssistantLanguage.entries
                    .forEachIndexed {
                        index,
                        language ->

                        SegmentedButton(
                            selected =
                                uiState.language ==
                                    language,

                            onClick = {

                                viewModel.setLanguage(
                                    language
                                )
                            },

                            shape =
                                SegmentedButtonDefaults
                                    .itemShape(
                                        index =
                                            index,

                                        count =
                                            AssistantLanguage
                                                .entries
                                                .size
                                    )
                        ) {

                            Text(
                                language.displayName
                            )
                        }
                    }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text =
                    uiState.statusMessage,

                fontSize =
                    16.sp,

                color =
                    MaterialTheme.colorScheme.primary
            )

            Spacer(
                Modifier.height(16.dp)
            )

            if (
                uiState.messages.isNotEmpty()
            ) {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),

                    state =
                        conversationListState,

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    items(
                        items =
                            uiState.messages,

                        key = { message ->

                            "${message.role}-" +
                                "${message.text.hashCode()}-" +
                                "${uiState.messages.indexOf(message)}"
                        }
                    ) { message ->

                        ConversationMessageItem(
                            message =
                                message
                        )
                    }
                }

            } else {

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Text(
                    text =
                        "Start a conversation with EnodaAI",

                    style =
                        MaterialTheme.typography.bodyMedium,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    Modifier.height(16.dp)
                )
            }

            when {

                !viewModel.isRecognizerAvailable -> {

                    Text(
                        text =
                            "Speech recognition is not available on this device. " +
                                "Install or enable the Google app, then retry.",

                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                !hasMicPermission -> {

                    ExtendedFloatingActionButton(
                        onClick = {

                            microphonePermissionLauncher
                                .launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                        },

                        icon = {

                            Icon(
                                Icons.Filled.Mic,
                                contentDescription =
                                    null
                            )
                        },

                        text = {

                            Text(
                                "Grant microphone access"
                            )
                        }
                    )

                    if (
                        permissionDenied
                    ) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "Voice input needs microphone access.",

                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )

                        TextButton(
                            onClick = {

                                openAppSettings(
                                    context
                                )
                            }
                        ) {

                            Text(
                                "Open app settings"
                            )
                        }
                    }
                }

                uiState.status ==
                    AssistantStatus.LISTENING -> {

                    Surface(
                        onClick = {},

                        shape =
                            CircleShape,

                        color =
                            MaterialTheme.colorScheme
                                .primaryContainer,

                        modifier =
                            Modifier.size(96.dp),

                        enabled =
                            false
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.Mic,

                                contentDescription =
                                    "Listening",

                                modifier =
                                    Modifier.size(40.dp),

                                tint =
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                            )
                        }
                    }
                }

                uiState.status ==
                    AssistantStatus.THINKING -> {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        CircularProgressIndicator()

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "Thinking...",

                            style =
                                MaterialTheme.typography
                                    .bodyMedium,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

                uiState.status ==
                    AssistantStatus.SPEAKING -> {

                    Surface(
                        onClick = {},

                        shape =
                            CircleShape,

                        color =
                            MaterialTheme.colorScheme
                                .secondaryContainer,

                        modifier =
                            Modifier.size(96.dp),

                        enabled =
                            false
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.VolumeUp,

                                contentDescription =
                                    "Speaking",

                                modifier =
                                    Modifier.size(40.dp),

                                tint =
                                    MaterialTheme.colorScheme
                                        .onSecondaryContainer
                            )
                        }
                    }
                }

                else -> {

                    ExtendedFloatingActionButton(
                        onClick =
                            viewModel::startListening,

                        icon = {

                            Icon(
                                Icons.Filled.Mic,

                                contentDescription =
                                    "Start listening"
                            )
                        },

                        text = {

                            Text(
                                "Speak"
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationMessageItem(
    message: ChatMessage
) {

    val isUser =
        message.role ==
            ChatRole.USER

    Column(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalAlignment =
            if (isUser) {
                Alignment.End
            } else {
                Alignment.Start
            }
    ) {

        Text(
            text =
                if (isUser) {
                    "You"
                } else {
                    "EnodaAI"
                },

            style =
                MaterialTheme.typography
                    .labelSmall,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Surface(
            shape =
                MaterialTheme.shapes.medium,

            color =
                if (isUser) {
                    MaterialTheme.colorScheme
                        .primaryContainer
                } else {
                    MaterialTheme.colorScheme
                        .secondaryContainer
                }
        ) {

            Text(
                text =
                    message.text,

                modifier =
                    Modifier.padding(
                        horizontal =
                            16.dp,

                        vertical =
                            12.dp
                    ),

                fontSize =
                    18.sp,

                color =
                    if (isUser) {
                        MaterialTheme.colorScheme
                            .onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme
                            .onSecondaryContainer
                    }
            )
        }
    }
}

private fun openAppSettings(
    context: android.content.Context
) {

    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,

            Uri.fromParts(
                "package",
                context.packageName,
                null
            )
        )

    context.startActivity(
        intent
    )
}