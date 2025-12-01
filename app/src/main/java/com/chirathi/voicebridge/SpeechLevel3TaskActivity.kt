package com.chirathi.voicebridge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class SpeechLevel3TaskActivity : AppCompatActivity() {

    data class SentenceItem(val sentence: String, val imageResourceId: Int)

    private val SentenceList = listOf(
        // CVC words starting with common phonemes for articulation practice
        SentenceItem("The big dog ran fast in the park", R.drawable.dog_level3),
        SentenceItem("The pretty bird flew up to the sky", R.drawable.bird_level3),
        SentenceItem("Ben builds big blue blocks", R.drawable.blue_box_level3),
        SentenceItem("Sam saw a sunfish swimming", R.drawable.fish_level3),
        SentenceItem("Kate keeps her kite in the kit", R.drawable.kite_level3),
        SentenceItem("The little lion likes to play", R.drawable.lion_level3),
        SentenceItem("The rabbit runs around the rock", R.drawable.rabbit_level3),
        SentenceItem("The sun is bright and warm today", R.drawable.sun_level3),
        SentenceItem("She gets the green grapes", R.drawable.grapes_level3),
        SentenceItem("Jhon rides a race car really fast", R.drawable.car_level3)
    )

    // State variable to track the current index in the list
    private var currentSentenceIndex = 0

    // Late-initialised Views
    private lateinit var tvSentence: TextView
    private lateinit var ivSentenceImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level3_task)

        tvSentence = findViewById(R.id.tvSentence)
        ivSentenceImage = findViewById(R.id.ivSentenceImage)
        val btnNext: Button = findViewById(R.id.btnNext)

        // Set the initial sentence and image
        displayCurrentSentence()

        btnNext.setOnClickListener {
            moveToNextSentence()
        }
    }

    // Updates the TextView and ImageView with the current sentence item from the list
    private fun displayCurrentSentence() {
        if (SentenceList.isNotEmpty()) {
            val currentItem = SentenceList[currentSentenceIndex]
            tvSentence.text = currentItem.sentence
            ivSentenceImage.setImageResource(currentItem.imageResourceId)
        }
    }

    // Increments the index to move to the next sentence.
    private fun moveToNextSentence() {

        currentSentenceIndex = (currentSentenceIndex + 1) % SentenceList.size

        // Update the views
        displayCurrentSentence()

    }
}