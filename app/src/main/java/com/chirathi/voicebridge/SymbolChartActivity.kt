package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SymbolChartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_chart)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        val goBtn = findViewById<Button>(R.id.btn_Go_symbol)
        goBtn.setOnClickListener {
            val intent = Intent(this, SymbolCard1Activity::class.java)
            startActivity(intent)
        }
    }
}