package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class RMScoreboardActivity : AppCompatActivity() {

    private lateinit var scoreValue: TextView
    private lateinit var scoreLabel: TextView
    private lateinit var resultText: TextView
    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var sparklesContainer: FrameLayout
    private lateinit var musicNotesContainer: FrameLayout
    private lateinit var replayButton: Button
    private lateinit var dashboardButton: Button
    private lateinit var ribbonImage: ImageView

    private var score = 0
    private var totalRounds = 0
    private var songTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rmscoreboard)

        initializeViews()

        score = intent.getIntExtra("SCORE", 0)
        totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        songTitle = intent.getStringExtra("SONG_TITLE") ?: ""

        scoreValue.text = "$score/$totalRounds"
        scoreLabel.text = "Rhythm Score"

        setResultText(score)
        setStarRating(score)
        setupAnimations(score)
        setupButtonListeners()
    }

    private fun initializeViews() {
        scoreValue = findViewById(R.id.scoreValue)
        scoreLabel = findViewById(R.id.scoreLabel)
        resultText = findViewById(R.id.youWinText)
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        sparklesContainer = findViewById(R.id.sparklesContainer)
        musicNotesContainer = findViewById(R.id.musicNotesContainer)
        replayButton = findViewById(R.id.replayButton)
        dashboardButton = findViewById(R.id.dashboardButton)
        ribbonImage = findViewById(R.id.ribbonImage)
    }

    private fun setResultText(score: Int) {
        when (score) {
            0 -> {
                resultText.text = "You Lose"
                resultText.setTextColor(Color.parseColor("#FFFFFF"))
            }
            1, 2 -> {
                resultText.text = "Nice Try!"
                resultText.setTextColor(Color.parseColor("#FFFFFF"))
            }
            3, 4 -> {
                resultText.text = "Well Done!"
                resultText.setTextColor(Color.parseColor("#FFFFFF"))
            }
            else -> {
                resultText.text = "You Win!"
                resultText.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }
    }

    private fun setStarRating(score: Int) {
        val stars = when (score) {
            0 -> 0
            1, 2 -> 1
            3, 4 -> 2
            else -> 3
        }

        if (stars >= 1) star1.setImageResource(R.drawable.star_filled)
        if (stars >= 2) star2.setImageResource(R.drawable.star_filled)
        if (stars == 3) star3.setImageResource(R.drawable.star_filled)

        animateStars(stars)
    }

    private fun animateStars(numStars: Int) {
        val stars = listOf(star1, star2, star3)

        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0 until numStars) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val scaleAnim = ObjectAnimator.ofFloat(stars[i], "scaleX", 0.5f, 1.2f, 1f)
                    scaleAnim.duration = 500
                    scaleAnim.interpolator = AccelerateDecelerateInterpolator()

                    val scaleYAnim = ObjectAnimator.ofFloat(stars[i], "scaleY", 0.5f, 1.2f, 1f)
                    scaleYAnim.duration = 500
                    scaleYAnim.interpolator = AccelerateDecelerateInterpolator()

                    try {
                        stars[i].setImageResource(R.drawable.star_filled_glow)
                    } catch (e: Exception) {
                        stars[i].setImageResource(R.drawable.star_filled)
                    }

                    scaleAnim.start()
                    scaleYAnim.start()

                    Handler(Looper.getMainLooper()).postDelayed({
                        stars[i].setImageResource(R.drawable.star_filled)
                    }, 500)

                    if (numStars >= 2) {
                        addSparkleAtStar(stars[i])
                    }

                }, i * 300L)
            }
        }, 500)
    }

    private fun setupAnimations(score: Int) {
        animateRibbon(score)

        when (score) {
            0 -> {
                animateResultText(false)
            }
            1, 2 -> {
                createSparkles(3)
                createMusicNotes(2)
                animateResultText(true)
            }
            3, 4 -> {
                createSparkles(8)
                createMusicNotes(4)
                animateResultText(true)
            }
            else -> {
                createSparkles(15)
                createMusicNotes(6)
                animateResultText(true)
            }
        }
    }

    private fun animateRibbon(score: Int) {
        if (score == 0) {
            // No bounce animation for lose
            val fadeAnim = ObjectAnimator.ofFloat(ribbonImage, "alpha", 0.7f, 0.5f, 0.7f)
            fadeAnim.duration = 2000
            fadeAnim.repeatCount = ObjectAnimator.INFINITE
            fadeAnim.repeatMode = ObjectAnimator.REVERSE
            fadeAnim.start()
        } else {
            // Bounce animation for scores > 0
            val bounceAnim = ObjectAnimator.ofFloat(ribbonImage, "translationY", 0f, -10f, 0f)
            bounceAnim.duration = 1500
            bounceAnim.repeatCount = ObjectAnimator.INFINITE
            bounceAnim.repeatMode = ObjectAnimator.REVERSE
            bounceAnim.start()
        }
    }

    private fun animateResultText(shouldPulse: Boolean) {
        if (shouldPulse) {
            val pulseAnim = ObjectAnimator.ofFloat(resultText, "scaleX", 1f, 1.1f, 1f)
            pulseAnim.duration = 1200
            pulseAnim.repeatCount = ObjectAnimator.INFINITE
            pulseAnim.repeatMode = ObjectAnimator.REVERSE
            pulseAnim.start()

            val pulseYAnim = ObjectAnimator.ofFloat(resultText, "scaleY", 1f, 1.1f, 1f)
            pulseYAnim.duration = 1200
            pulseYAnim.repeatCount = ObjectAnimator.INFINITE
            pulseYAnim.repeatMode = ObjectAnimator.REVERSE
            pulseYAnim.start()
        } else {
            val fadeAnim = ObjectAnimator.ofFloat(resultText, "alpha", 0.8f, 1f, 0.8f)
            fadeAnim.duration = 1500
            fadeAnim.repeatCount = ObjectAnimator.INFINITE
            fadeAnim.repeatMode = ObjectAnimator.REVERSE
            fadeAnim.start()
        }
    }

    private fun createSparkles(count: Int) {
        for (i in 0 until count) {
            Handler(Looper.getMainLooper()).postDelayed({
                addRandomSparkle()
            }, i * 200L)
        }
    }

    private fun addRandomSparkle() {
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(40, 40)

        val layoutParams = sparkle.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, sparklesContainer.width - 40)
        layoutParams.topMargin = Random.nextInt(0, sparklesContainer.height - 40)

        sparklesContainer.addView(sparkle)
        animateSparkle(sparkle)
    }

    private fun addSparkleAtStar(star: ImageView) {
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(30, 30)

        val location = IntArray(2)
        star.getLocationOnScreen(location)
        val containerLocation = IntArray(2)
        sparklesContainer.getLocationOnScreen(containerLocation)

        val x = location[0] - containerLocation[0] + star.width / 2 - 15
        val y = location[1] - containerLocation[1] + star.height / 2 - 15

        val layoutParams = sparkle.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = x
        layoutParams.topMargin = y

        sparklesContainer.addView(sparkle)
        animateSparkle(sparkle)
    }

    private fun animateSparkle(sparkle: ImageView) {
        val fadeAnim = ObjectAnimator.ofFloat(sparkle, "alpha", 0f, 1f, 0f)
        fadeAnim.duration = 1000
        fadeAnim.repeatCount = 3
        fadeAnim.repeatMode = ObjectAnimator.RESTART

        val scaleAnim = ObjectAnimator.ofFloat(sparkle, "scaleX", 0.5f, 1.5f, 0.5f)
        scaleAnim.duration = 1000
        scaleAnim.repeatCount = 3

        val scaleYAnim = ObjectAnimator.ofFloat(sparkle, "scaleY", 0.5f, 1.5f, 0.5f)
        scaleYAnim.duration = 1000
        scaleYAnim.repeatCount = 3

        fadeAnim.start()
        scaleAnim.start()
        scaleYAnim.start()

        Handler(Looper.getMainLooper()).postDelayed({
            sparklesContainer.removeView(sparkle)
        }, 3000)
    }

    private fun createMusicNotes(count: Int) {
        val musicNoteRes = listOf(
            R.drawable.m_one, R.drawable.m_two, R.drawable.m_three,
            R.drawable.m_four, R.drawable.m_five, R.drawable.m_six
        )

        for (i in 0 until count) {
            Handler(Looper.getMainLooper()).postDelayed({
                addMusicNote(musicNoteRes[i % musicNoteRes.size])
            }, i * 300L)
        }
    }

    private fun addMusicNote(noteRes: Int) {
        val note = ImageView(this)
        note.setImageResource(noteRes)
        note.layoutParams = FrameLayout.LayoutParams(60, 60)

        val layoutParams = note.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, musicNotesContainer.width - 60)
        layoutParams.topMargin = Random.nextInt(0, musicNotesContainer.height - 60)

        musicNotesContainer.addView(note)
        animateMusicNote(note)
    }

    private fun animateMusicNote(note: ImageView) {
        val floatAnim = ObjectAnimator.ofFloat(note, "translationY", 0f, -Random.nextInt(100, 300).toFloat())
        floatAnim.duration = 3000

        val breathAnim = ObjectAnimator.ofFloat(note, "scaleX", 0.8f, 1.2f, 0.8f)
        breathAnim.duration = 1000
        breathAnim.repeatCount = ValueAnimator.INFINITE
        breathAnim.repeatMode = ValueAnimator.REVERSE

        val breathYAnim = ObjectAnimator.ofFloat(note, "scaleY", 0.8f, 1.2f, 0.8f)
        breathYAnim.duration = 1000
        breathYAnim.repeatCount = ValueAnimator.INFINITE
        breathYAnim.repeatMode = ValueAnimator.REVERSE

        val shakeAnim = ObjectAnimator.ofFloat(note, "rotation", -15f, 15f, -15f)
        shakeAnim.duration = 800
        shakeAnim.repeatCount = ValueAnimator.INFINITE
        shakeAnim.repeatMode = ValueAnimator.REVERSE

        floatAnim.start()
        breathAnim.start()
        breathYAnim.start()
        shakeAnim.start()

        floatAnim.addUpdateListener {
            if (floatAnim.animatedFraction >= 0.8f) {
                note.alpha = note.alpha - 0.05f
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            musicNotesContainer.removeView(note)
            if (score >= 1) {
                addMusicNote(listOf(
                    R.drawable.m_one, R.drawable.m_two, R.drawable.m_three,
                    R.drawable.m_four, R.drawable.m_five, R.drawable.m_six
                ).random())
            }
        }, 3000)
    }

    private fun setupButtonListeners() {
        replayButton.setOnClickListener {
            val intent = Intent(this, RhythmSummaryActivity::class.java)
            intent.putExtra("SONG_TITLE", songTitle)
            startActivity(intent)
            finish()
        }

        dashboardButton.setOnClickListener {
            val intent = Intent(this, GameDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}