package com.example.groupchat.summarizer

import com.google.firebase.Timestamp

/**
 * Represents a single message stored in Firestore.
 *
 * Firestore document structure:
 * {
 *   "senderName": "Ravi",
 *   "text": "Let's meet at 5pm",
 *   "timestamp": <Firestore Timestamp>
 * }
 *
 * Default values are required for Firestore's toObject<ChatMessage>() deserialization.
 */
data class ChatMessage(
    val senderName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)
