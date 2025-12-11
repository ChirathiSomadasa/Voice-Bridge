package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MMScoreboardActivity : AppCompatActivity() {

    private lateinit var trophyImage: ImageView
    private lateinit var trophyBg: ImageView
    private lateinit var sparkle1: ImageView
    private lateinit var sparkle2: ImageView
    private lateinit var sparkle3: ImageView
    private lateinit var sparkle4: ImageView
    private lateinit var trophyContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mmscoreboard)

        // Get the data passed from the game activity
        val correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        val totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        val score = intent.getIntExtra("SCORE", 0)

        // Find views
        val scoreText = findViewById<TextView>(R.id.scoreText)
        val totalScoreText = findViewById<TextView>(R.id.totalScoreText)
        val performanceDetails = findViewById<TextView>(R.id.performanceDetails)
        val pandaImage = findViewById<ImageView>(R.id.pandaImage)
        val btnPlayAgain = findViewById<Button>(R.id.btnPlayAgain)
        val btnDashboard = findViewById<Button>(R.id.btnDashboard)

        // Animation views
        trophyImage = findViewById(R.id.trophyImage)
        trophyBg = findViewById(R.id.trophyBg)
        sparkle1 = findViewById(R.id.sparkle1)
        sparkle2 = findViewById(R.id.sparkle2)
        sparkle3 = findViewById(R.id.sparkle3)
        sparkle4 = findViewById(R.id.sparkle4)
        trophyContainer = findViewById(R.id.trophyContainer)

        // Set the score in the format "correct/total"
        scoreText.text = "$correctAnswers/$totalRounds"

        // Set total score
        totalScoreText.text = "Total Points: $score"

        // Calculate percentage and update UI
        val percentage = (correctAnswers.toFloat() / totalRounds) * 100
        updateUI(percentage, correctAnswers, totalRounds, trophyBg, pandaImage, performanceDetails)

        // Start animations with a delay
        Handler(Looper.getMainLooper()).postDelayed({
            startTrophyAnimations()
        }, 500)

        // Play Again button
        btnPlayAgain.setOnClickListener {
            val intent = Intent(this, MoodMatchSevenUpActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Dashboard button
        btnDashboard.setOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUI(
        percentage: Float,
        correctAnswers: Int,
        totalRounds: Int,
        trophyBg: ImageView,
        pandaImage: ImageView,
        performanceDetails: TextView
    ) {
        val trophyDrawable = when {
            percentage >= 80 -> {
                // Gold trophy
                trophyBg.setImageResource(R.drawable.circle_gold)
                pandaImage.setImageResource(R.drawable.panda)
                R.drawable.trophy
            }
            percentage >= 60 -> {
                // Silver trophy
                trophyBg.setImageResource(R.drawable.circle_silver)
                pandaImage.setImageResource(R.drawable.panda)
                R.drawable.trophy
            }
            percentage >= 40 -> {
                // Bronze trophy
                trophyBg.setImageResource(R.drawable.circle_bronze)
                pandaImage.setImageResource(R.drawable.panda)
                R.drawable.trophy
            }
            else -> {
                // Default trophy
                trophyBg.setImageResource(R.drawable.circle_blue)
                pandaImage.setImageResource(R.drawable.panda)
                R.drawable.trophy
            }
        }

        trophyImage.setImageResource(trophyDrawable)

        // Set performance message
        performanceDetails.text = when {
            percentage >= 80 -> "Excellent! You got ${percentage.toInt()}%\nEmotion Master! 🏆"
            percentage >= 60 -> "Great! You got ${percentage.toInt()}%\nWell done! 👍"
            percentage >= 40 -> "Good! You got ${percentage.toInt()}%\nKeep it up! 💪"
            else -> "You got ${percentage.toInt()}%\nPractice more to improve! 📚"
        }
    }

    private fun startTrophyAnimations() {
        // 1. Trophy bounce animation
        val bounceAnimation = ScaleAnimation(
            0.5f, 1.2f, 0.5f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = 0
            repeatMode = Animation.REVERSE
        }

        trophyImage.startAnimation(bounceAnimation)

        // 2. Continuous rotation for trophy background
        val rotateAnimation = android.view.animation.RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 3000
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
        }

        trophyBg.startAnimation(rotateAnimation)

        // 3. Sparkle animations with delays
        Handler(Looper.getMainLooper()).postDelayed({
            animateSparkle(sparkle1)
        }, 200)

        Handler(Looper.getMainLooper()).postDelayed({
            animateSparkle(sparkle2)
        }, 400)

        Handler(Looper.getMainLooper()).postDelayed({
            animateSparkle(sparkle3)
        }, 600)

        Handler(Looper.getMainLooper()).postDelayed({
            animateSparkle(sparkle4)
        }, 800)

        // 4. Pulse animation for trophy
        val pulseAnimation = ScaleAnimation(
            1f, 1.1f, 1f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            startOffset = 1000
        }

        trophyImage.startAnimation(pulseAnimation)

        // 5. Panda fade in animation
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeInAnimation.duration = 1000

        val pandaImage = findViewById<ImageView>(R.id.pandaImage)
        pandaImage.startAnimation(fadeInAnimation)
    }

    private fun animateSparkle(sparkle: ImageView) {
        sparkle.visibility = View.VISIBLE

        val sparkleAnimation = ScaleAnimation(
            0f, 1.2f, 0f, 1.2f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 500
            repeatCount = 1
            repeatMode = Animation.REVERSE
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    sparkle.visibility = View.INVISIBLE
                    // Repeat sparkle animation after delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isFinishing.not()) {
                            animateSparkle(sparkle)
                        }
                    }, 2000)
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }

        sparkle.startAnimation(sparkleAnimation)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear animations to prevent memory leaks
        trophyImage.clearAnimation()
        trophyBg.clearAnimation()
        sparkle1.clearAnimation()
        sparkle2.clearAnimation()
        sparkle3.clearAnimation()
        sparkle4.clearAnimation()
    }
}