package com.vishnu.assistant.core.network

/**
 * Kotlin twins of the backend's Pydantic models
 * (app/schemas/chat.py). Field names must match the JSON keys.
 */
data class ChatRequest(
    val text: String,
    val language: String? = null
)

data class ChatResponse(
    val reply: String
)
