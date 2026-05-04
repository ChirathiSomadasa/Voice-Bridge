package com.chirathi.voicebridge

import android.content.ClipData
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import java.text.SimpleDateFormat
import java.util.*
import android.view.animation.AnimationUtils
import android.view.HapticFeedbackConstants
import android.media.MediaPlayer
import com.airbnb.lottie.LottieAnimationView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class BlankWord(
    val index: Int,
    val word: String
)

class PhraseActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speakerBtn: LinearLayout? = null
    private var phraseText: TextView? = null
    private var greetingText: TextView? = null
    private var selectedPhrase: String = ""
    private var fullSentence: String = ""
    private lateinit var wordContainer: com.google.android.flexbox.FlexboxLayout
    private lateinit var lottieConfetti: LottieAnimationView
    private lateinit var lottieThinking: LottieAnimationView

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_SENTENCE_GENERATE_API_KEY
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phrase)

        wordContainer = findViewById(R.id.wordOptions)
        lottieConfetti = findViewById(R.id.lottieConfetti)
        lottieThinking = findViewById(R.id.lottieThinking)

        val selectedIconDrawable = intent.getIntExtra("SELECTED_ICON_DRAWABLE", R.drawable.play)
        selectedPhrase = intent.getStringExtra("SELECTED_PHRASE") ?: "Play doll"
        val isEmotionMode = intent.getBooleanExtra("IS_EMOTION_MODE", false)
        val isSymbolMode = intent.getBooleanExtra("IS_SYMBOL_MODE", false)
        val isQuickMode = intent.getBooleanExtra("IS_QUICK_MODE", false)
        val isPainMode = intent.getBooleanExtra("IS_PAIN_MODE", false)

        val quickWordImage = findViewById<ImageView>(R.id.imgQuickWord)
        phraseText = findViewById<TextView>(R.id.tvPhrase)
        greetingText = findViewById<TextView>(R.id.tvGreeting)
        val refreshBtn = findViewById<LinearLayout>(R.id.refresh)
        speakerBtn = findViewById(R.id.Speaker)

        val imageBytes = intent.getByteArrayExtra("DETECTED_IMAGE")
        if (imageBytes != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            quickWordImage.setImageBitmap(bitmap)
        } else {
            quickWordImage.setImageResource(selectedIconDrawable)
        }

        setTimeAndGreeting()

        tts = TextToSpeech(this, this)
        speakerBtn?.isEnabled = false

        refreshBtn.setOnClickListener { finish() }
        speakerBtn?.setOnClickListener { speakOut(phraseText?.text.toString()) }

        lottieThinking.visibility = View.VISIBLE
        lottieThinking.playAnimation()

        lifecycleScope.launch {
            fullSentence = when {
                isEmotionMode -> {
                    selectedPhrase
                }
                isQuickMode -> {
                    selectedPhrase
                }
                isPainMode -> {
                    selectedPhrase
                }
                isSymbolMode -> {
                    val parts = selectedPhrase.split(" ")
                    val v = parts.getOrElse(0) { "" }
                    val o = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                    generateSentenceWithLLM(v, o)
                }
                else -> {
                    val parts = selectedPhrase.split(", ")
                    val v = parts.getOrElse(0) { "detecting" }
                    val o = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                    generateSentenceWithLLM(v, o)
                }
            }

            // UI: Hide the thinking animation once the sentence is ready
            lottieThinking.visibility = View.GONE
            lottieThinking.cancelAnimation()

            // Process the full sentence into a version with blanks
            val result = generateMeaningfulBlanks(fullSentence, selectedPhrase)
            phraseText?.text = result.first

            // Short delay for better UX before showing word options
            delay(600)

            // Setup the interactive word bank
            val options = generateOptions(result.second)
            setupWordOptions(options)
            setupDragListener()
        }
    }

    private suspend fun generateSentenceWithLLM(verb: String, obj: String): String {
        return withContext(Dispatchers.IO) {
            Log.d("GEMINI_CHECK", "LLM process started for: $verb + $obj")
            try {
                val prompt =  """
    You are a smart AAC assistant for a child. 
    Generate a simple, natural 4-6 word sentence using the input.
    Input:
    - Verb: '${verb.ifEmpty { "detecting" }}'
    - Object: '$obj'
    Instructions:
    1. Always use correct prepositions (e.g., 'to', 'with', 'in', 'on', 'a', 'the') to make the sentence grammatically perfect.
    2. If Verb is 'detecting': Use "I see a..." or "There is a...".
    3. If Verb is 'have' (for pain/feelings): Use "I have a...".
    4. If Verb is an action (eat, drink, play, etc.): Use "I want to [verb] [object]" or "I want a [object]".
    5. Keep it very simple for a child.
    6. Output ONLY the sentence in lowercase without periods.
""".trimIndent()
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.trim()?.lowercase() ?: ""

                withContext(Dispatchers.Main) {
                    if (text.isNotEmpty()) {
                        Log.d("GEMINI_CHECK", "Response received: $text")
                        Toast.makeText(this@PhraseActivity, "Gemini Success: $text", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w("GEMINI_CHECK", "Response is empty!")
                        Toast.makeText(this@PhraseActivity, "Gemini returned empty response", Toast.LENGTH_SHORT).show()
                    }
                }

                if (text.isNotEmpty()) {
                    text.replaceFirstChar { it.uppercase() }
                } else {
                    buildMeaningfulSentence(verb, obj)
                }
            } catch (e: Exception) {
                buildMeaningfulSentence(verb, obj)
            }
        }
    }

    private fun buildMeaningfulSentence(verb: String, obj: String): String {
        val v = verb.lowercase()
        val o = obj.lowercase()

        val sentence = when (o) {
            "coffee", "tea", "water", "milk" -> "I want to $v some $o"
            "banana", "orange", "apple" -> "I want to $v the $o"
            "cup", "mug", "bottle", "kettle", "teapot" -> {
                if (v == "drink") "I want to drink from the $o"
                else if (v == "pour") "I want to pour the $o"
                else "I want to $v the $o"
            }
            "clothes", "shirt", "blouse", "frock", "jacket", "jean", "short", "slippers", "trouser", "tshirt", "shoes" -> {
                if (v == "wear") "I want to wear my $o"
                else "I want to $v the $o"
            }
            "chair", "sofa", "table" -> {
                if (v == "sit") "I want to sit on the $o"
                else if (v == "sleep") "I want to sleep on the $o"
                else "I want to $v the $o"
            }
            "fridge" -> {
                if (v == "look") "I want to look inside the $o"
                else "I want to $v the $o"
            }
            "doll", "teddy bear", "bricks", "building-box", "ball", "toy car" -> {
                if (v == "play") "I want to play with the $o"
                else if (v == "drive") "I want to drive my $o"
                else "I want to $v the $o"
            }
            "pen", "pencil", "notebook", "eraser", "ruler", "stapler", "correction-pen" -> {
                if (v == "write" || v == "draw") "I want to $v with my $o"
                else "I want to $v the $o"
            }
            "phone", "remote-control" -> {
                if (v == "watch") "I want to watch the $o"
                else "I want to $v with the $o"
            }
            else -> "I want to $v the $o"
        }
        return sentence.replaceFirstChar { it.uppercase() }
    }

    private fun generateMeaningfulBlanks(sentence: String, rawInput: String): Pair<String, List<BlankWord>> {
        val rawParts = rawInput.split(" ")
        val sentenceWords = sentence.split(" ").toMutableList()
        val blanks = mutableListOf<BlankWord>()

        for (part in rawParts) {
            val cleanPart = part.replace(Regex("[^a-zA-Z]"), "")
            if (cleanPart.isEmpty()) continue

            for (i in sentenceWords.indices) {
                val cleanWord = sentenceWords[i].replace(Regex("[^a-zA-Z]"), "")
                if (cleanWord.equals(cleanPart, ignoreCase = true) && sentenceWords[i] != "____") {
                    blanks.add(BlankWord(i, sentenceWords[i]))
                    sentenceWords[i] = "____"
                    break
                }
            }
        }
        return Pair(sentenceWords.joinToString(" "), blanks)
    }

    private fun setupWordOptions(options: List<String>) {
        wordContainer.removeAllViews()
        for (word in options) {
            val tv = TextView(this).apply {
                text = word
                textSize = 22f
                setPadding(28, 18, 28, 18)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundResource(R.drawable.button_green_light)

                val params = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                    com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    com.google.android.flexbox.FlexboxLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(10, 8, 10, 8)
                layoutParams = params

                setOnClickListener { handleWordSelection(word, this) }
                setOnLongClickListener {
                    val dragData = ClipData.newPlainText("word", word)
                    startDragAndDrop(dragData, View.DragShadowBuilder(this), this, 0)
                    true
                }
            }
            wordContainer.addView(tv)
        }
    }

    private fun handleWordSelection(word: String, view: View) {
        val currentText = phraseText?.text.toString()
        if (currentText.contains("____")) {
            val updatedText = currentText.replaceFirst("____", word)
            phraseText?.text = updatedText
            view.isEnabled = false
            view.alpha = 0.4f
            if (!updatedText.contains("____")) checkResult(updatedText)
        }
    }

    private fun checkResult(updatedText: String) {
        if (updatedText.equals(fullSentence, ignoreCase = true)) {
            speakOut(updatedText)
            lottieConfetti.visibility = View.VISIBLE
            lottieConfetti.playAnimation()
            lottieConfetti.postDelayed({ lottieConfetti.visibility = View.GONE }, 3000)
            playFeedbackSound(true)
        } else {
            Toast.makeText(this, "Try again!", Toast.LENGTH_SHORT).show()
            wordContainer.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
            wordContainer.performHapticFeedback(HapticFeedbackConstants.REJECT)
            playFeedbackSound(false)
            wordContainer.postDelayed({
                val result = generateMeaningfulBlanks(fullSentence, selectedPhrase)
                phraseText?.text = result.first
                wordContainer.children.forEach { child ->
                    child.isEnabled = true
                    child.alpha = 1f
                }
            }, 500)
        }
    }

    private fun playFeedbackSound(isSuccess: Boolean) {
        try {
            val soundRes = if (isSuccess) R.raw.correct_sound else R.raw.wrong_sound
            MediaPlayer.create(this, soundRes).apply {
                start()
                setOnCompletionListener { release() }
            }
        } catch (e: Exception) { Log.e("SoundError", "${e.message}") }
    }

    private fun setupDragListener() {
        phraseText?.setOnDragListener { _, event ->
            if (event.action == DragEvent.ACTION_DROP) {
                val draggedWord = event.clipData.getItemAt(0).text.toString()
                handleWordSelection(draggedWord, View(this))
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

    private fun setTimeAndGreeting() {
        val sriLankaTimeZone = TimeZone.getTimeZone("Asia/Colombo")
        val calendar = Calendar.getInstance(sriLankaTimeZone)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = sriLankaTimeZone
        }
        val currentTime = timeFormat.format(calendar.time)
        val greeting = when (calendar.get(Calendar.HOUR_OF_DAY)) {
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
            tts?.setPitch(1.8f)
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