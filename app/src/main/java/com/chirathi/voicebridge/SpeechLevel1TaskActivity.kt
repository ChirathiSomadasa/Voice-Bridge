package com.chirathi.voicebridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*
import java.util.concurrent.Executors

class SpeechLevel1TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // 1. FULL ALPHABET
    private val fullGraphemeList = listOf(
        "M", "A", "B", "T", "S",  // Batch 0
        "L", "R", "P", "F", "C",  // Batch 1
        "H", "N", "D", "G", "K",  // Batch 2
        "W", "Y", "J", "V", "Z",  // Batch 3
        "Q", "X", "E", "I", "O", "U" // Batch 4
    )

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

    private lateinit var tvLetter: TextView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var wav2Vec2Scorer: Wav2Vec2Scorer
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level1_task)

        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        wav2Vec2Scorer = Wav2Vec2Scorer(this)

        if (intent.hasExtra("BATCH_INDEX")) {
            currentBatchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        } else {
            if (currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                currentBatchIndex = prefs.getInt("SAVED_BATCH_LEVEL_1_$currentUserId", 0)
            } else {
                currentBatchIndex = 0
            }
        }

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

    private fun setupCurrentBatch() {
        val startIndex = currentBatchIndex * 5
        var endIndex = startIndex + 5
        if (endIndex > fullGraphemeList.size) {
            endIndex = fullGraphemeList.size
        }
        if (startIndex >= fullGraphemeList.size) {
            currentBatchIndex = 0
            setupCurrentBatch()
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
        val targetLabel = letter

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        executor.execute {
            val audioData = wav2Vec2Scorer.recordAudio(2500)
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }
                processingDialog = ProcessingDialog(this)
                processingDialog.show()
            }

            if (audioData != null) {
                val (pronunciationType, score) = wav2Vec2Scorer.predict(audioData, targetLabel)

                // Save Result to Firebase immediately
                saveToHistory(letter, score, 1, pronunciationType)

                runOnUiThread {
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }
                    letterScores[currentIndex] = score
                    FeedbackDialog(this).show(score = score, level = 1, word = letter, pronunciationType = pronunciationType)
                    btnNext.isEnabled = true
                }
            } else {
                runOnUiThread {
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }
                    Toast.makeText(this, "Recording Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToHistory(content: String, score: Int, level: Int, pronunciationType: String) {
        if (currentUserId.isEmpty()) return

        val category = when {
            score >= 75 -> "good"
            score >= 50 -> "moderate"
            else -> "poor"
        }

        val feedbackMap = hashMapOf(
            "userId" to currentUserId,
            "content" to content,
            "score" to score,
            "level" to level,
            "type" to pronunciationType,
            "category" to category,
            "item_type" to "letter",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("pronunciation_feedback")
            .add(feedbackMap)
            .addOnFailureListener { e -> e.printStackTrace() }
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
        val nextBatchStartIndex = (currentBatchIndex + 1) * 5
        val hasMoreLetters = nextBatchStartIndex < fullGraphemeList.size

        if (progressScore >= 75) {
            val nextBatch = currentBatchIndex + 1
            if (hasMoreLetters && currentUserId.isNotEmpty()) {
                // 1. Local Save
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("SAVED_BATCH_LEVEL_1_$currentUserId", nextBatch).apply()

                // 2. Firebase Sync
                val updateMap = hashMapOf("level1_batch" to nextBatch)
                FirebaseFirestore.getInstance().collection("student_progress")
                    .document(currentUserId)
                    .set(updateMap, SetOptions.merge())
            }

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
        intent.putExtra("CAN_CONTINUE", canContinue)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}