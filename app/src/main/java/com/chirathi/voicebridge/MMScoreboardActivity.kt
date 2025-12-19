package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnDashboard = findViewById(R.id.btnDashboard)
        btnUnlockGift = findViewById(R.id.btnUnlockGift)
        giftBoxIcon = findViewById(R.id.giftBoxIcon)

        // Initialize star views (make sure these IDs exist in your XML)
        // If you haven't added them to XML yet, you can create them programmatically
        initializeStars()

        // Set the score values - match the image format
        scoreValue.text = "Your Score"
        scoreLabel.text = "$score"

        // Animate score counting
        animateScoreCount(score)

        // Update star ratings based on correct answers
        updateStarRating(correctAnswers, totalRounds)

        // Start animations
        startTitleAnimation()
        startGiftBoxAnimation()
        startSparkleAnimations()

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

    private fun initializeStars() {
        // Try to find star views if they exist in XML
        val starLeftId = resources.getIdentifier("starLeft", "id", packageName)
        val starMiddleId = resources.getIdentifier("starMiddle", "id", packageName)
        val starRightId = resources.getIdentifier("starRight", "id", packageName)

        if (starLeftId != 0) starLeft = findViewById(starLeftId)
        if (starMiddleId != 0) starMiddle = findViewById(starMiddleId)
        if (starRightId != 0) starRight = findViewById(starRightId)

        // If stars don't exist in XML, create them programmatically
        if (starLeftId == 0) {
            createStarsProgrammatically()
        }
    }

    private fun createStarsProgrammatically() {
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

        // Create left star
        starLeft = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                40.dpToPx(), 40.dpToPx()
            )
            setImageResource(R.drawable.star_outline)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Create middle star
        starMiddle = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                60.dpToPx(), 60.dpToPx()
            )
            setImageResource(R.drawable.star_outline)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Create right star
        starRight = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                40.dpToPx(), 40.dpToPx()
            )
            setImageResource(R.drawable.star_outline)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Add stars to layout
        mainLayout.addView(starLeft)
        mainLayout.addView(starMiddle)
        mainLayout.addView(starRight)

        // Position stars using ConstraintSet programmatically
        positionStarsProgrammatically()
    }

    private fun positionStarsProgrammatically() {
        // This is a simplified version - you'd need to use ConstraintSet for proper positioning
        // For now, we'll just set positions relative to the title
        Handler(Looper.getMainLooper()).post {
            val titleLocation = IntArray(2)
            titleWon.getLocationOnScreen(titleLocation)

            starLeft.x = titleWon.x - 60.dpToPx()
            starLeft.y = titleWon.y - 60.dpToPx()

            starMiddle.x = titleWon.x + titleWon.width / 2 - 30.dpToPx()
            starMiddle.y = titleWon.y - 70.dpToPx()

            starRight.x = titleWon.x + titleWon.width + 20.dpToPx()
            starRight.y = titleWon.y - 60.dpToPx()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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

    private fun updateStarRating(correctAnswers: Int, totalRounds: Int) {
        // Determine how many stars to light up
        val starsToLight = when (correctAnswers) {
            in 0..2 -> 1
            in 3..4 -> 2
            5 -> 3
            else -> 0
        }

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

        // Show performance message
        val performanceText = when (starsToLight) {
            3 -> "Excellent! All stars earned! 🏆"
            2 -> "Great job! Almost perfect! 👍"
            1 -> "Good work! Keep practicing! 💪"
            else -> "Keep trying! You'll get better! 📚"
        }

        // You could show this in a Toast or TextView
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
        // Bounce animation for "YOU WON" title
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

    private fun startSparkleAnimations() {
        // Create sparkles every 2 seconds
        val sparkleRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    createSparkleEffect()
                    Handler(Looper.getMainLooper()).postDelayed(this, 2000)
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed(sparkleRunnable, 500)
    }

    private fun createSparkleEffect() {
        val mainLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

        // Create 3-5 sparkles
        val sparkleCount = (3..5).random()

        for (i in 1..sparkleCount) {
            val sparkle = ImageView(this)

            // Try to use sparkle drawable, or create a simple white circle
            try {
                sparkle.setImageResource(R.drawable.sparkle)
            } catch (e: Exception) {
                // Create a simple white circle programmatically
                sparkle.setBackgroundResource(android.R.drawable.ic_popup_reminder)
            }

            sparkle.layoutParams = android.view.ViewGroup.LayoutParams(30, 30)
            mainLayout.addView(sparkle)

            // Random position around the screen
            val randomX = (Math.random() * (mainLayout.width - 100)).toFloat() + 50
            val randomY = (Math.random() * (mainLayout.height - 300)).toFloat() + 150

            sparkle.x = randomX
            sparkle.y = randomY

            // Animate and remove
            animateSparkle(sparkle)
        }
    }

    private fun animateSparkle(sparkle: ImageView) {
        // Scale animation
        val scaleAnim = ScaleAnimation(
            0f, 1.5f, 0f, 1.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = 1
            repeatMode = ScaleAnimation.REVERSE
            setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    (sparkle.parent as? android.view.ViewGroup)?.removeView(sparkle)
                }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
        }

        // Fade animation
        val fadeAnim = ObjectAnimator.ofFloat(sparkle, "alpha", 0f, 1f, 0f)
        fadeAnim.duration = 800

        sparkle.startAnimation(scaleAnim)
        fadeAnim.start()
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