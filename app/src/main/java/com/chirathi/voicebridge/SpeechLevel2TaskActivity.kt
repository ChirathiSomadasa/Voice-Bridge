package com.chirathi.voicebridge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class SpeechLevel2TaskActivity : AppCompatActivity() {

    data class WordItem(val word: String, val imageResourceId: Int)

    private val wordList = listOf(
            // CVC words starting with common phonemes for articulation practice
            WordItem("Ball", R.drawable.ball),
            WordItem("Cat", R.drawable.cat),
            WordItem("Spoon", R.drawable.spoon),
            WordItem("Rabbit", R.drawable.rabbit),
            WordItem("Chair", R.drawable.chair)
    )

    // State variable to track the current index in the list
    private var currentWordIndex = 0

    // Late-initialised Views
    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        val btnNext: Button = findViewById(R.id.btnNext)

        // Set the initial word and image
        displayCurrentWord()

        btnNext.setOnClickListener {
            moveToNextWord()
        }
    }

    // Updates the TextView and ImageView with the current word item from the list
    private fun displayCurrentWord() {
        if (wordList.isNotEmpty()) {
            val currentItem = wordList[currentWordIndex]
            tvWord.text = currentItem.word
            ivWordImage.setImageResource(currentItem.imageResourceId)
        }
    }

    // Increments the index to move to the next word.
    private fun moveToNextWord() {

        currentWordIndex = (currentWordIndex + 1) % wordList.size

        // Update the views
        displayCurrentWord()

    }
}