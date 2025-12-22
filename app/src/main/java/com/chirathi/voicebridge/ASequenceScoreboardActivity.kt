package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100

    private lateinit var progressBar: ProgressBar
    private lateinit var starOverlay: ImageView
    private lateinit var completedText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var timeText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var pandaMascot: ImageView

    companion object {
        private const val TAG = "ScoreboardDebug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        Log.d(TAG, "=== onCreate STARTED ===")

        // Get data passed from game
        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 1)
        val attemptsFromGame = intent.getIntExtra("ATTEMPTS", 1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", 0L)

        attempts = attemptsFromGame
        accuracy = if (totalQuestions > 0) {
            (correctAnswers * 100) / totalQuestions
        } else {
            100
        }

        Log.d(TAG, "Accuracy: $accuracy%")

        // Initialize all views
        initializeViews()

        // Set stats to views
        setStatsToViews()

        // Start progress animation with delay
        Handler(Looper.getMainLooper()).postDelayed({
            animateProgressBar(accuracy)
        }, 500)

        // Animate panda mascot
        animatePandaMascot()

        // Button click listeners
        setupButtonListeners()
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views...")

        completedText = findViewById(R.id.completedText)
        attemptsText = findViewById(R.id.attemptsText)
        timeText = findViewById(R.id.timeText)
        accuracyText = findViewById(R.id.accuracyText)
        progressBar = findViewById(R.id.progressBar)
        starOverlay = findViewById(R.id.starOverlay)
        pandaMascot = findViewById(R.id.pandaMascot)
    }

    private fun setStatsToViews() {
        Log.d(TAG, "=== setStatsToViews STARTED ===")

        val displayAttempts = if (attempts <= 0) 1 else attempts
        attemptsText.text = displayAttempts.toString()

        // Format completion time as seconds
        val seconds = completionTime / 1000
        timeText.text = "${seconds}s"

        // Set accuracy
        accuracyText.text = "$accuracy%"

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun animateProgressBar(progress: Int) {
        Log.d(TAG, "Animating progress bar to: $progress%")

        // Animate the progress bar
        val animator = android.animation.ValueAnimator.ofInt(0, progress)
        animator.duration = 1500
        animator.addUpdateListener { animation ->
            val currentProgress = animation.animatedValue as Int
            progressBar.progress = currentProgress
        }
        animator.start()

        // Animate the star overlay
        animateStarOverlay()
    }

    private fun animateStarOverlay() {
        // Star bounce animation
        val bounceAnimation = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = 0
        }

        starOverlay.startAnimation(bounceAnimation)

        // After bounce, start continuous gentle bounce
        Handler(Looper.getMainLooper()).postDelayed({
            startGentleStarBounce()
        }, 800)
    }

    private fun startGentleStarBounce() {
        // Continuous gentle bounce for star
        val gentleBounce = ScaleAnimation(
            1f, 1.1f, 1f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }

        starOverlay.startAnimation(gentleBounce)
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
        starOverlay.clearAnimation()
        pandaMascot.clearAnimation()
    }
}