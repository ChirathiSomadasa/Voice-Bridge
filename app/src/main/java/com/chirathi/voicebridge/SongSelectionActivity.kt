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
            startActivity(intent)
        }

        // Make entire screen clickable to go back to game dashboard
        val rootLayout = findViewById<View>(android.R.id.content)
        rootLayout.setOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}