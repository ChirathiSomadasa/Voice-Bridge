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
            R.id.imgShirt to Pair(R.drawable.shirt, "Yes"),
            R.id.imgTShirt to Pair(R.drawable.tshirt, "Ok"),
            R.id.imgTrouser to Pair(R.drawable.jeans, "No"),
            R.id.imgFrock to Pair(R.drawable.frock, "Thank you"),
            R.id.imgShort to Pair(R.drawable.shorts, "Hello"),
            R.id.imgRainCoat to Pair(R.drawable.raincoat, "Good Bye"),
            R.id.imgSkirt to Pair(R.drawable.skirt, "I need to use washroom"),
            R.id.imgBlouse to Pair(R.drawable.blouse, "I'm hungry"),
            R.id.imgNightSuit to Pair(R.drawable.nightsuit, "I am thirsty"),
            R.id.imgVest to Pair(R.drawable.vest, "I want to play"),
            R.id.imgSlippers to Pair(R.drawable.slippers, "Please"),
            R.id.imgSandals to Pair(R.drawable.sandals, "Bad"),
            R.id.imgWatch to Pair(R.drawable.watch, "Good"),
            R.id.imgScarf to Pair(R.drawable.scarf, "I have pain"),
            R.id.imgHankerchief to Pair(R.drawable.hankerchief, "Stop"),
            R.id.imgWallet to Pair(R.drawable.wallet, "I need to use washroom"),
            R.id.imgGlasses to Pair(R.drawable.glasses, "I'm hungry"),
            R.id.imgSunGlasses to Pair(R.drawable.sunglasses, "I am thirsty"),
            R.id.imgSocks to Pair(R.drawable.socks, "I want to play"),
            R.id.imgUmbrella to Pair(R.drawable.umbrella, "Please"),
            R.id.imgTie to Pair(R.drawable.tie, "Bad"),
            R.id.imgBowTie to Pair(R.drawable.bow_tie, "Good"),
            R.id.imgCap to Pair(R.drawable.cap, "I have pain"),
            R.id.imgBelt to Pair(R.drawable.belt, "Stop")
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