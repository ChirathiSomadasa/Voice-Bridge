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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.min

class LetterProgressActivity : AppCompatActivity() {

    private lateinit var customDrawable: RoundedCircularProgressDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_letter_progress)

        // 1. GET DATA
        val score = intent.getIntExtra("PROGRESS_SCORE", 0)
        val batchIndex = intent.getIntExtra("BATCH_INDEX", 0)
        val canContinue = intent.getBooleanExtra("CAN_CONTINUE", false)
        val isPassed = score >= 75

        // 2. UI REFERENCES
        val tvScore = findViewById<TextView>(R.id.tvScore)
        val circleProgress = findViewById<ProgressBar>(R.id.circleProgress)
        val llTryAgain = findViewById<LinearLayout>(R.id.llTryAgain)
        val llHome = findViewById<LinearLayout>(R.id.llHome)
        val llContinue = findViewById<LinearLayout>(R.id.llContinue)

        // 3. COLOR LOGIC
        val colorResId = when {
            score >= 75 -> R.color.score_green
            score >= 50 -> R.color.score_orange
            else -> R.color.score_red
        }
        val color = ContextCompat.getColor(this, colorResId)

        // Set Text Color
        tvScore.setTextColor(color)

        // 4. SETUP CUSTOM DRAWABLE
        // create the drawable programmatically to get the rounded caps
        customDrawable = RoundedCircularProgressDrawable(
            context = this,
            progressColor = color,
            backgroundColor = Color.parseColor("#E0E0E0"),
            strokeWidthDp = 12f
        )
        circleProgress.progressDrawable = customDrawable

        // 5. ANIMATION
        val animator = ValueAnimator.ofInt(0, score)
        animator.duration = 1500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            tvScore.text = animatedValue.toString()
            circleProgress.progress = animatedValue
        }
        animator.start()

        // 6. BUTTON LAYOUT
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

        // 7. BUTTON ACTIONS
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
            val intent = Intent(this, SpeechLevel1TaskActivity::class.java)
            intent.putExtra("BATCH_INDEX", batchIndex + 1)
            startActivity(intent)
            finish()
        }
    }

    /**
     * CUSTOM DRAWABLE CLASS
     * This draws the ring manually so we can set Paint.Cap.ROUND
     */
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
            // Setup Background Paint (Gray Ring)
            backgroundPaint.color = backgroundColor
            backgroundPaint.style = Paint.Style.STROKE
            backgroundPaint.strokeWidth = strokeWidth

            // Setup Progress Paint (Colored Arc)
            progressPaint.color = progressColor
            progressPaint.style = Paint.Style.STROKE
            progressPaint.strokeWidth = strokeWidth
            progressPaint.strokeCap = Paint.Cap.ROUND
        }

        // ProgressBar calls this to set progress (0 to 10000)
        override fun onLevelChange(level: Int): Boolean {
            currentLevel = level
            invalidateSelf() // Redraw when progress changes
            return true
        }

        override fun draw(canvas: Canvas) {
            // Calculate bounds to fit inside the View, accounting for stroke thickness
            val halfStroke = strokeWidth / 2f
            rectF.set(
                bounds.left + halfStroke,
                bounds.top + halfStroke,
                bounds.right - halfStroke,
                bounds.bottom - halfStroke
            )

            // 1. Draw Full Background Ring
            canvas.drawOval(rectF, backgroundPaint)

            // 2. Draw Progress Arc
            // 10000 is the max level for ProgressBar
            val sweepAngle = (currentLevel / 10000f) * 360f

            // startAngle -90
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }

        override fun setAlpha(alpha: Int) {
            progressPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            progressPaint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}