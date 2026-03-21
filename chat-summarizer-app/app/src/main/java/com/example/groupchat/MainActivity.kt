package com.example.groupchat

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.groupchat.databinding.ActivityMainBinding
import com.example.groupchat.summarizer.SummaryBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // The group ID used to fetch messages from Firestore.
    // In Demo Mode this value is ignored – sample data is used instead.
    private val groupId = "demo_group"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show the current mode in the UI
        val isDemoMode = BuildConfig.GEMINI_API_KEY.isBlank()
        binding.tvMode.text = if (isDemoMode) {
            "🟡 Demo Mode – sample data, no API key needed"
        } else {
            "🟢 Production Mode – live Firebase + Gemini"
        }

        // Tapping the button opens the SummaryBottomSheet.
        // Demo Mode and Production Mode are handled automatically inside the sheet.
        binding.btnSummarize.setOnClickListener {
            SummaryBottomSheet.newInstance(groupId)
                .show(supportFragmentManager, "summary")
        }
    }
}
