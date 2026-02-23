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
import kotlin.random.Random

/**
 * RMScoreboardActivity — v2.1.0
 *
 * CRASH FIX
 * ─────────
 * Root cause of the restart loop:
 *   onResume() called fillJarWithCandies() on every resume, including the
 *   very first one (before the view has been laid out). At that point
 *   candyJarContainer.childCount can be 0, so:
 *       removeViews(1, childCount - 1)  →  removeViews(1, -1)
 *   throws IllegalArgumentException → uncaught → process killed →
 *   Android restarts the previous activity (RhythmSummaryActivity).
 *
 * Fix:
 *   1. onResume() no longer calls fillJarWithCandies(). Animation runs
 *      only once from onCreate() via a post-layout callback.
 *   2. fillJarWithCandies() guards the removeViews() call so it can never
 *      pass a negative count even if called redundantly.
 *   3. All Handler callbacks are cancelled in onDestroy() to prevent stale
 *      candy-drop animations firing after the activity is gone.
 */
class RMScoreboardActivity : AppCompatActivity() {

    private lateinit var candyJarContainer: FrameLayout
    private lateinit var candyJarImage:     ImageView
    private lateinit var candyCountText:    TextView
    private lateinit var resultText:        TextView
    private lateinit var effortMessage:     TextView
    private lateinit var sparklesContainer: FrameLayout
    private lateinit var replayButton:      Button
    private lateinit var dashboardButton:   Button
    private lateinit var jarFillProgress:   ProgressBar

    private val handler = Handler(Looper.getMainLooper())

    private var score        = 0
    private var totalRounds  = 0
    private var songTitle    = ""
    private var motivationId = 0
    private var unlockGift   = false

    private var avgAlpha        = 0f
    private var avgResponseTime = 0f
    private var engagementScore = 0f
    private var frustrationLevel = 0f
    private var accuracy        = 0f

    private val candyDrawables = listOf(
        R.drawable.candy_red,
        R.drawable.candy_blue,
        R.drawable.candy_green,
        R.drawable.candy_yellow,
        R.drawable.candy_purple
    )

    private val TAG = "RMScoreboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rmscoreboard)

        initializeViews()

        score        = intent.getIntExtra("SCORE", 0)
        totalRounds  = intent.getIntExtra("TOTAL_ROUNDS", 5)
        songTitle    = intent.getStringExtra("SONG_TITLE") ?: ""
        motivationId = intent.getIntExtra("MOTIVATION_ID", 0)
        unlockGift   = intent.getBooleanExtra("UNLOCK_GIFT", false)

        avgAlpha         = intent.getFloatExtra("AVG_ALPHA", 0f)
        avgResponseTime  = intent.getFloatExtra("AVG_RESPONSE_TIME", 0f)
        engagementScore  = intent.getFloatExtra("ENGAGEMENT_SCORE", 0.5f)
        frustrationLevel = intent.getFloatExtra("FRUSTRATION_LEVEL", 0.2f)
        accuracy         = intent.getFloatExtra("ACCURACY", 0.5f)

        Log.d(TAG, "Scoreboard — score=$score/$totalRounds  alpha=$avgAlpha  accuracy=$accuracy")

        candyCountText.text = getCandyCountDescription(score, totalRounds)

        setResultText()
        setEffortMessage()
        setupButtonListeners()

        if (unlockGift) showGiftUnlock()

        // ── [CRASH FIX] Run candy animation after the view is fully laid out ──
        // Using post() on the container ensures width/height are non-zero.
        candyJarContainer.post {
            setupCandyJarAnimation(score)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending candy-drop handlers to prevent leaks or crashes
        handler.removeCallbacksAndMessages(null)
    }

    // onResume intentionally does NOT call fillJarWithCandies().
    // Doing so caused a crash when childCount was 0 on the first resume.

    // =========================================================================
    // Views
    // =========================================================================

