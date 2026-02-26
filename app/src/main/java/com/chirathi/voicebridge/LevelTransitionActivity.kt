package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout

class LevelTransitionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_transition)

        // 1. Get Data
        val nextBatchIndex = intent.getIntExtra("NEXT_BATCH_INDEX", 0)
        val levelType = intent.getIntExtra("LEVEL_TYPE", 1) // 1=Letters, 2=Words, 3=Sentences

        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val btnLetsGo = findViewById<CardView>(R.id.btnLetsGo)

        // --- DYNAMIC TEXT LOGIC ---
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)

        val subtitleText = when (levelType) {
            1 -> "Get ready for new letters!"
            2 -> "Get ready for new words!"
            3 -> "Get ready for new sentences!"
            else -> "Get ready for the next challenge!"
        }
        tvSubtitle.text = subtitleText

        // 2. RANDOM Background
        val backgroundList = listOf(
            R.drawable.win_green_bg,
            R.drawable.win_orange_bg,
            R.drawable.win_pink_bg
        )
        rootLayout.setBackgroundResource(backgroundList.random())

        // 3. Determine target activity
        btnLetsGo.setOnClickListener {
            val targetActivity = when (levelType) {
                1 -> SpeechLevel1TaskActivity::class.java
                2 -> SpeechLevel2TaskActivity::class.java
                3 -> SpeechLevel3TaskActivity::class.java
                else -> SpeechLevel1TaskActivity::class.java
            }

            val intent = Intent(this, targetActivity)
            intent.putExtra("BATCH_INDEX", nextBatchIndex)
            startActivity(intent)
            finish()
        }
    }
}