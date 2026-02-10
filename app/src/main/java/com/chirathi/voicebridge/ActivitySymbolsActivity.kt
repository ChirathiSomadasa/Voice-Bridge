package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ActivitySymbolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symbols)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }

        setupIconClickListeners()
    }

    private fun setupIconClickListeners() {
        // Map of image view IDs to drawable resources and phrases
        val iconData = mapOf(
            R.id.imgCycle to Pair(R.drawable.cycle, "I want to cycle a bicycle"),
            R.id.imgTV to Pair(R.drawable.flatscreen_tv, "I want to watch TV"),
            R.id.imgStudy to Pair(R.drawable.study, "I want to do my studies"),
            R.id.imgBathe to Pair(R.drawable.bathe, "I want to have a bathe "),
            R.id.imgWashFace to Pair(R.drawable.wash_face, " I want to wash my face"),
            R.id.imgWashHands to Pair(R.drawable.wash_hands, "I want to wash my hands "),
            R.id.imgRun to Pair(R.drawable.run, "I want to run "),
            R.id.imgGo to Pair(R.drawable.go, "I want to go"),
            R.id.imgSit to Pair(R.drawable.sit, "I want to sit"),
            R.id.imgExercise to Pair(R.drawable.exercise, " I want to do exercise"),
            R.id.imgSwim to Pair(R.drawable.swim, " I want to swim"),
            R.id.imgSwing to Pair(R.drawable.swing, " I want to swing"),
            R.id.imgCelebrate to Pair(R.drawable.celebrate, " I want to celebrate"),
            R.id.imgCelebrateBirth to Pair(R.drawable.celebrate_birth, "Happy Birthday!"),
            R.id.imgSleep to Pair(R.drawable.sleep, "I want to sleep ")
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