package com.chirathi.voicebridge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class SpeechLevel1TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // 5 letters for this level
    private val graphemeList = listOf(
        "B", "L", "G", "K", "M"
    )

    private val pronunciationMap = mapOf(
        "B" to "bee",
        "L" to "ell",
        "G" to "gee",
        "K" to "kay",
        "M" to "em",
        "U" to "you",
        "P" to "pee",
        "R" to "ar",
        "S" to "ess",
        "Z" to "zed"
    )

    private var currentIndex = 0

    // Store score for each letter (max total = 500)
    private val letterScores = IntArray(graphemeList.size) { 0 }

    private lateinit var tvLetter: TextView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val azureSpeechHelper = AzureSpeechHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level1_task)

        tvLetter = findViewById(R.id.tvLetter)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()
        tts = TextToSpeech(this, this)

        btnNext.isEnabled = false
        displayCurrentLetter()

        llPlaySound.setOnClickListener {
            if (isTtsReady) {
                speakLetter(tvLetter.text.toString())
            }
        }

        llSpeakSound.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                assessLetterPronunciation()
            } else {
                Toast.makeText(this, "Microphone permission is required!", Toast.LENGTH_SHORT).show()
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextLetter()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.UK
            tts?.setSpeechRate(0.75f)
            isTtsReady = true
        }
    }

    private fun speakLetter(letter: String) {
        val speakText = pronunciationMap[letter] ?: letter.lowercase()
        tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "letterTTS")
    }

    private fun assessLetterPronunciation() {
        val letter = graphemeList[currentIndex]
        val referencePronunciation =
            pronunciationMap[letter] ?: letter.lowercase()

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        azureSpeechHelper.assess(
            referenceText = referencePronunciation,

            onResult = { result ->
                runOnUiThread {
                    listeningDialog.dismiss()
                    processingDialog = ProcessingDialog(this)
                    processingDialog.show()
                }

                Handler(Looper.getMainLooper()).postDelayed({

                    val score = result.accuracyScore.toInt()
                    letterScores[currentIndex] = score

                    val pronunciationType = when {
                        score >= 75 -> "GOOD_PRONUNCIATION"
                        score >= 50 -> "MODERATE_PRONUNCIATION"
                        else -> "POOR_PRONUNCIATION"
                    }

                    processingDialog.dismiss()

                    FeedbackDialog(this).show(
                        score = score,
                        level = 1,
                        word = letter,
                        pronunciationType = pronunciationType
                    )

                    btnNext.isEnabled = true

                }, 1000)
            },

            onError = { error ->
                runOnUiThread {
                    listeningDialog.dismiss()
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun displayCurrentLetter() {
        tvLetter.text = graphemeList[currentIndex]
    }

    private fun moveToNextLetter() {
        if (currentIndex < graphemeList.size - 1) {
            currentIndex++
            displayCurrentLetter()
            btnNext.isEnabled = false
        } else {
            finishLevel()
        }
    }

    // Progress = (sum / 500) * 100
    private fun calculateProgress(): Int {
        val totalScore = letterScores.sum() // max = 500
        return ((totalScore.toFloat() / 500f) * 100).toInt()
    }

    private fun finishLevel() {
        val progressScore = calculateProgress()

        if (progressScore >= 75) {
            successDialog = SuccessDialog(this)
            successDialog.show()

            successDialog.setOnDismissListener {
                goToProgress(progressScore)
            }
        } else {
            goToProgress(progressScore)
        }
    }

    private fun goToProgress(progressScore: Int) {
        val intent = Intent(this, LetterProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", progressScore)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
