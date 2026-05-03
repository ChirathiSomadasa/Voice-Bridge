package com.chirathi.voicebridge

import android.content.Context
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

class MoodMatchSevenDownActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    private var ageGroup = 6
    private val isOlderGroup get() = ageGroup >= 8

    private var childProfile: ChildProfileManager.ChildProfile? = null
    private lateinit var tracker: SessionStateTracker
    private var currentRoundSpec: PersonalizedContentSelector.MoodRoundSpec? = null

    // ── Strict Cross-Session Memory ───────────────────────────────────────────
    private val PREFS_NAME = "MoodMatchState"
    private var difficultyState = 0 // 0 to 4
    private val recentEmotions = mutableSetOf<String>()

    // Streaks for Difficulty Graduation (First Try Only)
    private var streakFirstTryCorrect = 0
    private var streakFirstTryWrong   = 0
    private var isFirstAttemptThisRound = true

    // Shuffling Minigames
    private var nextMinigameType = 0

    // ── Round state ───────────────────────────────────────────────────────────
    private var roundStartTime: Long = 0
    private var lastResponseTime: Long = 3000L
    private var consecutiveCorrect   = 0
    private var consecutiveWrong     = 0
    private var jitterSamples        = 0
    private var tapCount             = 0
    private var isPulsing            = false

    private val tapVariances = mutableListOf<Float>()

    // ── Observation-only session state ───────────────────────────────────────
    private var sessionTotalWrong             = 0
    private var sessionPeakConsecutiveWrong = 0
    private var sessionFrustration          = 0.2f
    private var sessionEngagement           = 0.6f
    private var roundsCompleted             = 0

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
    private var currentEmotionDisplay = ""
    private var correctOption         = 1
    private var score                 = 0
    private var correctAnswersCount   = 0
    private var currentRound          = 1
    private val totalRounds           = 5
    private var isAnswerSelected      = false
    private var nextButtonVisible     = false
    private var wrongAttempts         = 0

    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var pendingSpeech: String? = null
    private var correctSoundPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "MoodMatchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_down)

        ageGroup = intent.getIntExtra("AGE_GROUP", 6)

        if (!ChildSession.isInitialized) ChildSession.restore(this)

        loadCrossSessionMemory()

        childProfile = ChildProfileManager.load(this, ChildSession.childId)
        tracker = SessionStateTracker(
            ageGroup           = ageGroup,
            initialFrustration = childProfile?.avgFrustration ?: 0.20f,
            initialEngagement  = childProfile?.avgEngagement  ?: 0.60f
        )

        gameMaster = GameMasterModel(this)
        if (childProfile != null) {
            gameMaster.seedHistory(ChildProfileManager.getHistoryTensor(this, ChildSession.childId))
        }

        tts = TextToSpeech(this, this)
        try { correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound) }
        catch (e: Exception) { Log.w(TAG, "Sound init failed") }

        initializeViews()
        applyAgeUI()
        showHeader()
        setupGame()
    }

    private fun loadCrossSessionMemory() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        difficultyState = prefs.getInt("difficultyState_${ChildSession.childId}", 0)

        val savedEmotions = prefs.getString("recentEmotions_${ChildSession.childId}", "")
        if (!savedEmotions.isNullOrEmpty()) {
            recentEmotions.addAll(savedEmotions.split(","))
        }
        Log.d(TAG, "Loaded memory: state=$difficultyState, recentEmotions=$recentEmotions")
    }

    private fun saveCrossSessionMemory() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("difficultyState_${ChildSession.childId}", difficultyState)
            .putString("recentEmotions_${ChildSession.childId}", recentEmotions.joinToString(","))
            .apply()
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
        saveCrossSessionMemory()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        correctSoundPlayer?.release()
        gameMaster.close()
    }

    @Deprecated("Using deprecated startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MiniGameMoodMatchActivity.REQUEST_CODE) {
            Log.d(TAG, "🎮 Mini-game returned")
            tracker.update(true, 1, currentPrediction.frustrationRisk, consecutiveWrong)
        }
    }

    private fun showHeader() {
        listOf(topGameImage1, pandaImage, guessText).forEach { v ->
            v.visibility = View.VISIBLE; v.alpha = 0f
            v.animate().alpha(1f).setDuration(600).start()
        }
    }

    private fun applyAgeUI() {
        if (isOlderGroup) {
            val p = emotionImage.layoutParams
            p.width = dpToPx(260); p.height = dpToPx(260)
            emotionImage.layoutParams = p
        }
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

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
            startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }
    }

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
        btnNext.setOnClickListener  { goToNextRound() }
    }

    private fun onOptionTapped(option: Int) {
        if (nextButtonVisible || isAnswerSelected) return
        val responseTime = System.currentTimeMillis() - roundStartTime
        lastResponseTime = responseTime
        checkAnswer(option, responseTime)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!isAnswerSelected && !nextButtonVisible) { tapCount++; jitterSamples++ }
    }

    private fun startNewRound() {
        isAnswerSelected  = false
        isFirstAttemptThisRound = true
        wrongAttempts     = 0
        nextButtonVisible = false
        roundStartTime    = System.currentTimeMillis()
        wrongTappedOptions.clear()
        btnNext.visibility = View.GONE
        resetButtonColors()
        enableGameButtons(true)

        val jitter = tracker.avgJitter(tapVariances)
        val roundAccuracy = if (currentRound == 1) childProfile?.moodMatchAccuracy ?: 0.5f else tracker.liveAccuracy(currentRound - 1)

        currentPrediction = gameMaster.predictSafe(
            childId            = ChildSession.childId,
            age                = ageGroup.toFloat(),
            accuracy           = roundAccuracy,
            engagement         = tracker.engagement,
            frustration        = tracker.frustration,
            jitter             = jitter,
            rt                 = lastResponseTime.toFloat(),
            consecutiveCorrect = streakFirstTryCorrect.toFloat(),
            consecutiveWrong   = streakFirstTryWrong.toFloat()
        )

        tracker.recordFeatureVector(gameMaster.lastFeatureVector())

        val spec = PersonalizedContentSelector.selectMoodRound(
            ageGroup        = ageGroup,
            prediction      = currentPrediction,
            difficultyState = difficultyState,
            recentEmotions  = recentEmotions
        )

        currentRoundSpec = spec

        recentEmotions.add(spec.correctEmotionDrawable)
        if (recentEmotions.size > 5) {
            val iterator = recentEmotions.iterator()
            iterator.next()
            iterator.remove()
        }

        applyRoundLayout(spec)

        SessionLogger.logPrediction(
            context   = this,
            childId   = ChildSession.childId,
            round     = currentRound,
            gameType  = "mood_match",
            ageGroup  = ageGroup,
            features  = gameMaster.lastFeatureVector(),
            pred      = currentPrediction,
            diffLabel = "State:$difficultyState"
        )

        tracker.recordRound()
        tvRound.text = "Round: $currentRound/$totalRounds"
        Log.d(TAG, "Starting Round $currentRound: State=$difficultyState")
    }

    private fun applyRoundLayout(spec: PersonalizedContentSelector.MoodRoundSpec) {
        when (spec.layout) {
            PersonalizedContentSelector.MoodLayout.TWO_BUTTON -> {
                layoutYoung.visibility = View.VISIBLE
                layoutOlder.visibility = View.GONE

                val options = listOf(
                    spec.correctEmotionWord to spec.correctEmotionDrawable,
                    spec.distractorWords.first() to spec.distractors.first()
                ).shuffled()

                correctOption = options.indexOfFirst { it.second == spec.correctEmotionDrawable } + 1

                if (correctOption == 1) {
                    btnOption1.text = spec.correctEmotionWord
                    btnOption2.text = spec.distractorWords.first()
                } else {
                    btnOption1.text = spec.distractorWords.first()
                    btnOption2.text = spec.correctEmotionWord
                }
            }
            PersonalizedContentSelector.MoodLayout.FOUR_BUTTON,
            PersonalizedContentSelector.MoodLayout.SCENARIO -> {
                layoutYoung.visibility = View.GONE
                layoutOlder.visibility = View.VISIBLE

                val combinedPairs = mutableListOf<Pair<String, String>>()
                combinedPairs.add(spec.correctEmotionWord to spec.correctEmotionDrawable)
                for (i in spec.distractorWords.indices) {
                    combinedPairs.add(spec.distractorWords[i] to spec.distractors[i])
                }

                val options = combinedPairs.shuffled()
                correctOption = options.indexOfFirst { it.second == spec.correctEmotionDrawable } + 1
                val buttons = listOf(btnGrid1, btnGrid2, btnGrid3, btnGrid4)
                options.forEachIndexed { i, pair ->
                    if (i < buttons.size) buttons[i].text = pair.first
                }
            }
        }

        val imageName = if (spec.isScenario)
            extractResourceName(spec.scenarioImageDrawable, removeExtension = true)
        else
            extractResourceName(spec.correctEmotionDrawable, removeExtension = true)

        setEmotionImage(imageName)
        currentEmotionDisplay = spec.correctEmotionWord
    }

    private fun checkAnswer(selectedOption: Int, responseTime: Long) {
        val isCorrect = selectedOption == correctOption
        var minigameTriggeredNow = false

        if (isFirstAttemptThisRound) {
            isFirstAttemptThisRound = false

            if (isCorrect) {
                streakFirstTryCorrect++
                streakFirstTryWrong = 0
                consecutiveCorrect++
                consecutiveWrong = 0
            } else {
                streakFirstTryWrong++
                streakFirstTryCorrect = 0
                consecutiveWrong++
                consecutiveCorrect = 0
            }

            val oldState = difficultyState
            difficultyState = PersonalizedContentSelector.getNewDifficultyState(difficultyState, streakFirstTryCorrect, streakFirstTryWrong)

            if (difficultyState != oldState) {
                saveCrossSessionMemory()
                if (difficultyState > oldState) {
                    Log.d(TAG, "State Upgraded! $oldState -> $difficultyState")
                    streakFirstTryCorrect = 0
                } else {
                    Log.d(TAG, "State Downgraded! $oldState -> $difficultyState. Triggering minigame.")
                    streakFirstTryWrong = 0
                    minigameTriggeredNow = true
                }
            }
        }

        if (isCorrect) {
            isAnswerSelected = true
            tracker.update(true, wrongAttempts + 1, currentPrediction.frustrationRisk, consecutiveWrong)

            val points = when (wrongAttempts) {
                0 -> 10; 1 -> 6; 2 -> 3; else -> 1
            }
            score += points
            correctAnswersCount++
            tvScore.text = "Score: $score"
            updateSessionStateObservation(true, wrongAttempts + 1)

            val praise = if (ageGroup <= 7) "Well done!" else "Correct!"
            speakText(praise)
            playCorrectSound()
            highlightButton(selectedOption, true)
            darkenAllButtons(true)
            showNextButton()

        } else {
            wrongAttempts++
            updateSessionStateObservation(false, wrongAttempts)
            highlightButton(selectedOption, false)

            if (minigameTriggeredNow) {
                val msg = "Let's take a quick brain break!"
                speakText(msg)
                btnForOption(selectedOption).postDelayed({ launchMiniGame() }, 1000)
                tracker.update(false, 1, currentPrediction.frustrationRisk, consecutiveWrong)
            } else if (wrongAttempts == 1) {
                val msg = if (ageGroup <= 7) "Try again!" else "Good try! Try once more."
                speakText(msg)
                roundStartTime = System.currentTimeMillis()
                tracker.update(false, 1, currentPrediction.frustrationRisk, consecutiveWrong)
            } else if (wrongAttempts == 2) {
                deliverTherapeuticHint()
                tracker.update(false, 2, currentPrediction.frustrationRisk, consecutiveWrong)
            } else {
                isAnswerSelected = true
                wrongTappedOptions.add(selectedOption)
                val reveal = if (ageGroup <= 7) "This face shows $currentEmotionDisplay!" else "The answer is $currentEmotionDisplay."
                speakText(reveal)
                darkenAllButtons(true)
                tracker.update(false, 3, currentPrediction.frustrationRisk, consecutiveWrong)
                btnNext.postDelayed({ goToNextRound() }, 2500)
            }
        }
    }

    private fun deliverTherapeuticHint() {
        breatheCorrectButton(pulses = 3)
        val isTextOptions = layoutYoung.visibility == View.VISIBLE
        val speech = when (currentPrediction.optimalHint) {
            0 -> if (isTextOptions) "Tap the speakers to hear the words carefully..." else "Look at all the faces..."
            1 -> if (isTextOptions) "Listen to the sounds again..." else "Think about how the face looks..."
            else -> "It's $currentEmotionDisplay!"
        }
        speakText(speech)
    }

    @Suppress("DEPRECATION")
    private fun launchMiniGame() {
        // Toggle the minigame type dynamically between 0 and 1
        val typeToLaunch = nextMinigameType
        nextMinigameType = if (nextMinigameType == 0) 1 else 0

        startActivityForResult(
            Intent(this, MiniGameMoodMatchActivity::class.java).apply {
                putExtra(MiniGameMoodMatchActivity.EXTRA_TYPE, typeToLaunch)
                putExtra(MiniGameMoodMatchActivity.EXTRA_DIFF, currentPrediction.minigameDifficulty)
                putExtra(MiniGameMoodMatchActivity.EXTRA_AGE,  ageGroup)
            },
            MiniGameMoodMatchActivity.REQUEST_CODE
        )
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun updateSessionStateObservation(wasCorrect: Boolean, attemptNumber: Int) {
        roundsCompleted = currentRound
        if (wasCorrect) {
            val decay = when (attemptNumber) { 1 -> 0.10f; 2 -> 0.05f; else -> 0.02f }
            sessionFrustration = (sessionFrustration - decay).coerceAtLeast(0.10f)
            sessionEngagement  = (sessionEngagement  + decay).coerceAtMost(1.00f)
        } else {
            val growth = when (attemptNumber) { 1 -> 0.08f; 2 -> 0.12f; else -> 0.16f }
            sessionFrustration = (sessionFrustration + growth).coerceAtMost(1.00f)
            sessionEngagement  = (sessionEngagement  - growth * 0.5f).coerceAtLeast(0.10f)
            sessionTotalWrong++
            if (consecutiveWrong > sessionPeakConsecutiveWrong)
                sessionPeakConsecutiveWrong = consecutiveWrong
        }
    }

    private fun goToNextRound() {
        isPulsing = false
        currentRound++
        if (currentRound > totalRounds) showFeedbackPopup() else startNewRound()
    }

    private fun showFeedbackPopup() {
        saveCrossSessionMemory()
        FeedbackPopupDialog(
            context        = this,
            isGoodFeedback = correctAnswersCount >= 4,
            correctAnswers = correctAnswersCount,
            totalRounds    = totalRounds,
            score          = score
        ) { navigateToScoreboard() }.show()
    }

    private fun navigateToScoreboard() {
        val jitter = tracker.avgJitter(tapVariances)
        val sessionAccuracy = tracker.liveAccuracy(totalRounds)

        val sessionPrediction = gameMaster.predictSafe(
            childId            = ChildSession.childId,
            age                = ageGroup.toFloat(),
            accuracy           = sessionAccuracy,
            engagement         = tracker.engagement,
            frustration        = tracker.frustration,
            jitter             = jitter,
            rt                 = lastResponseTime.toFloat(),
            consecutiveCorrect = consecutiveCorrect.toFloat(),
            consecutiveWrong   = tracker.peakConsecWrong.toFloat()
        )

        val effectiveFriendAction = PersonalizedContentSelector.resolveFriendAction(sessionPrediction.frustrationRisk, tracker)

        ChildProfileManager.updateAfterSession(
            context                = this,
            childId                = ChildSession.childId,
            ageGroup               = ageGroup,
            gameType               = "mood_match",
            sessionAccuracy        = sessionAccuracy,
            sessionFrustration     = tracker.frustration,
            sessionEngagement      = tracker.engagement,
            sessionJitter          = jitter,
            sessionRt              = lastResponseTime.toFloat(),
            sessionAlpha           = sessionPrediction.nextAccuracy * 10f,
            peakConsecWrong        = tracker.peakConsecWrong,
            lastFiveFeatureVectors = tracker.getFlatHistory()
        )

        startActivity(Intent(this, MMScoreboardActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswersCount)
            putExtra("TOTAL_ROUNDS",    totalRounds)
            putExtra("SCORE",           score)
            putExtra("AGE_GROUP",       ageGroup)
            putExtra("MOTIVATION_ID",   sessionPrediction.motivation)
            putExtra("UNLOCK_GIFT",     effectiveFriendAction > 0)
            putExtra("FRIEND_ACTION",   effectiveFriendAction)
            putExtra("GAME_MODE",       "seven_down")
        })
        finish()
    }

    private fun allOptionButtons(): List<Button> = if (isOlderGroup || layoutOlder.visibility == View.VISIBLE)
        listOf(btnGrid1, btnGrid2, btnGrid3, btnGrid4)
    else listOf(btnOption1, btnOption2)

    private fun btnForOption(opt: Int): Button = if (isOlderGroup || layoutOlder.visibility == View.VISIBLE) when (opt) {
        1 -> btnGrid1; 2 -> btnGrid2; 3 -> btnGrid3; else -> btnGrid4
    } else when (opt) { 1 -> btnOption1; else -> btnOption2 }

    private fun resetButtonColors() {
        val orange = ContextCompat.getColor(this, R.color.dark_orange)
        allOptionButtons().forEach { btn ->
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(orange)
            btn.alpha = 1f; btn.isEnabled = true
        }
    }

    private fun highlightButton(option: Int, correct: Boolean) {
        val color = ContextCompat.getColor(this, R.color.dark_orange)
        btnForOption(option).backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun breatheCorrectButton(pulses: Int) {
        val btn = btnForOption(correctOption)
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_orange))
        btn.alpha = 1f
        isPulsing = true
        fun doPulse(remaining: Int) {
            if (remaining <= 0 || !isPulsing) { btn.alpha = 1f; isPulsing = false; return }
            btn.animate().alpha(0.3f).setDuration(400).withEndAction {
                if (!isPulsing) { btn.alpha = 1f; return@withEndAction }
                btn.animate().alpha(1f).setDuration(400).withEndAction { doPulse(remaining - 1) }.start()
            }.start()
        }
        doPulse(pulses)
    }

    private fun darkenAllButtons(keepCorrectBright: Boolean) {
        isPulsing = false
        val highlighted = android.graphics.Color.parseColor("#FFA726")
        val darkRed     = android.graphics.Color.parseColor("#B71C1C")
        val fullBright  = ContextCompat.getColor(this, R.color.light_orange)
        val lightOrange = ContextCompat.getColor(this, R.color.light_orange)

        allOptionButtons().forEach { it.isEnabled = false }

        for (opt in 1..allOptionButtons().size) {
            val btn = btnForOption(opt)
            val color = when {
                opt == correctOption && keepCorrectBright -> fullBright
                opt == correctOption                      -> highlighted
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

    private fun extractResourceName(fullPath: String, removeExtension: Boolean = false): String {
        var name = fullPath.substringAfterLast("/")
        if (removeExtension) name = name.substringBeforeLast(".")
        return name
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
        emotionImage.alpha      = 1.0f
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(if (ageGroup <= 7) 1.8f else 1.4f)
            tts.setSpeechRate(if (ageGroup <= 7) 0.85f else 0.90f)
            isTtsInitialized = true
            pendingSpeech?.let {
                tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, "")
                pendingSpeech = null
            }
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        else pendingSpeech = text
    }
}