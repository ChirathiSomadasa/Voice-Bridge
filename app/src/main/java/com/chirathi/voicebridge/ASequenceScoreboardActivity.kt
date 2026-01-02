package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100
    private var stickerAlreadyShown = false

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

    // Animation objects
    private lateinit var pandaHeartbeatAnimator: ObjectAnimator
    private lateinit var completedTextPulseAnimator: ObjectAnimator
    private lateinit var completedTextColorAnimator: ObjectAnimator

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

        // Check if sticker was already shown (passed from UnlockStickerActivity)
        stickerAlreadyShown = intent.getBooleanExtra("STICKER_ALREADY_SHOWN", false)

        // Clear the flag so it doesn't persist
        intent.removeExtra("STICKER_ALREADY_SHOWN")

        // Get data passed from game OR from UnlockStickerActivity
        val attemptsFromGame = intent.getIntExtra("ATTEMPTS", 1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", 0L)

        attempts = attemptsFromGame

        // Calculate accuracy based on attempts and time
        // If accuracy is passed, use it; otherwise calculate it
        if (intent.hasExtra("ACCURACY")) {
            accuracy = intent.getIntExtra("ACCURACY", 100)
        } else {
            accuracy = calculateAccuracy(attempts, completionTime)
        }

        Log.d(TAG, "Attempts: $attempts")
        Log.d(TAG, "Time: ${completionTime}ms")
        Log.d(TAG, "Accuracy: $accuracy%")
        Log.d(TAG, "Sticker already shown: $stickerAlreadyShown")

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

        // Initialize performance text
        performanceText = findViewById(R.id.performanceText)
    }

    private fun setStatsToViews() {
        Log.d(TAG, "=== setStatsToViews STARTED ===")

        // Display attempts - NO ANIMATION, keep steady
        attemptsText.text = attempts.toString()

        // Format completion time as seconds with one decimal - NO ANIMATION, keep steady
        val seconds = completionTime / 1000.0
        timeText.text = "%.1fs".format(seconds)

        // Set accuracy with counting animation only
        animateAccuracyCount(accuracy)

        // Set performance text based on accuracy - NO ANIMATION, keep steady
        updatePerformanceText(accuracy)

        // Check for perfect accuracy to trigger sticker unlock ONLY IF NOT ALREADY SHOWN
        if (accuracy == 100 && !stickerAlreadyShown) {
            Handler(Looper.getMainLooper()).postDelayed({
                triggerPerfectAccuracyCelebration()
            }, 2500) // Wait for accuracy animation to complete
        }

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun triggerPerfectAccuracyCelebration() {
        Log.d(TAG, "Perfect accuracy detected! Triggering celebration...")

        // Start sticker unlock activity WITH GAME DATA
        val intent = Intent(this, UnlockStickerActivity::class.java)
        intent.putExtra("ATTEMPTS", attempts)
        intent.putExtra("ELAPSED_TIME", completionTime)
        intent.putExtra("ACCURACY", accuracy)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // DON'T finish this activity - it will stay in back stack
        // User will return to it when sticker unlock finishes
    }

    private fun updatePerformanceText(accuracy: Int) {
        val (text, colorRes) = when {
            accuracy >= 90 -> {
                Pair("Outstanding! Perfect performance! 🏆", R.color.green_dark)
            }
            accuracy >= 70 -> {
                Pair("Excellent! Great job! 👍", R.color.green)
            }
            accuracy >= 50 -> {
                Pair("Good! Keep practicing! 📚", R.color.dark_orange)
            }
            accuracy >= 30 -> {
                Pair("Nice try! You're getting better! 🌟", R.color.light_blue)
            }
            else -> {
                Pair("Keep practicing! You'll improve! 💪", R.color.red_dark)
            }
        }

        performanceText.text = text
        performanceText.setTextColor(resources.getColor(colorRes, theme))
    }

    private fun updateStarRating(accuracy: Int) {
        // Determine how many stars to light up based on accuracy
        val starsToLight = when {
            accuracy >= 70 -> 3  // Three stars for 70-100%
            accuracy >= 50 -> 2  // Two stars for 50-69%
            accuracy >= 30 -> 1  // One star for 30-49%
            else -> 0            // No stars for 0-29%
        }

        // Light up stars - Simple bounce animation only, then steady
        val stars = listOf(starLeft, starMiddle, starRight)
        for (i in 0 until 3) {
            val star = stars[i]
            if (i < starsToLight) {
                star.setImageResource(R.drawable.star_filled_yellow)
                // Simple bounce entrance animation
                val bounceAnim = ScaleAnimation(
                    0f, 1.1f, 0f, 1.1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 600
                    interpolator = BounceInterpolator()
                    repeatCount = 0
                    startOffset = (i * 300).toLong()
                }
                star.startAnimation(bounceAnim)
            } else {
                star.setImageResource(R.drawable.star_outline)
            }
        }
    }

    private fun animateAccuracyCount(finalAccuracy: Int) {
        val animator = ValueAnimator.ofInt(0, finalAccuracy)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            accuracyText.text = "$currentValue%"

            // Change color based on value
            val colorRes = when {
                currentValue >= 80 -> R.color.green_dark
                currentValue >= 60 -> R.color.green
                currentValue >= 40 -> R.color.dark_orange
                currentValue >= 20 -> R.color.light_blue
                else -> R.color.red_dark
            }
            accuracyText.setTextColor(resources.getColor(colorRes, theme))
        }

        animator.start()
    }

    private fun startAnimations() {
        // Animate the "COMPLETED!!!" text with continuous effects
        animateCompletedText()

        // Panda mascot heartbeat animation (continuous)
        animatePandaHeartbeat()
    }

    private fun animateCompletedText() {
        // 1. Initial bounce animation
        val bounceAnim = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            interpolator = BounceInterpolator()
            repeatCount = 0
        }

        // 2. Continuous pulse animation
        completedTextPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
            completedText,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.08f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.08f, 1f)
        ).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            startDelay = 800
            interpolator = AccelerateDecelerateInterpolator()
        }

        // 3. Color cycling animation (green -> gold -> green)
        completedTextColorAnimator = ObjectAnimator.ofInt(
            completedText,
            "textColor",
            resources.getColor(R.color.green_dark, theme),
            resources.getColor(R.color.gold, theme),
            resources.getColor(R.color.green_dark, theme)
        )
        completedTextColorAnimator.duration = 3000
        completedTextColorAnimator.repeatCount = ValueAnimator.INFINITE
        completedTextColorAnimator.repeatMode = ValueAnimator.RESTART
        completedTextColorAnimator.setEvaluator(android.animation.ArgbEvaluator())
        completedTextColorAnimator.startDelay = 800

        // Start animations
        completedText.startAnimation(bounceAnim)
        Handler(Looper.getMainLooper()).postDelayed({
            completedTextPulseAnimator.start()
            completedTextColorAnimator.start()
        }, 800)
    }

    private fun animatePandaHeartbeat() {
        // Create a heartbeat-like animation: quick zoom in, hold, slow zoom out
        pandaHeartbeatAnimator = ObjectAnimator.ofPropertyValuesHolder(
            pandaMascot,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1.1f, 1f)
        ).apply {
            duration = 1200  // Heartbeat duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Add gentle floating animation
        val floatAnim = ObjectAnimator.ofFloat(
            pandaMascot,
            "translationY",
            0f,
            -15f,
            0f,
            5f,
            0f
        ).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
        }

        // Add gentle sway (rotation)
        val swayAnim = ObjectAnimator.ofFloat(
            pandaMascot,
            "rotation",
            -2f, 2f, -2f
        ).apply {
            duration = 6000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
        }

        // Start all animations with delay
        Handler(Looper.getMainLooper()).postDelayed({
            pandaHeartbeatAnimator.start()
            floatAnim.start()
            swayAnim.start()
        }, 500)
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners...")
        val btnTryAnother = findViewById<Button>(R.id.btnTryAnother)
        val btnHome = findViewById<Button>(R.id.btnHome)

        // Add simple click animations (one-time only)
        btnTryAnother.setOnClickListener {
            // Simple scale animation on click
            btnTryAnother.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnTryAnother.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            Log.d(TAG, "Play Again button clicked")
                            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(intent)
                            finish()
                        }
                        .start()
                }
                .start()
        }

        btnHome.setOnClickListener {
            // Simple scale animation on click
            btnHome.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnHome.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            Log.d(TAG, "Home button clicked")
                            val intent = Intent(this, GameDashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(intent)
                            finish()
                        }
                        .start()
                }
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart animations for panda and completed text
        if (::pandaHeartbeatAnimator.isInitialized) {
            pandaHeartbeatAnimator.start()
        }
        if (::completedTextPulseAnimator.isInitialized) {
            completedTextPulseAnimator.start()
        }
        if (::completedTextColorAnimator.isInitialized) {
            completedTextColorAnimator.start()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause animations
        if (::pandaHeartbeatAnimator.isInitialized) {
            pandaHeartbeatAnimator.pause()
        }
        if (::completedTextPulseAnimator.isInitialized) {
            completedTextPulseAnimator.pause()
        }
        if (::completedTextColorAnimator.isInitialized) {
            completedTextColorAnimator.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        // Clear all animations
        clearAnimations()
    }

    private fun clearAnimations() {
        // Clear all view animations
        completedText.clearAnimation()
        pandaMascot.clearAnimation()
        starLeft.clearAnimation()
        starMiddle.clearAnimation()
        starRight.clearAnimation()

        // Cancel animators
        if (::pandaHeartbeatAnimator.isInitialized) {
            pandaHeartbeatAnimator.cancel()
        }
        if (::completedTextPulseAnimator.isInitialized) {
            completedTextPulseAnimator.cancel()
        }
        if (::completedTextColorAnimator.isInitialized) {
            completedTextColorAnimator.cancel()
        }

        // Clear button animations
        findViewById<Button>(R.id.btnTryAnother).clearAnimation()
        findViewById<Button>(R.id.btnHome).clearAnimation()
    }
}