package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SymbolCard1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbol_card1)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        val snackBtn = findViewById<ImageView>(R.id.imgSnacks)
        snackBtn.setOnClickListener {
            val intent = Intent(this, SanackSymbolActivity::class.java)
            startActivity(intent)
        }

        val drinkBtn = findViewById<ImageView>(R.id.imgDrinks)
        drinkBtn.setOnClickListener {
            val intent = Intent(this, DrinkSymbolActivity::class.java)
            startActivity(intent)
        }
    }
}