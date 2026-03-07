package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import android.graphics.Color

/**
 * RhythmSummaryActivity — v9.8.0
 *
 * CHANGES IN THIS VERSION (on top of v9.7.1)
 * ─────────────────────────────────────────
 *  [L] DELAYED FEEDBACK OVERLAY
 *      Previously showFeedback() was called immediately on tap, covering
 *      the card colour highlights before the child could see them.
 *      Fix: card colours are applied instantly on tap, but the feedback
 *      overlay is posted via a FEEDBACK_DELAY_MS (1 000 ms) delay.
 *      The overlay is then shown for FEEDBACK_MS (950 ms) before the
 *      next action (retry reset or round advance).
 *      Total visible time per answer = ~2 seconds of feedback.
 */
class RhythmSummaryActivity : AppCompatActivity() {

    private lateinit var originalSongTitle: String

    private val TAG = "RhythmSummary"

    /** Delay after tap before the feedback overlay appears (card colours show first). */
    private val FEEDBACK_DELAY_MS = 1000L
    /** How long the feedback overlay stays fully visible (ms). */
    private val FEEDBACK_MS       = 950L
    /** Gap between overlay fade-out and the next round starting (ms). */
    private val ADVANCE_GAP_MS    = 150L

    private val handler = Handler(Looper.getMainLooper())
    private var isGameFinished = false

    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    private val AGE_GROUP = 6
    private var consecutiveCorrect = 0
    private var consecutiveWrong   = 0
    private var responseStartTime: Long = 0
    private val responseTimes = mutableListOf<Long>()

    private lateinit var backButton:        ImageView
    private lateinit var wordTitle:         TextView
    private lateinit var scoreText:         TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var optionsGrid:       GridLayout
    private lateinit var feedbackIcon:      ImageView
    private lateinit var feedbackText:      TextView
    private lateinit var feedbackOverlay:   FrameLayout
    private lateinit var levelBadge:        LinearLayout
    private lateinit var levelIndicator:    TextView

    private var currentRound           = 0
    private var score                  = 0
    private val totalRounds            = 5
    private var correctAnswerIndex     = 0
    private var isAnswerSelected       = false
    private var currentDifficultyLevel = 1

    /** True once the child has had one wrong attempt in the current round. */
    private var isRetryAttempt = false

    private val usedKeywords     = mutableSetOf<String>()
    private lateinit var currentKeywords:   List<Keyword>
    private lateinit var availableKeywords: MutableList<Keyword>

    private var correctSound: MediaPlayer? = null
    private var wrongSound:   MediaPlayer? = null

    data class Keyword  (val word: String, val imageRes: Int, val startTime: Int, val endTime: Int)
    data class GameRound(val keyword: String, val correctImageRes: Int, val options: List<Pair<String, Int>>)

