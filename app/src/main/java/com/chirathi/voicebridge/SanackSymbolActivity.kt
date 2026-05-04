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
        val snackList = listOf(
            SymbolItem(R.id.imgIceCream, R.drawable.ice_cream, "eat", "ice cream"),
            SymbolItem(R.id.imgChocolate, R.drawable.chocolate, "eat", "chocolate"),
            SymbolItem(R.id.imgCrsips, R.drawable.crisps, "eat", "crisps"),
            SymbolItem(R.id.imgBiscuit, R.drawable.biscuit, "eat", "biscuit"),
            SymbolItem(R.id.imgPeaNuts, R.drawable.nuts, "eat", "peanuts"),
            SymbolItem(R.id.imgPop, R.drawable.pop, "eat", "lollypop"),
            SymbolItem(R.id.imgYogurt, R.drawable.yogurt, "eat", "yogurt"),
            SymbolItem(R.id.imgCake, R.drawable.cake, "eat", "cake"),
            SymbolItem(R.id.imgToffee, R.drawable.toffee, "eat", "toffee")
        )

        // Set click listeners for all icons
        snackList.forEach { item ->
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