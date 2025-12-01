package com.chirathi.voicebridge

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SpeechLevel2TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class WordItem(val word: String, val imageResourceId: Int)

    private val wordList = listOf(
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
    private lateinit var llPlaySound: LinearLayout

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        val btnNext: Button = findViewById(R.id.btnNext)
        llPlaySound = findViewById(R.id.llPlaySound)

        // Initialize TTS
        tts = TextToSpeech(this, this)

        displayCurrentWord()

        btnNext.setOnClickListener { moveToNextWord() }

        llPlaySound.setOnClickListener {
            if (isTtsReady) {
                speakWord(tvWord.text.toString())
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


    private fun speakWord(word: String) {
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "wordID")
    }

    private fun displayCurrentWord() {
        val currentItem = wordList[currentWordIndex]
        tvWord.text = currentItem.word
        ivWordImage.setImageResource(currentItem.imageResourceId)
    }

    private fun moveToNextWord() {
        currentWordIndex = (currentWordIndex + 1) % wordList.size
        displayCurrentWord()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
