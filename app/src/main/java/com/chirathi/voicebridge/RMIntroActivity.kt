package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RMIntroActivity : AppCompatActivity() {

    private lateinit var currentSongTitle: String
    private val TAG = "RMIntroActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RMIntroActivity")

        try {
            setContentView(R.layout.activity_rmintro)
            Log.d(TAG, "Layout set successfully")

            // Get the song title from intent
            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            Log.d(TAG, "Received song title: $currentSongTitle")

            // Set click listener for the entire layout
            findViewById<View>(R.id.main).setOnClickListener {
                Log.d(TAG, "Screen tapped, navigating to RhythmSummaryActivity")
                navigateToRhythmSummary()
            }

            Log.d(TAG, "RMIntroActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error loading intro: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Starting animations")
        // Start animations when activity resumes
        startAnimations()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Stopping animations")
        // Stop animations when activity pauses
        stopAnimations()
    }

    private fun startAnimations() {
        try {
            val feedbackImage = findViewById<ImageView>(R.id.feedbackImage)
            val tapHint = findViewById<TextView>(R.id.tapHint)

            Log.d(TAG, "Starting bounce animation")
            // Bounce animation for panda
            ObjectAnimator.ofFloat(feedbackImage, "translationY", 0f, -30f, 0f).apply {
                duration = 1500
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = BounceInterpolator()
                start()
            }

            Log.d(TAG, "Starting pulse animation")
            // Pulse animation for tap hint
            ObjectAnimator.ofFloat(tapHint, "alpha", 0.5f, 1f, 0.5f).apply {
                duration = 2000
                repeatCount = ObjectAnimator.INFINITE
                start()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in startAnimations: ${e.message}", e)
        }
    }

    private fun stopAnimations() {
        try {
            val feedbackImage = findViewById<ImageView>(R.id.feedbackImage)
            val tapHint = findViewById<TextView>(R.id.tapHint)
            feedbackImage.clearAnimation()
            tapHint.clearAnimation()
            Log.d(TAG, "Animations stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopAnimations: ${e.message}", e)
        }
    }

    private fun navigateToRhythmSummary() {
        Log.d(TAG, "navigateToRhythmSummary called")

        try {
            // Stop animations first
            stopAnimations()

            Log.d(TAG, "Creating intent for RhythmSummaryActivity")
            val intent = Intent(this, RhythmSummaryActivity::class.java)
            intent.putExtra("SONG_TITLE", currentSongTitle)

            Log.d(TAG, "Starting RhythmSummaryActivity")
            startActivity(intent)
            Log.d(TAG, "RhythmSummaryActivity started successfully")

            finish()
            Log.d(TAG, "RMIntroActivity finished")

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to RhythmSummaryActivity: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()

            // Try to go back
            try {
                onBackPressed()
            } catch (e2: Exception) {
                Log.e(TAG, "Error going back: ${e2.message}", e2)
                finish()
            }
        }
    }
}