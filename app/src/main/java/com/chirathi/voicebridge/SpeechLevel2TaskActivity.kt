package com.chirathi.voicebridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class SpeechLevel2TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class WordItem(val word: String, val imageResId: Int)

    // Full List of Words
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

    // UI Components
    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Auth for User Specific Progress
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""

    // TFLite components
    private lateinit var featureExtractor: AudioFeatureExtractor
    private lateinit var tfliteHelper: PronunciationTFLiteHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        // Initialize Auth
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // ---------------- BATCH & PROGRESS LOGIC ----------------
        if (intent.hasExtra("BATCH_INDEX")) {
            currentBatchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        } else {
            // Load saved batch for LEVEL 2 specific to this user
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
        featureExtractor = AudioFeatureExtractor()

        try {
            tfliteHelper = PronunciationTFLiteHelper(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnNext.isEnabled = false
        displayCurrentWord()

        llPlaySound.setOnClickListener {
            if (isTtsReady) speakWord(tvWord.text.toString())
        }

        llSpeakSound.setOnClickListener {
            assessWordPronunciation()
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

        // Safety Reset if finished
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
        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        val mfccInput = featureExtractor.extractFeatures()

        val referenceWord = currentBatchList[currentIndex].word

        PronunciationAssesment().assess(referenceWord, onResult = { paResult ->
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }

                processingDialog = ProcessingDialog(this)
                processingDialog.show()

                val azureScore = paResult.pronunciationScore.toInt()
                val type = when {
                    azureScore >= 75 -> "GOOD_PRONUNCIATION"
                    azureScore >= 50 -> "MODERATE_PRONUNCIATION"
                    else -> "POOR_PRONUNCIATION"
                }

                wordScores[currentIndex] = azureScore

                Handler(Looper.getMainLooper()).postDelayed({
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }

                    FeedbackDialog(this).show(
                        score = azureScore,
                        level = 2,
                        word = referenceWord,
                        pronunciationType = type
                    )
                    btnNext.isEnabled = true
                }, 700)
            }
        }, onError = { errorMsg ->
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) listeningDialog.dismiss()
                if (::processingDialog.isInitialized && processingDialog.isShowing) processingDialog.dismiss()
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                btnNext.isEnabled = true
            }
        })
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
            // Save Progress (User Specific)
            if (hasMoreWords && currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("SAVED_BATCH_LEVEL_2_$currentUserId", currentBatchIndex + 1).apply()
            }

            // Show Success Dialog
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
        tts?.shutdown()
        super.onDestroy()
    }
}