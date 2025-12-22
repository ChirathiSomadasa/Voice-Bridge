package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MMScoreboardActivity : AppCompatActivity() {

    // Views from the new layout
    private lateinit var btnPlayAgain: Button
    private lateinit var btnDashboard: Button
    private lateinit var btnUnlockGift: CardView
    private lateinit var giftBoxIcon: ImageView
    private lateinit var scoreLabel: TextView
    private lateinit var titleWon: TextView
    private lateinit var scoreValue: TextView
    private lateinit var performanceText: TextView

    // Star views
    private lateinit var starLeft: ImageView
    private lateinit var starMiddle: ImageView
    private lateinit var starRight: ImageView

    // Game mode variable
    private var gameMode = "seven_down" // Default to seven_down

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mmscoreboard)

        // Get the data passed from the game activity
        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        val score = intent.getIntExtra("SCORE", 0)

        // Get game mode
        gameMode = intent.getStringExtra("GAME_MODE") ?: "seven_down"

        // Find views from the new layout
        titleWon = findViewById(R.id.titleWon)
        scoreValue = findViewById(R.id.scoreValue)
        scoreLabel = findViewById(R.id.scoreLabel)
        performanceText = findViewById(R.id.performanceText)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnDashboard = findViewById(R.id.btnDashboard)
        btnUnlockGift = findViewById(R.id.btnUnlockGift)
        giftBoxIcon = findViewById(R.id.giftBoxIcon)

        // Initialize star views
        starLeft = findViewById(R.id.starLeft)
        starMiddle = findViewById(R.id.starMiddle)
        starRight = findViewById(R.id.starRight)

        // Set the score values
        scoreValue.text = "Your Score"
        scoreLabel.text = "$score"

        // Animate score counting
        animateScoreCount(score)

        // Update title, stars, and performance text based on correct answers
        updateResults(correctAnswers)

        // Start animations
        startTitleAnimation()
        startGiftBoxAnimation()

        // Play Again button
        btnPlayAgain.setOnClickListener {
            val intent = when (gameMode) {
                "seven_up" -> Intent(this, MoodMatchSevenUpActivity::class.java)
                else -> Intent(this, MoodMatchSevenDownActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Dashboard button
        btnDashboard.setOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Unlock Gift button
        btnUnlockGift.setOnClickListener {
            // Handle unlock gift action
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateResults(correctAnswers: Int) {
        // Determine performance based on correct answers
        when (correctAnswers) {
            0, 1, 2 -> {
                // Score 0-2: You Tried
                titleWon.text = "YOU TRIED"
                titleWon.setTextColor(resources.getColor(R.color.light_blue, theme))
                performanceText.text = "Keep practicing! You'll get better! 📚"
                updateStarRating(1) // 1 star for 0-2 correct answers
            }
            3, 4 -> {
                // Score 3-4: Great Job
                titleWon.text = "GREAT JOB"
                titleWon.setTextColor(resources.getColor(R.color.light_green, theme))
                performanceText.text = "Almost perfect! Keep going! 👍"
                updateStarRating(2) // 2 stars for 3-4 correct answers
            }
            5 -> {
                // Score 5: Victory
                titleWon.text = "VICTORY"
                titleWon.setTextColor(resources.getColor(R.color.gold, theme))
                performanceText.text = "Excellent! Perfect score! 🏆"
                updateStarRating(3) // 3 stars for 5 correct answers
            }
            else -> {
                titleWon.text = "COMPLETED"
                performanceText.text = "Game completed!"
                updateStarRating(0)
            }
        }
    }

    private fun updateStarRating(starsToLight: Int) {
        // Light up stars
        val stars = listOf(starLeft, starMiddle, starRight)
        for (i in 0 until 3) {
            val star = stars[i]
            if (i < starsToLight) {
                star.setImageResource(R.drawable.star_filled)
                animateStarWithBounce(star, i * 200L) // Stagger the animations
            } else {
                star.setImageResource(R.drawable.star_outline)
            }
        }
    }

    private fun animateScoreCount(finalScore: Int) {
        // Animate the score counting from 0 to finalScore
        val animator = ValueAnimator.ofInt(0, finalScore)
        animator.duration = 1500

        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            scoreLabel.text = currentValue.toString()

            // Add comma formatting for large numbers
            if (finalScore >= 1000) {
                val formattedScore = String.format("%,d", currentValue)
                scoreLabel.text = formattedScore
            }
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

    private fun startTitleAnimation() {
        // Bounce animation for title
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
            titleWon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f, 1f)
        ).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 1000
        }

        titleWon.startAnimation(bounceAnim)
        pulseAnim.start()
    }

    private fun startGiftBoxAnimation() {
        // Blinking animation for gift box
        val blinkAnim = ObjectAnimator.ofFloat(giftBoxIcon, "alpha", 0.3f, 1f, 0.3f)
        blinkAnim.duration = 1500
        blinkAnim.repeatCount = ValueAnimator.INFINITE
        blinkAnim.repeatMode = ValueAnimator.REVERSE
        blinkAnim.start()

        // Bounce animation for gift box
        val bounceAnim = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        ).apply {
            duration = 2000
            interpolator = BounceInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            startDelay = 500
        }

        bounceAnim.start()

        // Rotate animation
        val rotateAnim = ObjectAnimator.ofFloat(giftBoxIcon, "rotation", 0f, 360f)
        rotateAnim.duration = 4000
        rotateAnim.repeatCount = ValueAnimator.INFINITE
        rotateAnim.interpolator = LinearInterpolator()
        rotateAnim.start()
    }

    override fun onResume() {
        super.onResume()
        // Restart animations
        startTitleAnimation()
        startGiftBoxAnimation()
    }

    override fun onPause() {
        super.onPause()
        // Clear animations
        titleWon.clearAnimation()
        giftBoxIcon.clearAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear all animations
        titleWon.clearAnimation()
        giftBoxIcon.clearAnimation()
        starLeft.clearAnimation()
        starMiddle.clearAnimation()
        starRight.clearAnimation()
    }
}