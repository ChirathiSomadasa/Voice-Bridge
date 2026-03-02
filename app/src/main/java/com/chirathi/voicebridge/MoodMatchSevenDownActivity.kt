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
    private var minigameLaunchedThisSession = false

    // Tracks which option numbers the child tapped wrong this round (for darkening)
    private val wrongTappedOptions = mutableSetOf<Int>()

    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var emotionImage: ImageView

    // Age 6-7
    private lateinit var layoutYoung:  LinearLayout
    private lateinit var btnOption1:   Button
    private lateinit var btnOption2:   Button
    private lateinit var soundOption1: LinearLayout
    private lateinit var soundOption2: LinearLayout

    // Age 8-10 (2×2 grid)
    private lateinit var layoutOlder: LinearLayout
    private lateinit var btnGrid1:    Button
    private lateinit var btnGrid2:    Button
    private lateinit var btnGrid3:    Button
    private lateinit var btnGrid4:    Button

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
    private var pendingSpeech: String? = null   // spoken immediately when TTS becomes ready
    private var correctSoundPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "MoodMatchActivity"
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
        // Only block taps when the round is fully done (next button visible or auto-advancing)
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

        val accuracy    = liveAccuracy()
        val engagement  = if (accuracy > 0.7f) 0.8f else 0.5f
        val frustration = when {
            consecutiveWrong >= 2 -> 0.7f
            consecutiveWrong == 1 -> 0.45f
            else                  -> 0.2f
        }
        val jitter = if (jitterSamples > 0) totalJitter / jitterSamples else 0.10f

        currentPrediction = gameMaster.predictSafe(
            childId            = 0,
            age                = ageGroup.toFloat(),
            accuracy           = accuracy,
            engagement         = engagement,
            frustration        = frustration,
            jitter             = jitter,
            rt                 = 3000f,
            consecutiveCorrect = consecutiveCorrect.toFloat(),
            consecutiveWrong   = consecutiveWrong.toFloat()
        )

        if (isOlderGroup) {
            // ── Age 8-10: use string arrays just like the younger group ──────
            // Level determines which correct-answer pool to draw from.
            val correctArrayId = when (currentPrediction.emotionLevel) {
                1    -> R.array.mood_age8_lvl1_correct
                else -> R.array.mood_age8_lvl0_correct
            }
            val possibleCorrect = resources.getStringArray(correctArrayId)

            // Combined pool of ALL age-8 emotions used as distractor source
            val allAge8 = resources.getStringArray(R.array.mood_age8_lvl0_correct) +
                    resources.getStringArray(R.array.mood_age8_lvl1_correct)

            // Pick correct emotion, avoiding repeats within the session
            val available = possibleCorrect.map { extractEmotionName(it) }.filter { it !in usedImages }
            if (available.isEmpty()) usedImages.clear()
            val pool = if (available.isNotEmpty()) available
            else possibleCorrect.map { extractEmotionName(it) }

            currentEmotion        = pool.random()
            usedImages.add(currentEmotion)
            currentEmotionDisplay = formatEmotionText(currentEmotion)

            // Set the image using the drawable path from the array
            val rawSelection = findImageForEmotion(currentEmotion, possibleCorrect)
            setEmotionImage(extractResourceName(rawSelection, removeExtension = true))

            // 3 unique distractors from the full age-8 pool — shuffle then take
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
            // ── Age 6-7: pull emotions from string-array resources ───────────

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
    // Answer checking — 3-try logic for both age groups
    //
    // Attempt 1 wrong → "Try again"      — all buttons stay enabled
    // Attempt 2 wrong → "It's <emotion>" — correct button breathes, wrong
    //                                       buttons tapped so far are darkened
    // Attempt 3 wrong → "Let's try another one" — all buttons darkened,
    //                                       auto-advance after short delay
    // Correct any time → celebrate, show Next button
    // =========================================================================

    private fun checkAnswer(selectedOption: Int, responseTime: Long) {
        val isCorrect = selectedOption == correctOption

        if (isCorrect) {
            isAnswerSelected = true

            // ── Tiered scoring: reward drops with each attempt needed ────────
            //   1st attempt → 10 pts  (counts toward accuracy)
            //   2nd attempt →  6 pts
            //   3rd attempt →  3 pts
            //   After 3 misses (child finally taps after auto-advance hint) → 1 pt
            val points = when (wrongAttempts) {
                0    -> { correctAnswersCount++; consecutiveCorrect++; consecutiveWrong = 0; 10 }
                1    -> { consecutiveCorrect = 0; consecutiveWrong = 0; 6 }
                2    -> { consecutiveCorrect = 0; consecutiveWrong = 0; 3 }
                else -> { consecutiveCorrect = 0; consecutiveWrong = 0; 1 }
            }
            score += points
            tvScore.text = "Score: $score"
            playCorrectSound()
            highlightButton(selectedOption, correct = true)
            darkenAllButtons(keepCorrectBright = true)
            showNextButton()
            Log.d(TAG, "CORRECT attempt=${wrongAttempts + 1} +${points}pts score=$score")

        } else {
            wrongAttempts++
            consecutiveWrong++
            consecutiveCorrect = 0
            Log.d(TAG, "WRONG attempt $wrongAttempts")

            // Colour the tapped button red immediately
            highlightButton(selectedOption, correct = false)

            when (wrongAttempts) {
                1 -> {
                    // ── First miss: say try again, all buttons stay tappable ──
                    // Do NOT add to wrongTappedOptions here — child must be able
                    // to tap the same wrong button again to reach miss 2 and 3.
                    speakText("Try again")
                    roundStartTime = System.currentTimeMillis()
                }

                2 -> {
                    // ── Second miss: reveal correct answer with breathing ─────
                    // Buttons stay clickable — child can still tap the correct one.
                    // We do NOT block or darken anything yet; only pulse the answer.
                    speakText("It's $currentEmotionDisplay")
                    roundStartTime = System.currentTimeMillis()
                    breatheCorrectButton(pulses = 3)

                    if (shouldLaunchMiniGame()) {
                        minigameLaunchedThisSession = true
                        btnForOption(selectedOption).postDelayed({ launchMiniGame() }, 900)
                    }
                }

                3 -> {
                    // ── Third miss: give up, auto-advance ────────────────────
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
        if (minigameLaunchedThisSession)        return false
        if (currentRound < 2)                   return false
        // Fire if the ML model says so, OR as a hard fallback when the child
        // has been struggling across multiple rounds (consecutiveWrong >= 3).
        val modelSaysYes = currentPrediction.minigameTrigger &&
                (wrongAttempts >= 2 || consecutiveWrong >= 2)
        val fallback     = consecutiveWrong >= 3
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
    // UI helpers
    // =========================================================================

    private fun allOptionButtons(): List<Button> = if (isOlderGroup)
        listOf(btnGrid1, btnGrid2, btnGrid3, btnGrid4)
    else listOf(btnOption1, btnOption2)

    private fun btnForOption(opt: Int): Button = if (isOlderGroup) when (opt) {
        1 -> btnGrid1; 2 -> btnGrid2; 3 -> btnGrid3; else -> btnGrid4
    } else when (opt) { 1 -> btnOption1; else -> btnOption2 }

    private fun resetButtonColors() {
        val green = ContextCompat.getColor(this, R.color.green)
        allOptionButtons().forEach { btn ->
            btn.setBackgroundColor(green)
            btn.isEnabled = true
        }
    }

    private fun highlightButton(option: Int, correct: Boolean) {
        val colorRes = if (correct) R.color.green else android.R.color.holo_red_light
        btnForOption(option).setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    /**
     * Breathing animation on the correct button using alpha — fades bright↔dim
     * [pulses] times. We deliberately avoid scale so the button never physically
     * grows and intercepts taps meant for a neighbouring wrong button.
     */
    private fun breatheCorrectButton(pulses: Int) {
        val btn = btnForOption(correctOption)
        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        btn.alpha = 1f

        fun doPulse(remaining: Int) {
            if (remaining <= 0) { btn.alpha = 1f; return }
            btn.animate()
                .alpha(0.3f)
                .setDuration(400)
                .withEndAction {
                    btn.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .withEndAction { doPulse(remaining - 1) }
                        .start()
                }.start()
        }
        doPulse(pulses)
    }

    /**
     * Darken only the buttons the child already tapped wrong (red → dark red).
     * The correct button and un-tapped buttons are left unchanged and still enabled.
     */
    private fun darkenWrongButtons() {
        val darkRed = android.graphics.Color.parseColor("#B71C1C")
        for (opt in wrongTappedOptions) {
            val btn = btnForOption(opt)
            btn.setBackgroundColor(darkRed)
            btn.isEnabled = false
        }
    }

    /**
     * Darken every button at the end of a round.
     *
     * @param keepCorrectBright  true  → correct button stays full green (used on
     *                                   a correct answer so the child sees it shine)
     *                           false → correct button also gets dark green (used
     *                                   when all 3 attempts are exhausted)
     */
    private fun darkenAllButtons(keepCorrectBright: Boolean) {
        val darkGreen = android.graphics.Color.parseColor("#1B5E20")
        val darkRed   = android.graphics.Color.parseColor("#B71C1C")
        val fullGreen = ContextCompat.getColor(this, R.color.green)

        allOptionButtons().forEachIndexed { _, btn ->
            btn.isEnabled = false
        }

        // Colour each button according to its role
        for (opt in 1..allOptionButtons().size) {
            val btn = btnForOption(opt)
            when {
                opt == correctOption && keepCorrectBright ->
                    btn.setBackgroundColor(fullGreen)           // stays bright green
                opt == correctOption ->
                    btn.setBackgroundColor(darkGreen)           // dark green on 3rd fail
                opt in wrongTappedOptions ->
                    btn.setBackgroundColor(darkRed)             // dark red for tapped-wrong
                else ->
                    btn.setBackgroundColor(darkGreen)           // dark green for untouched distractors
            }
        }
    }

    /** Disable all buttons and sound panels without changing their background colour. */
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
    // Navigation
    // =========================================================================

    private fun goToNextRound() {
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

    private fun navigateToScoreboard() {
        startActivity(Intent(this, MMScoreboardActivity::class.java).apply {
            putExtra("CORRECT_ANSWERS", correctAnswersCount)
            putExtra("TOTAL_ROUNDS",    totalRounds)
            putExtra("SCORE",           score)
            putExtra("AGE_GROUP",       ageGroup)
            putExtra("MOTIVATION_ID",   currentPrediction.motivation)
            putExtra("UNLOCK_GIFT",     currentPrediction.friendAction > 0)
            putExtra("GAME_MODE",       "seven_down")
        })
        finish()
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
            // Try stripping any path prefix e.g. if full path was accidentally passed
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
            // Flush anything that was queued before TTS was ready (e.g. round-1 prompts)
            pendingSpeech?.let { tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, ""); pendingSpeech = null }
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            // TTS not ready yet — save the most recent prompt and fire it once ready
            pendingSpeech = text
        }
    }
}