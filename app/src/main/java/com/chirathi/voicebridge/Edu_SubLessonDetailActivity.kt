package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chirathi.voicebridge.data.SubLessonModel
import android.speech.tts.TextToSpeech
import java.util.Locale


class Edu_SubLessonDetailActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var currentSubLessonIndex = 0
    private lateinit var subLessons: List<SubLessonModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_sub_lesson_detail)

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts.language = Locale.getDefault()
        }

        // Get sub-lessons from the intent
        subLessons = intent.getParcelableArrayListExtra("subLessons") ?: emptyList()

        // Initialize views
        val ivIcon = findViewById<ImageView>(R.id.ivSubLessonIcon)
        val tvTitle = findViewById<TextView>(R.id.tvSubLessonTitle)
        val tvContent = findViewById<TextView>(R.id.tvSubLessonContent)
        val tvQuestion = findViewById<TextView>(R.id.tvSubQuestion)
        val btnShowAnswer = findViewById<Button>(R.id.btnSubShowAnswer)
        val tvAnswer = findViewById<TextView>(R.id.tvSubAnswer)
        val btnHowTo = findViewById<Button>(R.id.btnSubHowTo)
        val tvHowTo = findViewById<TextView>(R.id.tvSubHowTo)
        val btnNextLesson = findViewById<Button>(R.id.btnNextSubLesson)

        updateUI(ivIcon, tvTitle, tvContent, tvQuestion, tvAnswer, tvHowTo)

        // Show answer logic
        btnShowAnswer.setOnClickListener {
            tvAnswer.visibility = View.VISIBLE
            val answerText = "Answer: ${subLessons[currentSubLessonIndex].correctAnswer}"
            tvAnswer.text = answerText
            speak(answerText)
        }

        // How-To question hint
        btnHowTo.setOnClickListener {
            if (tvHowTo.visibility == View.VISIBLE) {
                tvHowTo.visibility = View.GONE
            } else {
                val hint = buildHowToHint(
                    subLessons[currentSubLessonIndex].question,
                    subLessons[currentSubLessonIndex].correctAnswer
                )
                tvHowTo.text = hint
                tvHowTo.visibility = View.VISIBLE
                speak("How to answer: ${stripNewlinesForSpeech(hint)}")
            }
        }

        // Next lesson navigation
        btnNextLesson.setOnClickListener {
            if (currentSubLessonIndex < subLessons.size - 1) {
                currentSubLessonIndex++
                updateUI(ivIcon, tvTitle, tvContent, tvQuestion, tvAnswer, tvHowTo)
            } else {
                finish() // End the activity when all sub-lessons are complete
            }
        }
    }

    private fun updateUI(
        ivIcon: ImageView,
        tvTitle: TextView,
        tvContent: TextView,
        tvQuestion: TextView,
        tvAnswer: TextView,
        tvHowTo: TextView
    ) {
        val currentLesson = subLessons[currentSubLessonIndex]

        tvTitle.text = currentLesson.lessonTitle
        tvContent.text = currentLesson.lessonContent
        tvQuestion.text = currentLesson.question

        // Set the lesson icon using the iconName field
        val iconId = resources.getIdentifier(currentLesson.iconName, "drawable", packageName)
        if (iconId != 0) {
            ivIcon.setImageResource(iconId)
        } else {
            ivIcon.setImageResource(R.drawable.ic_launcher_foreground) // Default fallback
        }

        tvAnswer.visibility = View.GONE
        tvHowTo.visibility = View.GONE
    }

    private fun buildHowToHint(question: String, answer: String): String {
        return """
            How to answer:
            1. Listen to the question: "$question"
            2. Think carefully about your response.
            3. Say the answer: "$answer".
        """.trimIndent()
    }

    private fun stripNewlinesForSpeech(text: String): String {
        return text.replace("\n", ". ")
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "subLessonUtterance")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}