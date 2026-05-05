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
        val activityList = listOf(
            SymbolItem(R.id.imgCycle, R.drawable.cycle, "ride", "bicycle"),
            SymbolItem(R.id.imgTV, R.drawable.flatscreen_tv, "watch", "TV"),
            SymbolItem(R.id.imgStudy, R.drawable.study, "do", "studies"),
            SymbolItem(R.id.imgBathe, R.drawable.bathe, "take", "bath"),
            SymbolItem(R.id.imgWashFace, R.drawable.wash_face, "wash", "face"),
            SymbolItem(R.id.imgWashHands, R.drawable.wash_hands, "wash", "hands"),
            SymbolItem(R.id.imgRun, R.drawable.run, "go", "run"),
            SymbolItem(R.id.imgGo, R.drawable.go, "go", "out"),
            SymbolItem(R.id.imgSit, R.drawable.sit, "sit", "down"),
            SymbolItem(R.id.imgExercise, R.drawable.exercise, "do", "exercise"),
            SymbolItem(R.id.imgSwim, R.drawable.swim, "go", "swimming"),
            SymbolItem(R.id.imgSwing, R.drawable.swing, "play", "swing"),
            SymbolItem(R.id.imgCelebrate, R.drawable.celebrate, "start", "celebration"),
            SymbolItem(R.id.imgCelebrateBirth, R.drawable.celebrate_birth, "wish", "happy birthday"),
            SymbolItem(R.id.imgSleep, R.drawable.sleep, "go", "sleep")
        )

        // Set click listeners for all icons
        activityList.forEach { item ->
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