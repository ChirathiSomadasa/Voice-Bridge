package com.chirathi.voicebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RMIntroActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var progressBar: ProgressBar
    private lateinit var currentSongTitle: String

    private val updateHandler = Handler(Looper.getMainLooper())
    private val TAG = "RMIntroActivity"
    private var isActivityDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting RMIntroActivity")
        isActivityDestroyed = false

        try {
            // Make activity full screen
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )

            setContentView(R.layout.activity_rmintro)
            Log.d(TAG, "Layout set successfully")

            // Get the song title from intent
            currentSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
            Log.d(TAG, "Received song title: $currentSongTitle")

            // Initialize views
            videoView = findViewById(R.id.videoView)
            loadingIndicator = findViewById(R.id.loadingIndicator)
            progressBar = findViewById(R.id.progressBar)

            // Setup video player
            setupVideoPlayer()

            // Set click listener for the entire layout
            findViewById<View>(R.id.main).setOnClickListener {
                Log.d(TAG, "Screen tapped, skipping video")
                skipToNextActivity()
            }

            Log.d(TAG, "RMIntroActivity initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            // Move to next activity on error
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToRhythmSummary()
            }, 1000)
        }
    }

    private fun setupVideoPlayer() {
        Log.d(TAG, "Setting up video player")

        try {
            // Hide media controller
            videoView.setMediaController(null)

            // Set video URI from raw resources
            // IMPORTANT: Make sure you have a video file in raw folder
            // For testing, you can use a simple local video or use a placeholder
            val videoUri = Uri.parse("android.resource://" + packageName + "/raw/bears_fun_singing_game")
            Log.d(TAG, "Attempting to load video from: $videoUri")

            // Set listeners BEFORE setting video URI
            videoView.setOnPreparedListener {
                if (isActivityDestroyed) return@setOnPreparedListener
                Log.d(TAG, "Video prepared successfully")
                loadingIndicator.visibility = View.GONE
                progressBar.max = 100
                progressBar.progress = 0

                // Auto-start the video
                videoView.start()
                Log.d(TAG, "Video started automatically")

                // Start updating progress
                startProgressUpdate()
            }

            videoView.setOnCompletionListener {
                if (isActivityDestroyed) return@setOnCompletionListener
                Log.d(TAG, "Video completed")
                // When video completes, move to next activity
                navigateToRhythmSummary()
            }

            videoView.setOnErrorListener { _, what, extra ->
                if (isActivityDestroyed) return@setOnErrorListener true

                Log.e(TAG, "Video error: what=$what, extra=$extra")
                loadingIndicator.visibility = View.GONE

                // Don't show any dialog - just log and move on
                // Auto-move to next activity on error after delay
                updateHandler.postDelayed({
                    if (!isActivityDestroyed) {
                        navigateToRhythmSummary()
                    }
                }, 1000)
                true // Return true to indicate we handled the error (no default dialog)
            }

            // Now set the video URI
            videoView.setVideoURI(videoUri)

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video player: ${e.message}", e)
            loadingIndicator.visibility = View.GONE
            // Auto-move to next activity on error after delay
            updateHandler.postDelayed({
                if (!isActivityDestroyed) {
                    navigateToRhythmSummary()
                }
            }, 1000)
        }
    }

    private fun startProgressUpdate() {
        updateHandler.post(object : Runnable {
            override fun run() {
                try {
                    if (!isActivityDestroyed && videoView.isPlaying) {
                        val currentPosition = videoView.currentPosition
                        val duration = videoView.duration

                        if (duration > 0 && ::progressBar.isInitialized) {
                            val progress = (currentPosition * 100 / duration).toInt()
                            progressBar.progress = progress
                        }
                    }
                    // Update every 200ms for smooth progress
                    if (!isActivityDestroyed) {
                        updateHandler.postDelayed(this, 200)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating progress: ${e.message}", e)
                }
            }
        })
    }

    private fun skipToNextActivity() {
        if (isActivityDestroyed) return
        Log.d(TAG, "Skipping to next activity")
        // Stop the video
        videoView.stopPlayback()
        // Remove any pending updates
        updateHandler.removeCallbacksAndMessages(null)
        // Move to next activity
        navigateToRhythmSummary()
    }

    private fun navigateToRhythmSummary() {
        if (isActivityDestroyed) return
        Log.d(TAG, "navigateToRhythmSummary called")

        try {
            // Stop any running updates
            updateHandler.removeCallbacksAndMessages(null)

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
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Stopping video")
        // Skip to next activity if app goes to background
        if (!isActivityDestroyed) {
            skipToNextActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Ensuring full screen")
        // Ensure full screen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Cleaning up")
        isActivityDestroyed = true
        updateHandler.removeCallbacksAndMessages(null)
        try {
            videoView.stopPlayback()
            // Clear all listeners to prevent callbacks
            videoView.setOnPreparedListener(null)
            videoView.setOnCompletionListener(null)
            videoView.setOnErrorListener(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video playback: ${e.message}", e)
        }
    }
}