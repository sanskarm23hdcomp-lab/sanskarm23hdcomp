# 👀 How to Preview the Chat Summarizer App

The `chat-summarizer-app/` folder is a **complete, ready-to-open Android Studio project**.
It runs in **Demo Mode** out of the box — no Firebase account or API key is needed.

---

## What the App Looks Like

```
┌─────────────────────────────────────┐
│          Chat Summarizer Demo        │
├─────────────────────────────────────┤
│                                     │
│           Team Alpha 🚀             │
│                                     │
│   🟡 Demo Mode – no API key needed  │
│                                     │
│   ┌─────────────────────────────┐   │
│   │  📋 Summarize Today's Chat  │   │
│   └─────────────────────────────┘   │
│                                     │
│  Tap the button to generate an AI   │
│  summary of today's messages        │
│                                     │
└─────────────────────────────────────┘

         ↓ after tapping ↓

┌─────────────────────────────────────┐
│  📋 Today's Chat Summary            │
│  🟡 Demo summary                    │
├─────────────────────────────────────┤
│                                     │
│   ⏳  (loading spinner ~2s)         │
│                                     │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┤
│                                     │
│  The Team Alpha group discussed the │
│  upcoming product launch scheduled  │
│  for next Friday. Ravi confirmed    │
│  the backend API is complete and    │
│  Priya will start QA tomorrow.      │
│  The team agreed to meet at 5 PM    │
│  on Wednesday for a final review.   │
│  Ankit flagged a push-notification  │
│  issue that Ravi will resolve by    │
│  Tuesday. Launch is on track. ✅    │
│                                     │
└─────────────────────────────────────┘
```

---

## Step 1 – Install Android Studio

Download and install **Android Studio** (free):
👉 https://developer.android.com/studio

> Recommended version: **Android Studio Hedgehog (2023.1)** or newer.

---

## Step 2 – Open the Project

1. Clone or download this repository.
2. In Android Studio, choose **File → Open**.
3. Navigate to the **`chat-summarizer-app/`** folder inside the repo and click **OK**.
4. Wait for Gradle to sync (this downloads dependencies – takes 1–3 minutes the first time).

---

## Step 3 – Run on the Emulator (Demo Mode)

No configuration needed! Just:

1. In the toolbar, select a device from the **AVD dropdown** (e.g., *Pixel 6 API 34*).
   - If you don't have an emulator yet: **Tools → Device Manager → Create Device**.
2. Click the green ▶ **Run** button (or press `Shift + F10`).
3. The app installs on the emulator in about 30 seconds.
4. Tap **"📋 Summarize Today's Chat"** — a bottom sheet slides up with a demo summary after ~2 seconds.

> **Demo Mode** uses hardcoded sample messages and returns a pre-written summary.
> No internet connection, Firebase account, or Gemini API key is required.

---

## Step 4 – Switch to Production Mode (optional)

To use **real Firebase data + live Gemini AI** summaries:

### 4a – Firebase Setup
1. Create a project at https://console.firebase.google.com
2. Add an Android app with package name `com.example.groupchat`
3. Download `google-services.json` and place it in `chat-summarizer-app/app/`
4. In `app/build.gradle.kts`, uncomment:
   ```kotlin
   // id("com.google.gms.google-services")
   ```
   and uncomment the Firebase dependency block.

### 4b – Gemini API Key
1. Get a free key at https://aistudio.google.com/app/apikey
2. Add to `chat-summarizer-app/local.properties` (create if it doesn't exist):
   ```properties
   GEMINI_API_KEY=AIza...your_key_here...
   ```
3. Rebuild the project (`Build → Make Project`).

The app automatically switches to **🟢 Production Mode** when the key is present.

---

## Project Structure

```
chat-summarizer-app/
├── app/
│   ├── build.gradle.kts              ← Gradle config + BuildConfig API key
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/groupchat/
│       │   ├── MainActivity.kt       ← Entry point, "Summarize" button
│       │   └── summarizer/
│       │       ├── ChatMessage.kt           ← Data class
│       │       ├── ChatSummarizerService.kt ← Demo + Production logic
│       │       └── SummaryBottomSheet.kt    ← UI bottom sheet
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   └── fragment_summary_bottom_sheet.xml
│           └── values/
│               ├── strings.xml
│               ├── themes.xml
│               └── colors.xml
├── build.gradle.kts                  ← Project-level plugins
└── settings.gradle.kts
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Gradle sync fails | Check internet connection; try **File → Sync Project with Gradle Files** |
| `No such file: google-services.json` | Either add the file (production) or ensure the `google-services` plugin is commented out (demo) |
| Emulator is slow | Enable hardware acceleration in the AVD settings; use an `x86_64` system image |
| App crashes on launch | Check **Logcat** in Android Studio for the error message |

---

## Learn More

- Full implementation guide → [`CHAT_SUMMARIZER.md`](CHAT_SUMMARIZER.md)
- Example reference code → [`chat-summarizer/`](chat-summarizer/)
- Gemini API docs → https://ai.google.dev/gemini-api/docs
- Firebase Firestore docs → https://firebase.google.com/docs/firestore
