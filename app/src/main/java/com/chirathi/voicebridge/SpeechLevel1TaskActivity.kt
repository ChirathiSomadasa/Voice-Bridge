package com.chirathi.voicebridge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechLevel1TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val graphemeList = listOf(
        "B", "F", "G", "K", "M", "U", "P", "R", "S", "Z"
    )

    private var currentGraphemeIndex = 0
    private lateinit var tvLetter: TextView
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level1_task)

        tvLetter = findViewById(R.id.tvLetter)
        val btnNext: Button = findViewById(R.id.btnNext)
        val llPlaySound: LinearLayout = findViewById(R.id.llPlaySound)

        // Initialize Text-to-Speech
        tts = TextToSpeech(this, this)

        // Show first letter
        displayCurrentGrapheme()

        // NEXT button
        btnNext.setOnClickListener {
            moveToNextGrapheme()
        }

        // PLAY SOUND button
        llPlaySound.setOnClickListener {
            val text = tvLetter.text.toString()
            speakLetter(text)
        }
    }

    // TTS initialization
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.8f) // better for kids
        }
    }

    // Speak the letter
    private fun speakLetter(letter: String) {
        val speakText = when (letter) {
            "Z" -> "Zed"     // instead of "Zee"
            else -> letter
        }
        tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "letterID")
    }


    private fun displayCurrentGrapheme() {
        tvLetter.text = graphemeList[currentGraphemeIndex]
    }

    private fun moveToNextGrapheme() {
        currentGraphemeIndex = (currentGraphemeIndex + 1) % graphemeList.size
        displayCurrentGrapheme()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
