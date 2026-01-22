package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.data.AnswerType
import com.chirathi.voicebridge.data.SubLessonModel
import com.chirathi.voicebridge.DrawingCanvasView
import com.chirathi.voicebridge.data.OptionAdapter
import com.chirathi.voicebridge.data.MatchPairAdapter
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

        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts.language = Locale.getDefault()
        }

        subLessons = intent.getParcelableArrayListExtra("subLessons") ?: emptyList()
        if (subLessons.isEmpty()) { finish(); return }

        findViewById<View>(R.id.back)?.setOnClickListener { finish() }

//        val ivIcon = findViewById<ImageView>(R.id.ivSubLessonIcon)
        val tvTitle = findViewById<TextView>(R.id.tvSubLessonTitle)
        val tvContent = findViewById<TextView>(R.id.tvSubLessonContent)
        val tvQuestion = findViewById<TextView>(R.id.tvSubQuestion)
        val btnShowAnswer = findViewById<Button>(R.id.btnSubShowAnswer)
        val tvAnswer = findViewById<TextView>(R.id.tvSubAnswer)
        val btnHowTo = findViewById<Button>(R.id.btnSubHowTo)
        val tvHowTo = findViewById<TextView>(R.id.tvSubHowTo)
        val btnNextLesson = findViewById<Button>(R.id.btnNextSubLesson)

        val rvOptions = findViewById<RecyclerView>(R.id.rvSubOptions)
        val drawingCanvas = findViewById<DrawingCanvasView>(R.id.subDrawingCanvas)
        val btnClearDrawing = findViewById<Button>(R.id.btnSubClearDrawing)
        val rvMatchPairs = findViewById<RecyclerView>(R.id.rvSubMatchPairs)
        val tvMatchStatus = findViewById<TextView>(R.id.tvSubMatchStatus)

        fun applyLesson() {
            val lesson = subLessons[currentSubLessonIndex]

            val iconId = resources.getIdentifier(lesson.iconName, "drawable", packageName)
//            ivIcon.setImageResource(if (iconId != 0) iconId else R.drawable.lesson_icon)

            tvTitle.text = lesson.lessonTitle
            tvContent.text = lesson.lessonContent
            tvQuestion.text = lesson.question
            tvAnswer.visibility = View.GONE
            tvHowTo.visibility = View.GONE
            rvOptions.visibility = View.GONE
            drawingCanvas.visibility = View.GONE
            btnClearDrawing.visibility = View.GONE
            rvMatchPairs.visibility = View.GONE
            tvMatchStatus.visibility = View.GONE
            btnShowAnswer.visibility = View.VISIBLE

            when (lesson.answerType) {
                AnswerType.MCQ -> {
                    rvOptions.visibility = View.VISIBLE
                    rvOptions.layoutManager = LinearLayoutManager(this)
                    rvOptions.adapter = OptionAdapter(lesson.options) { chosen ->
                        val feedback = if (chosen.isCorrect) "Correct!" else "Try again."
                        tvAnswer.visibility = View.VISIBLE
                        tvAnswer.text = feedback
                        speak(feedback)
                    }
                }
                AnswerType.DRAW -> {
                    drawingCanvas.visibility = View.VISIBLE
                    btnClearDrawing.visibility = View.VISIBLE
                    btnClearDrawing.setOnClickListener { drawingCanvas.clearCanvas() }
                    btnShowAnswer.visibility = View.GONE
                }
                AnswerType.MATCH -> {
                    rvMatchPairs.visibility = View.VISIBLE
                    tvMatchStatus.visibility = View.VISIBLE
                    rvMatchPairs.layoutManager = LinearLayoutManager(this)
                    rvMatchPairs.adapter = MatchPairAdapter(lesson.matchPairs) { pair ->
                        val msg = "Pair: ${pair.left} ↔ ${pair.right}"
                        tvMatchStatus.text = msg
                        speak(msg)
                    }
                    btnShowAnswer.visibility = View.GONE
                }
                else -> { /* TEXT */ }
            }

            btnShowAnswer.setOnClickListener {
                tvAnswer.visibility = View.VISIBLE
                val answerText = "Answer: ${lesson.correctAnswer}"
                tvAnswer.text = answerText
                speak(answerText)
            }

            btnHowTo.setOnClickListener {
                if (tvHowTo.visibility == View.VISIBLE) {
                    tvHowTo.visibility = View.GONE
                } else {
                    val hint = if (lesson.howToSteps.isNotEmpty()) {
                        "How to answer:\n" + lesson.howToSteps.joinToString("\n") { "• $it" }
                    } else {
                        "How to answer:\n1. Listen to the question.\n2. Think.\n3. Answer."
                    }
                    tvHowTo.text = hint
                    tvHowTo.visibility = View.VISIBLE
                    speak("How to answer: ${hint.replace("\n", ". ")}")
                }
            }
        }

        btnNextLesson.setOnClickListener {
            if (currentSubLessonIndex < subLessons.size - 1) {
                currentSubLessonIndex++
                applyLesson()
            } else {
                finish()
            }
        }

        applyLesson()
    }

    private fun speak(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "subLessonUtterance")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop(); tts.shutdown()
        }
    }
}