package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SanackSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sanack_symbol)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgIceCream to Pair(R.drawable.ice_cream, "I want ice cream"),
            R.id.imgChocolate to Pair(R.drawable.chocolate, "I want chocolate"),
            R.id.imgCrsips to Pair(R.drawable.crisps, " I want crsips"),
            R.id.imgBiscuit to Pair(R.drawable.biscuit, "I want biscuit "),
            R.id.imgPeaNuts to Pair(R.drawable.nuts, "I want peanuts "),
            R.id.imgPop to Pair(R.drawable.pop, "I want lollypop"),
            R.id.imgYogurt to Pair(R.drawable.yogurt, "I want yogurt"),
            R.id.imgCake to Pair(R.drawable.cake, "I want cake"),
            R.id.imgToffee to Pair(R.drawable.toffee, "I want toffee")
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