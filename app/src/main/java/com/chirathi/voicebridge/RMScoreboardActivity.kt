package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.random.Random

class RMScoreboardActivity : AppCompatActivity() {

    private lateinit var candyJarContainer: FrameLayout
    private lateinit var candyJarImage: ImageView
    private lateinit var candyCountText: TextView
    private lateinit var resultText: TextView
    private lateinit var effortMessage: TextView
    private lateinit var sparklesContainer: FrameLayout
    private lateinit var replayButton: Button
    private lateinit var dashboardButton: Button
    private lateinit var jarFillProgress: ProgressBar

    private var score = 0
    private var totalRounds = 0
    private var songTitle = ""
    private var sessionMetrics: SessionMetrics? = null

    private val candyDrawables = listOf(
        R.drawable.candy_red,
        R.drawable.candy_blue,
        R.drawable.candy_green,
        R.drawable.candy_yellow,
        R.drawable.candy_purple
    )

    // Use a different TAG name to avoid conflict
    private val SCOREBOARD_TAG = "RMScoreboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rmscoreboard)

        initializeViews()

        score = intent.getIntExtra("SCORE", 0)
        totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        songTitle = intent.getStringExtra("SONG_TITLE") ?: ""
        sessionMetrics = intent.getSerializableExtra("SESSION_METRICS") as? SessionMetrics

        candyCountText.text = "$score candies"

