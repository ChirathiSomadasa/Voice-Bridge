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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeechLevel2TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var currentBatchList = listOf<TherapyDataPool.WordItem>()
    private var currentIndex = 0
    private lateinit var wordScores: IntArray
    private var isLevelCompleted = false

    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog
    private lateinit var soundManager: SoundManager
    private lateinit var btnBack: ImageView


    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""
    private lateinit var wav2Vec2Scorer: Wav2Vec2Scorer
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        soundManager = SoundManager(this)

        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: "guest_user"
        wav2Vec2Scorer = Wav2Vec2Scorer(this)

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        checkPermissions()
        tts = TextToSpeech(this, this)

        // Load the unique, non-repeating random word batch for this specific user
        loadUserSpecificWordBatch()

        btnNext.isEnabled = false

        if (!isLevelCompleted) {
            displayCurrentWord()
        }

        btnBack.setOnClickListener {
            soundManager.playClickSound()
            finish()
        }

        llPlaySound.setOnClickListener {
            soundManager.playClickSound()
            if (isTtsReady && !isLevelCompleted) speakWord(tvWord.text.toString())
        }

        llSpeakSound.setOnClickListener {
            soundManager.playClickSound()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (!isLevelCompleted) assessWordPronunciation()
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextWord()
        }

    }

    private fun loadUserSpecificWordBatch() {
        val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)

        val allBatches = TherapyDataPool.articulationWords.chunked(5)
        val totalBatches = allBatches.size

        var orderString = prefs.getString("WORD_ORDER_$currentUserId", null)

        var userOrderList = orderString?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()

        if (orderString == null || userOrderList.size != totalBatches) {
            val shuffledIndices = (0 until totalBatches).shuffled()
            orderString = shuffledIndices.joinToString(",")
            prefs.edit().putString("WORD_ORDER_$currentUserId", orderString).apply()
            userOrderList = shuffledIndices
        }

        val currentProgressIndex = prefs.getInt("SAVED_BATCH_LEVEL_2_$currentUserId", 0)

        val actualBatchIndexToLoad = userOrderList[currentProgressIndex % totalBatches]

        currentBatchList = allBatches[actualBatchIndexToLoad]
        wordScores = IntArray(currentBatchList.size) { 0 }
        currentIndex = 0
        isLevelCompleted = false
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.7f)
            isTtsReady = true
        }
    }

    private fun speakWord(word: String) {
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "ttsWord")
    }

    private fun assessWordPronunciation() {
        val currentWordItem = currentBatchList[currentIndex]
        val targetWord = currentWordItem.text

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        executor.execute {
            val audioData = wav2Vec2Scorer.recordAudio(3000)

            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }
                processingDialog = ProcessingDialog(this@SpeechLevel2TaskActivity)
                processingDialog.show()
            }

            if (audioData != null) {
                val (pronunciationType, score, predictedText) = wav2Vec2Scorer.predict(audioData, targetWord)

                val category = when {
                    score >= 75 -> "good"
                    score >= 50 -> "moderate"
                    else -> "bad"
                }

                saveToHistory(targetWord, score, 2, pronunciationType)

                CoroutineScope(Dispatchers.IO).launch {
                    val aiFeedbackText = FeedbackGenerator.getDynamicFeedback(score, category, "word", targetWord)

                    withContext(Dispatchers.Main) {
                        if (::processingDialog.isInitialized && processingDialog.isShowing) {
                            processingDialog.dismiss()
                        }

                        wordScores[currentIndex] = score

                        FeedbackDialog(this@SpeechLevel2TaskActivity).show(
                            score = score,
                            category = category,
                            feedbackMessage = aiFeedbackText,
                            targetText = targetWord,
                            predictedText = predictedText,
                            onClose = {
                                tts?.stop()
                            }
                        )
                        tts?.speak(aiFeedbackText, TextToSpeech.QUEUE_FLUSH, null, "feedbackTTS")
                        btnNext.isEnabled = true
                    }
                }
            } else {
                runOnUiThread {
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }
                    Toast.makeText(this@SpeechLevel2TaskActivity, "Recording Failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToHistory(content: String, score: Int, level: Int, pronunciationType: String) {
        if (currentUserId.isEmpty() || currentUserId == "guest_user") return

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
            "item_type" to "word",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("pronunciation_feedback")
            .add(feedbackMap)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun displayCurrentWord() {
        if (currentBatchList.isNotEmpty()) {
            val item = currentBatchList[currentIndex]
            tvWord.text = item.text
            ivWordImage.setImageResource(item.imageResId)
        }
    }

    private fun moveToNextWord() {
        if (currentIndex < currentBatchList.size - 1) {
            currentIndex++
            displayCurrentWord()
            btnNext.isEnabled = false
        } else {
            finishLevel()
        }
    }

    private fun calculateProgress(): Int {
        val totalMaxScore = currentBatchList.size * 100
        val earnedScore = wordScores.sum()
        return ((earnedScore.toFloat() / totalMaxScore.toFloat()) * 100).toInt()
    }

    private fun finishLevel() {
        val progress = calculateProgress()

        if (progress >= 75) {
            val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
            val currentProgressIndex = prefs.getInt("SAVED_BATCH_LEVEL_2_$currentUserId", 0)

            // Increment the completed batch count
            val nextProgressIndex = currentProgressIndex + 1
            prefs.edit().putInt("SAVED_BATCH_LEVEL_2_$currentUserId", nextProgressIndex).apply()

            if (currentUserId != "guest_user") {
                val updateMap = hashMapOf("level2_batch" to nextProgressIndex)
                FirebaseFirestore.getInstance().collection("student_progress")
                    .document(currentUserId)
                    .set(updateMap, SetOptions.merge())
            }

            successDialog = SuccessDialog(this)
            successDialog.show()
            successDialog.setOnDismissListener {

                goToProgress(progress, true)
            }
        } else {

            goToProgress(progress, false)
        }
    }

    private fun goToProgress(score: Int, canContinue: Boolean) {
        val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
        val currentProgressIndex = prefs.getInt("SAVED_BATCH_LEVEL_2_$currentUserId", 0)

        val intent = Intent(this, WordProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", score)
        intent.putExtra("BATCH_INDEX", currentProgressIndex)
        intent.putExtra("CAN_CONTINUE", canContinue)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        soundManager.release()
        super.onDestroy()
    }
}