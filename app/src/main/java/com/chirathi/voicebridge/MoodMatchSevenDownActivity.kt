package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MoodMatchSevenDownActivity : AppCompatActivity(),
    FeedbackDialogFragment.FeedbackCompletionListener {

    // Define all emotions
    private val emotions = listOf(
        "happyness", "sadness", "angryness", "scared",
        "bored", "shy", "proud"
    )

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

    private var currentEmotion = ""
    private var correctOption = 1 // 1 for option1, 2 for option2
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5
    private var isAnswerSelected = false
    private var isVideoFinished = false

    // Track used emotions to prevent duplicates
    private var usedEmotions = mutableSetOf<String>()

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val TAG = "MoodMatchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_down)

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

                // Fade in animation for top elements
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
        // Option buttons
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

        // Sound buttons
        soundOption1.setOnClickListener {
            playSound(btnOption1.text.toString().lowercase())
        }

        soundOption2.setOnClickListener {
            playSound(btnOption2.text.toString().lowercase())
        }

        // Next button
        btnNext.setOnClickListener {
            goToNextRound()
        }
    }

    private fun startNewRound() {
        isAnswerSelected = false
        btnNext.visibility = View.GONE

        // Select random emotion for this round without repeating
        currentEmotion = getUniqueEmotion()

        if (currentEmotion.isEmpty()) {
            // If we've used all emotions (shouldn't happen with 7 emotions and 5 rounds)
            currentEmotion = emotions.random()
        }

        Log.d(TAG, "Current emotion: $currentEmotion")

        // Load the image
        setEmotionImage(currentEmotion)

        // Determine correct and wrong options
        val availableWrongEmotions = emotions.filter { it != currentEmotion }
        val wrongEmotion = availableWrongEmotions.random()

        // Randomly assign correct option to button 1 or 2
        correctOption = Random.nextInt(1, 3)

        Log.d(TAG, "Correct option: $correctOption")

        if (correctOption == 1) {
            // Option 1 is correct, Option 2 is wrong
            btnOption1.text = currentEmotion.capitalize()
            btnOption2.text = wrongEmotion.capitalize()
            Log.d(TAG, "Option 1: $currentEmotion (correct), Option 2: $wrongEmotion")
        } else {
            // Option 2 is correct, Option 1 is wrong
            btnOption1.text = wrongEmotion.capitalize()
            btnOption2.text = currentEmotion.capitalize()
            Log.d(TAG, "Option 1: $wrongEmotion, Option 2: $currentEmotion (correct)")
        }

        // Reset button colors
        btnOption1.setBackgroundColor(resources.getColor(R.color.green))
        btnOption2.setBackgroundColor(resources.getColor(R.color.green))

        // Update round display (only if video finished)
        if (isVideoFinished) {
            tvRound.text = "Round: $currentRound/$totalRounds"
        }
    }

    private fun getUniqueEmotion(): String {
        // Get all emotions that haven't been used yet
        val availableEmotions = emotions.filter { !usedEmotions.contains(it) }

        return if (availableEmotions.isNotEmpty()) {
            val selected = availableEmotions.random()
            usedEmotions.add(selected)
            selected
        } else {
            // If all emotions have been used, clear the set and start over
            usedEmotions.clear()
            val selected = emotions.random()
            usedEmotions.add(selected)
            selected
        }
    }

    private fun setEmotionImage(emotion: String) {
        try {
            // Try to get the drawable resource
            val resourceId = resources.getIdentifier(emotion, "drawable", packageName)

            if (resourceId != 0) {
                emotionImage.setImageResource(resourceId)
                Log.d(TAG, "Successfully loaded image: $emotion, resourceId: $resourceId")
            } else {
                Log.e(TAG, "Image not found for emotion: $emotion")
                Toast.makeText(this, "Image not found: $emotion", Toast.LENGTH_SHORT).show()

                // Fallback - try with different naming or show default
                val fallbackResourceId = resources.getIdentifier(emotion, "drawable", packageName)
                if (fallbackResourceId != 0) {
                    emotionImage.setImageResource(fallbackResourceId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkAnswer(selectedOption: Int) {
        isAnswerSelected = true

        if (selectedOption == correctOption) {
            // Correct answer
            score += 10
            correctAnswersCount++

            // Update score display (only if video finished)
            if (isVideoFinished) {
                tvScore.text = "Score: $score"
            }

            if (selectedOption == 1) {
                btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                Toast.makeText(this, "Correct! Well done!", Toast.LENGTH_SHORT).show()
            } else {
                btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                Toast.makeText(this, "Correct! Well done!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Wrong answer
            if (selectedOption == 1) {
                btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                // Highlight correct answer
                btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            } else {
                btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                // Highlight correct answer
                btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            }
            Toast.makeText(
                this,
                "Wrong! The correct answer was ${currentEmotion.capitalize()}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Show next button
        btnNext.visibility = View.VISIBLE
    }

    private fun playSound(emotion: String) {
        // Stop any currently playing sound
        mediaPlayer?.release()

        try {
            val normalizedEmotion = emotion.lowercase()
            Log.d(TAG, "Attempting to play sound for: $normalizedEmotion")

            // Try to get the resource ID for the sound from raw folder
            val resourceId = resources.getIdentifier(normalizedEmotion, "raw", packageName)

            if (resourceId != 0) {
                mediaPlayer = MediaPlayer.create(this, resourceId)
                mediaPlayer?.setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                mediaPlayer?.start()
                Log.d(TAG, "Playing sound from raw folder: $normalizedEmotion")
            } else {
                // If raw resource not found, try in assets (for mp4 files)
                try {
                    Log.d(TAG, "Trying assets folder for: $normalizedEmotion.mp4")
                    val assetFileDescriptor = assets.openFd("$normalizedEmotion.mp4")
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(
                        assetFileDescriptor.fileDescriptor,
                        assetFileDescriptor.startOffset,
                        assetFileDescriptor.length
                    )
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    mediaPlayer?.setOnCompletionListener {
                        mediaPlayer?.release()
                        mediaPlayer = null
                    }
                    Log.d(TAG, "Playing sound from assets: $normalizedEmotion.mp4")
                } catch (e: Exception) {
                    Log.e(TAG, "Sound file not found in assets: ${e.message}")
                    Toast.makeText(
                        this,
                        "Sound file not found for $normalizedEmotion",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error playing sound", Toast.LENGTH_SHORT).show()
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
        intent.putExtra("GAME_MODE", "seven_down")
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
        mediaPlayer?.pause()
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
        mediaPlayer?.release()
        mediaPlayer = null
    }
}