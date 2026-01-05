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

class SpeechLevel2TaskActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    data class WordItem(val word: String, val imageResId: Int)

    private val wordList = listOf(
        WordItem("ball", R.drawable.ball),
        WordItem("cat", R.drawable.cat),
        WordItem("spoon", R.drawable.spoon),
        WordItem("rabbit", R.drawable.rabbit),
        WordItem("chair", R.drawable.chair),
        WordItem("tap", R.drawable.tap),
        WordItem("leaf", R.drawable.leaf),
        WordItem("shoe", R.drawable.shoe),
        WordItem("bottle", R.drawable.bottle),
        WordItem("cup", R.drawable.cup)
    )

    private var currentIndex = 0

    // Store scores
    private val wordScores = IntArray(wordList.size) { 0 }

    private lateinit var tvWord: TextView
    private lateinit var ivWordImage: ImageView
    private lateinit var llPlaySound: LinearLayout
    private lateinit var llSpeakSound: LinearLayout
    private lateinit var btnNext: Button

    private lateinit var listeningDialog: ListeningDialog
    private lateinit var processingDialog: ProcessingDialog

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val azureSpeechHelper = AzureSpeechHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2_task)

        tvWord = findViewById(R.id.tvWord)
        ivWordImage = findViewById(R.id.ivWordImage)
        llPlaySound = findViewById(R.id.llPlaySound)
        llSpeakSound = findViewById(R.id.llSpeakSound)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()
        tts = TextToSpeech(this, this)

        btnNext.isEnabled = false   //Disable Next initially
        displayCurrentWord()

        llPlaySound.setOnClickListener {
            if (isTtsReady) speakWord(tvWord.text.toString())
        }

        llSpeakSound.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                assessWordPronunciation()
            } else {
                checkPermissions()
            }
        }

        btnNext.setOnClickListener {
            moveToNextWord()
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
            isTtsReady = true
        }
    }

    private fun speakWord(word: String) {
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "ttsWord")
    }

    private fun assessWordPronunciation() {
        val referenceWord = wordList[currentIndex].word

        listeningDialog = ListeningDialog(this)
        listeningDialog.show()

        azureSpeechHelper.assess(
            referenceText = referenceWord,
            onResult = { result ->
                runOnUiThread {
                    listeningDialog.dismiss()
                    processingDialog = ProcessingDialog(this)
                    processingDialog.show()
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    val score = result.accuracyScore.toInt()

                    // Save score
                    wordScores[currentIndex] = score

                    val type = when {
                        score >= 75 -> "GOOD_PRONUNCIATION"
                        score >= 50 -> "MODERATE_PRONUNCIATION"
                        else -> "POOR_PRONUNCIATION"
                    }

                    processingDialog.dismiss()

                    FeedbackDialog(this).show(
                        score = score,
                        level = 2,
                        word = referenceWord,
                        pronunciationType = type
                    )

                    // Enable Next after speaking
                    btnNext.isEnabled = true

                }, 800)
            },
            onError = {
                runOnUiThread {
                    listeningDialog.dismiss()
                    Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun displayCurrentWord() {
        val item = wordList[currentIndex]
        tvWord.text = item.word
        ivWordImage.setImageResource(item.imageResId)
    }

    private fun moveToNextWord() {
        if (currentIndex < wordList.size - 1) {
            currentIndex++
            displayCurrentWord()
            btnNext.isEnabled = false   // lock again
        } else {
            finishLevel()
        }
    }

    private fun calculateProgress(): Int {
        return wordScores.sum() / wordScores.size
    }

    private fun finishLevel() {
        val progress = calculateProgress()
        val intent = Intent(this, WordProgressActivity::class.java)
        intent.putExtra("PROGRESS_SCORE", progress)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