    private fun initializeViews() {
        candyJarContainer = findViewById(R.id.candyJarContainer)
        candyJarImage     = findViewById(R.id.candyJarImage)
        candyCountText    = findViewById(R.id.candyCountText)
        resultText        = findViewById(R.id.resultText)
        effortMessage     = findViewById(R.id.effortMessage)
        sparklesContainer = findViewById(R.id.sparklesContainer)
        replayButton      = findViewById(R.id.replayButton)
        dashboardButton   = findViewById(R.id.dashboardButton)
        jarFillProgress   = findViewById(R.id.jarFillProgress)

        jarFillProgress.progress = 0
    }

    // =========================================================================
    // UI text
    // =========================================================================

    private fun getCandyCountDescription(score: Int, total: Int): String = when {
        score == 0               -> "Your jar is ready for candies! 🍬"
        score <= total / 4       -> "You got some candies! 🍬"
        score <= total / 2       -> "You are filling your jar! 🍭"
        score <= total * 3 / 4   -> "Lots of candies! 🌟"
        score < total            -> "Almost full! ✨"
        else                     -> "Full jar! Amazing! 🎉"
    }

    private fun setResultText() {
        val (text, color) = when {
            avgAlpha > 8.0f                          -> "Outstanding!" to "#FFD700"
            avgAlpha > 5.0f || score >= totalRounds * 2 / 3 -> "Great Job!"   to "#2196F3"
            avgAlpha > 2.0f || score > 0             -> "Good Effort!" to "#4CAF50"
            else                                     -> "Keep Trying!" to "#4CAF50"
        }
        resultText.text = text
        resultText.setTextColor(Color.parseColor(color))
        resultText.alpha = 0f
        resultText.translationY = 50f
        resultText.animate().alpha(1f).translationY(0f).setDuration(800).start()
    }

    private fun setEffortMessage() {
        val message = when (motivationId) {
            0    -> getStarsMessage()
            1    -> getQuoteMessage()
            2    -> getAnimationMessage()
            else -> getDefaultMessage()
        }
        effortMessage.text  = message
        effortMessage.alpha = 0f
        handler.postDelayed({
            effortMessage.animate().alpha(1f).setDuration(600).start()
        }, 500)
    }

    private fun getStarsMessage(): String = when {
        avgAlpha > 8.0f -> "You earned all the stars! ⭐⭐⭐"
        avgAlpha > 5.0f -> "You earned 2 stars! ⭐⭐"
        avgAlpha > 2.0f -> "You earned 1 star! ⭐"
        else            -> "Every try helps you learn! 🌟"
    }

    private fun getQuoteMessage(): String {
        val options = when {
            avgAlpha > 8.0f -> listOf("You're a superstar!", "Incredible listening!", "You nailed it!")
            avgAlpha > 5.0f -> listOf("You worked really hard!", "Amazing progress!", "Wonderful!")
            avgAlpha > 2.0f -> listOf("You're learning fast!", "Great focus today!", "Keep going!")
            else            -> listOf("You're learning so much!", "Every try makes you stronger!")
        }
        return options.random()
    }

    private fun getAnimationMessage(): String = when {
        avgAlpha > 8.0f -> "🎉 WOW! You're AMAZING! 🎉"
        avgAlpha > 5.0f -> "🌟 Fantastic work! 🌟"
        avgAlpha > 2.0f -> "✨ Keep it up! ✨"
        else            -> "🎵 You're getting better! 🎵"
    }

    private fun getDefaultMessage(): String {
        val msgs = mutableListOf<String>()
        if (engagementScore  > 0.5f) msgs.add("You were so focused!")
        if (frustrationLevel < 0.5f) msgs.add("You stayed calm!")
        if (accuracy         > 0.4f) msgs.add("You're understanding the words!")
        return msgs.randomOrNull() ?: "Great listening practice!"
    }

    // =========================================================================
    // Candy jar animation
    // =========================================================================

