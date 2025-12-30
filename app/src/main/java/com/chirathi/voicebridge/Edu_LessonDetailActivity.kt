//package com.chirathi.voicebridge
//
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import com.chirathi.voicebridge.data.LessonModel
//import android.speech.tts.TextToSpeech
//import java.util.Locale
//
//class Edu_LessonDetailActivity : AppCompatActivity() {
//
//    private lateinit var tts: TextToSpeech
//    private var ttsReady = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_edu_lesson_detail)
//
//        // Init TTS
//        tts = TextToSpeech(this) { status ->
//            ttsReady = status == TextToSpeech.SUCCESS
//            if (ttsReady) tts.language = Locale.getDefault()
//        }
//
//        val lesson = intent.getParcelableExtra<LessonModel>("lesson") ?: return
//
//        val ivIcon = findViewById<ImageView>(R.id.ivLessonIcon)
//        val tvTitle = findViewById<TextView>(R.id.tvLessonTitle)
//        val tvContent = findViewById<TextView>(R.id.tvLessonContent)
//        val tvQuestion = findViewById<TextView>(R.id.tvQuestion)
//        val btnHear = findViewById<Button>(R.id.btnHearQuestion)
//        val btnShow = findViewById<Button>(R.id.btnShowAnswer)
//        val btnHowTo = findViewById<Button>(R.id.btnHowTo)
//        val tvAnswer = findViewById<TextView>(R.id.tvAnswer)
//        val tvHowTo = findViewById<TextView>(R.id.tvHowTo)
//
//        val iconId = lesson.getIconResId(this)
//        if (iconId != 0) ivIcon.setImageResource(iconId) else ivIcon.setImageResource(R.drawable.ic_launcher_foreground)
//
//        tvTitle.text = lesson.lessonTitle
//        tvContent.text = lesson.lessonContent
//        tvQuestion.text = lesson.question
//
//        // Hear question aloud
//        btnHear.setOnClickListener {
//            speak("Question: ${lesson.question}")
//        }
//
//        // Show answer only when tapped; speak the answer in simple friendly wording
//        btnShow.setOnClickListener {
//            tvAnswer.visibility = View.VISIBLE
//            val friendlyAnswer = "The answer is: ${lesson.correctAnswer}"
//            tvAnswer.text = friendlyAnswer
//            speak(friendlyAnswer)
//        }
//
//        // Toggle simple "how to answer" hints (short, child-friendly steps + example)
//        btnHowTo.setOnClickListener {
//            if (tvHowTo.visibility == View.VISIBLE) {
//                tvHowTo.visibility = View.GONE
//            } else {
//                val hint = buildHowToHint(lesson.question, lesson.correctAnswer)
//                tvHowTo.text = hint
//                tvHowTo.visibility = View.VISIBLE
//                // optionally read hint aloud
//                speak("How to answer: ${stripNewlinesForSpeech(hint)}")
//            }
//        }
//    }
//
//    private fun buildHowToHint(question: String, answer: String): String {
//        // Keep hints short and simple for children
//        return "How to answer:\n" +
//                "1. Read or listen to the question: \"$question\"\n" +
//                "2. Think of the best word or number.\n" +
//                "3. Say the answer out loud or point to it.\n\n" +
//                "Example: If the question is \"$question\", the answer is \"$answer\"."
//    }
//
//    private fun stripNewlinesForSpeech(text: String): String {
//        return text.replace("\n", ". ")
//    }
//
//    private fun speak(text: String) {
//        if (ttsReady) {
//            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lessonUtterance")
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        if (::tts.isInitialized) {
//            tts.stop()
//            tts.shutdown()
//        }
//    }
//}

package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chirathi.voicebridge.data.LessonModel
import android.speech.tts.TextToSpeech
import java.util.Locale

class Edu_LessonDetailActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_lesson_detail)

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts.language = Locale.getDefault()
        }

        // Retrieve LessonModel from Intent
        val lesson = intent.getParcelableExtra<LessonModel>("lesson") ?: run {
            finish() // Close activity if no data passed
            return
        }

        val ivIcon = findViewById<ImageView>(R.id.ivLessonIcon)
        val tvTitle = findViewById<TextView>(R.id.tvLessonTitle)
        val tvContent = findViewById<TextView>(R.id.tvLessonContent)
        val tvQuestion = findViewById<TextView>(R.id.tvQuestion)
        val btnHear = findViewById<Button>(R.id.btnHearQuestion)
        val btnShow = findViewById<Button>(R.id.btnShowAnswer)
        val btnHowTo = findViewById<Button>(R.id.btnHowTo)
        val tvAnswer = findViewById<TextView>(R.id.tvAnswer)
        val tvHowTo = findViewById<TextView>(R.id.tvHowTo)

        // Set Icon
        val iconId = lesson.getIconResId(this)
        ivIcon.setImageResource(if (iconId != 0) iconId else R.drawable.lesson_icon)

        // Set Text Information
        tvTitle.text = lesson.lessonTitle
        tvContent.text = lesson.lessonContent
        tvQuestion.text = lesson.question

        // Question TTS
        btnHear.setOnClickListener {
            speak("Question: ${lesson.question}")
        }

        // Show Answer
        btnShow.setOnClickListener {
            tvAnswer.visibility = View.VISIBLE
            val friendlyAnswer = "The answer is: ${lesson.correctAnswer}"
            tvAnswer.text = friendlyAnswer
            speak(friendlyAnswer)
        }

        // How-To Hints
        btnHowTo.setOnClickListener {
            if (tvHowTo.visibility == View.VISIBLE) {
                tvHowTo.visibility = View.GONE
            } else {
                val hint = "How to answer:\n1. Read or listen to the question.\n2. Think carefully.\n3. Say the answer."
                tvHowTo.text = hint
                tvHowTo.visibility = View.VISIBLE
            }
        }
    }

    private fun speak(text: String) {
        if (ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
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
