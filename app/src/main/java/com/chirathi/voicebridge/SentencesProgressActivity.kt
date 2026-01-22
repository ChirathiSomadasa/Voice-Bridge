package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

class SentencesProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sentences_progress)

        val TryAgainBtn = findViewById<LinearLayout>(R.id.llTryAgain)
        TryAgainBtn.setOnClickListener {
            val intent = Intent(this, SpeechLevel3Activity::class.java)
            startActivity(intent)
        }

        val HomeBtn = findViewById<LinearLayout>(R.id.llHome)
        HomeBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}