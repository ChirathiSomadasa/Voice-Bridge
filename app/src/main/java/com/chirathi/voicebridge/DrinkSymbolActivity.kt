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
        val drinkList = listOf(
            SymbolItem(R.id.imgWater, R.drawable.water, "drink", "water"),
            SymbolItem(R.id.imgCoffee, R.drawable.coffee, "drink", "coffee"),
            SymbolItem(R.id.imgTea, R.drawable.tea, "drink", "tea"),
            SymbolItem(R.id.imgOrangeJuice, R.drawable.orange_juice, "drink", "orange juice"),
            SymbolItem(R.id.imgAppleJuice, R.drawable.apple_juice, "drink", "apple juice"),
            SymbolItem(R.id.imgGrapeJuice, R.drawable.grape_juice, "drink", "grape juice"),
            SymbolItem(R.id.imgPineapple, R.drawable.pineapple_juice, "drink", "pineapple juice"),
            SymbolItem(R.id.imgLemonade, R.drawable.lemonade, "drink", "lemon juice"),
            SymbolItem(R.id.imgMilkshake, R.drawable.milkshake, "drink", "milkshake"),
            SymbolItem(R.id.imgChocolateShake, R.drawable.chocolate_milkshake, "drink", "chocolate shake"),
            SymbolItem(R.id.imgMilk, R.drawable.milk, "drink", "milk")
        )

        // Set click listeners for all icons
        drinkList.forEach { item ->
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