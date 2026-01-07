package com.chirathi.voicebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private var currentIndex = 0

    private val sentenceScores = IntArray(sentenceList.size) { 0 }

    private lateinit var tvSentence: TextView
    private lateinit var ivSentenceImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val pronunciationAssesment = PronunciationAssesment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level3_task)

        tvSentence = findViewById(R.id.tvSentence)
        ivSentenceImage = findViewById(R.id.ivSentenceImage)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()
        tts = TextToSpeech(this, this)

        btnNext.isEnabled = false
        displayCurrentSentence()

        llPlaySound.setOnClickListener {
            if (isTtsReady) speakSentence(tvSentence.text.toString())
        }

        llSpeakSound.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                assessSentencePronunciation()
            } else {
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextSentence()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 100
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.7f)
            tts?.setPitch(1.1f)
            isTtsReady = true
        }
    }

    private fun speakSentence(sentence: String) {
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "sentenceTTS")
    }

    private fun assessSentencePronunciation() {
        val referenceSentence = sentenceList[currentIndex].sentence

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        pronunciationAssesment.assess(
            referenceText = referenceSentence,
            onResult = { result ->
                runOnUiThread {
                    listeningDialog.dismiss()
                    processingDialog = ProcessingDialog(this)
                    processingDialog.show()
                }

                Handler(Looper.getMainLooper()).postDelayed({

                    val score = result.accuracyScore.toInt()
                    sentenceScores[currentIndex] = score

                    val type = when {
                        score >= 75 -> "GOOD_PRONUNCIATION"
                        score >= 50 -> "MODERATE_PRONUNCIATION"
                        else -> "POOR_PRONUNCIATION"
                    }

                    processingDialog.dismiss()

                    FeedbackDialog(this).show(
                        score = score,
                        level = 3,
                        word = referenceSentence,
                        pronunciationType = type
                    )

                    btnNext.isEnabled = true

                }, 1000)
            },
            onError = {
                runOnUiThread {
                    listeningDialog.dismiss()
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun displayCurrentSentence() {
        val item = sentenceList[currentIndex]
        tvSentence.text = item.sentence
        ivSentenceImage.setImageResource(item.imageResourceId)
    }

    private fun moveToNextSentence() {
        if (currentIndex < sentenceList.size - 1) {
            currentIndex++
            displayCurrentSentence()
            btnNext.isEnabled = false
        } else {
            finishLevel()
        }
    }

    private fun calculateProgress(): Int {
        return sentenceScores.sum() / sentenceScores.size
    }

    private fun finishLevel() {
        val progress = calculateProgress()
        val intent = Intent(this, SentencesProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", progress)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
