package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

class MoodMatchSevenDownActivity : AppCompatActivity() {

    // Define all emotions
    private val emotions = listOf(
        "happy", "sad", "angry", "scared",
        "bored", "embarrassed", "proud"
    )

    private lateinit var emotionImage: ImageView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var soundOption1: LinearLayout
    private lateinit var soundOption2: LinearLayout
    private lateinit var btnNext: Button
    private lateinit var tvScore: TextView
    private lateinit var tvRound: TextView

    private var currentEmotion = ""
    private var correctOption = 1 // 1 for option1, 2 for option2
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5  // Changed from 10 to 5
    private var isAnswerSelected = false

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
        setupClickListeners()
        startNewRound()

        // Root layout click to exit - optional, you can remove if not needed
        val rootLayout = findViewById<View>(android.R.id.content)
        rootLayout.setOnClickListener {
            if (isAnswerSelected) {
                goToNextRound()
            }
        }
    }

    private fun initializeViews() {
        emotionImage = findViewById(R.id.emotionImage)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        soundOption1 = findViewById(R.id.soundOption1)
        soundOption2 = findViewById(R.id.soundOption2)
        btnNext = findViewById(R.id.btnNext)
        tvScore = findViewById(R.id.tvScore)
        tvRound = findViewById(R.id.tvRound)
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

        // Update round display
        tvRound.text = "Round: $currentRound/$totalRounds"
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
                // Check if file exists with different extensions
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
            correctAnswersCount++  // Increment correct answers count
            tvScore.text = "Score: $score"

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
            // Game over, go to scoreboard screen
            val intent = Intent(this, MMScoreboardActivity::class.java)
            // Pass the correct answers count and total rounds
            intent.putExtra("CORRECT_ANSWERS", correctAnswersCount)
            intent.putExtra("TOTAL_ROUNDS", totalRounds)
            intent.putExtra("SCORE", score)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        } else {
            startNewRound()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        // Resume game if needed
    }
}