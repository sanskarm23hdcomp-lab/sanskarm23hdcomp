# 📋 Android Group-Chat Summarizer – Implementation Guide

> **Goal:** Add a "Summarize Today's Chats" button in your Android group-chat app that fetches all messages sent on the current day from Firebase Firestore, sends them to the **Google Gemini API** (free tier available), and displays the AI-generated summary in a bottom sheet.

---

## Table of Contents
1. [How It Works](#how-it-works)
2. [Tech Stack](#tech-stack)
3. [Step-by-Step Setup](#step-by-step-setup)
   - [1 – Gradle Dependencies](#1--gradle-dependencies)
   - [2 – Get a Gemini API Key](#2--get-a-gemini-api-key)
   - [3 – Store Messages in Firestore](#3--store-messages-in-firestore)
   - [4 – ChatMessage Data Class](#4--chatmessage-data-class)
   - [5 – ChatSummarizerService](#5--chatsummarizerservice)
   - [6 – SummaryBottomSheet (UI)](#6--summarybottomsheet-ui)
   - [7 – Wire It Up in Your Activity / Fragment](#7--wire-it-up-in-your-activity--fragment)
4. [Useful YouTube Videos](#useful-youtube-videos)
5. [References](#references)

---

## How It Works

```
User taps "Summarize" button
        │
        ▼
Fetch today's messages from Firestore
(filter by timestamp >= start-of-day)
        │
        ▼
Build a prompt: "Summarize this group chat:\n<messages>"
        │
        ▼
POST to Gemini API  ──►  AI returns a 3-5 sentence summary
        │
        ▼
Show summary in a BottomSheetDialog
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| Backend / DB | Firebase Firestore |
| AI Summarization | Google Gemini API (`gemini-1.5-flash`, free tier) |
| Networking | OkHttp + Gson (or Retrofit) |
| UI | Material BottomSheetDialogFragment |

---

## Step-by-Step Setup

### 1 – Gradle Dependencies

Add these to your **app-level `build.gradle`** (or `build.gradle.kts`):

```groovy
dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Coroutines (async)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Material Design (BottomSheet)
    implementation("com.google.android.material:material:1.12.0")
}
```

> **`google-services.json`** – download it from the Firebase Console and place it in the `app/` folder.

---

### 2 – Get a Gemini API Key

1. Open [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Click **"Create API Key"**.
3. Copy the key and add it to your `local.properties` (never hard-code it):

```properties
# local.properties  (this file is NOT committed to git)
GEMINI_API_KEY=AIza...your_key_here...
```

4. Expose it as a `BuildConfig` field in `build.gradle`:

```groovy
android {
    defaultConfig {
        def geminiKey = project.findProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"${geminiKey}\"")
    }
}
```

> Add `local.properties` to `.gitignore` so your key is never leaked.

---

### 3 – Store Messages in Firestore

Each group has its own collection. Messages are stored with a server timestamp so you can filter by date:

```
Firestore
└── groups
    └── {groupId}
        └── messages         ← collection
            └── {messageId}
                ├── senderName : "Ravi"
                ├── text       : "Let's meet at 5pm"
                └── timestamp  : Timestamp (server timestamp)
```

When you send a message:

```kotlin
val msg = hashMapOf(
    "senderName" to currentUser.displayName,
    "text"       to messageText,
    "timestamp"  to FieldValue.serverTimestamp()
)
db.collection("groups").document(groupId)
  .collection("messages").add(msg)
```

---

### 4 – ChatMessage Data Class

```kotlin
// ChatMessage.kt
data class ChatMessage(
    val senderName: String = "",
    val text: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)
```

See the full file: [`src/main/java/com/example/groupchat/summarizer/ChatMessage.kt`](chat-summarizer/src/main/java/com/example/groupchat/summarizer/ChatMessage.kt)

---

### 5 – ChatSummarizerService

This class:
- Queries Firestore for messages sent **today** (midnight → now).
- Builds a plain-text prompt.
- POSTs the prompt to the Gemini REST API using OkHttp.
- Returns the summary text (suspend function, safe to call from a coroutine).

See the full implementation: [`src/main/java/com/example/groupchat/summarizer/ChatSummarizerService.kt`](chat-summarizer/src/main/java/com/example/groupchat/summarizer/ChatSummarizerService.kt)

Key snippet:

```kotlin
suspend fun summarizeTodaysChat(groupId: String): String {
    val messages = fetchTodaysMessages(groupId)   // Firestore query
    if (messages.isEmpty()) return "No messages today."
    val prompt = buildPrompt(messages)
    return callGeminiApi(prompt)                  // REST call
}
```

---

### 6 – SummaryBottomSheet (UI)

A `BottomSheetDialogFragment` that shows a loading spinner while the API call is in flight, then displays the summary text.

See the full implementation: [`src/main/java/com/example/groupchat/summarizer/SummaryBottomSheet.kt`](chat-summarizer/src/main/java/com/example/groupchat/summarizer/SummaryBottomSheet.kt)

Layout (`fragment_summary_bottom_sheet.xml`) snippet:

```xml
<LinearLayout ...>
    <TextView android:id="@+id/tvTitle" android:text="Today's Summary" ... />
    <ProgressBar android:id="@+id/progressBar" ... />
    <TextView android:id="@+id/tvSummary" android:visibility="gone" ... />
</LinearLayout>
```

---

### 7 – Wire It Up in Your Activity / Fragment

```kotlin
// In your GroupChatActivity or Fragment
binding.btnSummarize.setOnClickListener {
    SummaryBottomSheet.newInstance(groupId)
        .show(supportFragmentManager, "summary")
}
```

That's it! The bottom sheet handles fetching and displaying the summary autonomously.

---

## Useful YouTube Videos

| Video | Channel | What it covers |
|---|---|---|
| [Build AI Chat Summarizer App – Android Studio](https://www.youtube.com/results?search_query=android+gemini+api+chat+summarizer) | Search result | Gemini API + Android |
| [Firebase Firestore Query by Date in Android](https://www.youtube.com/results?search_query=firebase+firestore+query+by+date+android+kotlin) | Search result | Firestore date filtering |
| [BottomSheetDialogFragment in Android](https://www.youtube.com/results?search_query=BottomSheetDialogFragment+android+kotlin+tutorial) | Search result | Material bottom sheet UI |
| [Retrofit / OkHttp REST API in Android Kotlin](https://www.youtube.com/results?search_query=okhttp+retrofit+android+kotlin+rest+api) | Search result | Networking basics |

> **Tip:** Search these titles directly on YouTube for the most up-to-date tutorials.

---

## References

- [Google Gemini API Documentation](https://ai.google.dev/gemini-api/docs)
- [Google AI Studio (get API key)](https://aistudio.google.com/app/apikey)
- [Firebase Firestore – Query Data](https://firebase.google.com/docs/firestore/query-data/queries)
- [Material Bottom Sheet – Android](https://m3.material.io/components/bottom-sheets/overview)
- [OkHttp – Square](https://square.github.io/okhttp/)
