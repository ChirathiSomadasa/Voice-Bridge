package com.chirathi.voicebridge

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LetterProgressActivity : AppCompatActivity() {

    private lateinit var customDrawable: RoundedCircularProgressDrawable

    // Firebase Instances
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_letter_progress)

        // Initialize Auth
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 1. GET DATA
        val score = intent.getIntExtra("PROGRESS_SCORE", 0)
        val batchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        val canContinue = intent.getBooleanExtra("CAN_CONTINUE", false)
        val isPassed = score >= 75

        // 2. SAVE & CALCULATE OVERALL PROGRESS (Only if Passed)
        if (isPassed && currentUser != null) {
            saveAndCalculateProgress(currentUser.uid, batchIndex, score)
        }

        // 3. UI REFERENCES
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val circleProgress = findViewById<ProgressBar>(R.id.circleProgress)
        val llTryAgain = findViewById<LinearLayout>(R.id.llTryAgain)
        val llHome = findViewById<LinearLayout>(R.id.llHome)
        val llContinue = findViewById<LinearLayout>(R.id.llContinue)

        // 4. COLOR LOGIC
        val colorResId = when {
            score >= 75 -> R.color.score_green
            score >= 50 -> R.color.score_orange
            else -> R.color.score_red
        }
        val color = ContextCompat.getColor(this, colorResId)
        tvScore.setTextColor(color)

        // 5. CUSTOM DRAWABLE
        customDrawable = RoundedCircularProgressDrawable(
            context = this,
            progressColor = color,
            backgroundColor = Color.parseColor("#E0E0E0"),
            strokeWidthDp = 12f
        )
        circleProgress.progressDrawable = customDrawable

        // 6. ANIMATION
        val animator = ValueAnimator.ofInt(0, score)
        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            tvScore.text = animatedValue.toString()
            circleProgress.progress = animatedValue
        }
        animator.start()

        // 7. BUTTON VISIBILITY
        val retryParams = llTryAgain.layoutParams as LinearLayout.LayoutParams
        val homeParams = llHome.layoutParams as LinearLayout.LayoutParams
        val continueParams = llContinue.layoutParams as LinearLayout.LayoutParams

        if (isPassed && canContinue) {
            llContinue.visibility = View.VISIBLE
            retryParams.weight = 1f
            homeParams.weight = 1f
            continueParams.weight = 1f
            llContinue.layoutParams = continueParams
        } else {
            llContinue.visibility = View.GONE
            retryParams.weight = 1.5f
            homeParams.weight = 1.5f
        }

        // 8. BUTTON ACTIONS
        llTryAgain.setOnClickListener {
            val intent = Intent(this, SpeechLevel1TaskActivity::class.java)
            intent.putExtra("BATCH_INDEX", batchIndex)
            startActivity(intent)
            finish()
        }

        llHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        llContinue.setOnClickListener {
            val intent = Intent(this, LevelTransitionActivity::class.java)
            intent.putExtra("NEXT_BATCH_INDEX", batchIndex + 1)
            startActivity(intent)
            finish()
        }
    }

    // --- FIREBASE SAVING & CALCULATION ---
    private fun saveAndCalculateProgress(userId: String, batchIndex: Int, score: Int) {
        val batchKey = "batch_$batchIndex"
        val batchData = hashMapOf(batchKey to score)

        // Reference to the document that stores ALL batch scores for Level 1
        val scoresRef = db.collection("student_progress")
            .document(userId)
            .collection("level_1_letters")
            .document("batch_scores")

        // Step 1: Save the current batch score
        scoresRef.set(batchData, SetOptions.merge())
            .addOnSuccessListener {
                // Step 2: Read back all scores to calculate average
                scoresRef.get().addOnSuccessListener { document ->
                    if (document.exists() && document.data != null) {
                        var totalScore = 0
                        var batchCount = 0

                        // Iterate through all fields (batch_0, batch_1, etc.)
                        for ((key, value) in document.data!!) {
                            if (key.startsWith("batch_")) {
                                val batchScore = (value as? Long)?.toInt() ?: 0
                                totalScore += batchScore
                                batchCount++
                            }
                        }

                        // Step 3: Calculate Average
                        if (batchCount > 0) {
                            val overallAverage = totalScore / batchCount

                            // Step 4: Save the Overall Average to the main user document
                            val summaryData = hashMapOf("overall_letter_progress" to overallAverage)

                            db.collection("student_speech_progress")
                                .document(userId)
                                .set(summaryData, SetOptions.merge())
                                .addOnSuccessListener {
                                    // Optional: Log success (e.g. "Overall Progress Updated: $overallAverage%")
                                }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save progress", Toast.LENGTH_SHORT).show()
            }
    }

    // --- DRAWABLE CLASS ---
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