# chat-summarizer – Example Module

This folder contains **ready-to-copy Kotlin source files** for the group-chat summarizer feature described in [`CHAT_SUMMARIZER.md`](../CHAT_SUMMARIZER.md).

## Files

| File | Purpose |
|---|---|
| `ChatMessage.kt` | Data class that maps to a Firestore document |
| `ChatSummarizerService.kt` | Fetches today's messages & calls the Gemini API |
| `SummaryBottomSheet.kt` | Material bottom-sheet UI that shows the summary |

## Quick Start

1. Copy all three `.kt` files into your app's source set (update the package name).
2. Follow the setup instructions in [`CHAT_SUMMARIZER.md`](../CHAT_SUMMARIZER.md) to add dependencies and your Gemini API key.
3. Add the XML layout from the comment block at the top of `SummaryBottomSheet.kt` to `res/layout/fragment_summary_bottom_sheet.xml`.
4. Call `SummaryBottomSheet.newInstance(groupId).show(supportFragmentManager, "summary")` from your activity or fragment.
