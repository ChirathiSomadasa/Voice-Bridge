package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout

class LetterProgressActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_letter_progress)

        val TryAgainBtn = findViewById<LinearLayout>(R.id.llTryAgain)
        TryAgainBtn.setOnClickListener {
            val intent = Intent(this, SpeechLevel1Activity::class.java)
            startActivity(intent)
        }

        val HomeBtn = findViewById<LinearLayout>(R.id.llHome)
        HomeBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

    }
}