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

    // 1. FULL ALPHABET
    private val fullGraphemeList = listOf(
        "M", "A", "B", "T", "S",  // Batch 0
        "L", "R", "P", "F", "C",  // Batch 1
        "H", "N", "D", "G", "K",  // Batch 2
        "W", "Y", "J", "V", "Z",  // Batch 3
        "Q", "X", "E", "I", "O", "U" // Batch 4 (6 letters)
    )

    // Expanded Pronunciation Map for all 26 letters
    private val pronunciationMap = mapOf(
        "A" to "a", "B" to "bee", "C" to "see", "D" to "dee", "E" to "ee",
        "F" to "eff", "G" to "gee", "H" to "aitch", "I" to "eye", "J" to "jay",
        "K" to "kay", "L" to "ell", "M" to "em", "N" to "en", "O" to "oh",
        "P" to "pee", "Q" to "cue", "R" to "ar", "S" to "ess", "T" to "tee",
        "U" to "you", "V" to "vee", "W" to "double-u", "X" to "ex", "Y" to "why", "Z" to "zed"
    )

    private var currentBatchIndex = 0
    private var currentLetterList = listOf<String>()
    private var currentIndex = 0
    private lateinit var letterScores: IntArray

    // UI Components
    private lateinit var tvLetter: TextView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    // Dialogs
    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val pronunciationAssesment = PronunciationAssesment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level1_task)

        // 2. GET CURRENT BATCH INDEX (Defaults to 0)
        currentBatchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        setupCurrentBatch()

        tvLetter = findViewById(R.id.tvLetter)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()
        tts = TextToSpeech(this, this)

        btnNext.isEnabled = false
        displayCurrentLetter()

        llPlaySound.setOnClickListener {
            if (isTtsReady) speakLetter(tvLetter.text.toString())
        }

        llSpeakSound.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                assessLetterPronunciation()
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextLetter()
        }
    }

    // Logic to slice the full list into batches of 5
    private fun setupCurrentBatch() {
        val startIndex = currentBatchIndex * 5
        var endIndex = startIndex + 5

        // Handle end of list (last batch might have remaining letters)
        if (endIndex > fullGraphemeList.size) {
            endIndex = fullGraphemeList.size
        }

        // Safety check if batch is out of bounds
        if (startIndex >= fullGraphemeList.size) {
            finish() // Should not happen if logic is correct
            return
        }

        currentLetterList = fullGraphemeList.subList(startIndex, endIndex)
        letterScores = IntArray(currentLetterList.size) { 0 }
        currentIndex = 0
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
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
        val letter = currentLetterList[currentIndex]
        val referencePronunciation = pronunciationMap[letter] ?: letter.lowercase()

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        pronunciationAssesment.assess(
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
        tvLetter.text = currentLetterList[currentIndex]
    }

    private fun moveToNextLetter() {
        if (currentIndex < currentLetterList.size - 1) {
            currentIndex++
            displayCurrentLetter()
            btnNext.isEnabled = false
        } else {
            finishLevel()
        }
    }

    private fun calculateProgress(): Int {
        val totalMaxScore = currentLetterList.size * 100
        val earnedScore = letterScores.sum()
        return ((earnedScore.toFloat() / totalMaxScore.toFloat()) * 100).toInt()
    }

    private fun finishLevel() {
        val progressScore = calculateProgress()

        // Check if there are more letters after this batch
        val nextBatchStartIndex = (currentBatchIndex + 1) * 5
        val hasMoreLetters = nextBatchStartIndex < fullGraphemeList.size

        if (progressScore >= 75) {
            successDialog = SuccessDialog(this)
            successDialog.show()
            successDialog.setOnDismissListener {
                goToProgress(progressScore, hasMoreLetters)
            }
        } else {
            goToProgress(progressScore, hasMoreLetters)
        }
    }

    private fun goToProgress(score: Int, canContinue: Boolean) {
        val intent = Intent(this, LetterProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", score)
        intent.putExtra("BATCH_INDEX", currentBatchIndex)
        intent.putExtra("CAN_CONTINUE", canContinue) // Pass flag to show/hide Continue button
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}