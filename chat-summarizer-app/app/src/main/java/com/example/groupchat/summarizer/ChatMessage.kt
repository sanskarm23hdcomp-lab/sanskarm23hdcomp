package com.example.groupchat.summarizer

/**
 * Represents a single chat message.
 * Default values are required for Firestore's toObject<ChatMessage>() deserialization.
 * In Demo Mode the class is populated with hardcoded sample data.
 */
data class ChatMessage(
    val senderName: String = "",
    val text: String = ""
)
