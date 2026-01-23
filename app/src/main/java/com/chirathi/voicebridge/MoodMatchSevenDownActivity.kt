package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

class MoodMatchSevenDownActivity : AppCompatActivity(),
    FeedbackDialogFragment.FeedbackCompletionListener, TextToSpeech.OnInitListener {

    // --- AI MODEL & METRICS ---
    private lateinit var gameMaster: GameMasterModel
    private var currentLevel = 0 // 0=Basic, 1=Context, 2=Scenario
    private var currentMotivationId = 0 // 0=Stars, 1=Quote, 2=Anim
    private var currentFriendActionId = 0 // NEW: Capture Friend Action from AI

    private var totalAlpha = 0.0f
    private var totalResponseTime = 0.0f
    private var totalTaps = 0
    private var roundsProcessed = 0

    // Data Collection for Model
    private var startTimeMillis: Long = 0
    private var tapCount = 0 // Counts rapid taps (Frustration proxy)
    private val AGE_GROUP = 6.0f // Hardcoded for "SevenDown" activity

    // --- UI COMPONENTS ---
    private lateinit var videoView: VideoView
    private lateinit var emotionImage: ImageView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var soundOption1: LinearLayout
    private lateinit var soundOption2: LinearLayout
    private lateinit var btnNext: Button
    private lateinit var tvScore: TextView
    private lateinit var tvRound: TextView
    private lateinit var topGameImage1: ImageView
    private lateinit var pandaImage: ImageView
    private lateinit var guessText: TextView

    // --- GAME STATE ---
    private var currentEmotion = "" // The correct answer text (e.g. "Happy")
    private var currentEmotionDisplay = "" // Formatted emotion for display
    private var distractorEmotion = "" // The wrong answer text
    private var correctOption = 1 // 1 or 2
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5
    private var isAnswerSelected = false
    private var isVideoFinished = false
    private var isVideoPlaying = false
    private var nextButtonVisible = false

    // --- NEW: TRACK USED IMAGES AND ATTEMPTS ---
    private val usedImages = mutableSetOf<String>() // Track used images to avoid repetition
    private var wrongAttempts = 0 // Track wrong attempts in current round
    private var maxAttemptsPerRound = 3 // Maximum wrong attempts allowed before moving on
    private var isShowingHint = false // Track if we're showing a hint
    private var originalDifficultyLevel = 0 // Store original difficulty

    // --- TEXT TO SPEECH ---
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var ttsInitializationFailed = false

    // --- SOUND EFFECTS ---
    private lateinit var correctSoundPlayer: MediaPlayer

    companion object {
        private const val TAG = "MoodMatchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_down)

        clearPerformanceMetrics()

        // 1. Initialize Helper Classes
        tts = TextToSpeech(this, this)
        gameMaster = GameMasterModel(this)

        // Initialize sound effects
        correctSoundPlayer = MediaPlayer.create(this, R.raw.correct_sound)

        // 2. Initialize Views
        initializeViews()

        // 3. Setup Intro Video (Bear asking "How does he feel?")
        setupVideoPlayer()
        videoView.setOnCompletionListener {
            onVideoFinished()
        }

        // 4. Start video first
        videoView.start()
        isVideoPlaying = true

        // 5. Setup game immediately (emotion image and options will be visible)
        setupGame()
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
                    // 1. Voice Selection Strategy:
                    // First look for "child" specifically.
                    // If not found, look for a FEMALE voice (they pitch-shift better to child voices).
                    val availableVoices = tts.voices
                    val targetVoice = availableVoices?.find { voice ->
                        voice.name.contains("child", ignoreCase = true) ||
                                voice.name.contains("kids", ignoreCase = true)
                    } ?: availableVoices?.find { voice ->
                        // Fallback: Find a female voice (often 'en-us-x-sfg' or similar features)
                        !voice.name.contains("male", ignoreCase = true) &&
                                voice.name.contains("en-us", ignoreCase = true)
                    }

                    targetVoice?.let {
                        tts.voice = it
                        Log.d(TAG, "Selected voice: ${it.name}")
                    }

                    // 2. Child Simulation Settings:
                    // Pitch: 1.6f - 1.8f simulates a young child best
                    // Rate: 0.9f ensures they speak clearly and not too fast
                    tts.setPitch(1.8f)
                    tts.setSpeechRate(0.9f)

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
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        soundOption1 = findViewById(R.id.soundOption1)
        soundOption2 = findViewById(R.id.soundOption2)
        btnNext = findViewById(R.id.btnNext)
        tvScore = findViewById(R.id.tvScore)
        tvRound = findViewById(R.id.tvRound)
        topGameImage1 = findViewById(R.id.topGameImage1)
        pandaImage = findViewById(R.id.pandaImage)
        guessText = findViewById(R.id.guessText)

        // Header elements (visible only after video)
        topGameImage1.visibility = View.INVISIBLE
        pandaImage.visibility = View.INVISIBLE
        guessText.visibility = View.INVISIBLE

        // Game elements (always visible)
        emotionImage.visibility = View.VISIBLE
        btnOption1.visibility = View.VISIBLE
        btnOption2.visibility = View.VISIBLE
        soundOption1.visibility = View.VISIBLE
        soundOption2.visibility = View.VISIBLE
        tvScore.visibility = View.VISIBLE
        tvRound.visibility = View.VISIBLE
    }

    private fun setupVideoPlayer() {
        try {
            val videoPath = "android.resource://" + packageName + "/" + R.raw.animated_bear_asks_about_emotion
            val uri = Uri.parse(videoPath)
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = false
            }
            videoView.setOnErrorListener { _, _, _ ->
                Log.e(TAG, "Video playback error")
                isVideoPlaying = false
                videoView.visibility = View.GONE
                // If video fails, start game immediately
                runOnUiThread {
                    showHeaderAfterVideo()
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video setup error: ${e.message}")
            videoView.visibility = View.GONE
            isVideoPlaying = false
            // If video setup fails, start game immediately
            runOnUiThread {
                showHeaderAfterVideo()
            }
        }
    }

    private fun onVideoFinished() {
        if (isVideoFinished) return
        isVideoFinished = true
        isVideoPlaying = false

        Log.d(TAG, "Video finished, showing header")

        // Fade out video
        videoView.animate().alpha(0f).setDuration(500).withEndAction {
            videoView.visibility = View.GONE
            // Show header after video is gone
            showHeaderAfterVideo()
        }.start()
    }

    private fun showHeaderAfterVideo() {
        Log.d(TAG, "Showing header after video")

        // Show header elements only
        val headerViews = listOf(topGameImage1, pandaImage, guessText)
        headerViews.forEach { view ->
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(800).start()
        }
    }

    private fun setupGame() {
        setupClickListeners()
        startNewRound()
    }

    private fun setupClickListeners() {
        // Option Buttons
        btnOption1.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                Log.d(TAG, "Option 1 clicked, text: ${btnOption1.text}")
                checkAnswer(1)
            }
        }
        btnOption2.setOnClickListener {
            if (!isAnswerSelected && !nextButtonVisible) {
                Log.d(TAG, "Option 2 clicked, text: ${btnOption2.text}")
                checkAnswer(2)
            }
        }

        // TTS Buttons (Speaker Icons)
        soundOption1.setOnClickListener {
            if (!nextButtonVisible) {
                val text = btnOption1.text.toString()
                Log.d(TAG, "Speaking option 1: $text")
                speakText(text)
            }
        }
        soundOption2.setOnClickListener {
            if (!nextButtonVisible) {
                val text = btnOption2.text.toString()
                Log.d(TAG, "Speaking option 2: $text")
                speakText(text)
            }
        }

        // Next Button
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

    // --- CORE GAME LOOP ---
    private fun startNewRound() {
        Log.d(TAG, "Starting new round $currentRound")
        isAnswerSelected = false
        isShowingHint = false
        wrongAttempts = 0
        nextButtonVisible = false

        // Hide Next button and enable other buttons
        btnNext.visibility = View.GONE
        enableGameButtons(true)

        // Reset button colors - Use proper color resource
        val greenColor = ContextCompat.getColor(this, R.color.green)
        btnOption1.setBackgroundColor(greenColor)
        btnOption2.setBackgroundColor(greenColor)

        // Reset metrics for this specific round
        startTimeMillis = System.currentTimeMillis()
        tapCount = 0

        try {
            // 1. SELECT DATASET BASED ON AI MODEL DECISION (currentLevel)
            // If level is 0 -> Basic, 1 -> Contextual, 2 -> Scenario
            val correctArrayId = when(currentLevel) {
                0 -> R.array.mood_age6_lvl0_correct
                1 -> R.array.mood_age6_lvl1_correct
                2 -> R.array.mood_age6_lvl2_scenarios
                else -> R.array.mood_age6_lvl0_correct
            }

            Log.d(TAG, "Using array ID: $correctArrayId for level: $currentLevel")

            // Load distractors (Wrong answers)
            val distractorsArrayId = when(currentLevel) {
                0 -> R.array.mood_age6_lvl0_distractors
                1 -> R.array.mood_age6_lvl0_distractors
                2 -> R.array.mood_age6_lvl0_distractors
                else -> R.array.mood_age6_lvl0_distractors
            }

            val possibleCorrectAnswers = resources.getStringArray(correctArrayId)
            val possibleDistractors = resources.getStringArray(distractorsArrayId)

            Log.d(TAG, "Found ${possibleCorrectAnswers.size} possible correct answers")
            Log.d(TAG, "Found ${possibleDistractors.size} possible distractors")

            // 2. PICK CORRECT ANSWER - AVOID REPETITION WITH IMPROVED LOGIC
            if (possibleCorrectAnswers.isEmpty()) {
                Log.e(TAG, "No correct answers in array!")
                Toast.makeText(this, "Game data error. Please restart.", Toast.LENGTH_SHORT).show()
                return
            }

            // Create a pool of available answers (emotion names, not file paths)
            val availableEmotions = mutableListOf<String>()

            for (fullPath in possibleCorrectAnswers) {
                val emotionName = extractEmotionName(fullPath)
                if (!usedImages.contains(emotionName)) {
                    availableEmotions.add(emotionName)
                }
            }

            // If all emotions have been used, reset the used set
            val selectionPool = if (availableEmotions.isNotEmpty()) {
                availableEmotions
            } else {
                usedImages.clear() // Reset if we've used all emotions
                possibleCorrectAnswers.map { extractEmotionName(it) }
            }

            // Select a random emotion
            currentEmotion = selectionPool.random()
            usedImages.add(currentEmotion) // Mark emotion as used

            Log.d(TAG, "Selected emotion: $currentEmotion")
            Log.d(TAG, "Used emotions count: ${usedImages.size}")

            // Find corresponding image for the selected emotion
            val rawSelection = findImageForEmotion(currentEmotion, possibleCorrectAnswers)

            // For scenario level (level 2), we need to get the corresponding answer from lvl2_answers
            if (currentLevel == 2) {
                val scenarioIndex = possibleCorrectAnswers.indexOf(rawSelection)
                val answerArray = resources.getStringArray(R.array.mood_age6_lvl2_answers)
                if (scenarioIndex < answerArray.size) {
                    currentEmotion = extractEmotionName(answerArray[scenarioIndex])
                }
                // Note: currentEmotion is already set from selectionPool
            }

            // Get formatted emotion for display (capitalized)
            currentEmotionDisplay = formatEmotionText(currentEmotion)

            // Extract resource name WITHOUT file extension
            val correctImageRes = extractResourceName(rawSelection, removeExtension = true)

            Log.d(TAG, "Current emotion: $currentEmotion, Display: $currentEmotionDisplay, Image resource: $correctImageRes")

            // 3. SET IMAGE
            setEmotionImage(correctImageRes)

            // 4. PICK WRONG ANSWER (Ensure it's not same as correct)
            if (possibleDistractors.isEmpty()) {
                Log.e(TAG, "No distractors in array!")
                Toast.makeText(this, "Game data error. Please restart.", Toast.LENGTH_SHORT).show()
                return
            }

            var rawDistractor = possibleDistractors.random()
            var distractorEmotionName = extractEmotionName(rawDistractor)

            // Loop until we find a distinct wrong answer
            var attempts = 0
            while (distractorEmotionName.equals(currentEmotion, ignoreCase = true) && attempts < 10) {
                rawDistractor = possibleDistractors.random()
                distractorEmotionName = extractEmotionName(rawDistractor)
                attempts++
                Log.d(TAG, "Distractor attempt $attempts: $distractorEmotionName")
            }

            this.distractorEmotion = distractorEmotionName
            val distractorEmotionDisplay = formatEmotionText(distractorEmotionName)

            Log.d(TAG, "Selected distractor: $distractorEmotionName, Display: $distractorEmotionDisplay")

            // 5. RANDOMIZE POSITIONS
            correctOption = Random.nextInt(1, 3) // 1 or 2

            // Set button texts with formatted emotions (not hardcoded)
            if (correctOption == 1) {
                btnOption1.text = currentEmotionDisplay
                btnOption2.text = distractorEmotionDisplay
            } else {
                btnOption1.text = distractorEmotionDisplay
                btnOption2.text = currentEmotionDisplay
            }

            Log.d(TAG, "Correct option: $correctOption")
            Log.d(TAG, "Button1: ${btnOption1.text}, Button2: ${btnOption2.text}")

            // Update round display
            tvRound.text = "Round: $currentRound/$totalRounds"

        } catch (e: Exception) {
            Log.e(TAG, "Error in startNewRound: ${e.message}", e)
            Toast.makeText(this, "Error loading game data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to find image for selected emotion
    private fun findImageForEmotion(emotion: String, possibleCorrectAnswers: Array<String>): String {
        // First try to find exact match
        for (fullPath in possibleCorrectAnswers) {
            if (extractEmotionName(fullPath).equals(emotion, ignoreCase = true)) {
                return fullPath
            }
        }

        // If not found, return the first one (fallback)
        return possibleCorrectAnswers.first()
    }

    private fun enableGameButtons(enable: Boolean) {
        btnOption1.isEnabled = enable
        btnOption2.isEnabled = enable
        soundOption1.isEnabled = enable
        soundOption2.isEnabled = enable

        // Visual feedback for disabled state
        if (!enable) {
            btnOption1.alpha = 0.5f
            btnOption2.alpha = 0.5f
            soundOption1.alpha = 0.5f
            soundOption2.alpha = 0.5f
        } else {
            btnOption1.alpha = 1.0f
            btnOption2.alpha = 1.0f
            soundOption1.alpha = 1.0f
            soundOption2.alpha = 1.0f
        }
    }

    // Helper: Formats emotion text (capitalizes first letter)
    private fun formatEmotionText(emotion: String): String {
        return if (emotion.isNotEmpty()) {
            // Remove any file extension if present
            val cleanEmotion = emotion.replace(".png", "").replace(".jpg", "").replace(".jpeg", "")
            cleanEmotion.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            "Emotion"
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
            parts.size >= 2 -> parts.last()
            // For patterns like happy -> take as is
            else -> resName
        }

        Log.d(TAG, "Extracted emotion: $emotion from parts: $parts")
        return emotion
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

    private fun checkAnswer(selectedOption: Int) {
        Log.d(TAG, "Checking answer. Selected: $selectedOption, Correct: $correctOption")
        isAnswerSelected = true

        try {
            // --- 1. CALCULATE METRICS ---
            val endTime = System.currentTimeMillis()
            // Convert to seconds for the formula, guard against 0
            val responseTimeMs = (endTime - startTimeMillis).toFloat()
            val responseTimeSec = max(1.0f, responseTimeMs / 1000f)

            val isCorrect = (selectedOption == correctOption)
            val accuracyVal = if (isCorrect) 1.0f else 0.0f

            Log.d(TAG, "Response time: ${responseTimeMs}ms, Accuracy: $accuracyVal, Tap count: $tapCount")

            // Calculate Alpha (Achievement Index)
            // Formula: (Accuracy * 10 * AgeConstant) / log(RT + Attempts)
            // Note: We use ln (natural log) + 1 to avoid divide by zero
            val ageFactor = 1.0f + ((AGE_GROUP - 6) * 0.1f) // 1.0 for age 6
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
            // Input Vector: [Age, Hesitation, RapidTaps, Jitter, RT, Accuracy, Weight, Alpha]
            // Note: Jitter/Hesitation are simulated/placeholders here until touch listener is fully mapped
            val inputVector = floatArrayOf(
                AGE_GROUP,           // Age
                1500.0f,             // Hesitation (avg default)
                tapCount.toFloat(),  // Rapid Taps (from user interaction)
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
                currentLevel = decision.emotionLevel // Updates difficulty for NEXT round
                currentMotivationId = decision.motivationId // Updates reward type for Scoreboard
                currentFriendActionId = decision.friendAction // CAPTURE Friend Action

                Log.d(TAG, "AI Decision -> Next Level: $currentLevel, Reward: $currentMotivationId, FriendAction: $currentFriendActionId, Alpha: $alpha")
            }

            // --- 3. UI FEEDBACK ---
            if (isCorrect) {
                val roundScore = (alpha * 10).toInt()
                score += roundScore
                correctAnswersCount++
                tvScore.text = "Score: $score"

                playCorrectSound()

                Log.d(TAG, "Correct answer! Alpha: $alpha, Score added: $roundScore, Total: $score")

                // Show Next button ONLY on correct answer
                showNextButton()
                wrongAttempts = 0 // Reset wrong attempts

            } else {
                // Wrong answer handling
                wrongAttempts++

                Log.d(TAG, "Wrong answer. Attempt $wrongAttempts of $maxAttemptsPerRound")

                when (wrongAttempts) {
                    1 -> {
                        // First wrong attempt - Simple prompt
                        speakText("Try again")

                        // Reset for another attempt
                        isAnswerSelected = false
                        tapCount = 0 // Reset tap count for new attempt
                        startTimeMillis = System.currentTimeMillis() // Reset timer
                    }
                    2 -> {
                        // Second wrong attempt - Show answer with animation
                        speakText("It's $currentEmotionDisplay")
                        showCorrectButtonAnimation()

                        // Reset for another attempt
                        isAnswerSelected = false
                        tapCount = 0 // Reset tap count for new attempt
                        startTimeMillis = System.currentTimeMillis() // Reset timer
                    }
                    3 -> {
                        // Third wrong attempt - Move on with motivational message
                        // Show Next button immediately
                        showNextButton()

                        // Speak simple motivational message
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

    private fun showNextButton() {
        nextButtonVisible = true
        btnNext.visibility = View.VISIBLE
        enableGameButtons(false) // Disable other buttons when Next button is visible
    }

    private fun showCorrectButtonAnimation() {
        val correctButton = if (correctOption == 1) btnOption1 else btnOption2

        // Simple pulse animation on the correct button
        correctButton.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
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

    private fun speakText(text: String) {
        if (!isTtsInitialized) {
            Log.w(TAG, "TTS not initialized yet, skipping speech")
            return
        }

        try {
            // Use simple, clear speech for children
            tts.setPitch(1.1f) // Slightly higher pitch
            tts.setSpeechRate(0.85f) // Slower for better comprehension

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
        // Show the dialog
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

            intent.putExtra("AVG_ALPHA", avgAlpha)
            intent.putExtra("AVG_RESPONSE_TIME", avgResponseTime)
            intent.putExtra("AVG_TAPS", avgTaps)

            // MODEL LOGIC: Unlock gift if Action > 0 (Meaning AI detected frustration OR high success)
            val shouldUnlockGift = currentFriendActionId > 0
            intent.putExtra("UNLOCK_GIFT", shouldUnlockGift)

            intent.putExtra("GAME_MODE", "seven_down")

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to scoreboard: ${e.message}")
            Toast.makeText(this, "Error loading scoreboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::correctSoundPlayer.isInitialized) {
            correctSoundPlayer.release()
        }
        super.onDestroy()
    }
}