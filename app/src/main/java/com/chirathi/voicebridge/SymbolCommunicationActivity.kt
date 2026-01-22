package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SymbolCommunicationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_communication)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Quick Words button functionality
        val quickWordsBtn = findViewById<Button>(R.id.btn_quick_words)
        quickWordsBtn.setOnClickListener {
            val intent = Intent(this, QuickWordsActivity::class.java)
            startActivity(intent)
        }

        // symbol chart button functionality
        val symbolChartBtn = findViewById<Button>(R.id.btn_symbol_charts)
        symbolChartBtn.setOnClickListener {
            val intent = Intent(this, SymbolChartActivity::class.java)
            startActivity(intent)
        }

    }
}