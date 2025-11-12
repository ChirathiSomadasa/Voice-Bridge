package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class SpeechPracticeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_practice)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        val level1 = findViewById<Button>(R.id.btn_level1)
        level1.setOnClickListener {
            val intent = Intent(this, SpeechLevel1Activity::class.java)
            startActivity(intent)
        }

        val level2 = findViewById<Button>(R.id.btn_level2)
        level2.setOnClickListener {
            val intent = Intent(this, SpeechLevel2Activity::class.java)
            startActivity(intent)
        }
    }
}