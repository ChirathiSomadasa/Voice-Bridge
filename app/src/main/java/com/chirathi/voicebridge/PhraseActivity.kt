package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PhraseActivity : AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phrase)

        val refreshBtn = findViewById<ImageView>(R.id.ivRefresh)
        refreshBtn.setOnClickListener {
            val intent = Intent(this, QuickWordsChartsActivity::class.java)
            startActivity(intent)
        }

        val speakerBtn = findViewById<ImageView>(R.id.ivSpeaker)
        speakerBtn.setOnClickListener {
            val intent = Intent(this, QuickWordsChartsActivity::class.java)
            startActivity(intent)
        }

    }
}