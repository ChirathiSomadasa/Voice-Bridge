package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ColoursSymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_colours_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val colourList = listOf(
            SymbolItem(R.id.imgRed, R.drawable.red, "see", "red colour"),
            SymbolItem(R.id.imgYellow, R.drawable.yellow, "see", "yellow colour"),
            SymbolItem(R.id.imgGreen, R.drawable.green, "see", "green colour"),
            SymbolItem(R.id.imgBlue, R.drawable.blue, "see", "blue colour"),
            SymbolItem(R.id.imgBlack, R.drawable.black, "see", "black colour"),
            SymbolItem(R.id.imgPurple, R.drawable.purple, "see", "purple colour"),
            SymbolItem(R.id.imgPink, R.drawable.pink, "see", "pink colour"),
            SymbolItem(R.id.imgWhite, R.drawable.white, "see", "white colour")
        )

        // Set click listeners for all icons
        colourList.forEach { item ->
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