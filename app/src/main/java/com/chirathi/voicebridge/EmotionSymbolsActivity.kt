package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EmotionSymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_emotion_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgHappy to Pair(R.drawable.happy_emotion, "I want to drink water"),
            R.id.imgSad to Pair(R.drawable.sad_emotion, "Ok"),
            R.id.imgAngry to Pair(R.drawable.angry_emotion, "No"),
            R.id.imgAfraid to Pair(R.drawable.afraid_emotion, "Thank you")
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