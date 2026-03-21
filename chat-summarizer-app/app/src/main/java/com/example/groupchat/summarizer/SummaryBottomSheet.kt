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
import com.example.groupchat.R
import kotlinx.coroutines.launch

/**
 * SummaryBottomSheet
 *
 * Displays the AI-generated (or demo) summary of today's group chat.
 *
 *  • While the summary is loading   → shows a ProgressBar
 *  • Once the summary is ready      → hides the spinner, shows the text
 *  • On error                       → shows a friendly error message
 *
 * Demo Mode is enabled automatically when BuildConfig.GEMINI_API_KEY is blank.
 * No configuration is needed to see the demo summary.
 */
class SummaryBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String): SummaryBottomSheet =
            SummaryBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_GROUP_ID, groupId) }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_summary_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val tvSummary   = view.findViewById<TextView>(R.id.tvSummary)
        val tvMode      = view.findViewById<TextView>(R.id.tvMode)

        val groupId = arguments?.getString(ARG_GROUP_ID) ?: run {
            Toast.makeText(context, "Missing group ID", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val service = ChatSummarizerService()
        tvMode.text = if (service.isDemoMode) "🟡 Demo summary" else "🟢 Live summary"

        // lifecycleScope is tied to the fragment lifecycle – auto-cancelled on destroy.
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                tvSummary.visibility   = View.GONE

                val summary = service.summarizeTodaysChat(groupId)

                progressBar.visibility = View.GONE
                tvSummary.text         = summary
                tvSummary.visibility   = View.VISIBLE
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvSummary.text         = "⚠️ Failed to load summary:\n${e.localizedMessage}"
                tvSummary.visibility   = View.VISIBLE
            }
        }
    }
}
