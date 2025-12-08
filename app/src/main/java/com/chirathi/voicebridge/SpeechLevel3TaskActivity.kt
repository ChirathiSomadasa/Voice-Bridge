package com.chirathi.voicebridge

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SpeechLevel3TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class SentenceItem(val sentence: String, val imageResourceId: Int)

    private val sentenceList = listOf(
        SentenceItem("The big dog ran fast in the park", R.drawable.dog_level3),
        SentenceItem("The pretty bird flew up to the sky", R.drawable.bird_level3),
        SentenceItem("Ben builds big blue blocks", R.drawable.blue_box_level3),
        SentenceItem("Sam saw a sunfish swimming", R.drawable.fish_level3),
        SentenceItem("Kate keeps her kite in the kit", R.drawable.kite_level3),
        SentenceItem("The little lion likes to play", R.drawable.lion_level3),
        SentenceItem("The rabbit runs around the rock", R.drawable.rabbit_level3),
        SentenceItem("The sun is bright and warm today", R.drawable.sun_level3),
        SentenceItem("She gets the green grapes", R.drawable.grapes_level3),
        SentenceItem("John rides a race car really fast", R.drawable.car_level3)
    )

    private var currentSentenceIndex = 0

    private lateinit var tvSentence: TextView
    private lateinit var ivSentenceImage: ImageView
    private lateinit var llPlaySound: LinearLayout

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level3_task)

        tvSentence = findViewById(R.id.tvSentence)
        ivSentenceImage = findViewById(R.id.ivSentenceImage)
        val btnNext: Button = findViewById(R.id.btnNext)
        llPlaySound = findViewById(R.id.llPlaySound)

        // Initialize TTS
        tts = TextToSpeech(this, this)

        displayCurrentSentence()

        btnNext.setOnClickListener { moveToNextSentence() }

        llPlaySound.setOnClickListener {
            if (isTtsReady) {
                speakSentence(tvSentence.text.toString())
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.7f)  // slow for kids
            tts?.setPitch(1.1f)       // friendly tone
            isTtsReady = true
        }
    }


    private fun speakSentence(sentence: String) {
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "sentenceID")
    }

    private fun displayCurrentSentence() {
        val currentItem = sentenceList[currentSentenceIndex]
        tvSentence.text = currentItem.sentence
        ivSentenceImage.setImageResource(currentItem.imageResourceId)
    }

    private fun moveToNextSentence() {
        currentSentenceIndex = (currentSentenceIndex + 1) % sentenceList.size
        displayCurrentSentence()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
