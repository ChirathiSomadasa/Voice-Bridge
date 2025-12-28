package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ToysSymbolActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toys_symbol)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgDoll to Pair(R.drawable.doll, "Yes"),
            R.id.imgTeddyBear to Pair(R.drawable.teddy_bear, "Ok"),
            R.id.imgPhone to Pair(R.drawable.toy_telephone, "No"),
            R.id.imgToyBox to Pair(R.drawable.toy_box, "Thank you"),
            R.id.imgPullCar to Pair(R.drawable.pull_along_toy, "Hello"),
            R.id.imgSoldier to Pair(R.drawable.toy_soldier, "Good Bye"),
            R.id.imgColourBook to Pair(R.drawable.colouring_book, "I need to use washroom"),
            R.id.imgBall to Pair(R.drawable.toy_ball, "I'm hungry"),
            R.id.imgToyCar to Pair(R.drawable.toy_car, "I am thirsty"),
            R.id.imgKite to Pair(R.drawable.kite, "I want to play"),
            R.id.imgBubbles to Pair(R.drawable.bubbles, "Please")
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