package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ClothesSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothes_symbol)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgShirt to Pair(R.drawable.shirt, "I want to wear shirt"),
            R.id.imgTShirt to Pair(R.drawable.tshirt, "I want to wear shirt "),
            R.id.imgTrouser to Pair(R.drawable.jeans, " "),
            R.id.imgFrock to Pair(R.drawable.frock, " "),
            R.id.imgShort to Pair(R.drawable.shorts, " "),
            R.id.imgRainCoat to Pair(R.drawable.raincoat, ""),
            R.id.imgSkirt to Pair(R.drawable.skirt, " "),
            R.id.imgBlouse to Pair(R.drawable.blouse, " "),
            R.id.imgNightSuit to Pair(R.drawable.nightsuit, " "),
            R.id.imgVest to Pair(R.drawable.vest, " "),
            R.id.imgSlippers to Pair(R.drawable.slippers, " "),
            R.id.imgSandals to Pair(R.drawable.sandals, " "),
            R.id.imgWatch to Pair(R.drawable.watch, " "),
            R.id.imgScarf to Pair(R.drawable.scarf, " "),
            R.id.imgHankerchief to Pair(R.drawable.hankerchief, " "),
            R.id.imgWallet to Pair(R.drawable.wallet, " "),
            R.id.imgGlasses to Pair(R.drawable.glasses, " "),
            R.id.imgSunGlasses to Pair(R.drawable.sunglasses, " "),
            R.id.imgSocks to Pair(R.drawable.socks, " "),
            R.id.imgUmbrella to Pair(R.drawable.umbrella, " "),
            R.id.imgTie to Pair(R.drawable.tie, " "),
            R.id.imgBowTie to Pair(R.drawable.bow_tie, " "),
            R.id.imgCap to Pair(R.drawable.cap, " "),
            R.id.imgBelt to Pair(R.drawable.belt, " ")
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