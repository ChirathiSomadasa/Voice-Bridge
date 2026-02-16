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

class SpeechLevel3TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class SentenceItem(val sentence: String, val imageResourceId: Int)

    private val fullSentenceList = listOf(
        SentenceItem("The big dog ran fast in the park", R.drawable.dog1_level3),
        SentenceItem("The dog jumped into a brown mud puddle", R.drawable.dog2_level3),
        SentenceItem("The dog had a bath with white bubbles", R.drawable.dog3_level3),
        SentenceItem("The pretty bird flew up to the sky", R.drawable.bird1_level3),
        SentenceItem("The bird looked down at the trees", R.drawable.bird2_level3),
        SentenceItem("The bird sang a happy song for us", R.drawable.bird3_level3),
        SentenceItem("Ben builds big blue blocks", R.drawable.blue_box1_level3),
        SentenceItem("Ben makes a very tall tower", R.drawable.blue_box2_level3),
        SentenceItem("Oh no the tower fell down", R.drawable.blue_box3_level3),
        SentenceItem("Sam saw a sunfish swimming", R.drawable.fish1_level3),
        SentenceItem("The fish jumped high in the air", R.drawable.fish2_level3),
        SentenceItem("The fish swam away quickly", R.drawable.fish3_level3),
        SentenceItem("Kate keeps her kite in the kit", R.drawable.kite1_level3),
        SentenceItem("Kate takes the kite out on windy days", R.drawable.kite2_level3),
        SentenceItem("The kite flies high in the air", R.drawable.kite3_level3),
        SentenceItem("The little lion likes to play", R.drawable.lion1_level3),
        SentenceItem("The lion runs in the green grass", R.drawable.lion2_level3),
        SentenceItem("The lion roars at the little bugs", R.drawable.lion3_level3),
        SentenceItem("The rabbit runs around the rock", R.drawable.rabbit1_level3),
        SentenceItem("The rabbit is hiding from the fox", R.drawable.rabbit2_level3),
        SentenceItem("The rabbit finds a safe hole to sleep", R.drawable.rabbit3_level3),
        SentenceItem("The sun is bright and warm today", R.drawable.sun1_level3),
        SentenceItem("Sam can go to the beach", R.drawable.sun2_level3),
        SentenceItem("Sam will swim in the cool water", R.drawable.sun3_level3),
        SentenceItem("The girl gets the green grapes", R.drawable.grapes1_level3),
        SentenceItem("The grapes are sweet and yummy", R.drawable.grapes2_level3),
        SentenceItem("The girl shares them with her friends", R.drawable.grapes3_level3),
        SentenceItem("John rides a race car really fast", R.drawable.car1_level3),
        SentenceItem("He overtakes the leading car", R.drawable.car2_level3),
        SentenceItem("John wins the big race today", R.drawable.car3_level3)
    )

    private var currentBatchIndex = 0
    private var currentBatchList = listOf<SentenceItem>()
    private var currentIndex = 0
    private lateinit var sentenceScores: IntArray

    private lateinit var tvSentence: TextView
    private lateinit var ivSentenceImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog
    private lateinit var successDialog: SuccessDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = ""
    private lateinit var wav2Vec2Scorer: Wav2Vec2Scorer
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level3_task)

        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""
        wav2Vec2Scorer = Wav2Vec2Scorer(this)

        if (intent.hasExtra("BATCH_INDEX")) {
            currentBatchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        } else {
            if (currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                currentBatchIndex = prefs.getInt("SAVED_BATCH_LEVEL_3_$currentUserId", 0)
            }
        }
        setupCurrentBatch()

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                assessSentencePronunciation()
            } else {
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show()
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextSentence()
        }
    }

    private fun setupCurrentBatch() {
        val startIndex = currentBatchIndex * 3
        var endIndex = startIndex + 3
        if (endIndex > fullSentenceList.size) {
            endIndex = fullSentenceList.size
        }
        if (startIndex >= fullSentenceList.size) {
            currentBatchIndex = 0
            setupCurrentBatch()
            return
        }
        currentBatchList = fullSentenceList.subList(startIndex, endIndex)
        sentenceScores = IntArray(currentBatchList.size) { 0 }
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
            tts?.setPitch(1.1f)
            isTtsReady = true
        }
    }

    private fun speakSentence(sentence: String) {
        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "sentenceTTS")
    }

    private fun assessSentencePronunciation() {
        val targetSentence = currentBatchList[currentIndex].sentence

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        executor.execute {
            val audioData = wav2Vec2Scorer.recordAudio(6000)
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }
                processingDialog = ProcessingDialog(this)
                processingDialog.show()
            }

            if (audioData != null) {
                val (pronunciationType, score) = wav2Vec2Scorer.predict(audioData, targetSentence)

                // Save Result to Firebase immediately
                saveToHistory(targetSentence, score, 3, pronunciationType)

                runOnUiThread {
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }
                    sentenceScores[currentIndex] = score
                    FeedbackDialog(this).show(score = score, level = 3, word = targetSentence, pronunciationType = pronunciationType)
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
            "item_type" to "sentence",
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("pronunciation_feedback")
            .add(feedbackMap)
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    private fun displayCurrentSentence() {
        val item = currentBatchList[currentIndex]
        tvSentence.text = item.sentence
        ivSentenceImage.setImageResource(item.imageResourceId)
    }

    private fun moveToNextSentence() {
        if (currentIndex < currentBatchList.size - 1) {
            currentIndex++
            displayCurrentSentence()
            btnNext.isEnabled = false
        } else {
            finishLevel()
        }
    }

    private fun calculateProgress(): Int {
        val totalMaxScore = currentBatchList.size * 100
        val earnedScore = sentenceScores.sum()
        return ((earnedScore.toFloat() / totalMaxScore.toFloat()) * 100).toInt()
    }

    private fun finishLevel() {
        val progress = calculateProgress()
        val nextBatchStartIndex = (currentBatchIndex + 1) * 3
        val hasMoreSentences = nextBatchStartIndex < fullSentenceList.size

        if (progress >= 75) {
            val nextBatch = currentBatchIndex + 1
            if (hasMoreSentences && currentUserId.isNotEmpty()) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("SAVED_BATCH_LEVEL_3_$currentUserId", nextBatch).apply()

                // Firebase Sync
                val updateMap = hashMapOf("level3_batch" to nextBatch)
                FirebaseFirestore.getInstance().collection("student_progress")
                    .document(currentUserId)
                    .set(updateMap, SetOptions.merge())
            }

            successDialog = SuccessDialog(this)
            successDialog.show()
            successDialog.setOnDismissListener {
                goToProgress(progress, hasMoreSentences)
            }
        } else {
            goToProgress(progress, hasMoreSentences)
        }
    }

    private fun goToProgress(score: Int, canContinue: Boolean) {
        val intent = Intent(this, SentencesProgressActivity::class.java)
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