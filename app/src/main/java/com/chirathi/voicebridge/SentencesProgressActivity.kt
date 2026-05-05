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

class SentencesProgressActivity : AppCompatActivity() {

    private lateinit var customDrawable: RoundedCircularProgressDrawable
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sentences_progress)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // 1. GET DATA
        val score = intent.getIntExtra("PROGRESS_SCORE", 0)
        val batchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        val canContinue = intent.getBooleanExtra("CAN_CONTINUE", false)
        val isPassed = score >= 75

        // 2. SAVE TO FIREBASE
        if (currentUser != null) {
            saveAndCalculateSentenceProgress(currentUser.uid, batchIndex, score)
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

        // 5. DRAWABLE SETUP
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

        // 7. BUTTON VISIBILITY (Always Show All 3 Buttons)
        val retryParams = llTryAgain.layoutParams as LinearLayout.LayoutParams
        val homeParams = llHome.layoutParams as LinearLayout.LayoutParams
        val continueParams = llContinue.layoutParams as LinearLayout.LayoutParams

        llContinue.visibility = View.VISIBLE
        retryParams.weight = 1f
        homeParams.weight = 1f
        continueParams.weight = 1f
        llContinue.layoutParams = continueParams

        // 8. BUTTON ACTIONS
        llTryAgain.setOnClickListener {
            if (currentUser != null) {
                val prefs = getSharedPreferences("VoiceBridgePrefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("SAVED_BATCH_LEVEL_3_${currentUser.uid}", batchIndex).apply()

                val updateMap = hashMapOf("level3_batch" to batchIndex)
                db.collection("student_progress").document(currentUser.uid).set(updateMap, SetOptions.merge())
            }

            val intent = Intent(this, SpeechLevel3TaskActivity::class.java)
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
            intent.putExtra("LEVEL_TYPE", 3) // 3 for Sentences
            startActivity(intent)
            finish()
        }
    }

    private fun saveAndCalculateSentenceProgress(userId: String, batchIndex: Int, score: Int) {
        val batchKey = "batch_$batchIndex"
        val batchData = hashMapOf(batchKey to score)

        // 1. Reference to Batch Scores
        val scoresRef = db.collection("student_speech_progress")
            .document(userId)
            .collection("level_3_sentences")
            .document("batch_scores")

        // 2. Write Batch Score
        scoresRef.set(batchData, SetOptions.merge())
            .addOnSuccessListener {

                // 3. Read ALL scores to calculate average
                scoresRef.get().addOnSuccessListener { document ->
                    if (document.exists() && document.data != null) {
                        var totalScore = 0
                        var batchCount = 0

                        for ((key, value) in document.data!!) {
                            if (key.startsWith("batch_")) {
                                val batchScore = (value as? Long)?.toInt() ?: 0
                                totalScore += batchScore
                                batchCount++
                            }
                        }

                        if (batchCount > 0) {
                            val overallAverage = totalScore / batchCount
                            val summaryData = hashMapOf("overall_sentence_progress" to overallAverage)

                            // 4. Write Overall Average (Same root collection)
                            db.collection("student_speech_progress")
                                .document(userId)
                                .set(summaryData, SetOptions.merge())
                                .addOnSuccessListener {
                                    // Success
                                }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save progress", Toast.LENGTH_SHORT).show()
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