package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class QuickWordsChartsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_words_chart)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        val playIcon = findViewById<ImageView>(R.id.imgPlay)
        val stopIcon = findViewById<ImageView>(R.id.imgStop)

        playIcon.setOnClickListener {
            val intent = Intent(this, PhraseActivity::class.java)
            startActivity(intent)
        }
    }

}
