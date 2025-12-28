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
            R.id.imgCycle to Pair(R.drawable.cycle, "Yes"),
            R.id.imgTV to Pair(R.drawable.flatscreen_tv, "Ok"),
            R.id.imgStudy to Pair(R.drawable.study, "No"),
            R.id.imgBathe to Pair(R.drawable.bathe, "Thank you"),
            R.id.imgWashFace to Pair(R.drawable.wash_face, "Hello"),
            R.id.imgWashHands to Pair(R.drawable.wash_hands, "Good Bye"),
            R.id.imgRun to Pair(R.drawable.run, "I need to use washroom"),
            R.id.imgGo to Pair(R.drawable.go, "I'm hungry"),
            R.id.imgSit to Pair(R.drawable.sit, "I am thirsty"),
            R.id.imgExercise to Pair(R.drawable.exercise, "I want to play"),
            R.id.imgSwim to Pair(R.drawable.swim, "Please"),
            R.id.imgSwing to Pair(R.drawable.swing, "Bad"),
            R.id.imgCelebrate to Pair(R.drawable.celebrate, "Good"),
            R.id.imgCelebrateBirth to Pair(R.drawable.celebrate_birth, "I have pain"),
            R.id.imgSleep to Pair(R.drawable.sleep, "Stop")
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