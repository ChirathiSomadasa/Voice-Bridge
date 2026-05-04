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
        val sanitationList = listOf(
            SymbolItem(R.id.imgToothbrush, R.drawable.toothbrush, "use", "toothbrush"),
            SymbolItem(R.id.imgToothpaste, R.drawable.toothpaste, "use", "toothpaste"),
            SymbolItem(R.id.imgSoap, R.drawable.soap, "use", "soap"),
            SymbolItem(R.id.imgTowel, R.drawable.towel, "use", "towel"),
            SymbolItem(R.id.imgComb, R.drawable.comb, "use", "comb"),
            SymbolItem(R.id.imgTissues, R.drawable.tissues, "need", "tissue"),
            SymbolItem(R.id.imgShampoo, R.drawable.shampoo, "use", "shampoo"),
            SymbolItem(R.id.imgConditioner, R.drawable.hair_conditioner, "use", "conditioner"),
            SymbolItem(R.id.imgNailClippers, R.drawable.nail_clippers, "use", "nail clipper")
        )

        // Set click listeners for all icons
        sanitationList.forEach { item ->
            findViewById<ImageView>(item.id)?.setOnClickListener {
                // "use toothbrush" wage phrase ekak yawwama Gemini "I want to use my toothbrush" wage hadai
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