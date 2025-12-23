package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100

    private lateinit var completedText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var timeText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var pandaMascot: ImageView

    companion object {
        private const val TAG = "ScoreboardDebug"

        // Scoring constants (adjust these as needed)
        private const val MAX_ATTEMPTS_PENALTY = 5  // Maximum attempts before zero accuracy
        private const val MAX_TIME_PENALTY = 60000L // 60 seconds maximum time
        private const val IDEAL_TIME = 10000L      // 10 seconds for perfect score
        private const val IDEAL_ATTEMPTS = 1       // 1 attempt for perfect score
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        Log.d(TAG, "=== onCreate STARTED ===")

        // Get data passed from game
        val attemptsFromGame = intent.getIntExtra("ATTEMPTS", 1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", 0L)

        attempts = attemptsFromGame

        // Calculate accuracy based on attempts and time
        accuracy = calculateAccuracy(attempts, completionTime)

        Log.d(TAG, "Attempts: $attempts")
        Log.d(TAG, "Time: ${completionTime}ms")
        Log.d(TAG, "Calculated Accuracy: $accuracy%")

        // Initialize all views
        initializeViews()

        // Set stats to views
        setStatsToViews()

        // Animate panda mascot
        animatePandaMascot()

        // Button click listeners
        setupButtonListeners()
    }

    private fun calculateAccuracy(attempts: Int, timeMs: Long): Int {
        // Calculate accuracy based on two factors:
        // 1. Attempts efficiency (50% weight)
        // 2. Time efficiency (50% weight)

        // 1. Attempts Score (0-50 points)
        val attemptsScore = calculateAttemptsScore(attempts)

        // 2. Time Score (0-50 points)
        val timeScore = calculateTimeScore(timeMs)

        // Total accuracy (0-100%)
        val totalScore = attemptsScore + timeScore

        // Ensure it's between 0-100
        return totalScore.coerceIn(0, 100)
    }

    private fun calculateAttemptsScore(attempts: Int): Int {
        // Fewer attempts = higher score
        // Perfect score (50 points) for 1 attempt
        // Linear decrease up to MAX_ATTEMPTS_PENALTY attempts (0 points)

        val attemptsPenalty = when {
            attempts <= IDEAL_ATTEMPTS -> 0
            attempts >= MAX_ATTEMPTS_PENALTY -> 50
            else -> {
                val penaltyPerAttempt = 50.0 / (MAX_ATTEMPTS_PENALTY - IDEAL_ATTEMPTS)
                ((attempts - IDEAL_ATTEMPTS) * penaltyPerAttempt).toInt()
            }
        }

        return 50 - attemptsPenalty
    }

    private fun calculateTimeScore(timeMs: Long): Int {
        // Faster time = higher score
        // Perfect score (50 points) for <= IDEAL_TIME
        // Linear decrease up to MAX_TIME_PENALTY (0 points)

        val timeSeconds = timeMs / 1000
        Log.d(TAG, "Time in seconds: $timeSeconds")

        return when {
            timeMs <= IDEAL_TIME -> 50  // Perfect score for <= 10 seconds
            timeMs >= MAX_TIME_PENALTY -> 0  // Zero score for >= 60 seconds
            else -> {
                val timeOverIdeal = (timeMs - IDEAL_TIME).toDouble()
                val totalPenaltyRange = (MAX_TIME_PENALTY - IDEAL_TIME).toDouble()
                val penalty = (timeOverIdeal / totalPenaltyRange) * 50
                (50 - penalty).toInt()
            }
        }
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views...")

        completedText = findViewById(R.id.completedText)
        attemptsText = findViewById(R.id.attemptsText)
        timeText = findViewById(R.id.timeText)
        accuracyText = findViewById(R.id.accuracyText)
        pandaMascot = findViewById(R.id.pandaMascot)
    }

    private fun setStatsToViews() {
        Log.d(TAG, "=== setStatsToViews STARTED ===")

        // Display attempts
        attemptsText.text = attempts.toString()

        // Format completion time as seconds with one decimal
        val seconds = completionTime / 1000.0
        timeText.text = "%.1fs".format(seconds)

        // Set accuracy with color coding
        accuracyText.text = "$accuracy%"

        // Optional: Change text color based on accuracy
        val accuracyColor = when {
            accuracy >= 80 -> android.R.color.holo_green_dark
            accuracy >= 60 -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        accuracyText.setTextColor(resources.getColor(accuracyColor, theme))

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun animatePandaMascot() {
        // Add a gentle floating animation to the panda
        val floatAnimation = ScaleAnimation(
            1f, 1.05f, 1f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            startOffset = 300
        }

        pandaMascot.startAnimation(floatAnimation)
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners...")
        val btnTryAnother = findViewById<Button>(R.id.btnTryAnother)
        val btnHome = findViewById<Button>(R.id.btnHome)

        btnTryAnother.setOnClickListener {
            Log.d(TAG, "Play Again button clicked")
            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnHome.setOnClickListener {
            Log.d(TAG, "Home button clicked")
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Clear animations to prevent memory leaks
        pandaMascot.clearAnimation()
    }
}