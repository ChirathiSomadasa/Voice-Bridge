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

    data class WordItem(val word: String, val imageResId: Int)

    private val fullWordList = listOf(
        WordItem("ball", R.drawable.ball), WordItem("cat", R.drawable.cat),
        WordItem("spoon", R.drawable.spoon), WordItem("rabbit", R.drawable.rabbit),
        WordItem("chair", R.drawable.chair), WordItem("tap", R.drawable.tap),
        WordItem("leaf", R.drawable.leaf), WordItem("shoe", R.drawable.shoe),
        WordItem("bottle", R.drawable.bottle), WordItem("goat", R.drawable.goat),
        WordItem("sun", R.drawable.sun), WordItem("star", R.drawable.star),
        WordItem("bed", R.drawable.bed), WordItem("duck", R.drawable.duck),
        WordItem("lamp", R.drawable.lamp), WordItem("waterfall", R.drawable.waterfall),
        WordItem("tree", R.drawable.tree), WordItem("bird", R.drawable.bird),
        WordItem("flower", R.drawable.flower), WordItem("apple", R.drawable.apple),
        WordItem("book", R.drawable.book), WordItem("computer", R.drawable.computer),
        WordItem("zoo", R.drawable.zoo), WordItem("banana", R.drawable.banana),
        WordItem("house", R.drawable.house)
    )

    private var currentBatchIndex = 0
    private var currentBatchList = listOf<WordItem>()
    private var currentIndex = 0
    private lateinit var wordScores: IntArray

    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog
    private lateinit var soundManager: SoundManager

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
        currentUserId = auth.currentUser?.uid ?: ""
        wav2Vec2Scorer = Wav2Vec2Scorer(this)

        if (intent.hasExtra("BATCH_INDEX")) {
            currentBatchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        } else {
            if (currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                currentBatchIndex = prefs.getInt("SAVED_BATCH_LEVEL_2_$currentUserId", 0)
            }
        }
        setupCurrentBatch()

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()
        tts = TextToSpeech(this, this)

        btnNext.isEnabled = false
        displayCurrentWord()

        llPlaySound.setOnClickListener {
            soundManager.playClickSound() // Play UI pop sound
            if (isTtsReady) speakWord(tvWord.text.toString())
        }

        llSpeakSound.setOnClickListener {
            soundManager.playClickSound() // Play UI pop sound
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                assessWordPronunciation()
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextWord()
        }
    }

    private fun setupCurrentBatch() {
        val startIndex = currentBatchIndex * 5
        var endIndex = startIndex + 5
        if (endIndex > fullWordList.size) {
            endIndex = fullWordList.size
        }
        if (startIndex >= fullWordList.size) {
            currentBatchIndex = 0
            setupCurrentBatch()
            return
        }
        currentBatchList = fullWordList.subList(startIndex, endIndex)
        wordScores = IntArray(currentBatchList.size) { 0 }
        currentIndex = 0
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
        val targetWord = currentWordItem.word

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        executor.execute {
            // 1. Audio Recording
            val audioData = wav2Vec2Scorer.recordAudio(3000)

            // 2. Dismiss Listening and Show Processing
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }
                processingDialog = ProcessingDialog(this@SpeechLevel2TaskActivity)
                processingDialog.show()
            }

            if (audioData != null) {
                // 3. Get Score
                val (pronunciationType, score) = wav2Vec2Scorer.predict(audioData, targetWord)

                val category = when {
                    score >= 75 -> "good"
                    score >= 50 -> "moderate"
                    else -> "bad"
                }

                // Save Result to Firebase
                saveToHistory(targetWord, score, 2, pronunciationType)

                // 4. Launch Coroutine for API call while Processing Dialog is showing
                CoroutineScope(Dispatchers.IO).launch {

                    //  // Call the suspend function from FeedbackGenerator
                    val aiFeedbackText = FeedbackGenerator.getDynamicFeedback(score, category)

                    // 5. Switch back to Main Thread to update UI
                    withContext(Dispatchers.Main) {
                        if (::processingDialog.isInitialized && processingDialog.isShowing) {
                            processingDialog.dismiss()
                        }

                        wordScores[currentIndex] = score

                        FeedbackDialog(this@SpeechLevel2TaskActivity).show(
                            score = score,
                            category = category,
                            feedbackMessage = aiFeedbackText
                        )
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
            "item_type" to "word",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("pronunciation_feedback")
            .add(feedbackMap)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun displayCurrentWord() {
        val item = currentBatchList[currentIndex]
        tvWord.text = item.word
        ivWordImage.setImageResource(item.imageResId)
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
        val nextBatchStartIndex = (currentBatchIndex + 1) * 5
        val hasMoreWords = nextBatchStartIndex < fullWordList.size

        if (progress >= 75) {
            val nextBatch = currentBatchIndex + 1
            if (hasMoreWords && currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("SAVED_BATCH_LEVEL_2_$currentUserId", nextBatch).apply()

                // Firebase Sync
                val updateMap = hashMapOf("level2_batch" to nextBatch)
                FirebaseFirestore.getInstance().collection("student_progress")
                    .document(currentUserId)
                    .set(updateMap, SetOptions.merge())
            }

            successDialog = SuccessDialog(this)
            successDialog.show()
            successDialog.setOnDismissListener {
                goToProgress(progress, hasMoreWords)
            }
        } else {
            goToProgress(progress, hasMoreWords)
        }
    }

    private fun goToProgress(score: Int, canContinue: Boolean) {
        val intent = Intent(this, WordProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", score)
        intent.putExtra("BATCH_INDEX", currentBatchIndex)
        intent.putExtra("CAN_CONTINUE", canContinue)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        // Release Sound Pool to avoid memory leaks
        soundManager.release()
        super.onDestroy()
    }
}