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
        val iconData = mapOf(
            R.id.imgRed to Pair(R.drawable.red, "Red Colour"),
            R.id.imgYellow to Pair(R.drawable.yellow, "Yellow Colour"),
            R.id.imgGreen to Pair(R.drawable.green, "Green Colour"),
            R.id.imgBlue to Pair(R.drawable.blue, "Blue Colour"),
            R.id.imgBlack to Pair(R.drawable.black, "Black Colour"),
            R.id.imgPurple to Pair(R.drawable.purple, "Purple Colour"),
            R.id.imgPink to Pair(R.drawable.pink, "Pink Colour"),
            R.id.imgWhite to Pair(R.drawable.white, "White Colour")
        )

        // Set click listeners for all icons
        iconData.forEach { (imageViewId, data) ->
            val imageView = findViewById<ImageView>(imageViewId)
            imageView.setOnClickListener {
                val (drawableRes, phrase) = data
                navigateToPhraseActivity(drawableRes, phrase)
            }
        }
    }

    private fun navigateToPhraseActivity(drawableRes: Int, phrase: String) {
        val intent = Intent(this, PhraseActivity::class.java).apply {
            putExtra("SELECTED_ICON_DRAWABLE", drawableRes)
            putExtra("SELECTED_PHRASE", phrase)
        }
        startActivity(intent)
    }
}