package com.chirathi.voicebridge

import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

data class BlankWord(
    val index: Int,
    val word: String
)

class PhraseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speakerBtn: LinearLayout? = null
    private var phraseText: TextView? = null
    private var greetingText: TextView? = null
    private var selectedPhrase: String = "I want to play"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phrase)

        // Get the passed data from intent
        val selectedIconDrawable = intent.getIntExtra("SELECTED_ICON_DRAWABLE", R.drawable.play)
        selectedPhrase = intent.getStringExtra("SELECTED_PHRASE") ?: "I want to play"

        // Initialize views
        val quickWordImage = findViewById<ImageView>(R.id.imgQuickWord)
        phraseText = findViewById<TextView>(R.id.tvPhrase)
        greetingText = findViewById<TextView>(R.id.tvGreeting)
        val refreshBtn = findViewById<LinearLayout>(R.id.refresh)
        speakerBtn = findViewById(R.id.Speaker)

        // Set the icon and phrase based on what was clicked
        quickWordImage.setImageResource(selectedIconDrawable)
        phraseText?.text = selectedPhrase

        val imageBytes = intent.getByteArrayExtra("DETECTED_IMAGE")

        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            quickWordImage.setImageBitmap(bitmap)
        }
        setTimeAndGreeting()

        // Initialize TextToSpeech with specific engine if needed
        tts = TextToSpeech(this, this)

        speakerBtn?.isEnabled = false
        refreshBtn.setOnClickListener { finish() }
        speakerBtn?.setOnClickListener {
            val currentText = phraseText?.text.toString()
            speakOut(currentText)
        }

        val (blankSentence, hiddenWords) =
            generateSentenceWithBlanks(selectedPhrase)

        phraseText?.text = blankSentence

        val options = generateOptions(hiddenWords)

        val wordContainer = findViewById<LinearLayout>(R.id.wordOptions)

        for (word in options) {
            val tv = TextView(this).apply {
                text = word
                textSize = 20f
                setPadding(24, 16, 24, 16)
                setBackgroundResource(R.drawable.button_green_light)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                params.setMargins(20, 10, 20, 10) // LEFT, TOP, RIGHT, BOTTOM gap
                layoutParams = params

                setOnLongClickListener {
                    val dragData = ClipData.newPlainText("word", word)
                    startDragAndDrop(dragData, View.DragShadowBuilder(this), this, 0)
                    true
                }
            }
            wordContainer.addView(tv)
        }

        phraseText?.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val draggedWord = event.clipData.getItemAt(0).text.toString()
                    val currentText = phraseText?.text.toString()

                    // Replace the first occurrence of the blank
                    val updatedText = currentText.replaceFirst("____", draggedWord)
                    phraseText?.text = updatedText

                    // Check if all blanks are filled
                    if (!updatedText.contains("____")) {
                        // Small delay optional, but usually better to let the UI update first
                        speakOut(updatedText)
                    }
                }
            }
            true
        }

        fun isCorrect(filledSentence: String, original: String): Boolean {
            return filledSentence.equals(original, ignoreCase = true)
        }
    }


    private fun generateOptions(
        correctWords: List<BlankWord>
    ): List<String> {

        val extraWords = listOf("ball", "girl", "dog", "run","standing","pink","blue","black") // dummy distractors

        val options = mutableListOf<String>()
        options.addAll(correctWords.map { it.word })
        options.addAll(extraWords.shuffled().take(2))

        return options.shuffled()
    }

    private fun generateSentenceWithBlanks(sentence: String): Pair<String, List<BlankWord>> {
        val words = sentence.split(" ").toMutableList()

        // pick nouns / random words (simple version)
        val candidateIndexes = words.indices.filter {
            words[it].length > 3
        }

        val selectedIndexes = candidateIndexes.shuffled().take(2)

        val blanks = mutableListOf<BlankWord>()

        for (i in selectedIndexes) {
            blanks.add(BlankWord(i, words[i]))
            words[i] = "____"
        }

        return Pair(words.joinToString(" " ), blanks)
    }

    private fun setTimeAndGreeting() {
        // Set Sri Lanka timezone (Asia/Colombo)
        val sriLankaTimeZone = TimeZone.getTimeZone("Asia/Colombo")
        val calendar = Calendar.getInstance(sriLankaTimeZone)

        // Format time
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        timeFormat.timeZone = sriLankaTimeZone
        val currentTime = timeFormat.format(calendar.time)

        // Get appropriate greeting based on time
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = getEnglishGreeting(hour)

        // Set the greeting text with time and date
        greetingText?.text = "$greeting\n$currentTime"
    }

    private fun getEnglishGreeting(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good Morning!"
            in 12..15 -> "Good Afternoon!"
            in 16..18 -> "Good Evening!"
            else -> "Good Night!"
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setChildLikeVoice()
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun setChildLikeVoice() {
        val result = tts?.setLanguage(Locale.US)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "The Language is not supported!")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = tts!!.voices
            val tinyVoice = voices.firstOrNull { voice ->
                voice.locale == Locale.US &&
                        !voice.isNetworkConnectionRequired &&
                        voice.quality >= Voice.QUALITY_HIGH
            }

            if (tinyVoice != null) {
                tts!!.voice = tinyVoice
                setVoiceParameters()
                Log.d("TTS", "Using tiny voice: ${tinyVoice.name}")
            } else {
                setVoiceParameters()
            }
        }


        speakerBtn?.isEnabled = true
    }

    private fun setVoiceParameters() {
        try {
            // Higher pitch for child-like voice
            tts?.setPitch(1.9f)  // Higher pitch sounds more child-like

            // Slightly faster speech rate for energetic child voice
            tts?.setSpeechRate(0.9f)  // 1.0 is normal rate

            Log.d("TTS", "Voice parameters set: Pitch=1.9, Rate=0.9")
        } catch (e: Exception) {
            Log.e("TTS", "Error setting voice parameters: ${e.message}")
        }
    }

    private fun speakOut(textToSay: String) {
        if (textToSay.isEmpty()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null, "PhraseID")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(textToSay, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error speaking: ${e.message}")
        }
    }

    public override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        tts?.stop()
    }
}