package com.example.groupchat.summarizer

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * ChatSummarizerService
 *
 * Fetches all group chat messages sent today from Firebase Firestore and
 * sends them to the Google Gemini API to generate a concise summary.
 *
 * Usage (from a coroutine scope):
 * ```
 * val service = ChatSummarizerService(apiKey = BuildConfig.GEMINI_API_KEY)
 * val summary = service.summarizeTodaysChat(groupId = "my_group_id")
 * ```
 *
 * Required Gradle dependencies (app/build.gradle):
 *   implementation("com.google.firebase:firebase-firestore-ktx")
 *   implementation("com.squareup.okhttp3:okhttp:4.12.0")
 *   implementation("com.google.code.gson:gson:2.10.1")
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
 */
class ChatSummarizerService(
    private val apiKey: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val client: OkHttpClient = defaultClient
) {

    private val gson = Gson()

    companion object {
        /**
         * Shared OkHttpClient – reuse the same instance across calls so that
         * the connection pool and thread pool are shared efficiently.
         */
        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // Gemini REST endpoint (gemini-1.5-flash is free-tier friendly)
    private val geminiEndpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    /**
     * Returns a short summary of today's group chat messages.
     * Returns "No messages today." when there is nothing to summarise.
     *
     * Must be called from a coroutine (suspend function).
     */
    suspend fun summarizeTodaysChat(groupId: String): String {
        val messages = fetchTodaysMessages(groupId)
        if (messages.isEmpty()) return "No messages today."
        val prompt = buildPrompt(messages)
        return callGeminiApi(prompt)
    }

    // -------------------------------------------------------------------------
    // Firestore – fetch messages from midnight to now
    // -------------------------------------------------------------------------

    private suspend fun fetchTodaysMessages(groupId: String): List<ChatMessage> {
        val midnightTimestamp = getMidnightTimestamp()

        val snapshot = db.collection("groups")
            .document(groupId)
            .collection("messages")
            .whereGreaterThanOrEqualTo("timestamp", midnightTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
    }

    /** Returns a Firestore Timestamp for midnight of the current day (local time). */
    private fun getMidnightTimestamp(): Timestamp {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Timestamp(calendar.time)
    }

    // -------------------------------------------------------------------------
    // Prompt builder
    // -------------------------------------------------------------------------

    /**
     * Converts the list of ChatMessages into a plain-text conversation
     * and wraps it in an instruction prompt for Gemini.
     */
    private fun buildPrompt(messages: List<ChatMessage>): String {
        val conversation = messages.joinToString(separator = "\n") { msg ->
            "${msg.senderName}: ${msg.text}"
        }
        return """
            You are a helpful assistant. Summarize the following group chat conversation
            in 3 to 5 clear, concise sentences. Focus on the key topics discussed,
            decisions made, and any action items mentioned.

            Group Chat:
            $conversation

            Summary:
        """.trimIndent()
    }

    // -------------------------------------------------------------------------
    // Gemini REST API call
    // -------------------------------------------------------------------------

    /**
     * Sends [prompt] to the Gemini API and returns the generated text.
     * Throws an [IllegalStateException] if the response cannot be parsed.
     */
    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val requestBodyJson = buildGeminiRequestBody(prompt)

        val request = Request.Builder()
            .url("$geminiEndpoint?key=$apiKey")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API error ${response.code}: $responseBody")
        }

        extractSummaryText(responseBody)
    }

    /** Builds the JSON request body expected by the Gemini generateContent endpoint. */
    private fun buildGeminiRequestBody(prompt: String): String {
        val body = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )
        return gson.toJson(body)
    }

    /**
     * Parses the Gemini API JSON response and extracts the generated text.
     *
     * Expected shape:
     * {
     *   "candidates": [
     *     { "content": { "parts": [ { "text": "summary here" } ] } }
     *   ]
     * }
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractSummaryText(responseJson: String): String {
        val root = gson.fromJson(responseJson, Map::class.java) as Map<String, Any>
        val candidates = root["candidates"] as? List<*>
            ?: throw IllegalStateException("No candidates in Gemini response")
        val firstCandidate = candidates.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("Empty candidates list")
        val content = firstCandidate["content"] as? Map<*, *>
            ?: throw IllegalStateException("No content in candidate")
        val parts = content["parts"] as? List<*>
            ?: throw IllegalStateException("No parts in content")
        val firstPart = parts.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("Empty parts list")
        return firstPart["text"] as? String
            ?: throw IllegalStateException("No text in part")
    }
}
