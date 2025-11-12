package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class QuickWordsActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_words)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }
        val goBtn = findViewById<Button>(R.id.btn_Go_quick)
        goBtn.setOnClickListener {
            val intent = Intent(this, QuickWordsChartsActivity::class.java)
            startActivity(intent)
        }

    }
}