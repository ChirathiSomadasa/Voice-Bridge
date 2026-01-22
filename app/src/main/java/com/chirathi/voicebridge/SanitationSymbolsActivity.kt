package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SanitationSymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sanitation_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgToothbrush to Pair(R.drawable.toothbrush, " "),
            R.id.imgToothpaste to Pair(R.drawable.toothpaste, " "),
            R.id.imgSoap to Pair(R.drawable.soap, " "),
            R.id.imgTowel to Pair(R.drawable.towel, " "),
            R.id.imgComb to Pair(R.drawable.comb, " "),
            R.id.imgTissues to Pair(R.drawable.tissues, " "),
            R.id.imgShampoo to Pair(R.drawable.shampoo, " "),
            R.id.imgConditioner to Pair(R.drawable.hair_conditioner, " "),
            R.id.imgNailClippers to Pair(R.drawable.nail_clippers, " ")
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