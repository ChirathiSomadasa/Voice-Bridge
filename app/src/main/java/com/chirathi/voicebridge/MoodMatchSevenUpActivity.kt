package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MoodMatchSevenUpActivity : AppCompatActivity() {

    // Define all emotions for Seven Up (more emotions)
    private val emotions = listOf(
        "happy", "sad", "angry", "scared", "embarrassed", "bored",
        "proud", "surprise", "curious", "tired", "anxious", "greedy",
        "jealous", "cheerful", "sleepy", "disgusted", "calm", "frustrated"
    )

    private lateinit var emotionImage: ImageView
    private lateinit var btnOption1: Button
    private lateinit var btnOption2: Button
    private lateinit var btnOption3: Button
    private lateinit var btnOption4: Button
    private lateinit var btnNext: Button
    private lateinit var tvScore: TextView
    private lateinit var tvRound: TextView

    private var currentEmotion = ""
    private var correctOption = 0 // 1-4 for which option is correct
    private var score = 0
    private var correctAnswersCount = 0
    private var currentRound = 1
    private val totalRounds = 5
    private var isAnswerSelected = false

    // Track used emotions to prevent repeats within the same round
    private val usedEmotionsInRound = mutableSetOf<String>()

    companion object {
        private const val TAG = "MoodMatchSevenUp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_match_seven_up)

        initializeViews()
        setupClickListeners()
        startNewRound()
    }

    private fun initializeViews() {
        emotionImage = findViewById(R.id.emotionImage)
        btnOption1 = findViewById(R.id.btnOption1)
        btnOption2 = findViewById(R.id.btnOption2)
        btnOption3 = findViewById(R.id.btnOption3)
        btnOption4 = findViewById(R.id.btnOption4)
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

        // Next button
        btnNext.setOnClickListener {
            goToNextRound()
        }
    }

    private fun startNewRound() {
        isAnswerSelected = false
        btnNext.visibility = View.GONE
        usedEmotionsInRound.clear()

        // Select random emotion for this round (not used before in this round)
        currentEmotion = getRandomEmotion()
        usedEmotionsInRound.add(currentEmotion)

        Log.d(TAG, "Current emotion: $currentEmotion")

        // Load the image
        setEmotionImage(currentEmotion)

        // Determine correct and wrong options
        // Correct option position (1-4)
        correctOption = Random.nextInt(1, 5)

        // Get 3 unique wrong emotions
        val wrongEmotions = getUniqueWrongEmotions(3)

        Log.d(TAG, "Correct option position: $correctOption")
        Log.d(TAG, "Wrong emotions: $wrongEmotions")

        // Assign emotions to buttons
        val options = mutableListOf<String>()

        // Add correct emotion at correct position
        for (i in 1..4) {
            if (i == correctOption) {
                options.add(currentEmotion)
            } else {
                // Get next wrong emotion
                val wrongEmotion = wrongEmotions.removeFirst()
                options.add(wrongEmotion)
            }
        }

        // Set button texts
        btnOption1.text = options[0].capitalize()
        btnOption2.text = options[1].capitalize()
        btnOption3.text = options[2].capitalize()
        btnOption4.text = options[3].capitalize()

        // Reset button colors
        resetButtonColors()

        // Update round display
        tvRound.text = "Round: $currentRound/$totalRounds"
    }

    private fun getRandomEmotion(): String {
        val availableEmotions = emotions.filter { !usedEmotionsInRound.contains(it) }
        return if (availableEmotions.isNotEmpty()) {
            availableEmotions.random()
        } else {
            // If all emotions have been used, reset and pick any
            usedEmotionsInRound.clear()
            emotions.random()
        }
    }

    private fun getUniqueWrongEmotions(count: Int): MutableList<String> {
        val wrongEmotions = mutableListOf<String>()

        while (wrongEmotions.size < count) {
            val wrongEmotion = getRandomEmotion()
            if (wrongEmotion != currentEmotion && !wrongEmotions.contains(wrongEmotion)) {
                wrongEmotions.add(wrongEmotion)
                usedEmotionsInRound.add(wrongEmotion)
            }
        }

        return wrongEmotions
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
                // Try alternative naming (remove special characters, use underscores)
                val alternativeName = emotion.replace(" ", "_").lowercase()
                val altResourceId = resources.getIdentifier(alternativeName, "drawable", packageName)

                if (altResourceId != 0) {
                    emotionImage.setImageResource(altResourceId)
                } else {
                    Toast.makeText(this, "Image not found: $emotion", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}")
            e.printStackTrace()
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

        // Highlight all buttons
        highlightButtons(selectedOption)

        if (selectedOption == correctOption) {
            // Correct answer
            score += 20
            correctAnswersCount++
            tvScore.text = "Score: $score"
            Toast.makeText(this, "Correct! Well done!", Toast.LENGTH_SHORT).show()
        } else {
            // Wrong answer
            Toast.makeText(this, "Wrong! The correct answer was ${currentEmotion.capitalize()}", Toast.LENGTH_SHORT).show()
        }

        // Show next button
        btnNext.visibility = View.VISIBLE
    }

    private fun highlightButtons(selectedOption: Int) {
        // Reset all buttons first
        resetButtonColors()

        // Highlight correct answer in green
        when (correctOption) {
            1 -> btnOption1.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            2 -> btnOption2.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            3 -> btnOption3.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
            4 -> btnOption4.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
        }

        // If wrong answer selected, highlight it in red
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
            // Game over, go to scoreboard
            val intent = Intent(this, MMScoreboardActivity::class.java)
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
}