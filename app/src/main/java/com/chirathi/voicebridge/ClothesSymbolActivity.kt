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
            R.id.imgShirt to Pair(R.drawable.shirt, "I want to wear a shirt"),
            R.id.imgTShirt to Pair(R.drawable.tshirt, "I want to wear a t shirt "),
            R.id.imgTrouser to Pair(R.drawable.jeans, "I want to wear a trouser "),
            R.id.imgFrock to Pair(R.drawable.frock, " I want to wear a frock"),
            R.id.imgShort to Pair(R.drawable.shorts, " I want to wear a short"),
            R.id.imgRainCoat to Pair(R.drawable.raincoat, "I want to wear a raincoat"),
            R.id.imgSkirt to Pair(R.drawable.skirt, "I want to wear a skirt "),
            R.id.imgBlouse to Pair(R.drawable.blouse, "I want to wear a blouse "),
            R.id.imgNightSuit to Pair(R.drawable.nightsuit, "I want to wear a night suit "),
            R.id.imgVest to Pair(R.drawable.vest, "I want to wear a vest "),
            R.id.imgSlippers to Pair(R.drawable.slippers, "I want to wear slippers "),
            R.id.imgSandals to Pair(R.drawable.sandals, "I want to wear sandals "),
            R.id.imgWatch to Pair(R.drawable.watch, "I want to wear a watch"),
            R.id.imgScarf to Pair(R.drawable.scarf, " I want to wear a scarf"),
            R.id.imgHankerchief to Pair(R.drawable.hankerchief, "I want a hankerchief "),
            R.id.imgWallet to Pair(R.drawable.wallet, "I want a wallet "),
            R.id.imgGlasses to Pair(R.drawable.glasses, "I want to wear glasses "),
            R.id.imgSunGlasses to Pair(R.drawable.sunglasses, "I want to wear sunglass "),
            R.id.imgSocks to Pair(R.drawable.socks, "I want to wear socks "),
            R.id.imgUmbrella to Pair(R.drawable.umbrella, " I want umbrella"),
            R.id.imgTie to Pair(R.drawable.tie, " I want to wear a tie"),
            R.id.imgBowTie to Pair(R.drawable.bow_tie, "I want to wear a bow tie "),
            R.id.imgCap to Pair(R.drawable.cap, "I want +a cap "),
            R.id.imgBelt to Pair(R.drawable.belt, " I want a belt ")
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