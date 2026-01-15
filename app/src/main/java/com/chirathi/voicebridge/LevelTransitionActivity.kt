package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout

class LevelTransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_transition)

        // 1. Get Data
        val nextBatchIndex = intent.getIntExtra("NEXT_BATCH_INDEX", 0)
        // New: Check which level called this (1 = Letters, 2 = Words, 3 = Sentences)
        val levelType = intent.getIntExtra("LEVEL_TYPE", 1)

        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val btnLetsGo = findViewById<CardView>(R.id.btnLetsGo)

        // 2. RANDOM Background Logic
        val backgroundList = listOf(
            R.drawable.win_green_bg,
            R.drawable.win_orange_bg,
            R.drawable.win_pink_bg
        )
        rootLayout.setBackgroundResource(backgroundList.random())

        // 3. Determine which activity to open based on LEVEL_TYPE
        btnLetsGo.setOnClickListener {
            val targetActivity = when (levelType) {
                1 -> SpeechLevel1TaskActivity::class.java
                2 -> SpeechLevel2TaskActivity::class.java
                // 3 -> SpeechLevel3TaskActivity::class.java
                else -> SpeechLevel1TaskActivity::class.java
            }

            val intent = Intent(this, targetActivity)
            intent.putExtra("BATCH_INDEX", nextBatchIndex)
            startActivity(intent)
            finish()
        }
    }
}