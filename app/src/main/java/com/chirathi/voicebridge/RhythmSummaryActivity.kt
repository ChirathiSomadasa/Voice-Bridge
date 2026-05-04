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

class RhythmSummaryActivity : AppCompatActivity() {

    private val TAG = "RhythmSummary"

    private val FEEDBACK_DELAY_MS = 1000L
    private val FEEDBACK_MS       = 950L
    private val ADVANCE_GAP_MS    = 150L

    private val handler = Handler(Looper.getMainLooper())
    private var isGameFinished = false

    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()
    private val tracker = SessionStateTracker(ageGroup = ChildSession.ageGroup)

    private lateinit var originalSongTitle: String
    private var progressTier = RhythmFlashcardManager.ProgressTier.LOW

    private var summaryKeywordPool: List<RhythmFlashcardManager.SongKeyword> = emptyList()
    private val cyclingQueue = mutableListOf<RhythmFlashcardManager.SongKeyword>()

    private var currentRound           = 0
    private var score                  = 0
    private val totalRounds            = 5
    private var correctAnswerIndex     = 0
    private var isAnswerSelected       = false
    private var currentDifficultyLevel = 1
    private var isRetryAttempt         = false
    private var consecutiveCorrect     = 0
    private var consecutiveWrong       = 0

    // Tracks failures per whole flashcard round, not per click
    private var consecutiveFailedFlashcards = 0

    private val responseTimes          = mutableListOf<Long>()
    private var responseStartTime      = 0L

    private lateinit var backButton:        ImageView
    private lateinit var wordTitle:         TextView
    private lateinit var scoreText:         TextView
    private lateinit var progressContainer: LinearLayout
    private lateinit var optionsGrid:       GridLayout
    private lateinit var feedbackOverlay:   FrameLayout
    private lateinit var feedbackIcon:      ImageView
    private lateinit var feedbackText:      TextView
    private lateinit var levelBadge:        LinearLayout
    private lateinit var levelIndicator:    TextView

