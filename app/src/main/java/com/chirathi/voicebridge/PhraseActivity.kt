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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

// Word and its position in the sentence
data class BlankWord(
    val index: Int,
    val word: String
)

class PhraseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speakerBtn: LinearLayout? = null
    private var phraseText: TextView? = null
    private var greetingText: TextView? = null
    private var selectedPhrase: String = "" // This holds "Action Object" (e.g., "Play doll")
    private var fullSentence: String = ""   // This holds "I want to play with doll"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phrase)

        // 1. Get Data from Intent
        val selectedIconDrawable = intent.getIntExtra("SELECTED_ICON_DRAWABLE", R.drawable.play)
        selectedPhrase = intent.getStringExtra("SELECTED_PHRASE") ?: "Play doll"

        // 2. Build the Full Meaningful Sentence
        fullSentence = buildMeaningfulSentence(selectedPhrase)

        // 3. Initialize Views
        val quickWordImage = findViewById<ImageView>(R.id.imgQuickWord)
        phraseText = findViewById<TextView>(R.id.tvPhrase)
        greetingText = findViewById<TextView>(R.id.tvGreeting)
        val refreshBtn = findViewById<LinearLayout>(R.id.refresh)
        speakerBtn = findViewById(R.id.Speaker)

        // Set UI elements
        quickWordImage.setImageResource(selectedIconDrawable)
        val imageBytes = intent.getByteArrayExtra("DETECTED_IMAGE")
        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            quickWordImage.setImageBitmap(bitmap)
        }

        setTimeAndGreeting()

        // 4. Generate Blanks and Options
        // We hide the specific action (play) and object (doll)
        val (blankDisplay, hiddenWords) = generateMeaningfulBlanks(fullSentence, selectedPhrase)
        phraseText?.text = blankDisplay

        val options = generateOptions(hiddenWords)
        setupWordOptions(options)

        // 5. Setup Drag and Drop
        setupDragListener(hiddenWords)

        // 6. TTS Setup
        tts = TextToSpeech(this, this)
        speakerBtn?.isEnabled = false

        refreshBtn.setOnClickListener { finish() }
        speakerBtn?.setOnClickListener { speakOut(phraseText?.text.toString()) }
    }

    /**
     * Logic to create a natural sentence based on the Action and Object
     */
    private fun buildMeaningfulSentence(raw: String): String {
        val parts = raw.split(" ")
        if (parts.size < 2) return raw

        val verb = parts[0].lowercase()
        val obj = parts[1].lowercase()

        return when (obj) {
            // --- Food & Drinks ---
            "coffee", "tea", "water", "milk" -> "I want to $verb some $obj"
            "banana", "orange", "apple" -> "I want to $verb the $obj"
            "cup", "mug", "bottle", "kettle", "teapot" -> {
                if (verb == "drink") "I want to drink from the $obj"
                else if (verb == "pour") "I want to pour the $obj"
                else "I want to $verb the $obj"
            }

            // --- Clothing ---
            "clothes", "shirt", "blouse", "frock", "jacket", "jean", "short", "slippers", "trouser", "tshirt", "shoes" -> {
                if (verb == "wear") "I want to wear my $obj"
                else "I want to $verb the $obj"
            }

            // --- Furniture & Large Objects ---
            "chair", "sofa", "table" -> {
                if (verb == "sit") "I want to sit on the $obj"
                else if (verb == "sleep") "I want to sleep on the $obj"
                else "I want to $verb the $obj"
            }
            "fridge" -> {
                if (verb == "look") "I want to look inside the $obj"
                else "I want to $verb the $obj"
            }

            // --- Toys & Play ---
            "doll", "teddy bear", "bricks", "building-box", "ball", "toy car" -> {
                if (verb == "play") "I want to play with the $obj"
                else if (verb == "drive") "I want to drive my $obj"
                else "I want to $verb the $obj"
            }

            // --- Stationery & Small Tools ---
            "pen", "pencil", "notebook", "eraser", "ruler", "stapler", "correction-pen" -> {
                if (verb == "write" || verb == "draw") "I want to $verb with my $obj"
                else "I want to $verb the $obj"
            }

            // --- Tech & Devices ---
            "phone", "remote-control" -> {
                if (verb == "watch") "I want to watch the $obj"
                else "I want to $verb with the $obj"
            }

            // Default logic for anything else
            else -> "I want to $verb the $obj"
        }
    }

    /**
     * Specifically hides the Verb and Object from the sentence
     */
    private fun generateMeaningfulBlanks(sentence: String, rawInput: String): Pair<String, List<BlankWord>> {
        val rawParts = rawInput.split(" ")
        val verbToHide = rawParts[0]
        val objectToHide = rawParts[1]

        val sentenceWords = sentence.split(" ").toMutableList()
        val blanks = mutableListOf<BlankWord>()

        for (i in sentenceWords.indices) {
            // If the word matches the verb or object (ignoring case), we blank it
            if (sentenceWords[i].equals(verbToHide, ignoreCase = true) ||
                sentenceWords[i].equals(objectToHide, ignoreCase = true)) {

                blanks.add(BlankWord(i, sentenceWords[i]))
                sentenceWords[i] = "____"
            }
        }
        return Pair(sentenceWords.joinToString(" "), blanks)
    }

    private fun setupWordOptions(options: List<String>) {
        val wordContainer = findViewById<LinearLayout>(R.id.wordOptions)
        wordContainer.removeAllViews()

        for (word in options) {
            val tv = TextView(this).apply {
                text = word
                textSize = 22f
                setPadding(30, 20, 30, 20)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundResource(R.drawable.button_green_light)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(15, 10, 15, 10)
                layoutParams = params

                setOnLongClickListener {
                    val dragData = ClipData.newPlainText("word", word)
                    startDragAndDrop(dragData, View.DragShadowBuilder(this), this, 0)
                    true
                }
            }
            wordContainer.addView(tv)
        }
    }

    private fun setupDragListener(hiddenWords: List<BlankWord>) {
        phraseText?.setOnDragListener { _, event ->
            if (event.action == DragEvent.ACTION_DROP) {
                val draggedWord = event.clipData.getItemAt(0).text.toString()
                val currentText = phraseText?.text.toString()

                // Replace the first available blank
                val updatedText = currentText.replaceFirst("____", draggedWord)
                phraseText?.text = updatedText

                // If all blanks are filled, check if correct
                if (!updatedText.contains("____")) {
                    if (updatedText.equals(fullSentence, ignoreCase = true)) {
                        speakOut(updatedText)
                        Toast.makeText(this, "Excellent!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Not quite, try again!", Toast.LENGTH_SHORT).show()
                        // Reset blanks so the child can try again
                        val (resetText, _) = generateMeaningfulBlanks(fullSentence, selectedPhrase)
                        phraseText?.text = resetText
                    }
                }
            }
            true
        }
    }

    private fun generateOptions(correctWords: List<BlankWord>): List<String> {
        val distractors = listOf("ball", "run", "blue", "happy", "big", "cat", "apple", "jump")
        val options = mutableListOf<String>()
        options.addAll(correctWords.map { it.word })
        options.addAll(distractors.shuffled().take(2))
        return options.shuffled()
    }

    // --- TTS and UI Helpers (Keep as is) ---

    private fun setTimeAndGreeting() {
        val sriLankaTimeZone = TimeZone.getTimeZone("Asia/Colombo")
        val calendar = Calendar.getInstance(sriLankaTimeZone)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = sriLankaTimeZone
        }
        val currentTime = timeFormat.format(calendar.time)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Good Morning!"
            in 12..15 -> "Good Afternoon!"
            in 16..18 -> "Good Evening!"
            else -> "Good Night!"
        }
        greetingText?.text = "$greeting\n$currentTime"
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.US)
            tts?.setPitch(1.8f) // Child-like pitch
            tts?.setSpeechRate(0.9f)
            speakerBtn?.isEnabled = true
        }
    }

    private fun speakOut(text: String) {
        if (text.isNotEmpty() && !text.contains("____")) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PhraseID")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}