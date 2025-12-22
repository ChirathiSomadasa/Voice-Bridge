package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.BounceInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 0
    private var totalQuestions = 0
    private var correctAnswers = 0

    // Star views
    private lateinit var starLeft: ImageView
    private lateinit var starMiddle: ImageView
    private lateinit var starRight: ImageView

    private lateinit var completedText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var timeText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var pandaMascot: ImageView
    private lateinit var btnTryAnother: Button
    private lateinit var btnHome: Button

    companion object {
        private const val TAG = "ScoreboardDebug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        Log.d(TAG, "=== onCreate STARTED ===")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()}")

        // Get data passed from game with debugging
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", -1)
        totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", -1)
        val attemptsFromGame = intent.getIntExtra("ATTEMPTS", -1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", -1L)

        // Debug the received values
        Log.d(TAG, "Received values:")
        Log.d(TAG, "CORRECT_ANSWERS: $correctAnswers")
        Log.d(TAG, "TOTAL_QUESTIONS: $totalQuestions")
        Log.d(TAG, "ATTEMPTS: $attemptsFromGame")
        Log.d(TAG, "ELAPSED_TIME: $completionTime")

        // Use default values if not provided
        attempts = if (attemptsFromGame == -1) 1 else attemptsFromGame

        // Calculate accuracy - check for valid values
        accuracy = if (totalQuestions > 0 && correctAnswers >= 0) {
            // Use floating point division for accuracy
            ((correctAnswers.toDouble() / totalQuestions.toDouble()) * 100).toInt()
        } else {
            // If values are invalid, show some test values for debugging
            Log.d(TAG, "Using test values for debugging")
            correctAnswers = 3
            totalQuestions = 5
            ((3.0 / 5.0) * 100).toInt() // Should be 60%
        }

        Log.d(TAG, "Final values:")
        Log.d(TAG, "Correct Answers: $correctAnswers")
        Log.d(TAG, "Total Questions: $totalQuestions")
        Log.d(TAG, "Calculated Accuracy: $accuracy%")

        // Initialize all views
        initializeViews()

        // Set stats to views
        setStatsToViews()

        // Update star rating based on accuracy
        updateStarRating(accuracy)

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
        pandaMascot = findViewById(R.id.pandaMascot)
        btnTryAnother = findViewById(R.id.btnTryAnother)
        btnHome = findViewById(R.id.btnHome)

        // Initialize star views
        starLeft = findViewById(R.id.starLeft)
        starMiddle = findViewById(R.id.starMiddle)
        starRight = findViewById(R.id.starRight)

        // Check if star views were found
        if (::starLeft.isInitialized) {
            Log.d(TAG, "Star views initialized successfully")
        } else {
            Log.d(TAG, "ERROR: Star views not found!")
        }
    }

    private fun setStatsToViews() {
        Log.d(TAG, "=== setStatsToViews STARTED ===")

        // Set attempts (minimum 1)
        val displayAttempts = if (attempts <= 0) 1 else attempts
        attemptsText.text = displayAttempts.toString()
        Log.d(TAG, "Setting attempts to: $displayAttempts")

        // Format completion time as seconds (if valid)
        if (completionTime > 0) {
            val seconds = completionTime / 1000
            timeText.text = "${seconds}s"
            Log.d(TAG, "Setting time to: ${seconds}s")
        } else {
            timeText.text = "0s"
            Log.d(TAG, "Setting time to: 0s (invalid time received)")
        }

        // Set initial accuracy text - will be animated
        accuracyText.text = "0%"
        Log.d(TAG, "Initial accuracy text set to: 0%")

        // Animate accuracy counting
        animateAccuracyCount(accuracy)

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun updateStarRating(accuracy: Int) {
        Log.d(TAG, "Updating star rating for accuracy: $accuracy%")

        // Determine number of stars based on accuracy
        val starsToLight = when {
            accuracy <= 33 -> 1 // 0-33%: 1 star
            accuracy <= 66 -> 2 // 34-66%: 2 stars
            else -> 3 // 67-100%: 3 stars
        }

        Log.d(TAG, "Stars to light: $starsToLight")

        // Light up stars
        val stars = listOf(starLeft, starMiddle, starRight)
        for (i in 0 until 3) {
            val star = stars[i]
            if (i < starsToLight) {
                // Try different drawable names if star_filled_yellow doesn't exist
                try {
                    star.setImageResource(R.drawable.star_filled_yellow)
                } catch (e: Exception) {
                    Log.d(TAG, "star_filled_yellow not found, trying star_filled")
                    try {
                        star.setImageResource(R.drawable.star_filled)
                    } catch (e2: Exception) {
                        Log.d(TAG, "star_filled not found either")
                        // You might need to add the yellow star drawable to your resources
                    }
                }
                animateStarWithBounce(star, i * 200L) // Stagger the animations
                Log.d(TAG, "Lit up star at position: $i")
            } else {
                star.setImageResource(R.drawable.star_outline)
                Log.d(TAG, "Left outline star at position: $i")
            }
        }
    }

    private fun animateAccuracyCount(finalAccuracy: Int) {
        Log.d(TAG, "Animating accuracy from 0 to: $finalAccuracy%")

        // Ensure accuracy doesn't exceed 100%
        val safeFinalAccuracy = if (finalAccuracy > 100) 100 else finalAccuracy

        // Animate the accuracy counting from 0 to finalAccuracy
        val animator = ValueAnimator.ofInt(0, safeFinalAccuracy)
        animator.duration = 1500

        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            accuracyText.text = "$currentValue%"
        }

        animator.start()
    }

    private fun animateStarWithBounce(star: ImageView, delay: Long = 0) {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "Animating star with bounce, delay: ${delay}ms")

            // Bounce animation
            val bounceAnim = ScaleAnimation(
                0.5f, 1.2f, 0.5f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 800
                interpolator = BounceInterpolator()
                repeatCount = 0
            }

            // Glow effect
            val glowAnim = ObjectAnimator.ofFloat(star, "alpha", 0.7f, 1f, 0.7f)
            glowAnim.duration = 1000
            glowAnim.repeatCount = ValueAnimator.INFINITE
            glowAnim.repeatMode = ValueAnimator.REVERSE

            star.startAnimation(bounceAnim)
            glowAnim.start()
        }, delay)
    }

    private fun animatePandaMascot() {
        // Add a gentle floating animation to the panda
        val floatAnimation = ScaleAnimation(
            1f, 1.05f, 1f, 1.05f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            repeatCount = ScaleAnimation.INFINITE
            repeatMode = ScaleAnimation.REVERSE
            startOffset = 300
        }

        pandaMascot.startAnimation(floatAnimation)
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners...")

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

    override fun onResume() {
        super.onResume()
        // Restart animations
        animatePandaMascot()
    }

    override fun onPause() {
        super.onPause()
        // Clear animations
        pandaMascot.clearAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Clear all animations
        pandaMascot.clearAnimation()
        starLeft.clearAnimation()
        starMiddle.clearAnimation()
        starRight.clearAnimation()
    }
}