    private var correctSound: MediaPlayer? = null
    private var wrongSound:   MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rythm_summary)

        if (!ChildSession.isInitialized) ChildSession.restore(this)

        gameMaster = GameMasterModel(this)

        originalSongTitle = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"

        val shownWords = intent.getStringArrayListExtra(
            RhythmFlashcardManager.EXTRA_SHOWN_KEYWORDS) ?: arrayListOf()
        val tierName   = intent.getStringExtra(
            RhythmFlashcardManager.EXTRA_PROGRESS_TIER) ?: "LOW"

        progressTier = runCatching {
            RhythmFlashcardManager.ProgressTier.valueOf(tierName)
        }.getOrDefault(RhythmFlashcardManager.ProgressTier.LOW)

        var shownKeywords = RhythmFlashcardManager.fromWordList(shownWords, originalSongTitle)

        if (shownKeywords.isEmpty()) {
            val profile      = ChildProfileManager.load(this, ChildSession.childId)
            val fallbackTier = RhythmFlashcardManager.determineTierFromProfile(profile)
            progressTier     = fallbackTier
            shownKeywords    = RhythmFlashcardManager.selectSongFlashcards(originalSongTitle, fallbackTier)
        }

        summaryKeywordPool = RhythmFlashcardManager.selectSummaryKeywords(shownKeywords, progressTier)
        cyclingQueue.addAll(summaryKeywordPool.shuffled())

        SessionLogger.logRhythmSession(
            context      = this,
            childId      = ChildSession.childId,
            songTitle    = originalSongTitle,
            tier         = progressTier,
            shownCount   = shownKeywords.size,
            summaryWords = summaryKeywordPool.map { it.word },
            score        = 0,
            totalRounds  = totalRounds
        )

        initializeViews()
        initializeAudio()
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
            startActivity(Intent(this, SongSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
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

    private fun setupUI() {
        setupProgressDots()
        updateScore()
        updateLevelBadge()
    }

    private fun startNewRound() {
        if (isGameFinished) return
        if (currentRound >= totalRounds) {
            isGameFinished = true; navigateToScoreboard(); return
        }

        hideFeedbackNow()
        isAnswerSelected = false
        isRetryAttempt   = false
        optionsGrid.removeAllViews()

        val accuracy = if (currentRound > 0) score.toFloat() / currentRound else 0.5f
        val avgRT    = if (responseTimes.isNotEmpty()) responseTimes.average().toFloat() else 3000f

        currentPrediction = gameMaster.predictSafe(
            childId            = ChildSession.childId,
            age                = ChildSession.age.toFloat(),
            accuracy           = accuracy,
            engagement         = tracker.engagement,
            frustration        = tracker.frustration,
            rt                 = avgRT,
            consecutiveCorrect = consecutiveCorrect.toFloat(),
            consecutiveWrong   = consecutiveWrong.toFloat()
        )

        val keyword = getNextSummaryKeyword()

        // 1. Determine Option Count - STRICTLY 4 BY DEFAULT
        var optionCount = 4

        // 2. Determine Distractor Type
        var distractorType = currentPrediction.distractor

        // 3. OVERRIDE RULE: 2 wrong flashcard rounds in a row -> Easy mode!
        if (consecutiveFailedFlashcards >= 2) {
            optionCount = 2
            distractorType = 0 // Forces randomDistractors (apple, ball, cat, etc.)
            Log.d(TAG, "2+ consecutive failed flashcards! Forcing 2 options and easy distractors.")
        }

        val wrongOptions = buildDistractors(keyword.word, distractorType, optionCount - 1)
        val gameRound    = createGameRound(keyword.word, keyword.imageRes, wrongOptions)

        updateLevelBadge()
        wordTitle.text = "Find: ${keyword.word.uppercase()}"
        displayOptions(gameRound, optionCount)
        updateProgressDots()
        responseStartTime = System.currentTimeMillis()

        SessionLogger.logPrediction(
            context   = this,
            childId   = ChildSession.childId,
            round     = currentRound,
            gameType  = "rhythm",
            ageGroup  = ChildSession.ageGroup,
            features  = gameMaster.lastFeatureVector(),
            pred      = currentPrediction,
            diffLabel = "tier=${progressTier.name}-d${keyword.difficulty}"
        )
    }

    private fun getNextSummaryKeyword(): RhythmFlashcardManager.SongKeyword {
        if (summaryKeywordPool.isEmpty()) {
            val fallback = RhythmFlashcardManager.SONG_KEYWORDS["Row Row Row Your Boat"]!!
            summaryKeywordPool = fallback.take(5)
        }
        if (cyclingQueue.isEmpty()) {
            cyclingQueue.addAll(summaryKeywordPool.shuffled())
        }
        return cyclingQueue.removeFirst()
    }

    private fun buildDistractors(keyword: String, type: Int, count: Int): List<Pair<String, Int>> {
        return KeywordImageMapper.getDistractors(
            keyword = keyword,
            type    = type,
            count   = count
        )
    }

    private fun createGameRound(
        keyword:         String,
        correctImageRes: Int,
        wrongOptions:    List<Pair<String, Int>>
    ): Triple<String, Int, List<Pair<String, Int>>> {
        val allOptions = mutableListOf(Pair(keyword, correctImageRes))
        allOptions.addAll(wrongOptions)
        val shuffled = allOptions.shuffled()
        correctAnswerIndex = shuffled.indexOfFirst { it.first == keyword }
        return Triple(keyword, correctImageRes, shuffled)
    }

    private fun displayOptions(
        gameRound:   Triple<String, Int, List<Pair<String, Int>>>,
        optionCount: Int
    ) {
        val optionSize = resources.displayMetrics.widthPixels / 2 - 48.dpToPx()
        gameRound.third.take(optionCount).forEachIndexed { i, option ->
            createOptionCard(i, option.first, option.second, optionSize, gameRound.first)
        }
    }

    private fun createOptionCard(
        index:          Int,
        word:           String,
        imageResId:     Int,
        size:           Int,
        correctKeyword: String
    ) {
        val card = CardView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width      = size; height = size
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
                if (isGameFinished || isAnswerSelected || !isEnabled) return@setOnClickListener
                val rt = System.currentTimeMillis() - responseStartTime
                responseTimes.add(rt)
                handleOptionClick(this, index, word, correctKeyword, rt)
            }
        }
        card.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setImageResource(imageResId)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(16, 16, 16, 16)
        })
        optionsGrid.addView(card)
        card.alpha = 0f; card.translationY = 100f
        card.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(index * 100L).start()
    }

    private fun handleOptionClick(
        card:           CardView,
        selectedIndex:  Int,
        selectedWord:   String,
        correctKeyword: String,
        responseTime:   Long
    ) {
        if (!card.isClickable || isAnswerSelected) return

        if (selectedIndex == correctAnswerIndex) {
            isAnswerSelected = true
            disableAllCards()

            if (!isRetryAttempt) {
                score++
                consecutiveCorrect++
                consecutiveWrong = 0
                consecutiveFailedFlashcards = 0 // Reset flashcard streak!
            } else {
                consecutiveFailedFlashcards++ // They failed the 1st try of this flashcard
            }

            tracker.update(
                wasCorrect       = true,
                attemptNumber    = if (isRetryAttempt) 2 else 1,
                modelFrustRisk   = currentPrediction.frustrationRisk,
                consecutiveWrong = consecutiveWrong
            )
            tracker.recordFeatureVector(gameMaster.lastFeatureVector())

            card.setCardBackgroundColor(Color.parseColor("#C8E6C9"))
            playSound(correctSound)
            updateScore()
            updateProgressDots()

            handler.postDelayed({
                showFeedback(correct = true)
                handler.postDelayed({ hideFeedbackAnimated { advanceRound() } }, FEEDBACK_MS)
            }, FEEDBACK_DELAY_MS)

        } else {
            if (!isRetryAttempt) {
                disableAllCards()
                consecutiveWrong++
                consecutiveCorrect = 0

                tracker.update(
                    wasCorrect       = false,
                    attemptNumber    = 1,
                    modelFrustRisk   = currentPrediction.frustrationRisk,
                    consecutiveWrong = consecutiveWrong
                )

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
                isAnswerSelected = true
                disableAllCards()
                consecutiveWrong++
                consecutiveCorrect = 0
                consecutiveFailedFlashcards++ // Failed both tries of this flashcard

                card.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                colorCorrectCard(green = true)
                playSound(wrongSound)

                tracker.update(
                    wasCorrect       = false,
                    attemptNumber    = 2,
                    modelFrustRisk   = currentPrediction.frustrationRisk,
                    consecutiveWrong = consecutiveWrong
                )

                handler.postDelayed({
                    showFeedback(correct = false, message = "Good Try!")
                    handler.postDelayed({ hideFeedbackAnimated { advanceRound() } }, FEEDBACK_MS)
                }, FEEDBACK_DELAY_MS)
            }
        }
    }

    private fun colorCorrectCard(green: Boolean) {
        (optionsGrid.getChildAt(correctAnswerIndex) as? CardView)?.let { c ->
            c.setCardBackgroundColor(if (green) Color.parseColor("#C8E6C9") else Color.WHITE)
            c.animate().scaleX(if (green) 1.08f else 1f).scaleY(if (green) 1.08f else 1f)
                .setDuration(150).start()
        }
    }

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

    private fun showFeedback(correct: Boolean, message: String? = null) {
        if (correct) {
            feedbackIcon.setImageResource(R.drawable.correct_answer)
            feedbackText.text = message ?: "Well done!"
            feedbackText.setTextColor(Color.parseColor("#388E3C"))
        } else {
            feedbackIcon.setImageResource(R.drawable.delete)
            feedbackText.text = message ?: "Try again!"
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
            onDone(); return
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

    private fun advanceRound() {
        if (isGameFinished) return
        currentRound++
        if (currentRound >= totalRounds) {
            isGameFinished = true; navigateToScoreboard(); return
        }
        handler.postDelayed({ startNewRound() }, ADVANCE_GAP_MS)
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

    private fun updateScore() {
        scoreText.text = "$score/$totalRounds"
        scoreText.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
            .withEndAction { scoreText.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }.start()
    }

    private fun updateLevelBadge() {
        val modelTier    = RhythmFlashcardManager.determineTierFromModel(
            currentPrediction.rhythmComplexity, currentPrediction.optimalDifficulty)
        val effectiveTier = if (modelTier.ordinal > progressTier.ordinal) modelTier else progressTier

        val (label, colorRes) = when (effectiveTier) {
            RhythmFlashcardManager.ProgressTier.LOW    -> "Easy"   to R.color.green_dark
            RhythmFlashcardManager.ProgressTier.MEDIUM -> "Medium" to R.color.dark_orange
            RhythmFlashcardManager.ProgressTier.HIGH   -> "Hard"   to R.color.pink
        }
        currentDifficultyLevel = effectiveTier.ordinal + 1
        levelIndicator.text    = label
        levelBadge.background?.mutate()?.setTint(ContextCompat.getColor(this, colorRes))
    }

    private fun playSound(player: MediaPlayer?) {
        try { player?.let { if (it.isPlaying) { it.stop(); it.prepare() }; it.start() } }
        catch (e: Exception) { Log.w(TAG, "Sound error: ${e.message}") }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    private fun navigateToScoreboard() {
        handler.removeCallbacksAndMessages(null)

        val accuracy = if (currentRound > 0) score.toFloat() / currentRound else 0.5f
        val avgRT    = if (responseTimes.isNotEmpty()) responseTimes.average().toFloat() else 3000f
        val alpha    = try {
            gameMaster.calculateAlpha(accuracy, avgRT, ChildSession.age, consecutiveWrong)
        } catch (e: Exception) { 0f }

        SessionLogger.logRhythmSession(
            context      = this,
            childId      = ChildSession.childId,
            songTitle    = originalSongTitle,
            tier         = progressTier,
            shownCount   = summaryKeywordPool.size,
            summaryWords = summaryKeywordPool.map { it.word },
            score        = score,
            totalRounds  = totalRounds
        )

        ChildProfileManager.updateAfterSession(
            context                = this,
            childId                = ChildSession.childId,
            ageGroup               = ChildSession.ageGroup,
            gameType               = "rhythm",
            sessionAccuracy        = accuracy,
            sessionFrustration     = tracker.frustration,
            sessionEngagement      = tracker.engagement,
            sessionJitter          = 0.12f,
            sessionRt              = avgRT,
            sessionAlpha           = alpha,
            peakConsecWrong        = tracker.peakConsecWrong,
            lastFiveFeatureVectors = tracker.getFlatHistory(),
            preferredSong          = originalSongTitle
        )

        try {
            startActivity(Intent(this, RMScoreboardActivity::class.java).apply {
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
                putExtra("ENGAGEMENT_SCORE",    tracker.engagement)
                putExtra("FRUSTRATION_LEVEL",   tracker.frustration)
                putExtra("ACCURACY",            accuracy)
            })
            finish()
        } catch (e: Exception) {
            startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}