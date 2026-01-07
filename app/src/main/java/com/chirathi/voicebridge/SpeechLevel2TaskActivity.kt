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

    // TFLite components (kept for panel/demo)
    private lateinit var featureExtractor: AudioFeatureExtractor
    private lateinit var tfliteHelper: PronunciationTFLiteHelper

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
        featureExtractor = AudioFeatureExtractor()

        try {
            tfliteHelper = PronunciationTFLiteHelper(this)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                this,
                "Model load failed. Check TFLite file.",
                Toast.LENGTH_LONG
            ).show()
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
            tts?.language = Locale.US
            tts?.setSpeechRate(0.7f)
            isTtsReady = true
        }
    }

    private fun speakWord(word: String) {
        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "ttsWord")
    }


    private fun assessWordPronunciation() {

        // Show listening dialog
        listeningDialog = ListeningDialog(this)
        listeningDialog.show()


        val mfccInput = featureExtractor.extractFeatures()
        val modelProbability = tfliteHelper.predict(mfccInput)
        val modelScore = (modelProbability * 100).toInt()

        val referenceWord = wordList[currentIndex].word

        PronunciationAssesment().assess(referenceWord, onResult = { paResult ->

            runOnUiThread {
                // Safely dismiss listening dialog
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }

                //Show processing dialog
                processingDialog = ProcessingDialog(this)
                processingDialog.show()

                //Get score
                val azureScore = paResult.pronunciationScore.toInt()

                val type = when {
                    azureScore >= 75 -> "GOOD_PRONUNCIATION"
                    azureScore >= 50 -> "MODERATE_PRONUNCIATION"
                    else -> "POOR_PRONUNCIATION"
                }

                //Store score
                wordScores[currentIndex] = azureScore

                Handler(Looper.getMainLooper()).postDelayed({
                    if (::processingDialog.isInitialized && processingDialog.isShowing) {
                        processingDialog.dismiss()
                    }

                    //Show feedback dialog
                    FeedbackDialog(this).show(
                        score = azureScore,
                        level = 2,
                        word = referenceWord,
                        pronunciationType = type
                    )

                    btnNext.isEnabled = true
                }, 700) // 700ms delay
            }

        }, onError = { errorMsg ->
            runOnUiThread {
                if (::listeningDialog.isInitialized && listeningDialog.isShowing) {
                    listeningDialog.dismiss()
                }
                if (::processingDialog.isInitialized && processingDialog.isShowing) {
                    processingDialog.dismiss()
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                btnNext.isEnabled = true
            }
        })
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
            btnNext.isEnabled = false
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
