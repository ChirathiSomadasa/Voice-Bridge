package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DrinkSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drink_symbol)
        
        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgWater to Pair(R.drawable.water, "I want to drink water"),
            R.id.imgCoffee to Pair(R.drawable.coffee, "I want to drink coffee"),
            R.id.imgTea to Pair(R.drawable.tea, " "),
            R.id.imgOrangeJuice to Pair(R.drawable.orange_juice, " "),
            R.id.imgAppleJuice to Pair(R.drawable.apple_juice, " "),
            R.id.imgGrapeJuice to Pair(R.drawable.grape_juice, ""),
            R.id.imgPineapple to Pair(R.drawable.pineapple_juice, ""),
            R.id.imgLemonade to Pair(R.drawable.lemonade, ""),
            R.id.imgMilkshake to Pair(R.drawable.milkshake, " "),
            R.id.imgChocolateShake to Pair(R.drawable.chocolate_milkshake, ""),
            R.id.imgMilk to Pair(R.drawable.milk, "")
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