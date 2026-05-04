package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

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
        // Grammar eka poddak hadala thiyenne Gemini ekata lesi wenna
        val iconData = mapOf(
            R.id.imgHappy to Pair(R.drawable.happy_emotion, "I am happy"),
            R.id.imgSad to Pair(R.drawable.sad_emotion, "I am sad"),
            R.id.imgAngry to Pair(R.drawable.angry_emotion, "I am angry"),
            R.id.imgAfraid to Pair(R.drawable.afraid_emotion, "I am afraid")
        )

        iconData.forEach { (imageViewId, data) ->
            findViewById<ImageView>(imageViewId)?.setOnClickListener {
                val (drawableRes, phrase) = data
                navigateToPhraseActivity(drawableRes, phrase)
            }
        }
    }

    private fun navigateToPhraseActivity(drawableRes: Int, phrase: String) {
        val intent = Intent(this, PhraseActivity::class.java).apply {
            putExtra("SELECTED_ICON_DRAWABLE", drawableRes)
            putExtra("SELECTED_PHRASE", phrase)
            putExtra("IS_EMOTION_MODE", true)
        }
        startActivity(intent)
    }
}