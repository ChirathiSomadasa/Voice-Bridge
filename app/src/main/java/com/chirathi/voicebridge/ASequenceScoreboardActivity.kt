package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.Animation
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
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

    // Star rating views
    private lateinit var starLeft: ImageView
    private lateinit var starMiddle: ImageView
    private lateinit var starRight: ImageView

    // Performance text
    private lateinit var performanceText: TextView

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

        // Update star rating based on accuracy
        updateStarRating(accuracy)

        // Start animations
        startAnimations()

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

        // Initialize star rating views
        starLeft = findViewById(R.id.starLeft)
        starMiddle = findViewById(R.id.starMiddle)
        starRight = findViewById(R.id.starRight)

        // Initialize performance text (you'll need to add this to your XML)
        performanceText = findViewById(R.id.performanceText)
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

        // Animate accuracy counting
        animateAccuracyCount(accuracy)

        // Set performance text based on accuracy
        updatePerformanceText(accuracy)

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun updatePerformanceText(accuracy: Int) {
        when {
            accuracy >= 90 -> {
                performanceText.text = "Outstanding! Perfect performance! 🏆"
                performanceText.setTextColor(resources.getColor(R.color.green_dark, theme))
            }
            accuracy >= 70 -> {
                performanceText.text = "Excellent! Great job! 👍"
                performanceText.setTextColor(resources.getColor(R.color.green, theme))
            }
            accuracy >= 50 -> {
                performanceText.text = "Good! Keep practicing! 📚"
                performanceText.setTextColor(resources.getColor(R.color.dark_orange, theme))
            }
            accuracy >= 30 -> {
                performanceText.text = "Nice try! You're getting better! 🌟"
                performanceText.setTextColor(resources.getColor(R.color.light_blue, theme))
            }
            else -> {
                performanceText.text = "Keep practicing! You'll improve! 💪"
                performanceText.setTextColor(resources.getColor(R.color.red_dark, theme))
            }
        }
    }

    private fun updateStarRating(accuracy: Int) {
        // Determine how many stars to light up based on accuracy
        val starsToLight = when {
            accuracy >= 70 -> 3  // Three stars for 70-100%
            accuracy >= 50 -> 2  // Two stars for 50-69%
            accuracy >= 30 -> 1  // One star for 30-49%
            else -> 0            // No stars for 0-29%
        }

        // Light up stars
        val stars = listOf(starLeft, starMiddle, starRight)
        for (i in 0 until 3) {
            val star = stars[i]
            if (i < starsToLight) {
                star.setImageResource(R.drawable.star_filled_yellow) // Use your star resource
                animateStarWithBounce(star, i * 200L) // Stagger the animations
            } else {
                star.setImageResource(R.drawable.star_outline) // Use your outline star resource
            }
        }
    }

    private fun animateAccuracyCount(finalAccuracy: Int) {
        // Animate the accuracy counting from 0 to finalAccuracy
        val animator = ValueAnimator.ofInt(0, finalAccuracy)
        animator.duration = 1500

        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            accuracyText.text = "$currentValue%"
        }

        animator.start()
    }

    private fun animateStarWithBounce(star: ImageView, delay: Long = 0) {
        Handler(Looper.getMainLooper()).postDelayed({
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

    private fun startAnimations() {
        // Animate completed text
        animateCompletedText()

        // Animate panda mascot
        animatePandaMascot()

        // Animate stats cards with delay
        animateStatsCards()
    }

    private fun animateCompletedText() {
        // Bounce animation for completed text
        val bounceAnim = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            interpolator = BounceInterpolator()
            repeatCount = 0
        }

        // Pulse animation after bounce
        val pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
            completedText,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f, 1f)
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 1000
        }

        completedText.startAnimation(bounceAnim)
        pulseAnim.start()
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

        // Add rotation animation
        val rotateAnim = ObjectAnimator.ofFloat(pandaMascot, "rotation", -5f, 5f)
        rotateAnim.duration = 2000
        rotateAnim.repeatCount = ValueAnimator.INFINITE
        rotateAnim.repeatMode = ValueAnimator.REVERSE
        rotateAnim.interpolator = LinearInterpolator()

        pandaMascot.startAnimation(floatAnimation)
        rotateAnim.start()
    }

    private fun animateStatsCards() {
        val statsContainer = findViewById<android.widget.LinearLayout>(R.id.statsContainer)

        // Animate each stat card with a slight delay
        for (i in 0 until statsContainer.childCount) {
            val card = statsContainer.getChildAt(i)
            card.alpha = 0f
            card.translationY = 50f

            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay((i * 200).toLong())
                .start()
        }
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners...")
        val btnTryAnother = findViewById<Button>(R.id.btnTryAnother)
        val btnHome = findViewById<Button>(R.id.btnHome)

        // Add button animations
        btnTryAnother.setOnClickListener {
            animateButtonClick(btnTryAnother) {
                Log.d(TAG, "Play Again button clicked")
                val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

        btnHome.setOnClickListener {
            animateButtonClick(btnHome) {
                Log.d(TAG, "Home button clicked")
                val intent = Intent(this, GameDashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }
    }

    private fun animateButtonClick(button: Button, action: () -> Unit) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .withEndAction {
                        action()
                    }
                    .start()
            }
            .start()
    }

    override fun onResume() {
        super.onResume()
        // Restart animations
        startAnimations()
    }

    override fun onPause() {
        super.onPause()
        // Clear animations
        clearAnimations()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Clear all animations
        clearAnimations()
    }

    private fun clearAnimations() {
        completedText.clearAnimation()
        pandaMascot.clearAnimation()
        starLeft.clearAnimation()
        starMiddle.clearAnimation()
        starRight.clearAnimation()
    }
}