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
 * RhythmSummaryActivity — v9.5.0
 *
 * FIXES IN THIS VERSION (on top of v9.4.0)
 * ─────────────────────────────────────────
 *  [A] KEYWORD POOL LOCKED TO THE SONG THAT WAS PLAYED
 *      Previously the GameMaster prediction could switch currentSongTitle
 *      mid-game, which caused keywords from Row Row / Jack & Jill to appear
 *      inside a Twinkle Twinkle session.
 *      Fix: currentSongTitle is now stored once (from intent) as
 *      originalSongTitle and NEVER changed. The difficulty level (badge)
 *      still adapts from the model prediction; only the keyword pool is
 *      protected.
 *
 *  [B] TWO-OPTION CARDS ARE NOW SQUARE (same size as 4-option cards)
 *      Root cause: rowSpec = GridLayout.spec(index / 2, 1f)
 *      The 1f row-weight caused GridLayout to stretch the single row to
 *      fill all remaining height → tall rectangle instead of square.
 *      Fix: rowSpec uses no weight → GridLayout respects the explicit
 *      height = size set in LayoutParams → perfect square for both
 *      2-option and 4-option modes.
 *
 *  All other behaviour from v9.4.0 (performance-based distractor strategy,
 *  scaffolding, crash fix for RMScoreboardActivity) is unchanged.
 */
class RhythmSummaryActivity : AppCompatActivity() {

    // [FIX A] Song title never changes after being read from the intent.
    private lateinit var originalSongTitle: String

    private val TAG = "RhythmSummary"

    private val handler = Handler(Looper.getMainLooper())
    private var isGameFinished = false

    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    private val AGE_GROUP = 6
    private var consecutiveCorrect = 0
    private var consecutiveWrong   = 0
    private var responseStartTime: Long = 0
    private val responseTimes = mutableListOf<Long>()

    private lateinit var pandaImage:        ImageView
    private lateinit var wordTitle:         TextView
    private lateinit var scoreText:         TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var optionsGrid:       GridLayout
    private lateinit var feedbackIcon:      ImageView
    private lateinit var feedbackText:      TextView
    private lateinit var feedbackOverlay:   FrameLayout
    private lateinit var nextButton:        Button
    private lateinit var levelBadge: LinearLayout
    private lateinit var levelIndicator:    TextView

    private var currentRound           = 0
    private var score                  = 0
    private val totalRounds            = 5
    private var correctAnswerIndex     = 0
    private var isAnswerSelected       = false
    private var currentDifficultyLevel = 1

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

        // [FIX A] Read once; never overwrite from model prediction.
        originalSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"

        initializeAudio()
        setupSongData()
        setupUI()

