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
        WordItem("Chair", R.drawable.chair),
        WordItem("Tap", R.drawable.tap),
        WordItem("Leaf", R.drawable.leaf),
        WordItem("Shoe", R.drawable.shoe),
        WordItem("Bottle", R.drawable.bottle),
        WordItem("Cup", R.drawable.cup)
    )

    private var currentWordIndex = 0
    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        val btnNext: Button = findViewById(R.id.btnNext)

        displayCurrentWord()

        btnNext.setOnClickListener {
            moveToNextWord()
        }
    }

    private fun displayCurrentWord() {
        if (wordList.isNotEmpty()) {
            val currentItem = wordList[currentWordIndex]
            tvWord.text = currentItem.word
            ivWordImage.setImageResource(currentItem.imageResourceId)
        }
        // NOTE: If wordList is empty, the app will display nothing, which is safe.
    }

    private fun moveToNextWord() {
        if (wordList.isEmpty()) return // Prevent crash if list is unexpectedly empty

        // Increment the index and use the modulo operator to ensure it wraps around
        // to 0 when it reaches the size of the list.
        currentWordIndex = (currentWordIndex + 1) % wordList.size

        // Update the views
        displayCurrentWord()
    }
}