package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.random.Random

class MoodMatchSevenDownActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    private var ageGroup = 6
    private val isOlderGroup get() = ageGroup >= 8

    private var roundStartTime: Long = 0
    private var consecutiveCorrect = 0
    private var consecutiveWrong = 0
    private var totalJitter = 0f
    private var jitterSamples = 0
    private var tapCount = 0
    private var isPulsing = false
    private var minigameLaunchedThisSession = false

    // ── Session-level frustration tracking (never reset mid-session) ──────────
    //
    // These accumulate across ALL rounds and are used to make the final
    // session-end prediction that determines friendAction / gift unlock.
    // They are intentionally NOT reset when the child gets a correct answer —
    // a child who struggled all game and then scraped a correct answer on
    // attempt 3 should still trigger a comfort friend.

    /** Total wrong taps across the entire session (never resets). */
    private var sessionTotalWrong = 0

    /** Highest consecutiveWrong value seen at any point in the session. */
    private var sessionPeakConsecutiveWrong = 0

    /**
     * Running weighted frustration score 0..1, updated each round.
     * Increases on wrong attempts, decays slightly on first-attempt correct answers.
     * Never resets to 0 — preserves memory of earlier struggle.
     */
    private var sessionFrustration = 0.2f   // start at a mild baseline

    /**
     * Running weighted engagement score 0..1.
     * Decreases when child is struggling, recovers on correct answers.
     */
    private var sessionEngagement = 0.6f

    /** Total rounds completed (used for ratio calculations). */
    private var roundsCompleted = 0

    // ── Per-round wrong tracking ──────────────────────────────────────────────
    private val wrongTappedOptions = mutableSetOf<Int>()

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var emotionImage: ImageView
    private lateinit var layoutYoung:  LinearLayout
    private lateinit var btnOption1:   Button
    private lateinit var btnOption2:   Button
    private lateinit var soundOption1: LinearLayout
    private lateinit var soundOption2: LinearLayout
    private lateinit var layoutOlder:  LinearLayout
    private lateinit var btnGrid1:     Button
    private lateinit var btnGrid2:     Button
    private lateinit var btnGrid3:     Button
    private lateinit var btnGrid4:     Button
    private lateinit var btnNext:      Button
    private lateinit var tvScore:      TextView
    private lateinit var tvRound:      TextView
    private lateinit var topGameImage1:ImageView
    private lateinit var pandaImage:   ImageView
    private lateinit var guessText:    TextView

    // ── Game state ────────────────────────────────────────────────────────────
    private var currentEmotion        = ""
    private var currentEmotionDisplay = ""
    private var correctOption         = 1
    private var score                 = 0
    private var correctAnswersCount   = 0
    private var currentRound          = 1
    private val totalRounds           = 5
    private var isAnswerSelected      = false
    private var nextButtonVisible     = false
    private val usedImages            = mutableSetOf<String>()
    private var wrongAttempts         = 0
    private val roundDistractors      = mutableListOf<String>()

    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var pendingSpeech: String? = null
    private var correctSoundPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "MoodMatchActivity"

        // Frustration thresholds for session-end gift decision
        private const val FRUSTRATION_HIGH_THRESHOLD  = 0.55f  // comfort friend
        private const val FRUSTRATION_PEAK_THRESHOLD  = 0.70f  // strong comfort friend
        private const val PERFORMANCE_HIGH_THRESHOLD  = 0.82f  // trophy friend
    }

    // =========================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_down)

        ageGroup = intent.getIntExtra("AGE_GROUP", 6)
        Log.d(TAG, "ageGroup=$ageGroup isOlderGroup=$isOlderGroup")

        gameMaster = GameMasterModel(this)
        tts = TextToSpeech(this, this)
        try { correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound) }
        catch (e: Exception) { Log.w(TAG, "Sound init failed") }

        initializeViews()
        applyAgeUI()
        showHeader()
        setupGame()
    }
    override fun onResume() {
        super.onResume()
        CalmMusicManager.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        CalmMusicManager.onActivityPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        correctSoundPlayer?.release()
        gameMaster.close()
    }

    @Deprecated("Using deprecated startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MiniGameMoodMatchActivity.REQUEST_CODE)
            Log.d(TAG, "🎮 Mini-game returned")
    }

    // =========================================================================
    // Header
    // =========================================================================

    private fun showHeader() {
        listOf(topGameImage1, pandaImage, guessText).forEach { v ->
            v.visibility = View.VISIBLE; v.alpha = 0f
            v.animate().alpha(1f).setDuration(600).start()
        }
    }

    // =========================================================================
    // Age UI
    // =========================================================================

    private fun applyAgeUI() {
        if (isOlderGroup) {
            layoutYoung.visibility = View.GONE
            layoutOlder.visibility = View.VISIBLE
            val p = emotionImage.layoutParams
            p.width = dpToPx(260); p.height = dpToPx(260)
            emotionImage.layoutParams = p
        } else {
            layoutYoung.visibility = View.VISIBLE
            layoutOlder.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    // =========================================================================
    // View binding
    // =========================================================================

    private fun initializeViews() {
        emotionImage   = findViewById(R.id.emotionImage)
        layoutYoung    = findViewById(R.id.layoutYoung)
        btnOption1     = findViewById(R.id.btnOption1)
        btnOption2     = findViewById(R.id.btnOption2)
        soundOption1   = findViewById(R.id.soundOption1)
        soundOption2   = findViewById(R.id.soundOption2)
        layoutOlder    = findViewById(R.id.layoutOlder)
        btnGrid1       = findViewById(R.id.btnGrid1)
        btnGrid2       = findViewById(R.id.btnGrid2)
        btnGrid3       = findViewById(R.id.btnGrid3)
        btnGrid4       = findViewById(R.id.btnGrid4)
        btnNext        = findViewById(R.id.btnNext)
        tvScore        = findViewById(R.id.tvScore)
        tvRound        = findViewById(R.id.tvRound)
        topGameImage1  = findViewById(R.id.topGameImage1)
        pandaImage     = findViewById(R.id.pandaImage)
        guessText      = findViewById(R.id.guessText)

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            if (currentRound > 1 || isAnswerSelected || wrongAttempts > 0) {
                android.app.AlertDialog.Builder(this)
                    .setMessage("Leave this game? Your progress will be lost.")
                    .setPositiveButton("Leave") { _, _ ->
                        startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                        finish()
                    }
                    .setNegativeButton("Stay", null)
                    .show()
            } else {
                startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }

        topGameImage1.visibility = View.INVISIBLE
        pandaImage.visibility    = View.INVISIBLE
        guessText.visibility     = View.INVISIBLE
    }

    // =========================================================================
    // Game setup
    // =========================================================================

    private fun setupGame() {
        setupClickListeners()
        startNewRound()
    }

    private fun setupClickListeners() {
        btnOption1.setOnClickListener { onOptionTapped(1) }
        btnOption2.setOnClickListener { onOptionTapped(2) }
        soundOption1.setOnClickListener { if (!nextButtonVisible) speakText(btnOption1.text.toString()) }
        soundOption2.setOnClickListener { if (!nextButtonVisible) speakText(btnOption2.text.toString()) }

        btnGrid1.setOnClickListener { onOptionTapped(1) }
        btnGrid2.setOnClickListener { onOptionTapped(2) }
        btnGrid3.setOnClickListener { onOptionTapped(3) }
        btnGrid4.setOnClickListener { onOptionTapped(4) }

        btnNext.setOnClickListener { goToNextRound() }
    }

    private fun onOptionTapped(option: Int) {
        if (nextButtonVisible) return
        if (isAnswerSelected) return
        checkAnswer(option, System.currentTimeMillis() - roundStartTime)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!isAnswerSelected && !nextButtonVisible) { tapCount++; jitterSamples++ }
    }

    // =========================================================================
    // Round
    // =========================================================================

    private fun startNewRound() {
        Log.d(TAG, "=== ROUND $currentRound/$totalRounds ===")

        isAnswerSelected  = false
        wrongAttempts     = 0
        nextButtonVisible = false
        roundStartTime    = System.currentTimeMillis()
        tapCount          = 0
        roundDistractors.clear()
        wrongTappedOptions.clear()

        btnNext.visibility = View.GONE
        resetButtonColors()
        enableGameButtons(true)

        val jitter = if (jitterSamples > 0) totalJitter / jitterSamples else 0.10f

        // Use SESSION-LEVEL frustration and engagement for per-round prediction
        // so the model sees accumulated state, not just this round's snapshot.
        currentPrediction = gameMaster.predictSafe(
            childId             = 0,
            age                 = ageGroup.toFloat(),
            accuracy            = liveAccuracy(),
            engagement          = sessionEngagement,
            frustration         = sessionFrustration,
            jitter              = jitter,
            rt                  = 3000f,
            consecutiveCorrect  = consecutiveCorrect.toFloat(),
            consecutiveWrong    = consecutiveWrong.toFloat()
        )

        if (isOlderGroup) {
            val correctArrayId = when (currentPrediction.emotionLevel) {
                1    -> R.array.mood_age8_lvl1_correct
                else -> R.array.mood_age8_lvl0_correct
            }
            val possibleCorrect = resources.getStringArray(correctArrayId)
            val allAge8 = resources.getStringArray(R.array.mood_age8_lvl0_correct) +
                    resources.getStringArray(R.array.mood_age8_lvl1_correct)

            val available = possibleCorrect.map { extractEmotionName(it) }.filter { it !in usedImages }
            if (available.isEmpty()) usedImages.clear()
            val pool = if (available.isNotEmpty()) available
            else possibleCorrect.map { extractEmotionName(it) }

            currentEmotion        = pool.random()
            usedImages.add(currentEmotion)
            currentEmotionDisplay = formatEmotionText(currentEmotion)

            val rawSelection = findImageForEmotion(currentEmotion, possibleCorrect)
            setEmotionImage(extractResourceName(rawSelection, removeExtension = true))

            val distractors = allAge8
                .map { extractEmotionName(it) }
                .distinct()
                .filter { !it.equals(currentEmotion, ignoreCase = true) }
                .shuffled()
                .take(3)
                .map { formatEmotionText(it) }

            roundDistractors.clear()
            roundDistractors.addAll(distractors)

            val options = (listOf(currentEmotionDisplay) + roundDistractors).shuffled()
            correctOption = options.indexOf(currentEmotionDisplay) + 1
            btnGrid1.text = options[0]; btnGrid2.text = options[1]
            btnGrid3.text = options[2]; btnGrid4.text = options[3]

        } else {
            val arrayId = when (currentPrediction.emotionLevel) {
                1    -> R.array.mood_age6_lvl1_correct
                2    -> R.array.mood_age6_lvl2_scenarios
                else -> R.array.mood_age6_lvl0_correct
            }
            val possibleCorrect     = resources.getStringArray(arrayId)
            val possibleDistractors = resources.getStringArray(R.array.mood_age6_lvl0_distractors)

            val available = possibleCorrect.map { extractEmotionName(it) }.filter { it !in usedImages }
            if (available.isEmpty()) usedImages.clear()
            val pool = if (available.isNotEmpty()) available else possibleCorrect.map { extractEmotionName(it) }

            currentEmotion = pool.random()
            usedImages.add(currentEmotion)

            val rawSelection = findImageForEmotion(currentEmotion, possibleCorrect)

            if (currentPrediction.emotionLevel == 2) {
                val idx     = possibleCorrect.indexOf(rawSelection)
                val answers = resources.getStringArray(R.array.mood_age6_lvl2_answers)
                if (idx < answers.size) currentEmotion = extractEmotionName(answers[idx])
            }

            currentEmotionDisplay = formatEmotionText(currentEmotion)
            setEmotionImage(extractResourceName(rawSelection, removeExtension = true))

            var rawD  = possibleDistractors.random()
            var nameD = extractEmotionName(rawD)
            var att   = 0
            while (nameD.equals(currentEmotion, ignoreCase = true) && att < 10) {
                rawD = possibleDistractors.random(); nameD = extractEmotionName(rawD); att++
            }
            val dispD = formatEmotionText(nameD)
            correctOption = Random.nextInt(1, 3)
            if (correctOption == 1) { btnOption1.text = currentEmotionDisplay; btnOption2.text = dispD }
            else                    { btnOption1.text = dispD; btnOption2.text = currentEmotionDisplay }
        }

        tvRound.text = "Round: $currentRound/$totalRounds"
    }

    private fun liveAccuracy() = if (currentRound > 1)
        correctAnswersCount.toFloat() / (currentRound - 1) else 0.5f

    // =========================================================================
    // Session state updater
    //
    // Called after every answer (correct or wrong) to keep session-level
    // frustration and engagement signals up to date.
    //
    // KEY RULE: frustration can only increase quickly but decreases slowly.
    // A single correct answer on attempt 3 does NOT wipe out a session of
    // struggling — it only nudges frustration down slightly.
    // =========================================================================

    private fun updateSessionState(wasCorrect: Boolean, attemptNumber: Int) {
        roundsCompleted = currentRound

        if (wasCorrect) {
            when (attemptNumber) {
                1 -> {
                    // First-attempt correct: child is doing well
                    // Frustration decreases noticeably, engagement increases
                    sessionFrustration = (sessionFrustration - 0.10f).coerceAtLeast(0.10f)
                    sessionEngagement  = (sessionEngagement  + 0.10f).coerceAtMost(1.00f)
                }
                2 -> {
                    // Needed a hint but got there: small frustration nudge down
                    sessionFrustration = (sessionFrustration - 0.05f).coerceAtLeast(0.10f)
                    sessionEngagement  = (sessionEngagement  + 0.05f).coerceAtMost(1.00f)
                }
                else -> {
                    // Correct after 3+ attempts: barely moves frustration down
                    // The struggle this round still counts
                    sessionFrustration = (sessionFrustration - 0.02f).coerceAtLeast(0.10f)
                    sessionEngagement  = (sessionEngagement  + 0.02f).coerceAtMost(1.00f)
                }
            }
        } else {
            // Wrong answer: frustration rises, engagement drops
            // Each successive wrong in the session hits harder
            val wrongWeight = when (attemptNumber) {
                1    -> 0.08f   // first miss in a round
                2    -> 0.12f   // second miss: more frustrated
                else -> 0.16f   // third miss: significant frustration signal
            }
            sessionFrustration = (sessionFrustration + wrongWeight).coerceAtMost(1.00f)
            sessionEngagement  = (sessionEngagement  - wrongWeight * 0.5f).coerceAtLeast(0.10f)

            // Track session-level peaks
            sessionTotalWrong++
            if (consecutiveWrong > sessionPeakConsecutiveWrong) {
                sessionPeakConsecutiveWrong = consecutiveWrong
            }
        }

        Log.d(TAG, "Session state → frustration=${"%.2f".format(sessionFrustration)} " +
                "engagement=${"%.2f".format(sessionEngagement)} " +
                "totalWrong=$sessionTotalWrong peakConsecWrong=$sessionPeakConsecutiveWrong")
    }

    // =========================================================================
    // Answer checking
    // =========================================================================

    private fun checkAnswer(selectedOption: Int, responseTime: Long) {
        val isCorrect = selectedOption == correctOption

        if (isCorrect) {
            isAnswerSelected = true

            val points = when (wrongAttempts) {
                0    -> { correctAnswersCount++; consecutiveCorrect++; consecutiveWrong = 0; 10 }
                1    -> { consecutiveCorrect = 0; consecutiveWrong = 0; 6 }
                2    -> { consecutiveCorrect = 0; consecutiveWrong = 0; 3 }
                else -> { consecutiveCorrect = 0; consecutiveWrong = 0; 1 }
            }
            score += points
            tvScore.text = "Score: $score"

            // Update session state BEFORE resetting wrongAttempts
            updateSessionState(wasCorrect = true, attemptNumber = wrongAttempts + 1)

            playCorrectSound()
            highlightButton(selectedOption, correct = true)
            darkenAllButtons(keepCorrectBright = true)
            showNextButton()
            Log.d(TAG, "CORRECT attempt=${wrongAttempts + 1} +${points}pts score=$score")

        } else {
            wrongAttempts++
            consecutiveWrong++
            consecutiveCorrect = 0

            // Update session state immediately on each wrong tap
            updateSessionState(wasCorrect = false, attemptNumber = wrongAttempts)

            Log.d(TAG, "WRONG attempt $wrongAttempts")

            highlightButton(selectedOption, correct = false)

            when (wrongAttempts) {
                1 -> {
                    speakText("Try again")
                    roundStartTime = System.currentTimeMillis()
                }
                2 -> {
                    speakText("It's $currentEmotionDisplay")
                    roundStartTime = System.currentTimeMillis()
                    breatheCorrectButton(pulses = 3)
                    if (shouldLaunchMiniGame()) {
                        minigameLaunchedThisSession = true
                        btnForOption(selectedOption).postDelayed({ launchMiniGame() }, 900)
                    }
                }
                3 -> {
                    isAnswerSelected = true
                    wrongTappedOptions.add(selectedOption)
                    speakText("Let's try another one")
                    darkenAllButtons(keepCorrectBright = false)
                    if (shouldLaunchMiniGame()) {
                        minigameLaunchedThisSession = true
                        btnForOption(selectedOption).postDelayed({ launchMiniGame() }, 800)
                    }
                    btnNext.postDelayed({ goToNextRound() }, 2000)
                }
            }
        }
    }

    // =========================================================================
    // Mini-game
    // =========================================================================

    private fun shouldLaunchMiniGame(): Boolean {
        if (minigameLaunchedThisSession) return false
        if (currentRound < 2)           return false
        val modelSaysYes = currentPrediction.minigameTrigger &&
                (wrongAttempts >= 2 || consecutiveWrong >= 2)
        val fallback = consecutiveWrong >= 3
        return modelSaysYes || fallback
    }

    @Suppress("DEPRECATION")
    private fun launchMiniGame() {
        Log.d(TAG, "🎮 Launching mini-game")
        startActivityForResult(
            Intent(this, MiniGameMoodMatchActivity::class.java).apply {
                putExtra(MiniGameMoodMatchActivity.EXTRA_TYPE, (0..1).random())
                putExtra(MiniGameMoodMatchActivity.EXTRA_DIFF, currentPrediction.minigameDifficulty)
                putExtra(MiniGameMoodMatchActivity.EXTRA_AGE,  ageGroup)
            },
            MiniGameMoodMatchActivity.REQUEST_CODE
        )
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    // =========================================================================
    // Navigation — session-end prediction determines gift/friend
    // =========================================================================

    private fun goToNextRound() {
        isPulsing = false
        currentRound++
        if (currentRound > totalRounds) showFeedbackPopup() else startNewRound()
    }

    private fun showFeedbackPopup() {
        FeedbackPopupDialog(
            context        = this,
            isGoodFeedback = correctAnswersCount >= 4,
            correctAnswers = correctAnswersCount,
            totalRounds    = totalRounds,
            score          = score
        ) { navigateToScoreboard() }.show()
    }

    /**
     * Makes a FINAL session-end prediction using the accumulated session state,
     * not the last round's instantaneous snapshot.
     *
     * This is the prediction that determines friendAction / gift unlock.
     *
     * We use sessionFrustration and sessionEngagement which have been updated
     * after every single tap throughout the game — they represent the true
     * emotional arc of the session, not just the last moment.
     */
    private fun navigateToScoreboard() {
        val jitter = if (jitterSamples > 0) totalJitter / jitterSamples else 0.10f

        // Session accuracy: first-attempt correct / total rounds
        val sessionAccuracy = correctAnswersCount.toFloat() / totalRounds.toFloat()

        // Wrong ratio: total wrong taps / (totalRounds * 3 max attempts)
        val wrongRatio = sessionTotalWrong.toFloat() / (totalRounds * 3).toFloat()

        Log.d(TAG, "──────────────────────────────────────────")
        Log.d(TAG, "SESSION SUMMARY (for final prediction):")
        Log.d(TAG, "  correctAnswers       = $correctAnswersCount / $totalRounds")
        Log.d(TAG, "  sessionAccuracy      = ${"%.2f".format(sessionAccuracy)}")
        Log.d(TAG, "  sessionFrustration   = ${"%.2f".format(sessionFrustration)}")
        Log.d(TAG, "  sessionEngagement    = ${"%.2f".format(sessionEngagement)}")
        Log.d(TAG, "  sessionTotalWrong    = $sessionTotalWrong")
        Log.d(TAG, "  sessionPeakConsecW   = $sessionPeakConsecutiveWrong")
        Log.d(TAG, "  wrongRatio           = ${"%.2f".format(wrongRatio)}")
        Log.d(TAG, "──────────────────────────────────────────")

        // Make a fresh prediction using the full session's accumulated state
        val sessionPrediction = gameMaster.predictSafe(
            childId             = 0,
            age                 = ageGroup.toFloat(),
            accuracy            = sessionAccuracy,
            engagement          = sessionEngagement,
            frustration         = sessionFrustration,      // ← full session frustration
            jitter              = jitter,
            rt                  = 3000f,
            consecutiveCorrect  = consecutiveCorrect.toFloat(),
            consecutiveWrong    = sessionPeakConsecutiveWrong.toFloat()  // ← session peak, not current
        )

        // ── Determine friendAction ────────────────────────────────────────────
        //
        // Priority order:
        //   1. Model explicitly assigned a frustration-based friend (1, 2, 5)
        //      → trust the model
        //   2. Model assigned trophy friend (9) → trust the model
        //   3. Model returned 0 but session signals clearly show high frustration
        //      → override with a comfort friend based on session data
        //   4. Model returned 0 and session was genuinely fine → no gift

        val modelFriendAction = sessionPrediction.friendAction

        val effectiveFriendAction: Int = when {

            // Model explicitly triggered a friend → use it
            modelFriendAction > 0 -> {
                Log.d(TAG, "friendAction=$modelFriendAction from model prediction")
                modelFriendAction
            }

            // Model returned 0 but session frustration was clearly high
            // This catches cases where the model's threshold wasn't reached
            // even though the child visibly struggled (e.g. all questions needed
            // 2–3 attempts, consecutiveWrong peaked at 3, etc.)
            sessionFrustration >= FRUSTRATION_PEAK_THRESHOLD -> {
                // Very high frustration → Blanket Panda (most comforting)
                Log.d(TAG, "friendAction=1 overridden: sessionFrustration=${"%.2f".format(sessionFrustration)} >= $FRUSTRATION_PEAK_THRESHOLD")
                1
            }
            sessionFrustration >= FRUSTRATION_HIGH_THRESHOLD -> {
                // High frustration → random comfort friend (1, 2, or 5)
                val comfortFriend = listOf(1, 2, 5).random()
                Log.d(TAG, "friendAction=$comfortFriend overridden: sessionFrustration=${"%.2f".format(sessionFrustration)} >= $FRUSTRATION_HIGH_THRESHOLD")
                comfortFriend
            }
            sessionPeakConsecutiveWrong >= 3 && wrongRatio >= 0.4f -> {
                // Child had a streak of 3+ wrong AND overall got many wrong
                // → Hug Panda (persistence recognition)
                Log.d(TAG, "friendAction=2 overridden: peakConsecWrong=$sessionPeakConsecutiveWrong wrongRatio=${"%.2f".format(wrongRatio)}")
                2
            }

            // Session was fine → no gift
            else -> {
                Log.d(TAG, "friendAction=0: session frustration=${"%.2f".format(sessionFrustration)} below thresholds, no gift")
                0
            }
        }

        val unlockGift = effectiveFriendAction > 0

        Log.d(TAG, "navigateToScoreboard: correct=$correctAnswersCount/$totalRounds " +
                "modelFriendAction=$modelFriendAction effectiveFriendAction=$effectiveFriendAction " +
                "unlockGift=$unlockGift " +
                "frustration=${"%.2f".format(sessionFrustration)} engagement=${"%.2f".format(sessionEngagement)}")

        startActivity(Intent(this, MMScoreboardActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswersCount)
            putExtra("TOTAL_ROUNDS",    totalRounds)
            putExtra("SCORE",           score)
            putExtra("AGE_GROUP",       ageGroup)
            putExtra("MOTIVATION_ID",   sessionPrediction.motivation)
            putExtra("UNLOCK_GIFT",     unlockGift)
            putExtra("FRIEND_ACTION",   effectiveFriendAction)
            putExtra("GAME_MODE",       "seven_down")
        })
        finish()
    }

    // =========================================================================
    // UI helpers
    // =========================================================================

    private fun allOptionButtons(): List<Button> = if (isOlderGroup)
        listOf(btnGrid1, btnGrid2, btnGrid3, btnGrid4)
    else listOf(btnOption1, btnOption2)

    private fun btnForOption(opt: Int): Button = if (isOlderGroup) when (opt) {
        1 -> btnGrid1; 2 -> btnGrid2; 3 -> btnGrid3; else -> btnGrid4
    } else when (opt) { 1 -> btnOption1; else -> btnOption2 }

    private fun resetButtonColors() {
        val orange = ContextCompat.getColor(this, R.color.dark_orange)
        allOptionButtons().forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
            btn.alpha = 1f
            btn.isEnabled = true
        }
    }

    private fun highlightButton(option: Int, correct: Boolean) {
        val color = ContextCompat.getColor(this,
            if (correct) R.color.dark_orange else R.color.dark_orange)
        btnForOption(option).backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun breatheCorrectButton(pulses: Int) {
        val btn = btnForOption(correctOption)
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.dark_orange))
        btn.alpha = 1f
        isPulsing = true
        fun doPulse(remaining: Int) {
            if (remaining <= 0 || !isPulsing) {
                btn.alpha = 1f
                isPulsing = false
                return
            }
            btn.animate().alpha(0.3f).setDuration(400).withEndAction {
                if (!isPulsing) { btn.alpha = 1f; return@withEndAction }
                btn.animate().alpha(1f).setDuration(400).withEndAction { doPulse(remaining - 1) }.start()
            }.start()
        }
        doPulse(pulses)
    }

    private fun darkenAllButtons(keepCorrectBright: Boolean) {
        isPulsing = false
        val darkGreen   = android.graphics.Color.parseColor("#FFA726")
        val darkRed     = android.graphics.Color.parseColor("#B71C1C")
        val fullGreen   = ContextCompat.getColor(this, R.color.light_orange)
        val lightOrange = ContextCompat.getColor(this, R.color.light_orange)

        allOptionButtons().forEach { btn -> btn.isEnabled = false }

        for (opt in 1..allOptionButtons().size) {
            val btn = btnForOption(opt)
            val color = when {
                opt == correctOption && keepCorrectBright -> fullGreen
                opt == correctOption                      -> darkGreen
                opt in wrongTappedOptions                 -> darkRed
                else                                      -> lightOrange
            }
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        }
    }

    private fun enableGameButtons(enable: Boolean) {
        allOptionButtons().forEach { it.isEnabled = enable }
        if (!isOlderGroup) {
            soundOption1.isEnabled = enable
            soundOption2.isEnabled = enable
        }
    }

    private fun showNextButton() {
        nextButtonVisible = true
        btnNext.visibility = View.VISIBLE
    }

    private fun playCorrectSound() {
        try { correctSoundPlayer?.let { if (it.isPlaying) it.seekTo(0); it.start() } }
        catch (e: Exception) { Log.w(TAG, "Sound error") }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun findImageForEmotion(emotion: String, pool: Array<String>) =
        pool.firstOrNull { extractEmotionName(it).equals(emotion, ignoreCase = true) } ?: pool.first()

    private fun formatEmotionText(emotion: String) =
        emotion.replace(".png","").replace(".jpg","")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

    private fun extractResourceName(fullPath: String, removeExtension: Boolean = false): String {
        var name = fullPath.substringAfterLast("/")
        if (removeExtension) name = name.substringBeforeLast(".")
        return name
    }

    private fun extractEmotionName(fullPath: String): String {
        val res   = extractResourceName(fullPath, removeExtension = true)
        val parts = res.split("_")
        return if (parts.size >= 2) parts.last() else res
    }

    private fun setEmotionImage(resourceName: String) {
        var resId = resources.getIdentifier(resourceName, "drawable", packageName)
        if (resId == 0) {
            val last = resourceName.split("_").lastOrNull()
            last?.let { resId = resources.getIdentifier(it, "drawable", packageName) }
        }
        if (resId == 0) Log.w(TAG, "No drawable found for '$resourceName'")
        emotionImage.setImageResource(if (resId != 0) resId else R.drawable.panda_confused)
        emotionImage.visibility = View.VISIBLE
        emotionImage.alpha = 1.0f
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US); tts.setPitch(1.8f); tts.setSpeechRate(0.9f)
            isTtsInitialized = true
            pendingSpeech?.let { tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, ""); pendingSpeech = null }
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            pendingSpeech = text
        }
    }
}