        handler.postDelayed({ startNewRound() }, 1000)
        nextButton.setOnClickListener { onNextButtonClick() }
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
        pandaImage        = findViewById(R.id.pandaImage)
        wordTitle         = findViewById(R.id.wordTitle)
        scoreText         = findViewById(R.id.scoreText)
        progressContainer = findViewById(R.id.progressContainer)
        optionsGrid       = findViewById(R.id.optionsGrid)
        feedbackOverlay   = findViewById(R.id.feedbackOverlay)
        feedbackIcon      = findViewById(R.id.feedbackIcon)
        feedbackText      = findViewById(R.id.feedbackText)
        nextButton        = findViewById(R.id.nextButton)
        levelBadge        = findViewById(R.id.levelBadge)
        levelIndicator    = findViewById(R.id.levelIndicator)
    }

    private fun initializeAudio() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct_sound)
            wrongSound   = MediaPlayer.create(this, R.raw.wrong_sound)
        } catch (e: Exception) {
            Log.w(TAG, "Audio init failed: ${e.message}")
        }
    }

    // Keywords are always loaded from the original song — never switched.
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
        animatePanda(); setupProgressDots(); updateScore(); updateLevelBadge()
    }

    private fun animatePanda() {
        pandaImage.animate().translationY(-20f).setDuration(1000)
            .withEndAction { pandaImage.animate().translationY(0f).setDuration(1000).start() }
            .start()
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
        isAnswerSelected = false
        nextButton.visibility = View.GONE
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

        Log.d(TAG, "🤖 PREDICTION (fromModel=${currentPrediction.fromModel}):")
        Log.d(TAG, "   rhythmComplexity = ${currentPrediction.rhythmComplexity}")
        Log.d(TAG, "   distractor       = ${currentPrediction.distractor}")
        Log.d(TAG, "   nextAccuracy     = ${currentPrediction.nextAccuracy}")
        Log.d(TAG, "   frustrationRisk  = ${currentPrediction.frustrationRisk}")

        // [FIX A] Difficulty badge can still adapt, but the keyword pool
        // always stays tied to originalSongTitle — no song switch allowed.
        Log.d(TAG, "🎵 Song locked to: $originalSongTitle (prediction ignored for song switch)")
        updateLevelBadge()

        // ── Scaffolding: 2-column grid, only 2 cards when struggling ─
        val isScaffolding = consecutiveWrong >= 2
        val optionCount   = if (isScaffolding) 2 else 4
        if (isScaffolding) Log.d(TAG, "⚠️ consecutiveWrong=$consecutiveWrong → scaffolding to 2 options")

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

    // Performance-based distractor builder (unchanged from v9.4.0)
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
            isScaffolding || accuracy < 0.40f -> {
                Log.d(TAG, "   distractor strategy: ALL RANDOM (poor/scaffold)")
                KeywordImageMapper.getDistractors(keyword, 0, count)
            }
            accuracy < 0.70f -> {
                Log.d(TAG, "   distractor strategy: MIXED random+semantic+phonetic (average)")
                val random   = KeywordImageMapper.getDistractors(keyword, 0, 1)
                val semantic = KeywordImageMapper.getDistractors(keyword, 2, 1)
                    .filter { it.first != random.firstOrNull()?.first }
                val phonetic = KeywordImageMapper.getDistractors(keyword, 1, 1)
                    .filter { p -> p.first !in (random + semantic).map { it.first } }
                padWithRandom((random + semantic + phonetic).distinctBy { it.first })
            }
            else -> {
                Log.d(TAG, "   distractor strategy: SEMANTIC+PHONETIC (good)")
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
        // Card size is the same whether 2 or 4 options are shown.
        // Half the screen width minus margins → square cards in a 2-column grid.
        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()
        gameRound.options.take(optionCount).forEachIndexed { i, option ->
            createOptionCard(i, option.first, option.second, optionSize, gameRound.keyword)
        }
    }

    // [FIX B] rowSpec no longer carries a weight (1f).
    // Without the weight, GridLayout respects the explicit height = size
    // set in LayoutParams, giving a perfect square for both 2- and 4-option modes.
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
                height     = size                                    // explicit square height
                columnSpec = GridLayout.spec(index % 2, 1f)         // column weight keeps 2-col layout
                rowSpec    = GridLayout.spec(index / 2)              // [FIX B] no row weight → height respected
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
    // Click handling
    // =========================================================================

    private fun handleOptionClick(
        card: CardView,
        selectedIndex: Int,
        selectedWord: String,
        correctKeyword: String,
        responseTime: Long
    ) {
        if (!card.isClickable || isAnswerSelected) return
        isAnswerSelected = true
        disableAllCards()

        if (selectedIndex == correctAnswerIndex) {
            score++; consecutiveCorrect++; consecutiveWrong = 0
            showFeedback(true, card)
            playSound(correctSound)
        } else {
            consecutiveWrong++; consecutiveCorrect = 0
            showFeedback(false, card)
            highlightCorrectAnswer()
            playSound(wrongSound)
        }

        updateScore()
        updateProgressDots()
        handler.postDelayed({ hideFeedbackAndShowNextButton() }, 1500)
    }

    private fun disableAllCards() {
        for (i in 0 until optionsGrid.childCount)
            (optionsGrid.getChildAt(i) as? CardView)?.isClickable = false
    }

    private fun highlightCorrectAnswer() {
        (optionsGrid.getChildAt(correctAnswerIndex) as? CardView)?.let {
            it.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
            it.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
        }
    }

    private fun showFeedback(isCorrect: Boolean, selectedCard: CardView) {
        if (isCorrect) {
            selectedCard.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
            feedbackIcon.setImageResource(R.drawable.correct_answer)
            feedbackText.text = "Well done!"
            feedbackText.setTextColor(Color.parseColor("#388E3C"))
        } else {
            selectedCard.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
            feedbackIcon.setImageResource(R.drawable.delete)
            feedbackText.text = "Try again!"
            feedbackText.setTextColor(Color.parseColor("#D32F2F"))
        }
        feedbackIcon.visibility    = View.VISIBLE
        feedbackText.visibility    = View.VISIBLE
        feedbackOverlay.visibility = View.VISIBLE
        feedbackOverlay.alpha      = 0f
        feedbackOverlay.animate().alpha(1f).setDuration(300).start()
    }

    private fun hideFeedbackAndShowNextButton() {
        if (isGameFinished) return
        feedbackOverlay.animate().alpha(0f).setDuration(300).withEndAction {
            feedbackOverlay.visibility = View.GONE
            feedbackIcon.visibility    = View.GONE
            feedbackText.visibility    = View.GONE
        }.start()
        nextButton.visibility = View.VISIBLE
        nextButton.alpha      = 0f
        nextButton.animate().alpha(1f).setDuration(300).start()
    }

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
            1    -> "Easy"   to R.color.green_dark   // #FF6B35
            2    -> "Medium" to R.color.dark_orange      // #4A90E2
            else -> "Hard"   to R.color.pink            // your pink
        }

        currentDifficultyLevel = level
        levelIndicator.text    = label

        // mutate() prevents the tint from affecting other views that share
        // this same drawable reference. setTint() recolours the layer-list
        // shape without replacing it — so the flag corners are preserved.
        levelBadge.background
            ?.mutate()
            ?.setTint(ContextCompat.getColor(this, colorRes))

        levelBadge.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200)
            .withEndAction { levelBadge.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }
            .start()
    }

    // =========================================================================
    // Next button
    // =========================================================================

    private fun onNextButtonClick() {
        if (isGameFinished) return
        nextButton.visibility = View.GONE
        currentRound++
        Log.d(TAG, "onNextButtonClick → currentRound=$currentRound / $totalRounds")
        if (currentRound >= totalRounds) {
            isGameFinished = true
            navigateToScoreboard()
            return
        }
        startNewRound()
    }

    // =========================================================================
    // Scoreboard navigation — FLAG_ACTIVITY_CLEAR_TASK (unchanged from v9.4.0)
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
            Log.e(TAG,  "navigateToScoreboard FAILED: ${e.message}", e)
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