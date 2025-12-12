package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var line1: View
    private lateinit var line2: View
    private lateinit var pandaImage: ImageView
    private lateinit var celebrationText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var attemptsText: TextView
    private lateinit var timeText: TextView
    private lateinit var accuracyText: TextView

    private val sparkles = mutableListOf<ImageView>()

    // Handler for glow animations
    private val glowHandler = Handler(Looper.getMainLooper())
    private val glowRunnables = mutableListOf<Runnable>()

    companion object {
        private const val TAG = "ScoreboardDebug"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        Log.d(TAG, "=== onCreate STARTED ===")
        Log.d(TAG, "Intent received: ${intent}")
        Log.d(TAG, "Intent extras: ${intent.extras}")

        // Get data passed from game
        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 1)
        val attemptsFromGame = intent.getIntExtra("ATTEMPTS", 1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", 0L)

        // Use the attempts sent from the game
        attempts = attemptsFromGame

        // Calculate accuracy
        accuracy = if (totalQuestions > 0) {
            (correctAnswers * 100) / totalQuestions
        } else {
            100
        }

        Log.d(TAG, "=== RAW INTENT DATA ===")
        Log.d(TAG, "CORRECT_ANSWERS: $correctAnswers")
        Log.d(TAG, "TOTAL_QUESTIONS: $totalQuestions")
        Log.d(TAG, "ATTEMPTS: $attemptsFromGame")
        Log.d(TAG, "ELAPSED_TIME: $completionTime ms")
        Log.d(TAG, "=== CALCULATED VALUES ===")
        Log.d(TAG, "Attempts: $attempts")
        Log.d(TAG, "Completion time: $completionTime ms")
        Log.d(TAG, "Accuracy: $accuracy%")
        Log.d(TAG, "Formatted time: ${formatTime(completionTime)}")

        // Check if intent has any extras at all
        if (intent.extras == null) {
            Log.w(TAG, "⚠️ WARNING: Intent has NO extras!")
        } else {
            Log.d(TAG, "Intent has ${intent.extras!!.size()} extras")
            for (key in intent.extras!!.keySet()) {
                Log.d(TAG, "Extra key: '$key', value: ${intent.extras!!.get(key)}")
            }
        }

        // If completionTime is 0 or not provided, use a default
        if (completionTime == 0L) {
            Log.w(TAG, "⚠️ completionTime is 0, using default (10 seconds)")
            completionTime = 10000 // Default 10 seconds
        }

        // Initialize all views
        initializeViews()

        // Set stats to views
        setStatsToViews()

        // Set celebration message based on attempts and accuracy
        updateCelebrationMessage()

        // Start animations with delay
        Handler(Looper.getMainLooper()).postDelayed({
            startPandaAnimation()
            startStarAnimationSequence()
        }, 500)

        // Start sparkle animations
        startSparkleAnimations()

        // Button click listeners
        setupButtonListeners()

        Log.d(TAG, "=== onCreate COMPLETED ===")
    }

    private fun initializeViews() {
        Log.d(TAG, "Initializing views...")

        celebrationText = findViewById(R.id.celebrationText)
        subtitleText = findViewById(R.id.subtitleText)
        attemptsText = findViewById(R.id.attemptsText)
        timeText = findViewById(R.id.timeText)
        accuracyText = findViewById(R.id.accuracyText)

        // Log initial text values
        Log.d(TAG, "attemptsText initial text: ${attemptsText.text}")
        Log.d(TAG, "timeText initial text: ${timeText.text}")
        Log.d(TAG, "accuracyText initial text: ${accuracyText.text}")

        // Animation views
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        line1 = findViewById(R.id.line1)
        line2 = findViewById(R.id.line2)
        pandaImage = findViewById(R.id.pandaImage)

        // Sparkles
        sparkles.add(findViewById(R.id.sparkle1))
        sparkles.add(findViewById(R.id.sparkle2))
        sparkles.add(findViewById(R.id.sparkle3))
        sparkles.add(findViewById(R.id.sparkle4))
    }

    private fun setStatsToViews() {
        Log.d(TAG, "=== setStatsToViews STARTED ===")
        Log.d(TAG, "Raw attempts value: $attempts")

        // Make sure attempts is at least 1
        val displayAttempts = if (attempts <= 0) {
            Log.d(TAG, "Attempts was <= 0, setting to 1 for display")
            1
        } else {
            attempts
        }

        Log.d(TAG, "Display attempts: $displayAttempts")
        attemptsText.text = displayAttempts.toString()
        Log.d(TAG, "attemptsText after setting: ${attemptsText.text}")

        timeText.text = formatTime(completionTime)
        Log.d(TAG, "timeText after setting: ${timeText.text}")

        accuracyText.text = "$accuracy%"
        Log.d(TAG, "accuracyText after setting: ${accuracyText.text}")

        Log.d(TAG, "=== setStatsToViews COMPLETED ===")
    }

    private fun formatTime(millis: Long): String {
        Log.d(TAG, "formatTime called with: $millis ms")

        // Handle invalid or zero time
        if (millis <= 0) {
            Log.d(TAG, "formatTime: millis <= 0, returning '0s'")
            return "0s"
        }

        val seconds = millis / 1000
        val milliseconds = millis % 1000

        val result = if (seconds == 0L) {
            "${milliseconds}ms"
        } else if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }

        Log.d(TAG, "formatTime result: $result")
        return result
    }

    private fun updateCelebrationMessage() {
        Log.d(TAG, "=== updateCelebrationMessage ===")
        // Determine star rating based on attempts and time
        val starRating = calculateStarRating()

        Log.d(TAG, "Star rating calculated: $starRating")

        when (starRating) {
            3 -> {
                celebrationText.text = "🎉 Perfect! ⭐⭐⭐ 🎉"
                subtitleText.text = "Flawless victory! You're amazing!"
            }
            2 -> {
                celebrationText.text = "🌟 Excellent! ⭐⭐ 🌟"
                subtitleText.text = "Great job! Almost perfect!"
            }
            1 -> {
                celebrationText.text = "✨ Good Job! ⭐ ✨"
                subtitleText.text = "You completed the sequence!"
            }
            else -> {
                celebrationText.text = "✅ Complete! ✅"
                subtitleText.text = "You finished the activity!"
            }
        }

        Log.d(TAG, "Celebration text set to: ${celebrationText.text}")
        Log.d(TAG, "Subtitle text set to: ${subtitleText.text}")
    }

    private fun calculateStarRating(): Int {
        Log.d(TAG, "=== calculateStarRating ===")
        Log.d(TAG, "Attempts: $attempts")
        Log.d(TAG, "Completion time seconds: ${completionTime / 1000}")

        val seconds = completionTime / 1000

        val rating = when {
            attempts <= 2 && seconds <= 30 -> {
                Log.d(TAG, "Rating: 3 stars (attempts <= 2, seconds <= 30)")
                3
            }
            attempts <= 4 && seconds <= 60 -> {
                Log.d(TAG, "Rating: 2 stars (attempts <= 4, seconds <= 60)")
                2
            }
            else -> {
                Log.d(TAG, "Rating: 1 star (default)")
                1
            }
        }

        Log.d(TAG, "Final star rating: $rating")
        return rating
    }

    private fun startPandaAnimation() {
        Log.d(TAG, "Starting panda animation...")
        // Panda bounce animation
        val bounceAnimation = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = 0
        }

        pandaImage.startAnimation(bounceAnimation)

        // After bounce, start continuous gentle bounce
        Handler(Looper.getMainLooper()).postDelayed({
            startGentleBounceAnimation()
        }, 800)

        // Add fade-in effect
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000
        pandaImage.startAnimation(fadeIn)
    }

    private fun startGentleBounceAnimation() {
        Log.d(TAG, "Starting gentle bounce animation...")
        // Continuous gentle bounce for panda
        val gentleBounce = ScaleAnimation(
            1f, 1.05f, 1f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }

        pandaImage.startAnimation(gentleBounce)
    }

    private fun startStarAnimationSequence() {
        Log.d(TAG, "Starting star animation sequence...")
        val starRating = calculateStarRating()

        // Animate stars based on rating
        when (starRating) {
            3 -> {
                Log.d(TAG, "Animating 3 stars")
                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star1)
                    fillLine(line1)
                }, 300)

                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star2)
                    fillLine(line2)
                }, 800)

                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star3)
                    playFinalCelebration()
                }, 1300)
            }
            2 -> {
                Log.d(TAG, "Animating 2 stars")
                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star1)
                    fillLine(line1)
                }, 300)

                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star2)
                    // Don't fill line2 or star3
                    Handler(Looper.getMainLooper()).postDelayed({
                        playFinalCelebration()
                    }, 500)
                }, 800)
            }
            1 -> {
                Log.d(TAG, "Animating 1 star")
                Handler(Looper.getMainLooper()).postDelayed({
                    fillStar(star1)
                    // Only one star
                    Handler(Looper.getMainLooper()).postDelayed({
                        playFinalCelebration()
                    }, 500)
                }, 300)
            }
        }
    }

    private fun fillStar(star: ImageView) {
        Log.d(TAG, "Filling star...")
        // Change star from outline to filled
        star.setImageResource(R.drawable.star_filled)

        // Add bounce animation
        val bounce = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 600
            repeatCount = 1
            repeatMode = Animation.REVERSE
        }

        star.startAnimation(bounce)

        // Start glowing animation
        startGlowingAnimation(star)
    }

    private fun startGlowingAnimation(star: ImageView) {
        Log.d(TAG, "Starting glow animation...")
        val glowRunnable = object : Runnable {
            private var isGlowing = false
            private var isAnimating = true

            override fun run() {
                if (!isAnimating) return

                // Toggle between filled and glow versions
                if (isGlowing) {
                    star.setImageResource(R.drawable.star_filled)
                } else {
                    star.setImageResource(R.drawable.star_filled_glow)
                }
                isGlowing = !isGlowing

                // Schedule next toggle
                if (isAnimating) {
                    glowHandler.postDelayed(this, 500) // Change every 500ms
                }
            }

            fun stop() {
                isAnimating = false
                star.setImageResource(R.drawable.star_filled) // Reset to normal
            }
        }

        glowRunnables.add(glowRunnable)
        glowHandler.post(glowRunnable)
    }

    private fun fillLine(line: View) {
        Log.d(TAG, "Filling line...")
        // Animate line from gray to gold
        line.animate()
            .setDuration(500)
            .withStartAction {
                line.setBackgroundColor(resources.getColor(R.color.gold))
            }
            .start()

        // Add shimmer effect
        val scaleX = ScaleAnimation(
            0f, 1f, 1f, 1f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
        }

        line.startAnimation(scaleX)
    }

    private fun playFinalCelebration() {
        Log.d(TAG, "Playing final celebration...")
        // Big celebration animation for star container
        val celebrationAnimation = ScaleAnimation(
            0.8f, 1.5f, 0.8f, 1.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = 1
            repeatMode = Animation.REVERSE
        }

        val starContainer = findViewById<View>(R.id.starContainer)
        starContainer.startAnimation(celebrationAnimation)

        // Make panda do a happy dance
        pandaHappyDance()

        // Trigger confetti-like effect
        triggerConfetti()

        // Stop glowing after 5 seconds and keep stars bright
        Handler(Looper.getMainLooper()).postDelayed({
            stopGlowingAnimations()
            // Keep all stars in glow state permanently
            star1.setImageResource(R.drawable.star_filled_glow)
            star2.setImageResource(R.drawable.star_filled_glow)
            star3.setImageResource(R.drawable.star_filled_glow)
        }, 5000)
    }

    private fun stopGlowingAnimations() {
        Log.d(TAG, "Stopping glow animations...")
        // Clear all runnables
        glowRunnables.forEach {
            glowHandler.removeCallbacks(it)
        }
        glowRunnables.clear()
    }

    private fun pandaHappyDance() {
        Log.d(TAG, "Starting panda happy dance...")
        // Panda rotation animation
        val rotateAnimation = android.view.animation.RotateAnimation(
            -15f, 15f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            repeatCount = 4
            repeatMode = Animation.REVERSE
        }

        pandaImage.startAnimation(rotateAnimation)

        // Panda jump animation
        Handler(Looper.getMainLooper()).postDelayed({
            val jumpAnimation = android.view.animation.TranslateAnimation(
                0f, 0f, 0f, -50f
            ).apply {
                duration = 300
                repeatCount = 2
                repeatMode = Animation.REVERSE
            }
            pandaImage.startAnimation(jumpAnimation)
        }, 1200)
    }

    private fun startSparkleAnimations() {
        Log.d(TAG, "Starting sparkle animations...")
        sparkles.forEachIndexed { index, sparkle ->
            Handler(Looper.getMainLooper()).postDelayed({
                animateSparkle(sparkle)
            }, index * 400L)
        }
    }

    private fun animateSparkle(sparkle: ImageView) {
        sparkle.visibility = View.VISIBLE

        val sparkleAnimation = ScaleAnimation(
            0f, 1.5f, 0f, 1.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = 1
            repeatMode = Animation.REVERSE
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    sparkle.visibility = View.INVISIBLE
                    // Repeat after random delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!isFinishing) {
                            animateSparkle(sparkle)
                        }
                    }, (1000 + Math.random() * 2000).toLong())
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        sparkle.startAnimation(sparkleAnimation)
    }

    private fun triggerConfetti() {
        Log.d(TAG, "Triggering confetti...")
        // Simple confetti effect using text emojis
        for (i in 1..15) {
            Handler(Looper.getMainLooper()).postDelayed({
                createConfettiPiece()
            }, i * 100L)
        }
    }

    private fun createConfettiPiece() {
        val confetti = TextView(this).apply {
            text = when ((1..6).random()) {
                1 -> "✨"
                2 -> "⭐"
                3 -> "🎊"
                4 -> "🎉"
                5 -> "🌟"
                else -> "💫"
            }
            textSize = 24f
        }

        val container = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        container.addView(confetti)

        // Random position at top
        val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = (Math.random() * container.width).toInt()
            topMargin = 0
        }

        confetti.layoutParams = params

        // Animate falling with rotation
        confetti.animate()
            .translationY(container.height.toFloat())
            .translationX((Math.random() * 300 - 150).toFloat())
            .rotation((Math.random() * 720 - 360).toFloat())
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(2500)
            .withEndAction {
                container.removeView(confetti)
            }
            .start()
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "Setting up button listeners...")
        val btnTryAnother = findViewById<Button>(R.id.btnTryAnother)
        val btnHome = findViewById<Button>(R.id.btnHome)

        btnTryAnother.setOnClickListener {
            Log.d(TAG, "Try Another button clicked")
            // Go back to sequence selection
            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnHome.setOnClickListener {
            Log.d(TAG, "Home button clicked")
            // Go to main menu or dashboard
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
        star1.clearAnimation()
        star2.clearAnimation()
        star3.clearAnimation()
        line1.clearAnimation()
        line2.clearAnimation()
        pandaImage.clearAnimation()
        sparkles.forEach { it.clearAnimation() }

        // Stop all glow animations
        stopGlowingAnimations()
        glowHandler.removeCallbacksAndMessages(null)
    }
}