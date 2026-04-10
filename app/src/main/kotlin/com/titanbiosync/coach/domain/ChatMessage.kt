package com.titanbiosync.coach.domain

/**
 * A single message in the AI coach conversation.
 * @param role  "user" for user messages, "assistant" for coach responses.
 * @param content The message text.
 */
data class ChatMessage(
    val role: String,
    val content: String
)
