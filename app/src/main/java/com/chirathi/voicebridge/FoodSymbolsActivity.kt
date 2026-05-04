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
        val foodList = listOf(
            SymbolItem(R.id.imgHotDog, R.drawable.hot_dog, "eat", "hot dog"),
            SymbolItem(R.id.imgBurger, R.drawable.burger, "eat", "burger"),
            SymbolItem(R.id.imgPizza, R.drawable.pizza, "eat", "pizza"),
            SymbolItem(R.id.imgCupCake, R.drawable.cup_cake, "eat", "cupcake"),
            SymbolItem(R.id.imgFishBurger, R.drawable.fish_burger, "eat", "fish burger"),
            SymbolItem(R.id.imgToast, R.drawable.toast, "eat", "toasted bun"),
            SymbolItem(R.id.imgPancake, R.drawable.pancake, "eat", "pancake"),
            SymbolItem(R.id.imgSandwich, R.drawable.toasted_sandwich, "eat", "sandwich"),
            SymbolItem(R.id.imgScrambledEggs, R.drawable.scrambled_eggs, "eat", "scrambled egg"),
            SymbolItem(R.id.imgFriedEgg, R.drawable.fried_egg, "eat", "fried egg"),
            SymbolItem(R.id.imgCorn, R.drawable.sweetcorn, "eat", "corn"),
            SymbolItem(R.id.imgPasta, R.drawable.pasta, "eat", "pasta"),
            SymbolItem(R.id.imgNoodles, R.drawable.noodles, "eat", "noodles"),
            SymbolItem(R.id.imgCereal, R.drawable.cereal, "eat", "cereal"),
            SymbolItem(R.id.imgCheese, R.drawable.cheese, "eat", "cheese")
        )

        // Set click listeners for all icons
        foodList.forEach { item ->
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