    private val keywordImages: Map<String, Int> = KeywordImageMapper.primaryKeywords

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rythm_summary)

        Log.d(TAG, "=== Rhythm Summary Starting ===")
        gameMaster = GameMasterModel(this)
        Log.d(TAG, "✅ GameMaster initialized  modelLoaded=${gameMaster.modelLoaded}")

        initializeViews()
        originalSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"
        initializeAudio()
        setupSongData()
        setupUI()

        handler.postDelayed({ startNewRound() }, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        isGameFinished = true
        handler.removeCallbacksAndMessages(null)
        correctSound?.release()
        wrongSound?.release()
        try { gameMaster.close() } catch (_: Exception) {}
    }

    // =========================================================================
    // Init
    // =========================================================================

    private fun initializeViews() {
        backButton        = findViewById(R.id.backBtn)
        wordTitle         = findViewById(R.id.wordTitle)
        scoreText         = findViewById(R.id.scoreText)
        progressContainer = findViewById(R.id.progressContainer)
        optionsGrid       = findViewById(R.id.optionsGrid)
        feedbackOverlay   = findViewById(R.id.feedbackOverlay)
        feedbackIcon      = findViewById(R.id.feedbackIcon)
        feedbackText      = findViewById(R.id.feedbackText)
        levelBadge        = findViewById(R.id.levelBadge)
        levelIndicator    = findViewById(R.id.levelIndicator)

        backButton.setOnClickListener {
            isGameFinished = true
            handler.removeCallbacksAndMessages(null)
            startActivity(
                Intent(this, SongSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
            finish()
        }
    }

    private fun initializeAudio() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct_sound)
            wrongSound   = MediaPlayer.create(this, R.raw.wrong_sound)
        } catch (e: Exception) {
            Log.w(TAG, "Audio init failed: ${e.message}")
        }
    }

    private fun setupSongData() {
        currentKeywords = when (originalSongTitle) {
            "Twinkle Twinkle Little Star", "Twinkle Twinkle" -> listOf(
                Keyword("star",    R.drawable.rhy_song1_star,    8000,  10000),
                Keyword("world",   R.drawable.rhy_song1_world,   16000, 18000),
                Keyword("diamond", R.drawable.rhy_song1_diamond, 19000, 20000),
                Keyword("sun",     R.drawable.rhy_song1_sun,     40000, 42000),
                Keyword("light",   R.drawable.rhy_song1_light,   48000, 50000),
                Keyword("night",   R.drawable.rhy_song1_moon,    53000, 55000)
            )
            "Jack and Jill" -> listOf(
                Keyword("hill",  R.drawable.hill_image,  10500, 11000),
                Keyword("water", R.drawable.water_image, 12500, 14000),
                Keyword("crown", R.drawable.crown_image, 14500, 16000)
            )
            else -> listOf(
                Keyword("boat",       R.drawable.rhy_song0_boat,       11000, 12000),
                Keyword("stream",     R.drawable.rhy_song0_stream,     13000, 15000),
                Keyword("dream",      R.drawable.rhy_song0_dream,      18000, 20000),
                Keyword("creek",      R.drawable.rhy_song0_creek,      24000, 25000),
                Keyword("mouse",      R.drawable.rhy_song0_mouse,      27000, 28000),
                Keyword("river",      R.drawable.rhy_song0_river,      34000, 36000),
                Keyword("polar bear", R.drawable.rhy_song0_polar_bear, 37000, 38000),
                Keyword("crocodile",  R.drawable.rhy_song0_crocodile,  48000, 49000)
            )
        }
        availableKeywords = currentKeywords.toMutableList()
    }

    private fun setupUI() {
        setupProgressDots()
        updateScore()
        updateLevelBadge()
    }

    private fun setupProgressDots() {
        progressContainer.removeAllViews()
        for (i in 0 until totalRounds) {
            val dot = View(this).apply {
                val s = 16.dpToPx()
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    marginEnd = if (i < totalRounds - 1) 8.dpToPx() else 0
                }
                background = ContextCompat.getDrawable(this@RhythmSummaryActivity, R.drawable.circular_dot)
                setBackgroundColor(dotColor(i))
            }
            progressContainer.addView(dot)
        }
    }

    private fun updateProgressDots() {
        for (i in 0 until progressContainer.childCount)
            progressContainer.getChildAt(i).setBackgroundColor(dotColor(i))
    }

    private fun dotColor(i: Int) = when {
        i == currentRound -> Color.parseColor("#4CAF50")
        i < currentRound  -> Color.parseColor("#FF6B35")
        else              -> Color.parseColor("#BDBDBD")
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    // =========================================================================
    // Round logic
    // =========================================================================

    private fun startNewRound() {
        if (isGameFinished) return
        if (currentRound >= totalRounds) {
            isGameFinished = true
            navigateToScoreboard()
            return
        }

        Log.d(TAG, "=== ROUND ${currentRound + 1}/$totalRounds ===")

        hideFeedbackNow()

        isAnswerSelected = false
        isRetryAttempt   = false
        optionsGrid.removeAllViews()

        val accuracy    = if (currentRound > 0) score.toFloat() / currentRound else 0.5f
        val avgRT       = if (responseTimes.isNotEmpty()) responseTimes.average().toFloat() else 3000f
        val engagement  = if (accuracy > 0.7f) 0.8f else 0.5f
        val frustration = if (consecutiveWrong >= 2) 0.7f else 0.2f

        currentPrediction = gameMaster.predictSafe(
            childId            = 0,
            age                = AGE_GROUP.toFloat(),
            accuracy           = accuracy,
            engagement         = engagement,
            frustration        = frustration,
            rt                 = avgRT,
            consecutiveCorrect = consecutiveCorrect.toFloat(),
            consecutiveWrong   = consecutiveWrong.toFloat()
        )

        Log.d(TAG, "🤖 PREDICTION (fromModel=${currentPrediction.fromModel})")
        Log.d(TAG, "🎵 Song locked to: $originalSongTitle")
        updateLevelBadge()

        val isScaffolding = consecutiveWrong >= 2
        val optionCount   = if (isScaffolding) 2 else 4
        if (isScaffolding) Log.d(TAG, "⚠️ scaffolding to 2 options")

        val keyword         = getUniqueKeyword()
        val correctImageRes = keywordImages[keyword.word]
            ?: KeywordImageMapper.getImageResource(keyword.word)

        val wrongOptions = buildDistractors(keyword.word, accuracy, isScaffolding, optionCount - 1)
        val gameRound    = createGameRound(keyword.word, correctImageRes, wrongOptions)

        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        displayOptions(gameRound, optionCount)
        updateProgressDots()
        responseStartTime = System.currentTimeMillis()
    }

    private fun buildDistractors(
        keyword: String,
        accuracy: Float,
        isScaffolding: Boolean,
        count: Int
    ): List<Pair<String, Int>> {

        fun padWithRandom(list: List<Pair<String, Int>>): List<Pair<String, Int>> {
            if (list.size >= count) return list.take(count)
            val used = list.map { it.first }.toSet() + keyword.lowercase()
            val extra = KeywordImageMapper.getDistractors(keyword, 0, count - list.size + 3)
                .filter { it.first !in used }
                .take(count - list.size)
            return (list + extra).take(count)
        }

        return when {
            isScaffolding || accuracy < 0.40f -> KeywordImageMapper.getDistractors(keyword, 0, count)
            accuracy < 0.70f -> {
                val random   = KeywordImageMapper.getDistractors(keyword, 0, 1)
                val semantic = KeywordImageMapper.getDistractors(keyword, 2, 1)
                    .filter { it.first != random.firstOrNull()?.first }
                val phonetic = KeywordImageMapper.getDistractors(keyword, 1, 1)
                    .filter { p -> p.first !in (random + semantic).map { it.first } }
                padWithRandom((random + semantic + phonetic).distinctBy { it.first })
            }
            else -> {
                val semantic = KeywordImageMapper.getDistractors(keyword, 2, 2)
                val phonetic = KeywordImageMapper.getDistractors(keyword, 1, 1)
                    .filter { p -> p.first !in semantic.map { it.first } }
                padWithRandom((semantic + phonetic).distinctBy { it.first })
            }
        }
    }

    private fun getUniqueKeyword(): Keyword {
        if (availableKeywords.isEmpty() || usedKeywords.size >= currentKeywords.size * 0.7) {
            availableKeywords = currentKeywords.toMutableList()
            usedKeywords.clear()
        }
        val available = availableKeywords.filter { it.word !in usedKeywords }
        val keyword   = if (available.isNotEmpty()) available.random() else currentKeywords.random()
        usedKeywords.add(keyword.word)
        availableKeywords.remove(keyword)
        return keyword
    }

    private fun createGameRound(
        keyword: String,
        correctImageRes: Int,
        wrongOptions: List<Pair<String, Int>>
    ): GameRound {
        val allOptions = mutableListOf(Pair(keyword, correctImageRes))
        allOptions.addAll(wrongOptions)
        val shuffled = allOptions.shuffled()
        correctAnswerIndex = shuffled.indexOfFirst { it.first == keyword }
        return GameRound(keyword, correctImageRes, shuffled)
    }

    // =========================================================================
    // Option display
    // =========================================================================

    private fun displayOptions(gameRound: GameRound, optionCount: Int) {
        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()
        gameRound.options.take(optionCount).forEachIndexed { i, option ->
            createOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }
    }

    private fun createOptionCard(
        index: Int,
        word: String,
        imageResId: Int,
        size: Int,
        correctKeyword: String
    ) {
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width      = size
                height     = size
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec    = GridLayout.spec(index / 2)
                setMargins(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
            }
            radius        = 16.dpToPx().toFloat()
            cardElevation = 4.dpToPx().toFloat()
            isClickable   = true
            tag           = index
            setCardBackgroundColor(Color.WHITE)

            setOnClickListener {
                if (isGameFinished) return@setOnClickListener
                if (!isAnswerSelected && isEnabled) {
                    val rt = System.currentTimeMillis() - responseStartTime
                    responseTimes.add(rt)
                    handleOptionClick(this, index, word, correctKeyword, rt)
                }
            }
        }

        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setImageResource(imageResId)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(16, 16, 16, 16)
        }

        card.addView(imageView)
        optionsGrid.addView(card)

        card.alpha = 0f; card.translationY = 100f
        card.animate().alpha(1f).translationY(0f)
            .setDuration(500).setStartDelay(index * 100L).start()
    }

    // =========================================================================
    // Click handling  [FIX L — delayed feedback overlay]
    // =========================================================================

    private fun handleOptionClick(
        card: CardView,
        selectedIndex: Int,
        selectedWord: String,
        correctKeyword: String,
        responseTime: Long
    ) {
        if (!card.isClickable || isAnswerSelected) return

        if (selectedIndex == correctAnswerIndex) {
            // ── CORRECT ────────────────────────────────────────────────────
            isAnswerSelected = true
            disableAllCards()

            if (!isRetryAttempt) {
                score++
                consecutiveCorrect++
                consecutiveWrong = 0
            }

            // 1. Colour the card immediately so the child can see it.
            card.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
            playSound(correctSound)
            updateScore()
            updateProgressDots()

            // 2. After FEEDBACK_DELAY_MS, show overlay then advance.
            handler.postDelayed({
                showFeedback(correct = true)
                handler.postDelayed({
                    hideFeedbackAnimated { advanceRound() }
                }, FEEDBACK_MS)
            }, FEEDBACK_DELAY_MS)

        } else {
            // ── WRONG ──────────────────────────────────────────────────────
            if (!isRetryAttempt) {
                // First wrong — colour cards immediately, overlay after delay.
                disableAllCards()
                consecutiveWrong++
                consecutiveCorrect = 0

                card.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                colorCorrectCard(green = true)
                playSound(wrongSound)
                updateProgressDots()

                isRetryAttempt = true

                handler.postDelayed({
                    showFeedback(correct = false)
                    handler.postDelayed({
                        hideFeedbackAnimated { resetCardsForRetry(wrongCard = card) }
                    }, FEEDBACK_MS)
                }, FEEDBACK_DELAY_MS)

            } else {
                // Second wrong — colour cards immediately, overlay after delay, then advance.
                isAnswerSelected = true
                disableAllCards()
                card.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                colorCorrectCard(green = true)
                playSound(wrongSound)

                handler.postDelayed({
                    showFeedback(correct = false)
                    handler.postDelayed({
                        hideFeedbackAnimated { advanceRound() }
                    }, FEEDBACK_MS)
                }, FEEDBACK_DELAY_MS)
            }
        }
    }

    // =========================================================================
    // Card colour helpers
    // =========================================================================

    private fun colorCorrectCard(green: Boolean) {
        (optionsGrid.getChildAt(correctAnswerIndex) as? CardView)?.let { c ->
            if (green) {
                c.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
                c.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
            } else {
                c.setCardBackgroundColor(Color.WHITE)
                c.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
        }
    }

    /**
     * Resets ALL cards to white and re-enables them all.
     * No colour hint remains — the retry is a genuine fresh attempt.
     */
    private fun resetCardsForRetry(wrongCard: CardView) {
        if (isGameFinished) return
        for (i in 0 until optionsGrid.childCount) {
            val c = optionsGrid.getChildAt(i) as? CardView ?: continue
            c.setCardBackgroundColor(Color.WHITE)
            c.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            c.isClickable = true
        }
    }

    private fun disableAllCards() {
        for (i in 0 until optionsGrid.childCount)
            (optionsGrid.getChildAt(i) as? CardView)?.isClickable = false
    }

    // =========================================================================
    // Feedback overlay
    // =========================================================================

    private fun showFeedback(correct: Boolean) {
        if (correct) {
            feedbackIcon.setImageResource(R.drawable.correct_answer)
            feedbackText.text = "Well done!"
            feedbackText.setTextColor(Color.parseColor("#388E3C"))
        } else {
            feedbackIcon.setImageResource(R.drawable.delete)
            feedbackText.text = "Try again!"
            feedbackText.setTextColor(Color.parseColor("#D32F2F"))
        }
        feedbackIcon.visibility    = View.VISIBLE
        feedbackText.visibility    = View.VISIBLE
        feedbackOverlay.visibility = View.VISIBLE
        feedbackOverlay.animate().cancel()
        feedbackOverlay.alpha = 0f
        feedbackOverlay.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideFeedbackAnimated(onDone: () -> Unit) {
        if (feedbackOverlay.visibility != View.VISIBLE || feedbackOverlay.alpha == 0f) {
            feedbackOverlay.visibility = View.GONE
            feedbackIcon.visibility    = View.GONE
            feedbackText.visibility    = View.GONE
            onDone()
            return
        }
        feedbackOverlay.animate().cancel()
        feedbackOverlay.animate().alpha(0f).setDuration(200).withEndAction {
            feedbackOverlay.visibility = View.GONE
            feedbackIcon.visibility    = View.GONE
            feedbackText.visibility    = View.GONE
            onDone()
        }.start()
    }

    private fun hideFeedbackNow() {
        feedbackOverlay.animate().cancel()
        feedbackOverlay.alpha      = 0f
        feedbackOverlay.visibility = View.GONE
        feedbackIcon.visibility    = View.GONE
        feedbackText.visibility    = View.GONE
    }

    // =========================================================================
    // Round advance
    // =========================================================================

    private fun advanceRound() {
        if (isGameFinished) return
        currentRound++
        Log.d(TAG, "advanceRound → currentRound=$currentRound / $totalRounds")
        if (currentRound >= totalRounds) {
            isGameFinished = true
            navigateToScoreboard()
            return
        }
        handler.postDelayed({ startNewRound() }, ADVANCE_GAP_MS)
    }

    // =========================================================================
    // Audio / score / badge
    // =========================================================================

    private fun playSound(player: MediaPlayer?) {
        try {
            player?.let { if (it.isPlaying) { it.stop(); it.prepare() }; it.start() }
        } catch (e: Exception) { Log.w(TAG, "Sound error: ${e.message}") }
    }

    private fun updateScore() {
        scoreText.text = "$score/$totalRounds"
        scoreText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
            .withEndAction { scoreText.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }
            .start()
    }

    private fun updateLevelBadge() {
        val level = when {
            currentPrediction.rhythmComplexity < 0.33f -> 1
            currentPrediction.rhythmComplexity < 0.66f -> 2
            else                                        -> 3
        }
        val (label, colorRes) = when (level) {
            1    -> "Easy"   to R.color.green_dark
            2    -> "Medium" to R.color.dark_orange
            else -> "Hard"   to R.color.pink
        }
        currentDifficultyLevel = level
        levelIndicator.text    = label
        levelBadge.background?.mutate()?.setTint(ContextCompat.getColor(this, colorRes))
        levelBadge.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200)
            .withEndAction { levelBadge.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }
            .start()
    }

    // =========================================================================
    // Scoreboard navigation
    // =========================================================================

    private fun navigateToScoreboard() {
        handler.removeCallbacksAndMessages(null)

        val accuracy = if (currentRound > 0) score.toFloat() / currentRound else 0.5f
        val avgRT    = if (responseTimes.isNotEmpty()) responseTimes.average().toFloat() else 3000f

        val alpha = try {
            gameMaster.calculateAlpha(
                accuracy     = accuracy,
                responseTime = avgRT,
                age          = AGE_GROUP,
                errorCount   = consecutiveWrong
            )
        } catch (e: Exception) {
            Log.w(TAG, "calculateAlpha unavailable: ${e.message}")
            0f
        }

        Log.d(TAG, "Navigating to scoreboard — score=$score/$totalRounds  alpha=$alpha")

        try {
            startActivity(
                Intent(this, RMScoreboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("SCORE",               score)
                    putExtra("TOTAL_ROUNDS",        totalRounds)
                    putExtra("SONG_TITLE",          originalSongTitle)
                    putExtra("FINAL_LEVEL",         currentDifficultyLevel)
                    putExtra("MOTIVATION_ID",       currentPrediction.motivation)
                    putExtra("UNLOCK_GIFT",         currentPrediction.friendAction > 0)
                    putExtra("AVG_ALPHA",           alpha)
                    putExtra("AVG_RESPONSE_TIME",   avgRT)
                    putExtra("CONSECUTIVE_CORRECT", consecutiveCorrect)
                    putExtra("CONSECUTIVE_WRONG",   consecutiveWrong)
                    putExtra("ENGAGEMENT_SCORE",    if (accuracy > 0.7f) 0.8f else 0.5f)
                    putExtra("FRUSTRATION_LEVEL",   if (consecutiveWrong >= 2) 0.7f else 0.2f)
                    putExtra("ACCURACY",            accuracy)
                }
            )
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "navigateToScoreboard FAILED: ${e.message}", e)
            try {
                startActivity(
                    Intent(this, GameDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
            } catch (_: Exception) {}
            finish()
        }
    }
}