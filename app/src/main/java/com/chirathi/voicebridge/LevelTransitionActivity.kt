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

        // 1. Get the NEXT batch index
        val nextBatchIndex = intent.getIntExtra("NEXT_BATCH_INDEX", 0)

        val rootLayout = findViewById<ConstraintLayout>(R.id.rootLayout)
        val btnLetsGo = findViewById<CardView>(R.id.btnLetsGo)

        // 2. RANDOM Background Logic
        // Define list of backgrounds
        val backgroundList = listOf(
            R.drawable.win_green_bg,
            R.drawable.win_orange_bg,
            R.drawable.win_pink_bg
        )

        // Pick one randomly
        val randomBackground = backgroundList.random()

        // Apply it
        rootLayout.setBackgroundResource(randomBackground)

        // 3. Button Action -> Start the Task with NEW letters
        btnLetsGo.setOnClickListener {
            val intent = Intent(this, SpeechLevel1TaskActivity::class.java)
            intent.putExtra("BATCH_INDEX", nextBatchIndex)
            startActivity(intent)
            finish()
        }
    }
}