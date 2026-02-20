package com.chirathi.voicebridge

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.random.Random

/**
 * MoodMatchSevenDownActivity — v9.2 + MiniGame integration
 *
 * MINIGAME TRIGGER RULES
 * ──────────────────────
 * The mini-game launches AT MOST ONCE per 5-round session.
 * It is a re-engagement break, not a reward.
 *
 * It fires when ALL of these are true:
 *   1. Model says minigameTrigger == true
 *      (model fires this when engagement < 0.40 OR frustration 0.50–0.80)
 *   2. Child shows distraction signals in this round:
 *         • wrongAttempts >= 2 on current round, OR
 *         • consecutiveWrong >= 2 across rounds
 *   3. Not already launched this session (minigameLaunchedThisSession == false)
 *   4. At least round 2 (so child has warmed up first)
 *
 * Delay of 800 ms after showing the "correct button" hint so the child
 * sees the feedback before the screen transitions.
 */
class MoodMatchSevenDownActivity : AppCompatActivity(),
    FeedbackDialogFragment.FeedbackCompletionListener, TextToSpeech.OnInitListener {

    // ── Model ─────────────────────────────────────────────────────────────────
    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    // ── Session tracking ──────────────────────────────────────────────────────
    private val AGE_GROUP = 6
    private var roundStartTime: Long = 0
    private var consecutiveCorrect = 0
    private var consecutiveWrong = 0
    private var totalJitter = 0f
    private var jitterSamples = 0
    private var tapCount = 0

    // ── Mini-game guard ───────────────────────────────────────────────────────
    // Once this flips true it stays true for the whole 5-round session.
    private var minigameLaunchedThisSession = false

    // ── UI ────────────────────────────────────────────────────────────────────
    private lateinit var videoView:    VideoView
    private lateinit var emotionImage: ImageView
    private lateinit var btnOption1:   Button
    private lateinit var btnOption2:   Button
    private lateinit var soundOption1: LinearLayout
    private lateinit var soundOption2: LinearLayout
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
    private var isVideoFinished       = false
    private var isVideoPlaying        = false
    private var nextButtonVisible     = false
    private val usedImages            = mutableSetOf<String>()
    private var wrongAttempts         = 0
    private val maxAttemptsPerRound   = 3

    // ── TTS & Sound ───────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var correctSoundPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "MoodMatchActivity"
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_down)

        gameMaster = GameMasterModel(this)
        Log.d(TAG, "✅ GameMaster initialized  modelLoaded=${gameMaster.modelLoaded}")

        tts = TextToSpeech(this, this)
        try { correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound) }
        catch (e: Exception) { Log.w(TAG, "Sound init failed: ${e.message}") }

        initializeViews()
        setupVideoPlayer()
        videoView.start()
        isVideoPlaying = true
        setupGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        correctSoundPlayer?.release()
        gameMaster.close()
    }

    // =========================================================================
    // Mini-game result — no bonus points, just resume
    // =========================================================================

    @Deprecated("Using deprecated startActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MiniGameMoodMatchActivity.REQUEST_CODE) {
            // Mini-game done — next button is already showing, child taps to continue
            Log.d(TAG, "🎮 Mini-game returned — resuming session")
        }
    }

    // =========================================================================
    // Mini-game trigger
    // =========================================================================

    /**
     * Decides whether to launch the mini-game right now.
     *
     * Called from checkAnswer() only when the child has used at least 2 wrong
     * attempts, meaning real distraction signals are present.
     */
    private fun shouldLaunchMiniGame(): Boolean {
        if (minigameLaunchedThisSession) return false   // already used this session
        if (currentRound < 2)            return false   // too early, child hasn't warmed up
        if (!currentPrediction.minigameTrigger) return false  // model doesn't recommend it

        // Confirm distraction: 2+ wrong attempts THIS round OR 2+ consecutive wrong rounds
        val distractedThisRound   = wrongAttempts >= 2
        val distractedAcrossRounds= consecutiveWrong >= 2

        return distractedThisRound || distractedAcrossRounds
    }

    @Suppress("DEPRECATION")
    private fun launchMiniGame() {
        minigameLaunchedThisSession = true
        Log.d(TAG, "🎮 Launching mini-game type=${currentPrediction.minigameType} diff=${currentPrediction.minigameDifficulty}")

        // Clamp type to valid range — model can output values > 1
        val safeType = (0..1).random()
        val intent = Intent(this, MiniGameMoodMatchActivity::class.java).apply {
            putExtra(MiniGameMoodMatchActivity.EXTRA_TYPE, safeType)
            putExtra(MiniGameMoodMatchActivity.EXTRA_DIFF, currentPrediction.minigameDifficulty)
            putExtra(MiniGameMoodMatchActivity.EXTRA_AGE,  AGE_GROUP)
        }
        startActivityForResult(intent, MiniGameMoodMatchActivity.REQUEST_CODE)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    // =========================================================================
    // Init
    // =========================================================================

    private fun initializeViews() {
        videoView      = findViewById(R.id.videoView)
        emotionImage   = findViewById(R.id.emotionImage)
        btnOption1     = findViewById(R.id.btnOption1)
        btnOption2     = findViewById(R.id.btnOption2)
        soundOption1   = findViewById(R.id.soundOption1)
        soundOption2   = findViewById(R.id.soundOption2)
        btnNext        = findViewById(R.id.btnNext)
        tvScore        = findViewById(R.id.tvScore)
        tvRound        = findViewById(R.id.tvRound)
        topGameImage1  = findViewById(R.id.topGameImage1)
        pandaImage     = findViewById(R.id.pandaImage)
        guessText      = findViewById(R.id.guessText)

        topGameImage1.visibility = View.INVISIBLE
        pandaImage.visibility    = View.INVISIBLE
        guessText.visibility     = View.INVISIBLE
    }

    private fun setupVideoPlayer() {
        try {
            val uri = Uri.parse("android.resource://$packageName/${R.raw.animated_bear_asks_about_emotion}")
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { it.isLooping = false }
            videoView.setOnCompletionListener { onVideoFinished() }
        } catch (e: Exception) {
            Log.e(TAG, "Video error: ${e.message}")
            videoView.visibility = View.GONE
            isVideoPlaying = false
            showHeaderAfterVideo()
        }
    }

    private fun onVideoFinished() {
        if (isVideoFinished) return
        isVideoFinished = true; isVideoPlaying = false
        videoView.animate().alpha(0f).setDuration(500).withEndAction {
            videoView.visibility = View.GONE
            showHeaderAfterVideo()
        }.start()
    }

    private fun showHeaderAfterVideo() {
        listOf(topGameImage1, pandaImage, guessText).forEach { v ->
            v.visibility = View.VISIBLE; v.alpha = 0f
            v.animate().alpha(1f).setDuration(800).start()
        }
    }

    private fun setupGame() {
        setupClickListeners()
        startNewRound()
    }

    private fun setupClickListeners() {
        btnOption1.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible)
                checkAnswer(1, System.currentTimeMillis() - roundStartTime)
        }
        btnOption2.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible)
                checkAnswer(2, System.currentTimeMillis() - roundStartTime)
        }
        soundOption1.setOnClickListener { if (!nextButtonVisible) speakText(btnOption1.text.toString()) }
        soundOption2.setOnClickListener { if (!nextButtonVisible) speakText(btnOption2.text.toString()) }
        btnNext.setOnClickListener { goToNextRound() }
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

        btnNext.visibility = View.GONE
        enableGameButtons(true)
        val green = ContextCompat.getColor(this, R.color.green)
        btnOption1.setBackgroundColor(green)
        btnOption2.setBackgroundColor(green)

        // Build features for this round
        val accuracy    = liveAccuracy()
        val engagement  = if (accuracy > 0.7f) 0.8f else 0.5f
        val frustration = when {
            consecutiveWrong >= 2 -> 0.7f
            consecutiveWrong == 1 -> 0.45f
            else                  -> 0.2f
        }
        val jitter = if (jitterSamples > 0) totalJitter / jitterSamples else 0.10f

        currentPrediction = gameMaster.predictSafe(
            childId             = 0,
            age                 = AGE_GROUP.toFloat(),
            accuracy            = accuracy,
            engagement          = engagement,
            frustration         = frustration,
            jitter              = jitter,
            rt                  = 3000f,
            consecutiveCorrect  = consecutiveCorrect.toFloat(),
            consecutiveWrong    = consecutiveWrong.toFloat()
        )

        Log.d(TAG, "🤖 PREDICTION (fromModel=${currentPrediction.fromModel}):")
        Log.d(TAG, "   emotionLevel      = ${currentPrediction.emotionLevel}")
        Log.d(TAG, "   minigameTrigger   = ${currentPrediction.minigameTrigger}")
        Log.d(TAG, "   minigameType      = ${currentPrediction.minigameType}")
        Log.d(TAG, "   nextAccuracy      = ${currentPrediction.nextAccuracy}")
        Log.d(TAG, "   frustrationRisk   = ${currentPrediction.frustrationRisk}")
        Log.d(TAG, "   minigameLaunched? = $minigameLaunchedThisSession")

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
            val idx = possibleCorrect.indexOf(rawSelection)
            val answers = resources.getStringArray(R.array.mood_age6_lvl2_answers)
            if (idx < answers.size) currentEmotion = extractEmotionName(answers[idx])
        }

        currentEmotionDisplay = formatEmotionText(currentEmotion)
        setEmotionImage(extractResourceName(rawSelection, removeExtension = true))

        var rawDistractor  = possibleDistractors.random()
        var distractorName = extractEmotionName(rawDistractor)
        var att = 0
        while (distractorName.equals(currentEmotion, ignoreCase = true) && att < 10) {
            rawDistractor  = possibleDistractors.random()
            distractorName = extractEmotionName(rawDistractor)
            att++
        }
        val distractorDisplay = formatEmotionText(distractorName)

        correctOption = Random.nextInt(1, 3)
        if (correctOption == 1) {
            btnOption1.text = currentEmotionDisplay; btnOption2.text = distractorDisplay
        } else {
            btnOption1.text = distractorDisplay;     btnOption2.text = currentEmotionDisplay
        }

        tvRound.text = "Round: $currentRound/$totalRounds"
        Log.d(TAG, "Level=${currentPrediction.emotionLevel} emotion=$currentEmotion correctOption=$correctOption")
    }

    private fun liveAccuracy() = if (currentRound > 1) {
        correctAnswersCount.toFloat() / (currentRound - 1)
    } else 0.5f

    // =========================================================================
    // Answer checking
    // =========================================================================

    private fun checkAnswer(selectedOption: Int, responseTime: Long) {
        isAnswerSelected = true
        val isCorrect = (selectedOption == correctOption)

        if (isCorrect) {
            score += 10; correctAnswersCount++
            consecutiveCorrect++; consecutiveWrong = 0
            tvScore.text = "Score: $score"
            playCorrectSound()
            wrongAttempts = 0
            showNextButton()
            Log.d(TAG, "✅ CORRECT! Score: $score")

        } else {
            wrongAttempts++; consecutiveWrong++; consecutiveCorrect = 0
            Log.d(TAG, "❌ WRONG — attempt $wrongAttempts/$maxAttemptsPerRound")

            when (wrongAttempts) {
                1 -> {
                    speakText("Try again")
                    isAnswerSelected = false
                    roundStartTime = System.currentTimeMillis()
                }
                2 -> {
                    // Second wrong: give hint + check if mini-game should launch
                    speakText("It's $currentEmotionDisplay")
                    showCorrectButtonAnimation()
                    isAnswerSelected = false
                    roundStartTime = System.currentTimeMillis()

                    // ── Mini-game trigger on distraction ──────────────────────
                    if (shouldLaunchMiniGame()) {
                        Log.d(TAG, "🎮 Distraction detected at attempt 2 — launching mini-game")
                        // Set flag NOW before the delay — blocks attempt 3 from also triggering
                        minigameLaunchedThisSession = true
                        btnOption1.postDelayed({ launchMiniGame() }, 900)
                    }
                }
                3 -> {
                    showNextButton()
                    speakText("Good try! Next one")

                    // ── Mini-game trigger on 3rd wrong — only fires if attempt 2 did NOT trigger ───
                    if (shouldLaunchMiniGame()) {
                        Log.d(TAG, "🎮 Distraction detected at attempt 3 — launching mini-game")
                        minigameLaunchedThisSession = true
                        btnOption1.postDelayed({ launchMiniGame() }, 800)
                    }
                }
            }
        }
    }

    private fun playCorrectSound() {
        try { correctSoundPlayer?.let { if (it.isPlaying) it.seekTo(0); it.start() } }
        catch (e: Exception) { Log.w(TAG, "Sound error: ${e.message}") }
    }

    private fun showNextButton() {
        nextButtonVisible = true
        btnNext.visibility = View.VISIBLE
        enableGameButtons(false)
    }

    private fun showCorrectButtonAnimation() {
        val btn = if (correctOption == 1) btnOption1 else btnOption2
        btn.animate().scaleX(1.15f).scaleY(1.15f).setDuration(300)
            .withEndAction { btn.animate().scaleX(1f).scaleY(1f).setDuration(300).start() }
            .start()
    }

    private fun enableGameButtons(enable: Boolean) {
        val a = if (enable) 1.0f else 0.5f
        listOf(btnOption1, btnOption2).forEach { it.isEnabled = enable; it.alpha = a }
        listOf(soundOption1, soundOption2).forEach { it.isEnabled = enable; it.alpha = a }
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    private fun goToNextRound() {
        currentRound++
        if (currentRound > totalRounds) showFeedbackPopup() else startNewRound()
    }

    private fun showFeedbackPopup() {
        FeedbackDialogFragment.newInstance(
            correctAnswersCount >= 4, correctAnswersCount, totalRounds, score
        ).show(supportFragmentManager, "feedback")
    }

    override fun onFeedbackCompleted(correctAnswers: Int, totalRounds: Int, score: Int) {
        navigateToScoreboard(correctAnswers, totalRounds, score)
    }

    private fun navigateToScoreboard(correctAnswers: Int, totalRounds: Int, score: Int) {
        startActivity(Intent(this, MMScoreboardActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswers)
            putExtra("TOTAL_ROUNDS",    totalRounds)
            putExtra("SCORE",           score)
            putExtra("MOTIVATION_ID",   currentPrediction.motivation)
            putExtra("UNLOCK_GIFT",     currentPrediction.friendAction > 0)
            putExtra("GAME_MODE",       "seven_down")
        })
        finish()
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun findImageForEmotion(emotion: String, pool: Array<String>): String =
        pool.firstOrNull { extractEmotionName(it).equals(emotion, ignoreCase = true) } ?: pool.first()

    private fun formatEmotionText(emotion: String): String {
        val clean = emotion.replace(".png", "").replace(".jpg", "")
        return clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

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
        if (resId == 0) resId = resources.getIdentifier("mood_$resourceName", "drawable", packageName)
        emotionImage.setImageResource(if (resId != 0) resId else R.drawable.panda_confused)
        emotionImage.visibility = View.VISIBLE
        emotionImage.alpha = 1.0f
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.8f)
            tts.setSpeechRate(0.9f)
            isTtsInitialized = true
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }
}