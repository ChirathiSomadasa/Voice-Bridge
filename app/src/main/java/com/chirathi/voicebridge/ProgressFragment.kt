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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProgressFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var mainProgressLayout: FrameLayout
    private lateinit var subProgressLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        mainProgressLayout = view.findViewById(R.id.mainProgressLayout)
        subProgressLayout = view.findViewById(R.id.subProgressLayout)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadProgress(currentUser.uid, view)
        } else {
            showEmptyState(view)
        }
    }

    private fun loadProgress(userId: String, view: View) {
        val userProgressRef = db.collection("student_speech_progress").document(userId)

        userProgressRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val letterScore = document.getLong("overall_letter_progress")?.toInt() ?: 0
                    val wordScore = document.getLong("overall_word_progress")?.toInt() ?: 0
                    val sentenceScore = document.getLong("overall_sentence_progress")?.toInt() ?: 0

                    setupLevelUI(
                        score = letterScore,
                        frame = view.findViewById(R.id.frameLevel1),
                        progressBar = view.findViewById(R.id.circleProgressLevel1),
                        scoreText = view.findViewById(R.id.tvScoreLevel1),
                        statusText = view.findViewById(R.id.tvStatusLevel1)
                    )

                    setupLevelUI(
                        score = wordScore,
                        frame = view.findViewById(R.id.frameLevel2),
                        progressBar = view.findViewById(R.id.circleProgressLevel2),
                        scoreText = view.findViewById(R.id.tvScoreLevel2),
                        statusText = view.findViewById(R.id.tvStatusLevel2)
                    )

                    setupLevelUI(
                        score = sentenceScore,
                        frame = view.findViewById(R.id.frameLevel3),
                        progressBar = view.findViewById(R.id.circleProgressLevel3),
                        scoreText = view.findViewById(R.id.tvScoreLevel3),
                        statusText = view.findViewById(R.id.tvStatusLevel3)
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

                    setupMainProgress(overallAverage, view)
                    revealContent()

                } else {
                    showEmptyState(view)
                }
            }
            .addOnFailureListener {
                showEmptyState(view)
            }
    }

    private fun revealContent() {
        loadingProgressBar.visibility = View.GONE
        mainProgressLayout.visibility = View.VISIBLE
        subProgressLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState(view: View) {
        setupLevelUI(0, view.findViewById(R.id.frameLevel1), view.findViewById(R.id.circleProgressLevel1), view.findViewById(R.id.tvScoreLevel1), view.findViewById(R.id.tvStatusLevel1))
        setupLevelUI(0, view.findViewById(R.id.frameLevel2), view.findViewById(R.id.circleProgressLevel2), view.findViewById(R.id.tvScoreLevel2), view.findViewById(R.id.tvStatusLevel2))
        setupLevelUI(0, view.findViewById(R.id.frameLevel3), view.findViewById(R.id.circleProgressLevel3), view.findViewById(R.id.tvScoreLevel3), view.findViewById(R.id.tvStatusLevel3))
        setupMainProgress(0, view)
        revealContent()
    }

    private fun setupLevelUI(
        score: Int,
        frame: FrameLayout,
        progressBar: ProgressBar,
        scoreText: TextView,
        statusText: TextView
    ) {
        val context = context ?: return

        if (score > 0) {
            frame.visibility = View.VISIBLE
            statusText.visibility = View.GONE

            val color = ContextCompat.getColor(context, getColorForScore(score))
            scoreText.text = score.toString()
            scoreText.setTextColor(color)

            val drawable = RoundedCircularProgressDrawable(
                context = context,
                progressColor = color,
                backgroundColor = Color.parseColor("#CCCBCB"),
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

    private fun setupMainProgress(score: Int, view: View) {
        val context = context ?: return
        val circleProgress = view.findViewById<ProgressBar>(R.id.circleProgress)
        val tvScore = view.findViewById<TextView>(R.id.tvScore)
        val color = ContextCompat.getColor(context, getColorForScore(score))

        tvScore.text = score.toString()
        tvScore.setTextColor(color)

        val drawable = RoundedCircularProgressDrawable(
            context = context,
            progressColor = color,
            backgroundColor = Color.parseColor("#CCCBCB"),
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