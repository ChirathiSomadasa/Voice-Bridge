package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ASequenceScoreboardActivity : AppCompatActivity() {

    private var attempts = 1
    private var completionTime = 0L
    private var startTime: Long = 0L  // Fixed: Initialize with default value

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var line1: View
    private lateinit var line2: View
    private lateinit var pandaImage: ImageView
    private val sparkles = mutableListOf<ImageView>()

    // Handler for glow animations
    private val glowHandler = Handler(Looper.getMainLooper())
    private val glowRunnables = mutableListOf<Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        // Get data passed from game
        attempts = intent.getIntExtra("ATTEMPTS", 1)
        startTime = intent.getLongExtra("START_TIME", System.currentTimeMillis())
        completionTime = System.currentTimeMillis() - startTime

        // Initialize views
        val celebrationText = findViewById<TextView>(R.id.celebrationText)
        val subtitleText = findViewById<TextView>(R.id.subtitleText)
        val attemptsText = findViewById<TextView>(R.id.attemptsText)
        val timeText = findViewById<TextView>(R.id.timeText)
        val accuracyText = findViewById<TextView>(R.id.accuracyText)
        val btnTryAnother = findViewById<Button>(R.id.btnTryAnother)
        val btnHome = findViewById<Button>(R.id.btnHome)

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

        // Set stats
        attemptsText.text = attempts.toString()
        timeText.text = formatTime(completionTime)
        accuracyText.text = "100%"

        // Set celebration message based on attempts
        updateCelebrationMessage(celebrationText, subtitleText, attempts)

        // Start animations with delay
        Handler(Looper.getMainLooper()).postDelayed({
            startPandaAnimation()
            startStarAnimationSequence()
        }, 500)

        // Start sparkle animations
        startSparkleAnimations()

        // Button click listeners
        btnTryAnother.setOnClickListener {
            // Go back to sequence selection
            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnHome.setOnClickListener {
            // Go to main menu or dashboard
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        return if (seconds < 60) {
            "${seconds}s"
        } else {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            "${minutes}m ${remainingSeconds}s"
        }
    }

    private fun updateCelebrationMessage(
        celebrationText: TextView,
        subtitleText: TextView,
        attempts: Int
    ) {
        when (attempts) {
            1 -> {
                celebrationText.text = "🎉 Perfect on First Try! 🎉"
                subtitleText.text = "You're a sequencing superstar! ⭐"
            }
            in 2..3 -> {
                celebrationText.text = "🎊 Excellent Work! 🎊"
                subtitleText.text = "You figured it out quickly! 🚀"
            }
            else -> {
                celebrationText.text = "🌟 Great Persistence! 🌟"
                subtitleText.text = "You never gave up! 💪"
            }
        }
    }

    private fun startPandaAnimation() {
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
        // Animate stars one by one
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
            // Final celebration
            playFinalCelebration()
        }, 1300)
    }

    private fun fillStar(star: ImageView) {
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
        // Clear all runnables
        glowRunnables.forEach {
            glowHandler.removeCallbacks(it)
        }
        glowRunnables.clear()
    }

    private fun pandaHappyDance() {
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

    override fun onDestroy() {
        super.onDestroy()
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