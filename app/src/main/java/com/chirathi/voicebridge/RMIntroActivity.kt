package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RMIntroActivity : AppCompatActivity() {

    private lateinit var currentSongTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rmintro)

        // Get the song title from intent
        currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"

        // Set click listener for the entire layout
        findViewById<View>(R.id.main).setOnClickListener {
            navigateToRhythmSummary()
        }
    }

    override fun onResume() {
        super.onResume()
        // Start animations when activity resumes
        startAnimations()
    }

    override fun onPause() {
        super.onPause()
        // Stop animations when activity pauses
        stopAnimations()
    }

    private fun startAnimations() {
        val feedbackImage = findViewById<ImageView>(R.id.feedbackImage)
        val tapHint = findViewById<TextView>(R.id.tapHint)

        // Bounce animation for panda
        ObjectAnimator.ofFloat(feedbackImage, "translationY", 0f, -30f, 0f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = BounceInterpolator()
            start()
        }

        // Pulse animation for tap hint
        ObjectAnimator.ofFloat(tapHint, "alpha", 0.5f, 1f, 0.5f).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopAnimations() {
        val feedbackImage = findViewById<ImageView>(R.id.feedbackImage)
        val tapHint = findViewById<TextView>(R.id.tapHint)
        feedbackImage.clearAnimation()
        tapHint.clearAnimation()
    }

    private fun navigateToRhythmSummary() {
        // Stop animations first
        stopAnimations()

        // Navigate immediately
        val intent = Intent(this, RhythmSummaryActivity::class.java)
        intent.putExtra("SONG_TITLE", currentSongTitle)
        startActivity(intent)
        finish()
    }
    
}