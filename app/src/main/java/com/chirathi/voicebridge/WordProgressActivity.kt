package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

class WordProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word_progress)

        val TryAgainBtn = findViewById<LinearLayout>(R.id.llTryAgain)
        TryAgainBtn.setOnClickListener {
            val intent = Intent(this, SpeechLevel2Activity::class.java)
            startActivity(intent)
        }

        val HomeBtn = findViewById<LinearLayout>(R.id.llHome)
        HomeBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}