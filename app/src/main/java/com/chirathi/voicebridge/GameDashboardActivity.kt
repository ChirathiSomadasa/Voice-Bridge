package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class GameDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game_dashboard)

        val moodMatchBtn = findViewById<Button>(R.id.btn_game1)
        val myDayBtn = findViewById<Button>(R.id.btn_level2)
        val backBtn = findViewById<ImageView>(R.id.backGame)

        // Mood Match button click - navigate to PandaIntroActivity
        moodMatchBtn.setOnClickListener {
            val intent = Intent(this, PandaIntroActivity::class.java)
            startActivity(intent)
        }

        // My Day button click - navigate to RoutineSelectionActivity
        myDayBtn.setOnClickListener {
            val intent = Intent(this, RoutineSelectionActivity::class.java)
            startActivity(intent)
        }

        // Back button click
        backBtn.setOnClickListener {
            finish()
        }
    }
}