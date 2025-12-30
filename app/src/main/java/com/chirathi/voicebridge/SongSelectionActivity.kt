package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat

class SongSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_selection)

        // Row Row Row Your Boat button click
        val rowRowBoatLayout = findViewById<LinearLayoutCompat>(R.id.rowRowBoatLayout)
        rowRowBoatLayout.setOnClickListener {
            val intent = Intent(this, MusicPlayerActivity::class.java)
            intent.putExtra("song_title", "Row Row Row Your Boat")
            intent.putExtra("song_duration", "1:05")
            startActivity(intent)
        }

        // Back button functionality
        findViewById<View>(android.R.id.content).setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, GameDashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}