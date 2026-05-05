package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ToysSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toys_symbol)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val toyList = listOf(
            SymbolItem(R.id.imgDoll, R.drawable.doll, "play", "doll"),
            SymbolItem(R.id.imgTeddyBear, R.drawable.teddy_bear, "play", "teddy bear"),
            SymbolItem(R.id.imgPhone, R.drawable.toy_telephone, "play", "toy phone"),
            SymbolItem(R.id.imgToyBox, R.drawable.toy_box, "open", "toy box"),
            SymbolItem(R.id.imgPullCar, R.drawable.pull_along_toy, "pull", "toy car"),
            SymbolItem(R.id.imgSoldier, R.drawable.toy_soldier, "play", "toy soldier"),
            SymbolItem(R.id.imgColourBook, R.drawable.colouring_book, "use", "colouring book"),
            SymbolItem(R.id.imgBall, R.drawable.toy_ball, "play", "ball"),
            SymbolItem(R.id.imgToyCar, R.drawable.toy_car, "play", "toy car"),
            SymbolItem(R.id.imgKite, R.drawable.kite, "fly", "kite"),
            SymbolItem(R.id.imgBubbles, R.drawable.bubbles, "play", "bubbles")
        )

        // Set click listeners for all icons
        toyList.forEach { item ->
            findViewById<ImageView>(item.id)?.setOnClickListener {
                val phrase = "${item.verb} ${item.obj}"
                navigateToPhraseActivity(item.imageRes, phrase)
            }
        }
    }

    private fun navigateToPhraseActivity(drawableRes: Int, phrase: String) {
        val intent = Intent(this, PhraseActivity::class.java).apply {
            putExtra("SELECTED_ICON_DRAWABLE", drawableRes)
            putExtra("SELECTED_PHRASE", phrase)
            putExtra("IS_SYMBOL_MODE", true)
        }
        startActivity(intent)
    }
}