        setResultText(score)
        setEffortMessage()
        setupCandyJarAnimation(score)
        setupButtonListeners()
    }

    private fun initializeViews() {
        candyJarContainer = findViewById(R.id.candyJarContainer)
        candyJarImage = findViewById(R.id.candyJarImage)
        candyCountText = findViewById(R.id.candyCountText)
        resultText = findViewById(R.id.resultText)
        effortMessage = findViewById(R.id.effortMessage)
        sparklesContainer = findViewById(R.id.sparklesContainer)
        replayButton = findViewById(R.id.replayButton)
        dashboardButton = findViewById(R.id.dashboardButton)
        jarFillProgress = findViewById(R.id.jarFillProgress)

        // Set initial jar as empty
        candyJarImage.setImageResource(R.drawable.empty_jar)
        jarFillProgress.progress = 0
    }

    private fun setResultText(score: Int) {
        when {
            score == 0 -> {
                resultText.text = "You Tried!"
                resultText.setTextColor(Color.parseColor("#FF9800"))
            }
            score <= totalRounds / 3 -> {
                resultText.text = "Good Effort!"
                resultText.setTextColor(Color.parseColor("#4CAF50"))
            }
            score <= totalRounds * 2 / 3 -> {
                resultText.text = "Great Job!"
                resultText.setTextColor(Color.parseColor("#2196F3"))
            }
            else -> {
                resultText.text = "Amazing!"
                resultText.setTextColor(Color.parseColor("#9C27B0"))
            }
        }

        // Animate text
        resultText.alpha = 0f
        resultText.translationY = 50f
        resultText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .start()
    }

    private fun setEffortMessage() {
        val messages = when {
            score == 0 -> listOf(
                "Every try counts!",
                "You showed great persistence!",
                "You listened carefully!"
            )
            score <= totalRounds / 3 -> listOf(
                "You're learning!",
                "Great focus today!",
                "You're getting better!"
            )
            score <= totalRounds * 2 / 3 -> listOf(
                "You worked really hard!",
                "What excellent attention!",
                "You're doing wonderful!"
            )
            else -> listOf(
                "You're a superstar!",
                "Incredible concentration!",
                "You nailed it!"
            )
        }

        effortMessage.text = messages.random()

        // Personalized message based on session metrics
        sessionMetrics?.let { metrics ->
            metrics.behavioralProfile?.let { profile ->
                val personalizedMessages = mutableListOf<String>()

                if (profile.engagementScore > 0.7) {
                    personalizedMessages.add("You were so focused!")
                }
                if (profile.frustrationLevel < 0.3) {
                    personalizedMessages.add("You stayed calm and kept trying!")
                }
                if (profile.accuracy > 0.6) {
                    personalizedMessages.add("You're really understanding the words!")
                }

                if (personalizedMessages.isNotEmpty()) {
                    effortMessage.text = personalizedMessages.random()
                }
            }
        }

        effortMessage.alpha = 0f
        Handler(Looper.getMainLooper()).postDelayed({
            effortMessage.animate()
                .alpha(1f)
                .setDuration(600)
                .start()
        }, 500)
    }

    private fun setupCandyJarAnimation(score: Int) {
        // Calculate fill percentage
        val fillPercentage = (score.toFloat() / totalRounds.toFloat()) * 100f

        // Animate progress bar
        jarFillProgress.max = 100
        val progressAnimator = ObjectAnimator.ofInt(jarFillProgress, "progress", 0, fillPercentage.toInt())
        progressAnimator.duration = 1500
        progressAnimator.interpolator = AccelerateDecelerateInterpolator()
        progressAnimator.start()

        // Fill jar with candies
        Handler(Looper.getMainLooper()).postDelayed({
            fillJarWithCandies(score)
        }, 500)

        // Add sparkles for good scores
        if (score > totalRounds / 2) {
            createSparkles(score * 2)
        }
    }

    private fun fillJarWithCandies(candyCount: Int) {
        candyJarImage.setImageResource(R.drawable.empty_jar)

        // Clear existing candies
        candyJarContainer.removeViews(1, candyJarContainer.childCount - 1)

        // Calculate positions for candies
        val jarWidth = candyJarContainer.width
        val jarHeight = candyJarContainer.height
        val candySize = (jarWidth * 0.15).toInt()

        // Layer candies from bottom up
        val rows = 3 // Maximum rows of candies
        val candiesPerRow = 4

        for (i in 0 until candyCount) {
            val row = i / candiesPerRow
            if (row >= rows) break // Don't overflow jar

            val positionInRow = i % candiesPerRow
            val delay = i * 100L

            Handler(Looper.getMainLooper()).postDelayed({
                addCandyToJar(i, row, positionInRow, candySize, jarWidth, jarHeight)
            }, delay)
        }

        // Update jar image based on fill level
        Handler(Looper.getMainLooper()).postDelayed({
            updateJarImage(candyCount)
        }, candyCount * 100L + 500)
    }

    private fun addCandyToJar(index: Int, row: Int, position: Int, candySize: Int, jarWidth: Int, jarHeight: Int) {
        val candy = ImageView(this)
        val candyDrawable = candyDrawables[index % candyDrawables.size]

        candy.setImageResource(candyDrawable)
        candy.layoutParams = FrameLayout.LayoutParams(candySize, candySize)

        val layoutParams = candy.layoutParams as FrameLayout.LayoutParams

        // Calculate position within jar
        val jarPadding = (jarWidth * 0.1).toInt()
        val availableWidth = jarWidth - jarPadding * 2 - candySize
        val xPosition = jarPadding + (availableWidth * position / 3).toInt()

        // Stack from bottom
        val rowHeight = (jarHeight * 0.25).toInt()
        val yBase = (jarHeight * 0.65).toInt() // Start from 65% of jar height
        val yPosition = yBase - (row * rowHeight)

        layoutParams.leftMargin = xPosition
        layoutParams.topMargin = yPosition

        // Random rotation
        candy.rotation = Random.nextFloat() * 360

        candyJarContainer.addView(candy)

        // Drop animation
        candy.translationY = -jarHeight.toFloat()
        candy.scaleX = 0.5f
        candy.scaleY = 0.5f

        candy.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Bounce effect
        Handler(Looper.getMainLooper()).postDelayed({
            candy.animate()
                .translationY(-10f)
                .setDuration(200)
                .withEndAction {
                    candy.animate()
                        .translationY(0f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }, 600)

        // Update candy count text
        candyCountText.text = "${index + 1} candies"
    }

    private fun updateJarImage(candyCount: Int) {
        val fillLevel = if (totalRounds > 0) candyCount.toFloat() / totalRounds.toFloat() else 0f
        Log.d(SCOREBOARD_TAG, "🎯 UPDATE JAR: Candies=$candyCount, Total=$totalRounds, Fill=$fillLevel")

        // NEVER show empty jar - always show at least low jar
        val jarResource = when {
            candyCount == 0 -> {
                Log.d(SCOREBOARD_TAG, "🟡 Showing LOW jar (0 candies)")
                R.drawable.jar_low // Changed from empty_jar
            }
            fillLevel < 0.3 -> {
                Log.d(SCOREBOARD_TAG, "🟡 Showing LOW jar (<30%)")
                R.drawable.jar_low
            }
            fillLevel < 0.6 -> {
                Log.d(SCOREBOARD_TAG, "🟠 Showing MEDIUM jar (30-60%)")
                R.drawable.jar_medium
            }
            fillLevel < 0.9 -> {
                Log.d(SCOREBOARD_TAG, "🟢 Showing HIGH jar (60-90%)")
                R.drawable.jar_high
            }
            else -> {
                Log.d(SCOREBOARD_TAG, "🔴 Showing FULL jar (90-100%)")
                R.drawable.jar_full
            }
        }

        // Set the jar image
        try {
            candyJarImage.setImageResource(jarResource)
            Log.d(SCOREBOARD_TAG, "✅ Jar image updated")
        } catch (e: Resources.NotFoundException) {
            Log.e(SCOREBOARD_TAG, "❌ Resource not found", e)
            // Fallback - at least show something
            candyJarImage.setImageResource(R.drawable.jar_low)
        }
    }

    private fun celebrateFullJar() {
        resultText.text = "Jar is FULL!"

        // Big celebration animation
        candyJarImage.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(300)
            .withEndAction {
                candyJarImage.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()

        // Extra sparkles
        createSparkles(20)

        // Confetti-like candies falling
        createFallingCandies(10)
    }

    private fun createSparkles(count: Int) {
        for (i in 0 until count) {
            Handler(Looper.getMainLooper()).postDelayed({
                addRandomSparkle()
            }, i * 100L)
        }
    }

    private fun addRandomSparkle() {
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(40, 40)

        val layoutParams = sparkle.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, sparklesContainer.width - 40)
        layoutParams.topMargin = Random.nextInt(0, sparklesContainer.height - 40)

        sparklesContainer.addView(sparkle)
        animateSparkle(sparkle)
    }

    private fun animateSparkle(sparkle: ImageView) {
        val fadeAnim = ObjectAnimator.ofFloat(sparkle, "alpha", 0f, 1f, 0f)
        fadeAnim.duration = 1000
        fadeAnim.repeatCount = 3

        val scaleAnim = ObjectAnimator.ofFloat(sparkle, "scaleX", 0.5f, 1.5f, 0.5f)
        scaleAnim.duration = 1000
        scaleAnim.repeatCount = 3

        val scaleYAnim = ObjectAnimator.ofFloat(sparkle, "scaleY", 0.5f, 1.5f, 0.5f)
        scaleYAnim.duration = 1000
        scaleYAnim.repeatCount = 3

        fadeAnim.start()
        scaleAnim.start()
        scaleYAnim.start()

        Handler(Looper.getMainLooper()).postDelayed({
            sparklesContainer.removeView(sparkle)
        }, 3000)
    }

    private fun createFallingCandies(count: Int) {
        for (i in 0 until count) {
            Handler(Looper.getMainLooper()).postDelayed({
                addFallingCandy()
            }, i * 200L)
        }
    }

    private fun addFallingCandy() {
        val candy = ImageView(this)
        val candyDrawable = candyDrawables.random()

        candy.setImageResource(candyDrawable)
        val size = Random.nextInt(30, 60)
        candy.layoutParams = FrameLayout.LayoutParams(size, size)

        val layoutParams = candy.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, candyJarContainer.width - size)
        layoutParams.topMargin = -size // Start above container

        candyJarContainer.addView(candy)
        animateFallingCandy(candy, size)
    }

    private fun animateFallingCandy(candy: ImageView, size: Int) {
        val fallDistance = candyJarContainer.height + size
        val fallAnim = ObjectAnimator.ofFloat(candy, "translationY", 0f, fallDistance.toFloat())
        fallAnim.duration = 2000

        val rotationAnim = ObjectAnimator.ofFloat(candy, "rotation", 0f, Random.nextFloat() * 720 - 360)
        rotationAnim.duration = 2000

        fallAnim.start()
        rotationAnim.start()

        Handler(Looper.getMainLooper()).postDelayed({
            candyJarContainer.removeView(candy)
        }, 2000)
    }

    private fun setupButtonListeners() {
        replayButton.setOnClickListener {
            val intent = Intent(this, RhythmSummaryActivity::class.java)
            intent.putExtra("SONG_TITLE", songTitle)

            // Pass therapeutic intent for continuity
            val therapeuticIntent = TherapeuticIntent(
                primaryGoal = "maintain_progress",
                uiComplexity = 0.6f,
                sessionDuration = 10,
                adaptiveScaling = true
            )
            intent.putExtra("THERAPEUTIC_INTENT", therapeuticIntent)

            startActivity(intent)
            finish()
        }

        dashboardButton.setOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Add candy jar click listener for fun interaction (VISUAL ONLY)
        candyJarContainer.setOnClickListener {
            // Just visual feedback, no candy changes
            candyJarImage.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction {
                    candyJarImage.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure jar is properly filled
        if (score > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                fillJarWithCandies(score)
            }, 100)
        }
    }
}