package com.chirathi.voicebridge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MoodMatchSevenUpActivity : AppCompatActivity(),
    FeedbackDialogFragment.FeedbackCompletionListener {

    // Define all emotions for Seven Up (more emotions)
    private val emotions = listOf(
        "happy", "sad", "angry", "scared", "shy", "bored",
        "proud", "surprise", "curious", "tired", "anxious", "greedy",
        "jealous", "cheerful", "sleepy", "disgusted"
    )

    // Similar emotions that shouldn't appear together in same round
    private val similarEmotions = mapOf(
        "happy" to listOf("cheerful", "proud"),
        "cheerful" to listOf("happy", "proud"),
        "proud" to listOf("happy", "cheerful"),
        "tired" to listOf("sleepy", "bored"),
        "sleepy" to listOf("tired", "bored", "sad"),
        "bored" to listOf("tired", "sleepy"),
        "scared" to listOf("anxious"),
        "anxious" to listOf("scared"),
        "jealous" to listOf("sad"),
        "shy" to listOf("sad"),
    )

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

    private var currentEmotion = ""
    private var correctOption = 0
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5
    private var isAnswerSelected = false
    private var isVideoFinished = false

    // Track all emotions used in current game session
    private val allUsedEmotions = mutableSetOf<String>()
    // Track emotions used in current round (to prevent repeats within same round)
    private val usedEmotionsInCurrentRound = mutableSetOf<String>()

    companion object {
        private const val TAG = "MoodMatchSevenUp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_up)

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
                // If video fails, show top UI immediately
                runOnUiThread {
                    onVideoFinished()
                }
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up video: ${e.message}")
            // If any error, show top UI immediately
            runOnUiThread {
                onVideoFinished()
            }
        }
    }

    private fun onVideoFinished() {
        if (isVideoFinished) return

        isVideoFinished = true

        // Fade out video
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
            if (!isAnswerSelected) {
                checkAnswer(1)
            }
        }

        btnOption2.setOnClickListener {
            if (!isAnswerSelected) {
                checkAnswer(2)
            }
        }

        btnOption3.setOnClickListener {
            if (!isAnswerSelected) {
                checkAnswer(3)
            }
        }

        btnOption4.setOnClickListener {
            if (!isAnswerSelected) {
                checkAnswer(4)
            }
        }

        btnNext.setOnClickListener {
            goToNextRound()
        }
    }

    private fun startNewRound() {
        isAnswerSelected = false
        btnNext.visibility = View.GONE

        // Clear only the current round tracker, keep the allUsedEmotions
        usedEmotionsInCurrentRound.clear()

        // Select random emotion for this round
        currentEmotion = getRandomEmotion()
        usedEmotionsInCurrentRound.add(currentEmotion)
        allUsedEmotions.add(currentEmotion)

        Log.d(TAG, "Current emotion: $currentEmotion")
        Log.d(TAG, "All used emotions: $allUsedEmotions")

        // Load the image
        setEmotionImage(currentEmotion)

        // Determine correct option position
        correctOption = Random.nextInt(1, 5)

        // Get 3 unique wrong emotions (avoiding similar emotions)
        val wrongEmotions = getUniqueWrongEmotions(3)

        Log.d(TAG, "Correct option position: $correctOption")
        Log.d(TAG, "Wrong emotions: $wrongEmotions")

        // Assign emotions to buttons
        val options = mutableListOf<String>()
        for (i in 1..4) {
            if (i == correctOption) {
                options.add(currentEmotion)
            } else {
                options.add(wrongEmotions.removeFirst())
            }
        }

        // Set button texts
        btnOption1.text = options[0].replaceFirstChar { it.uppercase() }
        btnOption2.text = options[1].replaceFirstChar { it.uppercase() }
        btnOption3.text = options[2].replaceFirstChar { it.uppercase() }
        btnOption4.text = options[3].replaceFirstChar { it.uppercase() }

        // Reset button colors
        resetButtonColors()

        // Update round display (only if video finished)
        if (isVideoFinished) {
            tvRound.text = "Round: $currentRound/$totalRounds"
        }
    }

    private fun getRandomEmotion(): String {
        // First, try to get an emotion not used in current round AND not used in entire game
        val availableEmotions = emotions.filter {
            !usedEmotionsInCurrentRound.contains(it) && !allUsedEmotions.contains(it)
        }

        if (availableEmotions.isNotEmpty()) {
            return availableEmotions.random()
        }

        // If all emotions have been used in the entire game (5 rounds * 4 emotions = 20 emotions shown)
        // Then we can start reusing emotions
        if (allUsedEmotions.size >= 15) { // After about 15 emotions shown
            val emotionsNotInCurrentRound = emotions.filter { !usedEmotionsInCurrentRound.contains(it) }
            if (emotionsNotInCurrentRound.isNotEmpty()) {
                return emotionsNotInCurrentRound.random()
            }
        }

        // Last resort: if we can't find any emotion not in current round
        // Reset the current round tracker and pick any emotion
        usedEmotionsInCurrentRound.clear()
        return emotions.random()
    }

    private fun getUniqueWrongEmotions(count: Int): MutableList<String> {
        val wrongEmotions = mutableListOf<String>()
        val attemptsLimit = 100
        var attempts = 0

        while (wrongEmotions.size < count && attempts < attemptsLimit) {
            attempts++

            val wrongEmotion = getRandomWrongEmotion()

            // Check if this emotion is already selected or is similar to current emotion
            if (wrongEmotion != currentEmotion &&
                !wrongEmotions.contains(wrongEmotion) &&
                !isSimilarToCurrent(wrongEmotion)) {

                // Also check if it's similar to any already selected wrong emotions
                var isSimilarToOthers = false
                for (emotion in wrongEmotions) {
                    if (isSimilar(emotion, wrongEmotion)) {
                        isSimilarToOthers = true
                        break
                    }
                }

                if (!isSimilarToOthers) {
                    wrongEmotions.add(wrongEmotion)
                    usedEmotionsInCurrentRound.add(wrongEmotion)
                }
            }
        }

        // If we couldn't get enough unique non-similar emotions, just get any unique ones
        if (wrongEmotions.size < count) {
            while (wrongEmotions.size < count) {
                val backupEmotion = getRandomWrongEmotion()
                if (backupEmotion != currentEmotion && !wrongEmotions.contains(backupEmotion)) {
                    wrongEmotions.add(backupEmotion)
                    usedEmotionsInCurrentRound.add(backupEmotion)
                }
            }
        }

        return wrongEmotions
    }

    private fun getRandomWrongEmotion(): String {
        // First try emotions not used in current round
        val emotionsNotInCurrentRound = emotions.filter { !usedEmotionsInCurrentRound.contains(it) }
        if (emotionsNotInCurrentRound.isNotEmpty()) {
            return emotionsNotInCurrentRound.random()
        }

        // Last resort: return any emotion
        return emotions.random()
    }

    private fun isSimilarToCurrent(emotion: String): Boolean {
        return isSimilar(currentEmotion, emotion)
    }

    private fun isSimilar(emotion1: String, emotion2: String): Boolean {
        return similarEmotions[emotion1]?.contains(emotion2) == true ||
                similarEmotions[emotion2]?.contains(emotion1) == true
    }

    private fun setEmotionImage(emotion: String) {
        try {
            val resourceId = resources.getIdentifier(emotion, "drawable", packageName)
            if (resourceId != 0) {
                emotionImage.setImageResource(resourceId)
            } else {
                val alternativeName = emotion.replace(" ", "_").lowercase()
                val altResourceId = resources.getIdentifier(alternativeName, "drawable", packageName)
                if (altResourceId != 0) {
                    emotionImage.setImageResource(altResourceId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}")
        }
    }

    private fun resetButtonColors() {
        btnOption1.setBackgroundColor(resources.getColor(R.color.green))
        btnOption2.setBackgroundColor(resources.getColor(R.color.green))
        btnOption3.setBackgroundColor(resources.getColor(R.color.green))
        btnOption4.setBackgroundColor(resources.getColor(R.color.green))
    }

    private fun checkAnswer(selectedOption: Int) {
        isAnswerSelected = true

        highlightButtons(selectedOption)

        if (selectedOption == correctOption) {
            score += 20
            correctAnswersCount++

            // Update score display (only if video finished)
            if (isVideoFinished) {
                tvScore.text = "Score: $score"
            }

            Toast.makeText(this, "Correct! Well done!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong! The correct answer was ${currentEmotion.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
        }

        btnNext.visibility = View.VISIBLE
    }

    private fun highlightButtons(selectedOption: Int) {
        resetButtonColors()

        when (correctOption) {
            1 -> btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            2 -> btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            3 -> btnOption3.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            4 -> btnOption4.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        }

        if (selectedOption != correctOption) {
            when (selectedOption) {
                1 -> btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                2 -> btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                3 -> btnOption3.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                4 -> btnOption4.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun goToNextRound() {
        currentRound++

        if (currentRound > totalRounds) {
            // Game over, show feedback popup based on score
            Log.d(TAG, "=== GAME OVER ===")
            Log.d(TAG, "Final Score: $score, Correct Answers: $correctAnswersCount/$totalRounds")
            showFeedbackPopup()
        } else {
            startNewRound()
        }
    }

    private fun showFeedbackPopup() {
        // Determine if it's good or bad feedback
        // For 4 options, let's use 3+ correct as good feedback (60%+)
        val isGoodFeedback = correctAnswersCount >= 4

        Log.d(TAG, "=== SHOWING FEEDBACK ===")
        Log.d(TAG, "Is good feedback: $isGoodFeedback")
        Log.d(TAG, "Correct answers: $correctAnswersCount")
        Log.d(TAG, "Total rounds: $totalRounds")
        Log.d(TAG, "Score: $score")

        // Create the dialog with data
        val feedbackDialog = FeedbackDialogFragment.newInstance(
            isGood = isGoodFeedback,
            correctAnswers = correctAnswersCount,
            totalRounds = totalRounds,
            score = score
        )

        // Show the dialog
        feedbackDialog.show(supportFragmentManager, "feedback_dialog")

        Log.d(TAG, "Feedback dialog shown")
    }

    // Interface callback implementation
    override fun onFeedbackCompleted(correctAnswers: Int, totalRounds: Int, score: Int) {
        Log.d(TAG, "=== FEEDBACK COMPLETED CALLBACK ===")
        Log.d(TAG, "Received from dialog - Correct: $correctAnswers, Total: $totalRounds, Score: $score")

        // Navigate to scoreboard
        navigateToScoreboard(correctAnswers, totalRounds, score)
    }

    private fun navigateToScoreboard(correctAnswers: Int, totalRounds: Int, score: Int) {
        Log.d(TAG, "Navigating to scoreboard...")

        val intent = Intent(this, MMScoreboardActivity::class.java)
        intent.putExtra("CORRECT_ANSWERS", correctAnswers)
        intent.putExtra("TOTAL_ROUNDS", totalRounds)
        intent.putExtra("SCORE", score)
        intent.putExtra("GAME_MODE", "seven_up")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        Log.d(TAG, "Starting scoreboard activity")
        startActivity(intent)

        Log.d(TAG, "Finishing game activity")
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume video if it's still playing
        if (!isVideoFinished && videoView.visibility == View.VISIBLE) {
            videoView.resume()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}