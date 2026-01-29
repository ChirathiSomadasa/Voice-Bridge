package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

class MoodMatchSevenUpActivity : AppCompatActivity(),
    FeedbackDialogFragment.FeedbackCompletionListener, TextToSpeech.OnInitListener {

    // --- AI MODEL & METRICS ---
    private lateinit var gameMaster: GameMasterModel
    private var currentLevel = 0 // 0=Basic, 1=Context, 2=Scenario
    private var currentMotivationId = 0 // 0=Stars, 1=Quote, 2=Anim
    private var currentFriendActionId = 0 // Capture Friend Action from AI

    private var totalAlpha = 0.0f
    private var totalResponseTime = 0.0f
    private var totalTaps = 0
    private var roundsProcessed = 0

    // Data Collection for Model
    private var startTimeMillis: Long = 0
    private var tapCount = 0 // Counts rapid taps (Frustration proxy)
    private val AGE_GROUP = 9.0f // Hardcoded for "SevenUp" activity (age 9+)

    // --- UI COMPONENTS ---
    private lateinit var videoView: VideoView
    private lateinit var emotionImage: ImageView
    private lateinit var topGameImage1: ImageView
    private lateinit var pandaImage: ImageView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnOption4: Button
    private lateinit var btnNext: Button
    private lateinit var tvScore: TextView
    private lateinit var tvRound: TextView
    private lateinit var guessText: TextView

    // --- GAME STATE ---
    private var currentEmotionResource = "" // The correct answer drawable resource name
    private var currentEmotionDisplay = "" // Formatted emotion for display
    private var correctOption = 0
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5
    private var isAnswerSelected = false
    private var isVideoFinished = false
    private var nextButtonVisible = false

    // --- NEW: TRACK ATTEMPTS AND WRONG ANSWERS ---
    private var wrongAttempts = 0
    private val maxAttemptsPerRound = 3
    private var wrongButtonsRemoved = mutableSetOf<Int>() // Track which wrong buttons were removed

    // Track all images used in current game session
    private val allUsedImages = mutableSetOf<String>()

    // --- TEXT TO SPEECH ---
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var ttsInitializationFailed = false

    // --- SOUND EFFECTS ---
    private lateinit var correctSoundPlayer: MediaPlayer

    companion object {
        private const val TAG = "MoodMatchSevenUp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_up)

        clearPerformanceMetrics()

        // 1. Initialize Helper Classes
        tts = TextToSpeech(this, this)
        gameMaster = GameMasterModel(this)

        // Initialize sound effects
        correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound)

        initializeViews()

        // Setup video
        setupVideoPlayer()
        videoView.setOnCompletionListener {
            onVideoFinished()
        }

        // Start the game immediately (game area is visible)
        setupGame()

        // Start video in background
        videoView.start()
    }

    private fun clearPerformanceMetrics() {
        val prefs = getSharedPreferences("GamePerformance", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    // --- TTS INITIALIZATION ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language not supported!")
                ttsInitializationFailed = true
            } else {
                isTtsInitialized = true

                try {
                    // Voice Selection Strategy for older children
                    val availableVoices = tts.voices
                    val targetVoice = availableVoices?.find { voice ->
                        voice.name.contains("female", ignoreCase = true) &&
                                voice.name.contains("en-us", ignoreCase = true)
                    } ?: availableVoices?.firstOrNull()

                    targetVoice?.let {
                        tts.voice = it
                        Log.d(TAG, "Selected voice: ${it.name}")
                    }

                    // Settings for older children (9+)
                    tts.setPitch(1.2f) // Slightly higher than normal
                    tts.setSpeechRate(0.95f) // Normal to slightly fast

                } catch (e: Exception) {
                    Log.e(TAG, "Error setting TTS voice: ${e.message}")
                }

                Log.d(TAG, "TTS Initialized successfully")
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed!")
            ttsInitializationFailed = true
        }
    }

    private fun initializeViews() {
        videoView = findViewById(R.id.videoView)
        emotionImage = findViewById(R.id.emotionImage)
        topGameImage1 = findViewById(R.id.topGameImage1)
        pandaImage = findViewById(R.id.pandaImage)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnOption4 = findViewById(R.id.btnOption4)
        btnNext = findViewById(R.id.btnNext)
        tvScore = findViewById(R.id.tvScore)
        tvRound = findViewById(R.id.tvRound)
        guessText = findViewById(R.id.guessText)
    }

    private fun setupVideoPlayer() {
        try {
            val videoPath = "android.resource://" + packageName + "/" + R.raw.animated_bear_asks_about_emotion
            val uri = Uri.parse(videoPath)
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = false
            }

            videoView.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Video playback error: what=$what, extra=$extra")
                runOnUiThread {
                    onVideoFinished()
                }
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video: ${e.message}")
            runOnUiThread {
                onVideoFinished()
            }
        }
    }

    private fun onVideoFinished() {
        if (isVideoFinished) return
        isVideoFinished = true

        videoView.animate()
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                videoView.visibility = View.GONE

                // Make top game elements visible
                topGameImage1.visibility = View.VISIBLE
                pandaImage.visibility = View.VISIBLE
                tvScore.visibility = View.VISIBLE
                tvRound.visibility = View.VISIBLE
                guessText.visibility = View.VISIBLE

                // Fade in animation for top elements only
                topGameImage1.alpha = 0f
                pandaImage.alpha = 0f
                tvScore.alpha = 0f
                tvRound.alpha = 0f
                guessText.alpha = 0f

                topGameImage1.animate().alpha(1f).setDuration(1000).start()
                pandaImage.animate().alpha(1f).setDuration(1000).start()
                tvScore.animate().alpha(1f).setDuration(1000).start()
                tvRound.animate().alpha(1f).setDuration(1000).start()
                guessText.animate().alpha(1f).setDuration(1000).start()
            }
            .start()
    }

    private fun setupGame() {
        setupClickListeners()
        startNewRound()
    }

    private fun setupClickListeners() {
        btnOption1.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                checkAnswer(1)
            }
        }

        btnOption2.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                checkAnswer(2)
            }
        }

        btnOption3.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                checkAnswer(3)
            }
        }

        btnOption4.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                checkAnswer(4)
            }
        }

        btnNext.setOnClickListener {
            goToNextRound()
        }
    }

    // --- TRACK TAPS FOR "JITTER/FRUSTRATION" METRIC ---
    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!isAnswerSelected && !nextButtonVisible) {
            tapCount++
            Log.d(TAG, "Tap count increased: $tapCount")
        }
    }

    private fun startNewRound() {
        Log.d(TAG, "Starting new round $currentRound")
        isAnswerSelected = false
        nextButtonVisible = false
        wrongAttempts = 0
        wrongButtonsRemoved.clear()

        // Hide Next button and enable all buttons
        btnNext.visibility = View.GONE
        enableAllButtons(true)
        resetButtonColors()
        showAllButtons() // Make sure all buttons are visible

        // Reset metrics for this specific round
        startTimeMillis = System.currentTimeMillis()
        tapCount = 0

        try {
            // 1. SELECT DATASET BASED ON AI MODEL DECISION (currentLevel)
            // For age 8/9, use the age8 arrays
            val correctArrayId = when(currentLevel) {
                0 -> R.array.mood_age8_lvl0_correct
                1 -> R.array.mood_age8_lvl1_correct
                2 -> R.array.mood_age8_lvl2_scenarios
                else -> R.array.mood_age8_lvl0_correct
            }

            Log.d(TAG, "Using array ID: $correctArrayId for level: $currentLevel")

            // Load correct answers
            val possibleCorrectAnswers = resources.getStringArray(correctArrayId)

            // For scenario level (level 2), we need to get answers from lvl2_answers
            val answerArrayId = if (currentLevel == 2) {
                R.array.mood_age8_lvl2_answers
            } else {
                // For levels 0 and 1, we can use the same array for display names
                correctArrayId
            }
            val possibleAnswers = resources.getStringArray(answerArrayId)

            // Load distractors (use lvl0 distractors for all levels)
            val distractorsArrayId = R.array.mood_age6_lvl0_distractors
            val possibleDistractors = resources.getStringArray(distractorsArrayId)

            Log.d(TAG, "Found ${possibleCorrectAnswers.size} possible correct answers")
            Log.d(TAG, "Found ${possibleDistractors.size} possible distractors")

            // 2. PICK CORRECT ANSWER - AVOID REPETITION
            if (possibleCorrectAnswers.isEmpty()) {
                Log.e(TAG, "No correct answers in array!")
                Toast.makeText(this, "Game data error. Please restart.", Toast.LENGTH_SHORT).show()
                return
            }

            // Create a pool of available images
            val availableImages = mutableListOf<String>()

            for (fullPath in possibleCorrectAnswers) {
                val resourceName = extractResourceName(fullPath, removeExtension = true)
                if (!allUsedImages.contains(resourceName)) {
                    availableImages.add(fullPath)
                }
            }

            // If all images have been used, reset the used set
            val selectionPool = if (availableImages.isNotEmpty()) {
                availableImages
            } else {
                allUsedImages.clear() // Reset if we've used all images
                possibleCorrectAnswers.toList()
            }

            // Select a random image
            val rawSelection = selectionPool.random()
            currentEmotionResource = extractResourceName(rawSelection, removeExtension = true)
            allUsedImages.add(currentEmotionResource)

            // Get corresponding answer for display
            val correctIndex = possibleCorrectAnswers.indexOf(rawSelection)
            val rawAnswer = if (correctIndex < possibleAnswers.size && correctIndex >= 0) {
                possibleAnswers[correctIndex]
            } else {
                rawSelection // Fallback
            }

            // Extract emotion name for display
            currentEmotionDisplay = extractEmotionName(rawAnswer)

            Log.d(TAG, "Selected image: $currentEmotionResource")
            Log.d(TAG, "Emotion display: $currentEmotionDisplay")
            Log.d(TAG, "All used images count: ${allUsedImages.size}")

            // 3. SET IMAGE
            setEmotionImage(currentEmotionResource)

            // 4. PICK WRONG ANSWERS (3 unique ones)
            if (possibleDistractors.isEmpty()) {
                Log.e(TAG, "No distractors in array!")
                Toast.makeText(this, "Game data error. Please restart.", Toast.LENGTH_SHORT).show()
                return
            }

            val wrongEmotions = mutableListOf<String>()
            var attempts = 0
            while (wrongEmotions.size < 3 && attempts < 50) {
                val rawDistractor = possibleDistractors.random()
                val distractorName = extractEmotionName(rawDistractor)

                // Make sure it's not the same as correct emotion
                if (distractorName != currentEmotionDisplay && !wrongEmotions.contains(distractorName)) {
                    wrongEmotions.add(distractorName)
                }
                attempts++
            }

            // If we couldn't get 3 unique, fill with any
            while (wrongEmotions.size < 3) {
                val rawDistractor = possibleDistractors.random()
                val distractorName = extractEmotionName(rawDistractor)
                if (!wrongEmotions.contains(distractorName)) {
                    wrongEmotions.add(distractorName)
                }
            }

            Log.d(TAG, "Selected wrong emotions: $wrongEmotions")

            // 5. RANDOMIZE POSITIONS
            correctOption = Random.nextInt(1, 5)

            // Create list of all options
            val allOptions = mutableListOf<String>()
            for (i in 1..4) {
                if (i == correctOption) {
                    allOptions.add(currentEmotionDisplay)
                } else {
                    allOptions.add(wrongEmotions.removeFirst())
                }
            }

            // Set button texts
            btnOption1.text = formatEmotionText(allOptions[0])
            btnOption2.text = formatEmotionText(allOptions[1])
            btnOption3.text = formatEmotionText(allOptions[2])
            btnOption4.text = formatEmotionText(allOptions[3])

            Log.d(TAG, "Correct option: $correctOption")
            Log.d(TAG, "Button1: ${btnOption1.text}, Button2: ${btnOption2.text}, Button3: ${btnOption3.text}, Button4: ${btnOption4.text}")

            // Update round display
            if (isVideoFinished) {
                tvRound.text = "Round: $currentRound/$totalRounds"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in startNewRound: ${e.message}", e)
            Toast.makeText(this, "Error loading game data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper: Extracts resource name with option to remove file extension
    private fun extractResourceName(fullPath: String, removeExtension: Boolean = false): String {
        var resourceName = fullPath.substringAfterLast("/")
        if (removeExtension) {
            // Remove file extension if present
            resourceName = resourceName.substringBeforeLast(".")
        }
        return resourceName
    }

    private fun extractEmotionName(fullPath: String): String {
        var resName = extractResourceName(fullPath, removeExtension = true)
        Log.d(TAG, "Extracting emotion from resource name: $resName")

        // Split by "_" and take the last part, but handle special cases
        val parts = resName.split("_")

        // Handle different naming patterns
        val emotion = when {
            // For patterns like mood_lvl0_shared_happy -> take "happy"
            // For patterns like mood_lvl1_age8_anxious -> take "anxious"
            parts.size >= 2 -> parts.last()
            // For patterns like happy -> take as is
            else -> resName
        }

        Log.d(TAG, "Extracted emotion: $emotion from parts: $parts")
        return emotion
    }

    // Helper: Formats emotion text (capitalizes first letter)
    private fun formatEmotionText(emotion: String): String {
        return if (emotion.isNotEmpty()) {
            emotion.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "Emotion"
        }
    }

    private fun setEmotionImage(resourceName: String) {
        Log.d(TAG, "Setting emotion image: $resourceName")

        // First try to find the exact resource
        var resourceId = resources.getIdentifier(resourceName, "drawable", packageName)

        // If not found, try without any potential prefixes
        if (resourceId == 0) {
            // Try to extract just the emotion name
            val emotionName = resourceName.split("_").lastOrNull()
            emotionName?.let {
                resourceId = resources.getIdentifier(it, "drawable", packageName)
            }
        }

        // If still not found, try common naming patterns
        if (resourceId == 0) {
            // Try with "mood_" prefix
            resourceId = resources.getIdentifier("mood_$resourceName", "drawable", packageName)
        }

        if (resourceId != 0) {
            emotionImage.setImageResource(resourceId)
            Log.d(TAG, "Image resource ID found: $resourceId for $resourceName")
        } else {
            // Fallback image if not found
            Log.e(TAG, "Image not found: $resourceName, using fallback")
            emotionImage.setImageResource(R.drawable.panda_confused)
        }

        // Make sure image is visible
        emotionImage.visibility = View.VISIBLE
        emotionImage.alpha = 1.0f
    }

    private fun resetButtonColors() {
        val greenColor = ContextCompat.getColor(this, R.color.green)
        btnOption1.setBackgroundColor(greenColor)
        btnOption2.setBackgroundColor(greenColor)
        btnOption3.setBackgroundColor(greenColor)
        btnOption4.setBackgroundColor(greenColor)
    }

    private fun enableAllButtons(enable: Boolean) {
        btnOption1.isEnabled = enable
        btnOption2.isEnabled = enable
        btnOption3.isEnabled = enable
        btnOption4.isEnabled = enable

        // Visual feedback for disabled state
        val alpha = if (enable) 1.0f else 0.5f
        btnOption1.alpha = alpha
        btnOption2.alpha = alpha
        btnOption3.alpha = alpha
        btnOption4.alpha = alpha
    }

    private fun showAllButtons() {
        btnOption1.visibility = View.VISIBLE
        btnOption2.visibility = View.VISIBLE
        btnOption3.visibility = View.VISIBLE
        btnOption4.visibility = View.VISIBLE
    }

    private fun checkAnswer(selectedOption: Int) {
        Log.d(TAG, "Checking answer. Selected: $selectedOption, Correct: $correctOption")
        isAnswerSelected = true

        try {
            // --- 1. CALCULATE METRICS ---
            val endTime = System.currentTimeMillis()
            val responseTimeMs = (endTime - startTimeMillis).toFloat()
            val responseTimeSec = max(1.0f, responseTimeMs / 1000f)

            val isCorrect = (selectedOption == correctOption)
            val accuracyVal = if (isCorrect) 1.0f else 0.0f

            Log.d(TAG, "Response time: ${responseTimeMs}ms, Accuracy: $accuracyVal, Tap count: $tapCount")

            // Calculate Alpha (Achievement Index)
            val ageFactor = 1.0f + ((AGE_GROUP - 6) * 0.1f) // Higher for older children
            val alpha = (accuracyVal * 10f * ageFactor) / ln(responseTimeSec + 1.0f)

            Log.d(TAG, "Calculated Alpha: $alpha, Age factor: $ageFactor")

            totalAlpha += alpha
            totalResponseTime += responseTimeMs
            totalTaps += tapCount
            roundsProcessed++

            val avgAlpha = if (roundsProcessed > 0) totalAlpha / roundsProcessed else 0.0f
            val avgResponseTime = if (roundsProcessed > 0) totalResponseTime / roundsProcessed else 0.0f
            val avgTaps = if (roundsProcessed > 0) totalTaps / roundsProcessed else 0

            val prefs = getSharedPreferences("GamePerformance", MODE_PRIVATE)
            prefs.edit()
                .putFloat("avgAlpha", avgAlpha)
                .putFloat("avgResponseTime", avgResponseTime)
                .putInt("avgTaps", avgTaps)
                .apply()

            Log.d(TAG, "Performance Metrics - Avg Alpha: $avgAlpha, Avg RT: $avgResponseTime, Avg Taps: $avgTaps")

            // --- 2. AI MODEL INFERENCE ---
            val inputVector = floatArrayOf(
                AGE_GROUP,           // Age
                1500.0f,             // Hesitation (avg default)
                tapCount.toFloat(),  // Rapid Taps
                0.08f,               // Jitter (baseline)
                responseTimeMs,      // RT in ms
                accuracyVal,         // Accuracy
                0.5f,                // Weight (Difficulty)
                alpha                // Alpha
            )

            Log.d(TAG, "Input vector for AI: ${inputVector.joinToString()}")

            // Get Decision from TFLite Model (only on correct answer for efficiency)
            if (isCorrect) {
                val decision = gameMaster.predict(inputVector)

                Log.d(TAG, "AI Decision: Level=${decision.emotionLevel}, Motivation=${decision.motivationId}, FriendAction=${decision.friendAction}")

                // APPLY AI DECISIONS
                currentLevel = decision.emotionLevel
                currentMotivationId = decision.motivationId
                currentFriendActionId = decision.friendAction

                Log.d(TAG, "AI Decision -> Next Level: $currentLevel, Reward: $currentMotivationId, FriendAction: $currentFriendActionId, Alpha: $alpha")
            }

            // --- 3. UI FEEDBACK ---
            if (isCorrect) {
                val roundScore = (alpha * 10).toInt()
                score += roundScore
                correctAnswersCount++

                if (isVideoFinished) {
                    tvScore.text = "Score: $score"
                }

                playCorrectSound()

                Log.d(TAG, "Correct answer! Alpha: $alpha, Score added: $roundScore, Total: $score")

                // Show Next button ONLY on correct answer
                showNextButton()
                wrongAttempts = 0

            } else {
                // Wrong answer handling
                wrongAttempts++
                wrongButtonsRemoved.add(selectedOption)

                Log.d(TAG, "Wrong answer. Attempt $wrongAttempts of $maxAttemptsPerRound")

                when (wrongAttempts) {
                    1 -> {
                        // First wrong attempt - Remove the wrong button and say "Try again"
                        removeWrongButton(selectedOption)
                        speakText("Try again")

                        // Reset for another attempt
                        isAnswerSelected = false
                        tapCount = 0
                        startTimeMillis = System.currentTimeMillis()
                    }
                    2 -> {
                        // Second wrong attempt - Show answer with animation
                        speakText("It's $currentEmotionDisplay")
                        showCorrectButtonAnimation()

                        // Reset for another attempt
                        isAnswerSelected = false
                        tapCount = 0
                        startTimeMillis = System.currentTimeMillis()
                    }
                    3 -> {
                        // Third wrong attempt - Move on with motivational message
                        showNextButton()
                        speakText("Good try! Next one")

                        // Reset difficulty for next round (make it easier)
                        if (currentLevel > 0) {
                            currentLevel--
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in checkAnswer: ${e.message}", e)
            // Fallback behavior if AI model fails
            if (selectedOption == correctOption) {
                playCorrectSound()
                showNextButton()
            } else {
                wrongAttempts++
                if (wrongAttempts >= maxAttemptsPerRound) {
                    showNextButton()
                    speakText("Good try! Next one")
                } else {
                    isAnswerSelected = false
                    startTimeMillis = System.currentTimeMillis()
                }
            }
        }
    }

    private fun removeWrongButton(wrongButton: Int) {
        // Make the wrong button invisible
        when (wrongButton) {
            1 -> {
                btnOption1.visibility = View.INVISIBLE
                btnOption1.isEnabled = false
            }
            2 -> {
                btnOption2.visibility = View.INVISIBLE
                btnOption2.isEnabled = false
            }
            3 -> {
                btnOption3.visibility = View.INVISIBLE
                btnOption3.isEnabled = false
            }
            4 -> {
                btnOption4.visibility = View.INVISIBLE
                btnOption4.isEnabled = false
            }
        }
    }

    private fun showCorrectButtonAnimation() {
        val correctButton = when (correctOption) {
            1 -> btnOption1
            2 -> btnOption2
            3 -> btnOption3
            4 -> btnOption4
            else -> btnOption1
        }

        // Pulse animation on the correct button
        correctButton.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(300)
            .withEndAction {
                correctButton.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    private fun showNextButton() {
        nextButtonVisible = true
        btnNext.visibility = View.VISIBLE
        enableAllButtons(false) // Disable other buttons when Next button is visible
    }

    private fun playCorrectSound() {
        try {
            if (correctSoundPlayer.isPlaying) {
                correctSoundPlayer.seekTo(0)
            }
            correctSoundPlayer.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing correct sound: ${e.message}")
        }
    }

    private fun speakText(text: String) {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not initialized yet, skipping speech")
            return
        }

        try {
            tts.setPitch(1.1f)
            tts.setSpeechRate(0.85f)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
            Log.d(TAG, "Speaking text: $text")
        } catch (e: Exception) {
            Log.e(TAG, "TTS error: ${e.message}")
        }
    }

    private fun goToNextRound() {
        Log.d(TAG, "Going to next round. Current: $currentRound, Total: $totalRounds")
        currentRound++
        if (currentRound > totalRounds) {
            showFeedbackPopup()
        } else {
            startNewRound()
        }
    }

    private fun showFeedbackPopup() {
        Log.d(TAG, "Showing feedback popup. Correct: $correctAnswersCount/$totalRounds")
        val isGoodFeedback = correctAnswersCount >= 4

        val feedbackDialog = FeedbackDialogFragment.newInstance(
            isGood = isGoodFeedback,
            correctAnswers = correctAnswersCount,
            totalRounds = totalRounds,
            score = score
        )
        feedbackDialog.show(supportFragmentManager, "feedback_dialog")
    }

    // Callback when "See Scoreboard" is clicked in the popup
    override fun onFeedbackCompleted(correctAnswers: Int, totalRounds: Int, score: Int) {
        Log.d(TAG, "Feedback completed, navigating to scoreboard")
        navigateToScoreboard(correctAnswers, totalRounds, score)
    }

    private fun navigateToScoreboard(correctAnswers: Int, totalRounds: Int, score: Int) {
        try {
            val prefs = getSharedPreferences("GamePerformance", MODE_PRIVATE)
            val avgAlpha = prefs.getFloat("avgAlpha", 0.0f)
            val avgResponseTime = prefs.getFloat("avgResponseTime", 0.0f)
            val avgTaps = prefs.getInt("avgTaps", 0)

            val intent = Intent(this, MMScoreboardActivity::class.java)
            intent.putExtra("CORRECT_ANSWERS", correctAnswers)
            intent.putExtra("TOTAL_ROUNDS", totalRounds)
            intent.putExtra("SCORE", score)

            // PASS AI DECISIONS
            intent.putExtra("MOTIVATION_ID", currentMotivationId)

            // Pass performance metrics
            intent.putExtra("AVG_ALPHA", avgAlpha)
            intent.putExtra("AVG_RESPONSE_TIME", avgResponseTime)
            intent.putExtra("AVG_TAPS", avgTaps)

            // MODEL LOGIC: Unlock gift if Action > 0
            val shouldUnlockGift = currentFriendActionId > 0
            intent.putExtra("UNLOCK_GIFT", shouldUnlockGift)

            intent.putExtra("GAME_MODE", "seven_up")

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to scoreboard: ${e.message}")
            Toast.makeText(this, "Error loading scoreboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isVideoFinished && videoView.visibility == View.VISIBLE) {
            videoView.resume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::correctSoundPlayer.isInitialized) {
            correctSoundPlayer.release()
        }
        videoView.stopPlayback()
    }
}