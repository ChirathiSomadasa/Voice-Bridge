package com.chirathi.voicebridge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class SpeechLevel1TaskActivity : AppCompatActivity() {

    private val graphemeList = listOf(
        "B", "F", "G", "K",  "M", "U", "P", "R", "S", "Z",
    )
    // State variable to track the current index in the list
    private var currentGraphemeIndex = 0

    private lateinit var tvLetter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level1_task)

        tvLetter = findViewById(R.id.tvLetter)
        val btnNext: Button = findViewById(R.id.btnNext)

        // Set the initial letter from the list
        displayCurrentGrapheme()

        // Set the click listener for the Next button
        btnNext.setOnClickListener {
            moveToNextGrapheme()
        }
    }

    // Updates the TextView with the current grapheme from the list.
    private fun displayCurrentGrapheme() {
        if (graphemeList.isNotEmpty()) {
            tvLetter.text = graphemeList[currentGraphemeIndex]
        }
    }
    // Increments the index to move to the next grapheme
    private fun moveToNextGrapheme() {

        currentGraphemeIndex = (currentGraphemeIndex + 1) % graphemeList.size

        displayCurrentGrapheme()

    }
}