package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FoodSymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgHotDog to Pair(R.drawable.hot_dog, "I want to eat hot dog"),
            R.id.imgBurger to Pair(R.drawable.burger, "I want to eat burger"),
            R.id.imgPizza to Pair(R.drawable.pizza, " "),
            R.id.imgCupCake to Pair(R.drawable.cup_cake, " "),
            R.id.imgFishBurger to Pair(R.drawable.fish_burger, " "),
            R.id.imgToast to Pair(R.drawable.toast, ""),
            R.id.imgPancake to Pair(R.drawable.pancake, ""),
            R.id.imgSandwich to Pair(R.drawable.toasted_sandwich, " "),
            R.id.imgScrambledEggs to Pair(R.drawable.scrambled_eggs, " "),
            R.id.imgFriedEgg to Pair(R.drawable.fried_egg, " "),
            R.id.imgCorn to Pair(R.drawable.sweetcorn, " "),
            R.id.imgPasta to Pair(R.drawable.pasta, " "),
            R.id.imgNoodles to Pair(R.drawable.noodles, " "),
            R.id.imgCereal to Pair(R.drawable.cereal, " "),
            R.id.imgCheese to Pair(R.drawable.cheese, " ")
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