    private fun setupCandyJarAnimation(score: Int) {
        val displayScore = score.coerceAtLeast(1)   // always show ≥ 1 candy
        val fillPct      = (displayScore.toFloat() / totalRounds.toFloat() * 100f).toInt()

        // Animate progress bar
        jarFillProgress.max = 100
        ObjectAnimator.ofInt(jarFillProgress, "progress", 0, fillPct).apply {
            duration     = 1500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Drop candies in with a staggered delay
        handler.postDelayed({ fillJarWithCandies(displayScore) }, 500)

        // Sparkles scaled to performance
        val sparkleCount = when {
            avgAlpha > 8.0f -> 30
            avgAlpha > 5.0f -> 20
            avgAlpha > 2.0f -> 10
            else            -> 5
        }
        createSparkles(sparkleCount)
    }

    private fun fillJarWithCandies(candyCount: Int) {
        try {
            candyJarImage.setImageResource(R.drawable.jar_low)
        } catch (_: Exception) {}

        // ── [CRASH FIX] Guard: only removeViews if there is something to remove
        val existingExtras = candyJarContainer.childCount - 1   // subtract candyJarImage itself
        if (existingExtras > 0) {
            try {
                candyJarContainer.removeViews(1, existingExtras)
            } catch (e: Exception) {
                Log.w(TAG, "removeViews failed (non-fatal): ${e.message}")
            }
        }

        val jarWidth  = candyJarContainer.width.takeIf  { it > 0 } ?: 800
        val jarHeight = candyJarContainer.height.takeIf { it > 0 } ?: 800
        val candySize = (jarWidth * 0.15).toInt()
        val candiesPerRow = 4

        for (i in 0 until candyCount) {
            val row         = i / candiesPerRow
            if (row >= 3) break
            val posInRow    = i % candiesPerRow
            handler.postDelayed({ addCandyToJar(i, row, posInRow, candySize, jarWidth, jarHeight) }, i * 100L)
        }

        handler.postDelayed({ updateJarImage(candyCount) }, candyCount * 100L + 500)
    }

    private fun addCandyToJar(
        index: Int, row: Int, position: Int,
        candySize: Int, jarWidth: Int, jarHeight: Int
    ) {
        // Bail out if activity is finishing (prevents view-after-detach crash)
        if (isFinishing || isDestroyed) return

        val candy = ImageView(this)
        candy.setImageResource(candyDrawables[index % candyDrawables.size])
        candy.layoutParams = FrameLayout.LayoutParams(candySize, candySize)

        val lp         = candy.layoutParams as FrameLayout.LayoutParams
        val jarPad     = (jarWidth * 0.1).toInt()
        val available  = jarWidth - jarPad * 2 - candySize
        lp.leftMargin  = jarPad + (available * position / 3).coerceAtLeast(0)
        lp.topMargin   = ((jarHeight * 0.65) - row * (jarHeight * 0.25)).toInt().coerceAtLeast(0)

        candy.rotation = Random.nextFloat() * 360
        candyJarContainer.addView(candy)

        candy.translationY = -jarHeight.toFloat()
        candy.scaleX = 0.5f; candy.scaleY = 0.5f
        candy.animate().translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(AccelerateDecelerateInterpolator()).start()

        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                candy.animate().translationY(-10f).setDuration(200)
                    .withEndAction { candy.animate().translationY(0f).setDuration(200).start() }
                    .start()
            }
        }, 600)

        candyCountText.text = getCandyCountDescription(index + 1, totalRounds)
    }

    private fun updateJarImage(candyCount: Int) {
        if (isFinishing || isDestroyed) return
        val fill = if (totalRounds > 0) candyCount.toFloat() / totalRounds else 0f
        val res  = when {
            candyCount <= 1 -> R.drawable.jar_low
            fill < 0.3f     -> R.drawable.jar_low
            fill < 0.6f     -> R.drawable.jar_medium
            fill < 0.9f     -> R.drawable.jar_high
            else            -> R.drawable.jar_full
        }
        try { candyJarImage.setImageResource(res) } catch (_: Resources.NotFoundException) {}
        if (fill >= 0.9f) celebrateFullJar()
    }

    private fun celebrateFullJar() {
        resultText.text = "Jar is FULL! 🎉🎉🎉"
        candyJarImage.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
            .withEndAction { candyJarImage.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            .start()
        createSparkles(30)
        createFallingCandies(15)
    }

    // =========================================================================
    // Sparkles & falling candies
    // =========================================================================

    private fun createSparkles(count: Int) {
        for (i in 0 until count) {
            handler.postDelayed({ addRandomSparkle() }, i * 100L)
        }
    }

    private fun addRandomSparkle() {
        if (isFinishing || isDestroyed) return
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(40, 40)
        val lp = sparkle.layoutParams as FrameLayout.LayoutParams
        val cw = sparklesContainer.width.coerceAtLeast(40)
        val ch = sparklesContainer.height.coerceAtLeast(40)
        lp.leftMargin = Random.nextInt(0, cw - 40)
        lp.topMargin  = Random.nextInt(0, ch - 40)
        sparklesContainer.addView(sparkle)
        ObjectAnimator.ofFloat(sparkle, "alpha",  0f, 1f, 0f).apply { duration = 1000; repeatCount = 3; start() }
        ObjectAnimator.ofFloat(sparkle, "scaleX", 0.5f, 1.5f, 0.5f).apply { duration = 1000; repeatCount = 3; start() }
        ObjectAnimator.ofFloat(sparkle, "scaleY", 0.5f, 1.5f, 0.5f).apply { duration = 1000; repeatCount = 3; start() }
        handler.postDelayed({ sparklesContainer.removeView(sparkle) }, 3000)
    }

    private fun createFallingCandies(count: Int) {
        for (i in 0 until count) handler.postDelayed({ addFallingCandy() }, i * 200L)
    }

    private fun addFallingCandy() {
        if (isFinishing || isDestroyed) return
        val candy = ImageView(this)
        candy.setImageResource(candyDrawables.random())
        val size = Random.nextInt(30, 60)
        candy.layoutParams = FrameLayout.LayoutParams(size, size)
        val lp = candy.layoutParams as FrameLayout.LayoutParams
        val cw = candyJarContainer.width.coerceAtLeast(size)
        lp.leftMargin = Random.nextInt(0, cw - size)
        lp.topMargin  = -size
        candyJarContainer.addView(candy)
        val fall = candyJarContainer.height + size
        ObjectAnimator.ofFloat(candy, "translationY", 0f, fall.toFloat()).apply { duration = 2000; start() }
        ObjectAnimator.ofFloat(candy, "rotation", 0f, Random.nextFloat() * 720 - 360).apply { duration = 2000; start() }
        handler.postDelayed({ candyJarContainer.removeView(candy) }, 2000)
    }

    private fun showGiftUnlock() {
        Toast.makeText(this, "🎁 Special gift unlocked! 🎁", Toast.LENGTH_LONG).show()
        createSparkles(20)
    }

    // =========================================================================
    // Buttons
    // =========================================================================

    private fun setupButtonListeners() {
        replayButton.setOnClickListener {
            startActivity(Intent(this, RhythmSummaryActivity::class.java).apply {
                // Do NOT use CLEAR_TASK here — replay should be a normal start
                putExtra("SONG_TITLE", songTitle)
            })
            finish()
        }

        dashboardButton.setOnClickListener {
            startActivity(Intent(this, GameDashboardActivity::class.java))
            finish()
        }

        candyJarContainer.setOnClickListener {
            candyJarImage.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
                .withEndAction { candyJarImage.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }
                .start()
            val msgs = listOf("Yum! 🍬", "So sweet! 🍭", "Delicious! 🍫", "Your candies! ✨")
            candyCountText.text = msgs.random()
            handler.postDelayed({ candyCountText.text = getCandyCountDescription(score, totalRounds) }, 2000)
        }
    }
}