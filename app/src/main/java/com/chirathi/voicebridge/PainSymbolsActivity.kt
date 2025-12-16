package com.chirathi.voicebridge

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PainSymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pain_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }
    }
}