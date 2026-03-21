package com.example.groupchat.summarizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * SummaryBottomSheet
 *
 * A Material BottomSheetDialogFragment that:
 *  1. Shows a loading spinner while fetching and summarising today's group messages.
 *  2. Displays the AI-generated summary once it is ready.
 *  3. Shows an error message if the API call fails.
 *
 * Usage from your Activity or Fragment:
 * ```
 * binding.btnSummarize.setOnClickListener {
 *     SummaryBottomSheet.newInstance(groupId = "your_group_id")
 *         .show(supportFragmentManager, "summary")
 * }
 * ```
 *
 * Layout file required: res/layout/fragment_summary_bottom_sheet.xml
 * (see the XML snippet in CHAT_SUMMARIZER.md or below)
 *
 * -----------------------------------------------------------------------
 * Minimal layout (fragment_summary_bottom_sheet.xml):
 * -----------------------------------------------------------------------
 * <?xml version="1.0" encoding="utf-8"?>
 * <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:orientation="vertical"
 *     android:padding="24dp">
 *
 *     <TextView
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:text="Today's Chat Summary"
 *         android:textSize="18sp"
 *         android:textStyle="bold"
 *         android:layout_marginBottom="16dp" />
 *
 *     <ProgressBar
 *         android:id="@+id/progressBar"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:layout_gravity="center_horizontal" />
 *
 *     <TextView
 *         android:id="@+id/tvSummary"
 *         android:layout_width="match_parent"
 *         android:layout_height="wrap_content"
 *         android:visibility="gone"
 *         android:textSize="15sp"
 *         android:lineSpacingExtra="4dp" />
 *
 * </LinearLayout>
 * -----------------------------------------------------------------------
 */
class SummaryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): SummaryBottomSheet {
            return SummaryBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout.
        // R.layout.fragment_summary_bottom_sheet refers to the XML file described
        // in the comment block at the top of this file (and in CHAT_SUMMARIZER.md).
        return inflater.inflate(R.layout.fragment_summary_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvSummary   = view.findViewById<TextView>(R.id.tvSummary)

        val groupId = arguments?.getString(ARG_GROUP_ID)
            ?: run {
                Toast.makeText(context, "Missing group ID", Toast.LENGTH_SHORT).show()
                dismiss()
                return
            }

        // Read the API key from BuildConfig (set via local.properties + build.gradle).
        // Replace the string below with: BuildConfig.GEMINI_API_KEY
        val apiKey = getApiKey()
        val service = ChatSummarizerService(apiKey = apiKey)

        // lifecycleScope is tied to the fragment's lifecycle and is automatically
        // cancelled when the fragment is destroyed, preventing coroutine leaks.
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                tvSummary.visibility = View.GONE

                val summary = service.summarizeTodaysChat(groupId)

                progressBar.visibility = View.GONE
                tvSummary.text = summary
                tvSummary.visibility = View.VISIBLE
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvSummary.text = "Failed to load summary: ${e.localizedMessage}"
                tvSummary.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No manual cancel needed – lifecycleScope handles cleanup automatically.
    }

    /**
     * Returns the Gemini API key.
     *
     * In production, replace this with:
     *   return BuildConfig.GEMINI_API_KEY
     *
     * Never hard-code your API key in source code.
     * Store it in local.properties and expose it via BuildConfig
     * (see CHAT_SUMMARIZER.md – Step 2 for full instructions).
     */
    private fun getApiKey(): String {
        // Replace this with: return BuildConfig.GEMINI_API_KEY
        // See CHAT_SUMMARIZER.md – Step 2 for full setup instructions.
        val key = BuildConfig.GEMINI_API_KEY
        check(key.isNotBlank()) {
            "GEMINI_API_KEY is not configured. " +
                "Add it to local.properties and expose it via BuildConfig. " +
                "See CHAT_SUMMARIZER.md – Step 2 for instructions."
        }
        return key
    }
}
