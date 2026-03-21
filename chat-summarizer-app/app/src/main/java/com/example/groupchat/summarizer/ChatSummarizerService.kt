package com.example.groupchat.summarizer

import com.example.groupchat.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * ChatSummarizerService
 *
 * - **Demo Mode** (default when GEMINI_API_KEY is blank):
 *     Returns hardcoded sample messages and a pre-written summary after a short
 *     simulated delay. No Firebase or Gemini account needed.
 *
 * - **Production Mode** (when GEMINI_API_KEY is set in local.properties):
 *     Fetches today's messages from Firebase Firestore and calls the Gemini API.
 *     Requires `google-services.json` in the `app/` folder.
 *
 * Usage (suspend function – call from a coroutine):
 * ```
 * val service = ChatSummarizerService()
 * val summary = service.summarizeTodaysChat(groupId)
 * ```
 */
class ChatSummarizerService(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val client: OkHttpClient = defaultClient
) {
    /** true when no API key is configured → use sample data instead of real APIs. */
    val isDemoMode: Boolean get() = apiKey.isBlank()

    private val gson = Gson()

    private val geminiEndpoint =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    companion object {
        /** Duration of the simulated network delay in Demo Mode (milliseconds). */
        private const val DEMO_DELAY_MS = 1800L

        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns a short summary of today's group chat messages.
     * Automatically uses demo data when no API key is configured.
     */
    suspend fun summarizeTodaysChat(groupId: String): String {
        return if (isDemoMode) {
            generateDemoSummary()
        } else {
            val messages = fetchTodaysMessages(groupId)
            if (messages.isEmpty()) return "No messages today."
            callGeminiApi(buildPrompt(messages))
        }
    }

    // -------------------------------------------------------------------------
    // Demo Mode – no external services required
    // -------------------------------------------------------------------------

    private suspend fun generateDemoSummary(): String {
        // Simulate a realistic network round-trip so the loading spinner is visible.
        delay(DEMO_DELAY_MS)
        return "The Team Alpha group discussed the upcoming product launch scheduled for next Friday. " +
            "Ravi confirmed the backend API is complete and Priya will start QA testing tomorrow. " +
            "The team agreed to meet at 5 PM on Wednesday for a final review. " +
            "Ankit flagged a push-notification setup issue that Ravi will resolve before Wednesday. " +
            "Overall, the launch is on track. ✅"
    }

    /** Sample messages shown in the chat list (for UI demo purposes). */
    fun getDemoMessages(): List<ChatMessage> = listOf(
        ChatMessage("Ravi",  "Hey team, backend API is ready 🎉"),
        ChatMessage("Priya", "Great! I'll start QA testing tomorrow morning."),
        ChatMessage("Ankit", "We should meet before the launch – Wednesday 5 PM?"),
        ChatMessage("Ravi",  "Wednesday works for me 👍"),
        ChatMessage("Priya", "Same here. Also, are push notifications set up?"),
        ChatMessage("Ankit", "Not yet. Ravi can you handle that before Wed?"),
        ChatMessage("Ravi",  "On it, will be done by Tuesday."),
        ChatMessage("Priya", "Perfect. Launch is on track! 🚀")
    )

    // -------------------------------------------------------------------------
    // Production Mode – Firebase Firestore + Gemini API
    // -------------------------------------------------------------------------

    /**
     * Fetches all messages stored in Firestore for the given group
     * from midnight today to now.
     *
     * NOTE: Requires Firebase Firestore dependency and google-services.json.
     * Uncomment the Firebase dependencies in app/build.gradle.kts to use this.
     */
    private suspend fun fetchTodaysMessages(groupId: String): List<ChatMessage> {
        // Firebase Firestore import is intentionally commented out so the demo
        // compiles without the Firebase dependency. Uncomment when using production mode:
        //
        // val db = FirebaseFirestore.getInstance()
        // val calendar = Calendar.getInstance().apply {
        //     set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        //     set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
        // }
        // val snapshot = db.collection("groups").document(groupId)
        //     .collection("messages")
        //     .whereGreaterThanOrEqualTo("timestamp", Timestamp(calendar.time))
        //     .orderBy("timestamp", Query.Direction.ASCENDING)
        //     .get().await()
        // return snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }

        // Placeholder – unreachable in demo mode.
        return emptyList()
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        val conversation = messages.joinToString("\n") { "${it.senderName}: ${it.text}" }
        return """
            You are a helpful assistant. Summarize the following group chat conversation
            in 3 to 5 clear, concise sentences. Focus on the key topics discussed,
            decisions made, and any action items mentioned.

            Group Chat:
            $conversation

            Summary:
        """.trimIndent()
    }

    private suspend fun callGeminiApi(prompt: String): String = withContext(Dispatchers.IO) {
        val bodyJson = gson.toJson(
            mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))))
        )
        val request = Request.Builder()
            .url("$geminiEndpoint?key=$apiKey")
            .post(bodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IllegalStateException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API error ${response.code}: $responseBody")
        }

        extractSummaryText(responseBody)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractSummaryText(json: String): String {
        val root        = gson.fromJson(json, Map::class.java) as Map<String, Any>
        val candidates  = root["candidates"] as? List<*>
            ?: throw IllegalStateException("No candidates in Gemini response")
        val candidate   = candidates.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("Empty candidates list")
        val content     = candidate["content"] as? Map<*, *>
            ?: throw IllegalStateException("No content in candidate")
        val parts       = content["parts"] as? List<*>
            ?: throw IllegalStateException("No parts in content")
        val part        = parts.firstOrNull() as? Map<*, *>
            ?: throw IllegalStateException("Empty parts list")
        return part["text"] as? String
            ?: throw IllegalStateException("No text in part")
    }
}
