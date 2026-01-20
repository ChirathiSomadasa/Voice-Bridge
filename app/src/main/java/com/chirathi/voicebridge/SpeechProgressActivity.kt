package com.chirathi.voicebridge

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SpeechProgressActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var mainProgressLayout: FrameLayout
    private lateinit var subProgressLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_progress)

        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        mainProgressLayout = findViewById(R.id.mainProgressLayout)
        subProgressLayout = findViewById(R.id.subProgressLayout)

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener { finish() }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadProgress(currentUser.uid)
        } else {
            showEmptyState()
        }
    }

    private fun loadProgress(userId: String) {
        val userProgressRef = db.collection("student_speech_progress").document(userId)

        userProgressRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val letterScore = document.getLong("overall_letter_progress")?.toInt() ?: 0
                    val wordScore = document.getLong("overall_word_progress")?.toInt() ?: 0
                    val sentenceScore = document.getLong("overall_sentence_progress")?.toInt() ?: 0

                    setupLevelUI(
                        score = letterScore,
                        frame = findViewById(R.id.frameLevel1),
                        progressBar = findViewById(R.id.circleProgressLevel1),
                        scoreText = findViewById(R.id.tvScoreLevel1),
                        statusText = findViewById(R.id.tvStatusLevel1)
                    )

                    setupLevelUI(
                        score = wordScore,
                        frame = findViewById(R.id.frameLevel2),
                        progressBar = findViewById(R.id.circleProgressLevel2),
                        scoreText = findViewById(R.id.tvScoreLevel2),
                        statusText = findViewById(R.id.tvStatusLevel2)
                    )

                    setupLevelUI(
                        score = sentenceScore,
                        frame = findViewById(R.id.frameLevel3),
                        progressBar = findViewById(R.id.circleProgressLevel3),
                        scoreText = findViewById(R.id.tvScoreLevel3),
                        statusText = findViewById(R.id.tvStatusLevel3)
                    )

                    var totalScore = 0
                    var activeCount = 0
                    if (letterScore > 0) { totalScore += letterScore; activeCount++ }
                    if (wordScore > 0) { totalScore += wordScore; activeCount++ }
                    if (sentenceScore > 0) { totalScore += sentenceScore; activeCount++ }

                    val overallAverage = if (activeCount > 0) totalScore / activeCount else 0

                    // --- NEW: Save the Calculated Overall Progress to Firebase ---
                    val updateData = hashMapOf("overall_progress" to overallAverage)
                    userProgressRef.set(updateData, SetOptions.merge())
                    // -------------------------------------------------------------

                    setupMainProgress(overallAverage)
                    revealContent()

                } else {
                    showEmptyState()
                }
            }
            .addOnFailureListener {
                showEmptyState()
            }
    }

    private fun revealContent() {
        loadingProgressBar.visibility = View.GONE
        mainProgressLayout.visibility = View.VISIBLE
        subProgressLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        setupLevelUI(0, findViewById(R.id.frameLevel1), findViewById(R.id.circleProgressLevel1), findViewById(R.id.tvScoreLevel1), findViewById(R.id.tvStatusLevel1))
        setupLevelUI(0, findViewById(R.id.frameLevel2), findViewById(R.id.circleProgressLevel2), findViewById(R.id.tvScoreLevel2), findViewById(R.id.tvStatusLevel2))
        setupLevelUI(0, findViewById(R.id.frameLevel3), findViewById(R.id.circleProgressLevel3), findViewById(R.id.tvScoreLevel3), findViewById(R.id.tvStatusLevel3))
        setupMainProgress(0)
        revealContent()
    }

    private fun setupLevelUI(
        score: Int,
        frame: FrameLayout,
        progressBar: ProgressBar,
        scoreText: TextView,
        statusText: TextView
    ) {
        if (score > 0) {
            frame.visibility = View.VISIBLE
            statusText.visibility = View.GONE

            val color = ContextCompat.getColor(this, getColorForScore(score))
            scoreText.text = score.toString()
            scoreText.setTextColor(color)

            val drawable = RoundedCircularProgressDrawable(
                context = this,
                progressColor = color,
                backgroundColor = Color.parseColor("#E0E0E0"),
                strokeWidthDp = 8f
            )
            progressBar.progressDrawable = drawable
            progressBar.rotation = 0f

            val animator = ValueAnimator.ofInt(0, score)
            animator.duration = 1000
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            animator.start()

        } else {
            frame.visibility = View.GONE
            statusText.visibility = View.VISIBLE
        }
    }

    private fun setupMainProgress(score: Int) {
        val circleProgress = findViewById<ProgressBar>(R.id.circleProgress)
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val color = ContextCompat.getColor(this, getColorForScore(score))

        tvScore.text = score.toString()
        tvScore.setTextColor(color)

        val drawable = RoundedCircularProgressDrawable(
            context = this,
            progressColor = color,
            backgroundColor = Color.parseColor("#E0E0E0"),
            strokeWidthDp = 12f
        )
        circleProgress.progressDrawable = drawable
        circleProgress.rotation = 0f

        val animator = ValueAnimator.ofInt(0, score)
        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            circleProgress.progress = animatedValue
            tvScore.text = animatedValue.toString()
        }
        animator.start()
    }

    private fun getColorForScore(score: Int): Int {
        return when {
            score >= 75 -> R.color.score_green
            score >= 50 -> R.color.score_orange
            else -> R.color.score_red
        }
    }

    private class RoundedCircularProgressDrawable(
        context: Context,
        private var progressColor: Int,
        private val backgroundColor: Int,
        strokeWidthDp: Float
    ) : Drawable() {
        private val strokeWidth: Float = strokeWidthDp * context.resources.displayMetrics.density
        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rectF = RectF()
        private var currentLevel = 0

        init {
            backgroundPaint.color = backgroundColor
            backgroundPaint.style = Paint.Style.STROKE
            backgroundPaint.strokeWidth = strokeWidth
            progressPaint.color = progressColor
            progressPaint.style = Paint.Style.STROKE
            progressPaint.strokeWidth = strokeWidth
            progressPaint.strokeCap = Paint.Cap.ROUND
        }

        override fun onLevelChange(level: Int): Boolean {
            currentLevel = level
            invalidateSelf()
            return true
        }

        override fun draw(canvas: Canvas) {
            val halfStroke = strokeWidth / 2f
            rectF.set(bounds.left + halfStroke, bounds.top + halfStroke, bounds.right - halfStroke, bounds.bottom - halfStroke)
            canvas.drawOval(rectF, backgroundPaint)
            val sweepAngle = (currentLevel / 10000f) * 360f
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }

        override fun setAlpha(alpha: Int) { progressPaint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { progressPaint.colorFilter = colorFilter }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}