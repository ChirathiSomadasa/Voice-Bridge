package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

/**
 * RMScoreboardActivity — v2.3.0
 *
 * CHANGES FROM v2.2.0
 * ─────────────────────────────────────────────────
 *  [A] TTS ADDED
 *      Human-like, child-friendly voice reads the therapeutic feedback
 *      after AI generation completes. Only message + encouragement are
 *      spoken (headline is visual only). Two separate speak() calls with
 *      a 700 ms silent gap between them give natural pacing. Pitch and
 *      speech rate are tuned for clarity with young listeners.
 *      TTS is shut down cleanly in onDestroy().
 */
class RMScoreboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

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

    private var score         = 0
    private var totalRounds   = 0
    private var songTitle     = ""
    private var motivationId  = 0
    private var unlockGift    = false

    private var avgAlpha         = 0f
    private var avgResponseTime  = 0f
    private var engagementScore  = 0f
    private var frustrationLevel = 0f
    private var accuracy         = 0f
    private var consecutiveWrong = 0

    // ── TTS ──────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var pendingMsg = ""
    private var pendingEnc = ""

    private val candyDrawables = listOf(
        R.drawable.candy_red,   R.drawable.candy_blue,
        R.drawable.candy_green, R.drawable.candy_yellow,
        R.drawable.candy_purple
    )

    private val TAG = "RMScoreboardActivity"
    private val AGE_GROUP = 6   // rhythm game is always age 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rmscoreboard)

        initializeViews()

        score            = intent.getIntExtra("SCORE", 0)
        totalRounds      = intent.getIntExtra("TOTAL_ROUNDS", 5)
        songTitle        = intent.getStringExtra("SONG_TITLE") ?: ""
        motivationId     = intent.getIntExtra("MOTIVATION_ID", 0)
        unlockGift       = intent.getBooleanExtra("UNLOCK_GIFT", false)
        avgAlpha         = intent.getFloatExtra("AVG_ALPHA", 0f)
        avgResponseTime  = intent.getFloatExtra("AVG_RESPONSE_TIME", 0f)
        engagementScore  = intent.getFloatExtra("ENGAGEMENT_SCORE", 0.5f)
        frustrationLevel = intent.getFloatExtra("FRUSTRATION_LEVEL", 0.2f)
        accuracy         = intent.getFloatExtra("ACCURACY", 0.5f)
        consecutiveWrong = intent.getIntExtra("CONSECUTIVE_WRONG", 0)

        Log.d(TAG, "score=$score/$totalRounds alpha=$avgAlpha accuracy=$accuracy")

        // ── Initialise TTS ────────────────────────────────────────────────────
        tts = TextToSpeech(this, this)

        // ── Candy description (no number) ─────────────────────────────────────
        candyCountText.text = candyDescription(score, totalRounds)

        // ── Loading placeholders — friendly, zero numbers ─────────────────────
        resultText.text    = "Well done!"
        effortMessage.text = "Getting your special message…"

        setupButtonListeners()
        if (unlockGift) showGiftUnlock()

        // ── Candy animation (unchanged, runs after layout) ────────────────────
        candyJarContainer.post { setupCandyJarAnimation(score) }

        // ── AI feedback (runs in parallel) ────────────────────────────────────
        fetchAIFeedback()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) tts.shutdown()
    }

    // =========================================================================
    // TTS
    // =========================================================================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.3f)       // warm and child-friendly without being shrill
            tts.setSpeechRate(0.75f) // slow enough for young listeners to follow
            isTtsReady = true
            if (pendingMsg.isNotBlank()) {
                speakFeedback(pendingMsg, pendingEnc)
                pendingMsg = ""
                pendingEnc = ""
            }
        }
    }

    /**
     * Speaks message and encouragement as two separate utterances with a
     * 700 ms silent gap so the child can absorb each sentence naturally.
     * Punctuation is preserved so the TTS engine pauses at sentence ends.
     */
    private fun speakFeedback(message: String, encouragement: String) {
        if (!isTtsReady) return
        fun clean(s: String) = s.replace(Regex("[^a-zA-Z0-9 .,!?']"), "").trim()
        tts.speak(clean(message),      TextToSpeech.QUEUE_FLUSH, null, "msg")
        tts.playSilentUtterance(700,   TextToSpeech.QUEUE_ADD,   "pause")
        tts.speak(clean(encouragement),TextToSpeech.QUEUE_ADD,   null, "enc")
    }

    // =========================================================================
    // AI feedback
    // =========================================================================

    private fun fetchAIFeedback() {
        val correctCount = score
        val errorCount   = totalRounds - score

        val level = TherapeuticFeedbackGenerator.classify(
            correctCount = correctCount,
            errorCount   = errorCount,
            attempts     = totalRounds,
            finalAlpha   = avgAlpha
        )

        val session = SessionContext(
            activityType       = TherapeuticFeedbackGenerator.ActivityType.RHYTHM_LISTENING,
            performanceLevel   = level,
            childAge           = AGE_GROUP,
            errorCount         = errorCount,
            correctCount       = correctCount,
            attempts           = totalRounds,
            songTitle          = songTitle,
            consecutiveWrong   = consecutiveWrong,
            avgResponseTimeMs  = avgResponseTime.toLong()
        )

        lifecycleScope.launch {
            val feedback = TherapeuticFeedbackGenerator.generate(
                context = this@RMScoreboardActivity,
                session = session
            )
            applyFeedback(feedback)
        }
    }

    private fun applyFeedback(feedback: TherapeuticFeedback) {
        runOnUiThread {
            // Result headline — animated, colour driven by alpha (unchanged)
            resultText.text = feedback.headline
            resultText.setTextColor(Color.parseColor(headlineColor()))
            resultText.alpha        = 0f
            resultText.translationY = 50f
            resultText.animate().alpha(1f).translationY(0f).setDuration(800).start()

            // Effort/encouragement body
            effortMessage.text  = "${feedback.message}\n\n${feedback.encouragement}"
            effortMessage.alpha = 0f
            handler.postDelayed({
                effortMessage.animate().alpha(1f).setDuration(600).start()
            }, 500)

            // ── Speak feedback (message + encouragement only, no headline) ────
            if (isTtsReady) {
                speakFeedback(feedback.message, feedback.encouragement)
            } else {
                pendingMsg = feedback.message
                pendingEnc = feedback.encouragement
            }

            Log.d(TAG, "AI feedback applied (fromAI=${feedback.fromAI}): ${feedback.headline}")
        }
    }

    /** Returns the hex colour for the headline based on alpha — no text involved. */
    private fun headlineColor(): String = when {
        avgAlpha > 8.0f -> "#FFD700"
        avgAlpha > 5.0f -> "#2196F3"
        else            -> "#4CAF50"
    }

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
    // Candy count description — words only, zero numbers
    // =========================================================================

    private fun candyDescription(score: Int, total: Int): String = when {
        score == 0              -> "Your jar is ready — let's fill it!"
        score <= total / 4      -> "A few yummy candies inside!"
        score <= total / 2      -> "Your jar is filling up nicely!"
        score <= total * 3 / 4  -> "So many colourful candies!"
        score < total           -> "Your jar is almost bursting!"
        else                    -> "Your jar is completely full!"
    }

    // =========================================================================
    // Candy jar animation — unchanged from v2.2.0 (crash-safe)
    // =========================================================================

    private fun setupCandyJarAnimation(score: Int) {
        val displayScore = score.coerceAtLeast(1)
        val fillPct      = (displayScore.toFloat() / totalRounds.toFloat() * 100f).toInt()

        jarFillProgress.max = 100
        ObjectAnimator.ofInt(jarFillProgress, "progress", 0, fillPct).apply {
            duration = 1500; interpolator = AccelerateDecelerateInterpolator(); start()
        }
        handler.postDelayed({ fillJarWithCandies(displayScore) }, 500)

        val sparkleCount = when {
            avgAlpha > 8.0f -> 30; avgAlpha > 5.0f -> 20; avgAlpha > 2.0f -> 10; else -> 5
        }
        createSparkles(sparkleCount)
    }

    private fun fillJarWithCandies(candyCount: Int) {
        try { candyJarImage.setImageResource(R.drawable.jar_low) } catch (_: Exception) {}

        val existingExtras = candyJarContainer.childCount - 1
        if (existingExtras > 0) {
            try { candyJarContainer.removeViews(1, existingExtras) }
            catch (e: Exception) { Log.w(TAG, "removeViews non-fatal: ${e.message}") }
        }

        val jarWidth      = candyJarContainer.width.takeIf  { it > 0 } ?: 800
        val jarHeight     = candyJarContainer.height.takeIf { it > 0 } ?: 800
        val candySize     = (jarWidth * 0.15).toInt()
        val candiesPerRow = 4

        for (i in 0 until candyCount) {
            val row = i / candiesPerRow
            if (row >= 3) break
            val posInRow = i % candiesPerRow
            handler.postDelayed({ addCandyToJar(i, row, posInRow, candySize, jarWidth, jarHeight) }, i * 100L)
        }
        handler.postDelayed({ updateJarImage(candyCount) }, candyCount * 100L + 500)
    }

    private fun addCandyToJar(index: Int, row: Int, position: Int, candySize: Int, jarWidth: Int, jarHeight: Int) {
        if (isFinishing || isDestroyed) return
        val candy = ImageView(this)
        candy.setImageResource(candyDrawables[index % candyDrawables.size])
        candy.layoutParams = FrameLayout.LayoutParams(candySize, candySize)
        val lp        = candy.layoutParams as FrameLayout.LayoutParams
        val jarPad    = (jarWidth * 0.1).toInt()
        val available = jarWidth - jarPad * 2 - candySize
        lp.leftMargin = jarPad + (available * position / 3).coerceAtLeast(0)
        lp.topMargin  = ((jarHeight * 0.65) - row * (jarHeight * 0.25)).toInt().coerceAtLeast(0)
        candy.rotation = Random.nextFloat() * 360
        candyJarContainer.addView(candy)
        candy.translationY = -jarHeight.toFloat()
        candy.scaleX = 0.5f; candy.scaleY = 0.5f
        candy.animate().translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(AccelerateDecelerateInterpolator()).start()
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                candy.animate().translationY(-10f).setDuration(200)
                    .withEndAction { candy.animate().translationY(0f).setDuration(200).start() }.start()
            }
        }, 600)
        candyCountText.text = candyDescription(index + 1, totalRounds)
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
        candyJarImage.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
            .withEndAction { candyJarImage.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }.start()
        createSparkles(30)
        createFallingCandies(15)
    }

    // =========================================================================
    // Sparkles & falling candies — unchanged
    // =========================================================================

    private fun createSparkles(count: Int) {
        for (i in 0 until count) handler.postDelayed({ addRandomSparkle() }, i * 100L)
    }

    private fun addRandomSparkle() {
        if (isFinishing || isDestroyed) return
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(40, 40)
        val lp = sparkle.layoutParams as FrameLayout.LayoutParams
        val cw = sparklesContainer.width.coerceAtLeast(40)
        val ch = sparklesContainer.height.coerceAtLeast(40)
        lp.leftMargin = Random.nextInt(0, cw - 40); lp.topMargin = Random.nextInt(0, ch - 40)
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
        lp.leftMargin = Random.nextInt(0, cw - size); lp.topMargin = -size
        candyJarContainer.addView(candy)
        val fall = candyJarContainer.height + size
        ObjectAnimator.ofFloat(candy, "translationY", 0f, fall.toFloat()).apply { duration = 2000; start() }
        ObjectAnimator.ofFloat(candy, "rotation", 0f, Random.nextFloat() * 720 - 360).apply { duration = 2000; start() }
        handler.postDelayed({ candyJarContainer.removeView(candy) }, 2000)
    }

    private fun showGiftUnlock() {
        Toast.makeText(this, "Special gift unlocked!", Toast.LENGTH_LONG).show()
        createSparkles(20)
    }

    // =========================================================================
    // Buttons — unchanged
    // =========================================================================

    private fun setupButtonListeners() {
        replayButton.setOnClickListener {
            if (::tts.isInitialized && isTtsReady) tts.stop()
            startActivity(Intent(this, RhythmSummaryActivity::class.java).apply {
                putExtra("SONG_TITLE", songTitle)
            })
            finish()
        }

        dashboardButton.setOnClickListener {
            if (::tts.isInitialized && isTtsReady) tts.stop()
            startActivity(Intent(this, GameDashboardActivity::class.java))
            finish()
        }

        candyJarContainer.setOnClickListener {
            candyJarImage.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
                .withEndAction { candyJarImage.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }.start()
            val msgs = listOf("Yum!", "So sweet!", "Delicious!", "Your candies!")
            candyCountText.text = msgs.random()
            handler.postDelayed({ candyCountText.text = candyDescription(score, totalRounds) }, 2000)
        }
    }
}