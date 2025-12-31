package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class RMScoreboardActivity : AppCompatActivity() {

    private lateinit var scoreValue: TextView
    private lateinit var scoreLabel: TextView
    private lateinit var youWinText: TextView
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

        // Initialize views
        initializeViews()

        // Get data from intent
        score = intent.getIntExtra("SCORE", 0)
        totalRounds = intent.getIntExtra("TOTAL_ROUNDS", 5)
        songTitle = intent.getStringExtra("SONG_TITLE") ?: ""

        // Set score text - format as "score/totalRounds"
        scoreValue.text = "$score/$totalRounds"
        scoreLabel.text = "Rhythm Score" // This is already set in XML, but keeping for reference

        // Set star rating based on score
        setStarRating(score)

        // Setup animations
        setupAnimations()

        // Setup button click listeners
        setupButtonListeners()
    }

    private fun initializeViews() {
        scoreValue = findViewById(R.id.scoreValue)
        scoreLabel = findViewById(R.id.scoreLabel)
        youWinText = findViewById(R.id.youWinText)
        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        sparklesContainer = findViewById(R.id.sparklesContainer)
        musicNotesContainer = findViewById(R.id.musicNotesContainer)
        replayButton = findViewById(R.id.replayButton)
        dashboardButton = findViewById(R.id.dashboardButton)
        ribbonImage = findViewById(R.id.ribbonImage)
    }

    private fun setStarRating(score: Int) {
        val stars = when (score) {
            0 -> 0
            in 1..2 -> 1
            in 3..4 -> 2
            else -> 3
        }

        // Update star images
        if (stars >= 1) star1.setImageResource(R.drawable.star_filled)
        if (stars >= 2) star2.setImageResource(R.drawable.star_filled)
        if (stars == 3) star3.setImageResource(R.drawable.star_filled)

        // Animate stars
        animateStars(stars)
    }

    private fun animateStars(numStars: Int) {
        val stars = listOf(star1, star2, star3)

        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0 until numStars) {
                Handler(Looper.getMainLooper()).postDelayed({
                    // Scale animation
                    val scaleAnim = ObjectAnimator.ofFloat(stars[i], "scaleX", 0.5f, 1.2f, 1f)
                    scaleAnim.duration = 500
                    scaleAnim.interpolator = AccelerateDecelerateInterpolator()

                    val scaleYAnim = ObjectAnimator.ofFloat(stars[i], "scaleY", 0.5f, 1.2f, 1f)
                    scaleYAnim.duration = 500
                    scaleYAnim.interpolator = AccelerateDecelerateInterpolator()

                    // Change to glow star temporarily if you have it
                    try {
                        stars[i].setImageResource(R.drawable.star_filled_glow)
                    } catch (e: Exception) {
                        // If glow star doesn't exist, just use regular filled star
                        stars[i].setImageResource(R.drawable.star_filled)
                    }

                    scaleAnim.start()
                    scaleYAnim.start()

                    // Revert to filled star after animation
                    Handler(Looper.getMainLooper()).postDelayed({
                        stars[i].setImageResource(R.drawable.star_filled)
                    }, 500)

                    // Add sparkle effect
                    addSparkleAtStar(stars[i])

                }, i * 300L) // Stagger the star animations
            }
        }, 500) // Delay before starting star animations
    }

    private fun setupAnimations() {
        // Animate ribbon (instead of panda)
        animateRibbon()

        // Create sparkles
        createSparkles()

        // Create music notes
        createMusicNotes()

        // Animate "You Win" text
        animateYouWinText()
    }

    private fun animateRibbon() {
        // Bounce animation for ribbon
        val bounceAnim = ObjectAnimator.ofFloat(ribbonImage, "translationY", 0f, -10f, 0f)
        bounceAnim.duration = 1500
        bounceAnim.repeatCount = ObjectAnimator.INFINITE
        bounceAnim.repeatMode = ObjectAnimator.REVERSE
        bounceAnim.start()

        // Slight rotation animation for "You Win" text
        val rotateAnim = RotateAnimation(-3f, 3f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f)
        rotateAnim.duration = 2000
        rotateAnim.repeatCount = Animation.INFINITE
        rotateAnim.repeatMode = Animation.REVERSE
        youWinText.startAnimation(rotateAnim)
    }

    private fun animateYouWinText() {
        // Pulse animation for "You Win" text
        val pulseAnim = ObjectAnimator.ofFloat(youWinText, "scaleX", 1f, 1.1f, 1f)
        pulseAnim.duration = 1200
        pulseAnim.repeatCount = ObjectAnimator.INFINITE
        pulseAnim.repeatMode = ObjectAnimator.REVERSE
        pulseAnim.start()

        val pulseYAnim = ObjectAnimator.ofFloat(youWinText, "scaleY", 1f, 1.1f, 1f)
        pulseYAnim.duration = 1200
        pulseYAnim.repeatCount = ObjectAnimator.INFINITE
        pulseYAnim.repeatMode = ObjectAnimator.REVERSE
        pulseYAnim.start()
    }

    private fun createSparkles() {
        // Create multiple sparkles
        for (i in 0..15) {
            Handler(Looper.getMainLooper()).postDelayed({
                addRandomSparkle()
            }, i * 200L)
        }
    }

    private fun addRandomSparkle() {
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(40, 40)

        // Random position
        val layoutParams = sparkle.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, sparklesContainer.width - 40)
        layoutParams.topMargin = Random.nextInt(0, sparklesContainer.height - 40)

        sparklesContainer.addView(sparkle)

        // Twinkle animation
        animateSparkle(sparkle)
    }

    private fun addSparkleAtStar(star: ImageView) {
        val sparkle = ImageView(this)
        sparkle.setImageResource(R.drawable.sparkle_yellow)
        sparkle.layoutParams = FrameLayout.LayoutParams(30, 30)

        // Position near the star
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

        // Twinkle animation
        animateSparkle(sparkle)
    }

    private fun animateSparkle(sparkle: ImageView) {
        // Fade in and out animation
        val fadeAnim = ObjectAnimator.ofFloat(sparkle, "alpha", 0f, 1f, 0f)
        fadeAnim.duration = 1000
        fadeAnim.repeatCount = 3
        fadeAnim.repeatMode = ObjectAnimator.RESTART

        // Scale animation
        val scaleAnim = ObjectAnimator.ofFloat(sparkle, "scaleX", 0.5f, 1.5f, 0.5f)
        scaleAnim.duration = 1000
        scaleAnim.repeatCount = 3

        val scaleYAnim = ObjectAnimator.ofFloat(sparkle, "scaleY", 0.5f, 1.5f, 0.5f)
        scaleYAnim.duration = 1000
        scaleYAnim.repeatCount = 3

        fadeAnim.start()
        scaleAnim.start()
        scaleYAnim.start()

        // Remove sparkle after animation
        Handler(Looper.getMainLooper()).postDelayed({
            sparklesContainer.removeView(sparkle)
        }, 3000)
    }

    private fun createMusicNotes() {
        val musicNoteRes = listOf(
            R.drawable.m_one, R.drawable.m_two, R.drawable.m_three,
            R.drawable.m_four, R.drawable.m_five, R.drawable.m_six
        )

        // Create 6 music notes
        for (i in 0..5) {
            Handler(Looper.getMainLooper()).postDelayed({
                addMusicNote(musicNoteRes[i])
            }, i * 300L)
        }
    }

    private fun addMusicNote(noteRes: Int) {
        val note = ImageView(this)
        note.setImageResource(noteRes)
        note.layoutParams = FrameLayout.LayoutParams(60, 60)

        // Random starting position
        val layoutParams = note.layoutParams as FrameLayout.LayoutParams
        layoutParams.leftMargin = Random.nextInt(0, musicNotesContainer.width - 60)
        layoutParams.topMargin = Random.nextInt(0, musicNotesContainer.height - 60)

        musicNotesContainer.addView(note)

        // Dancing/shaking animation
        animateMusicNote(note)
    }

    private fun animateMusicNote(note: ImageView) {
        // Floating animation
        val floatAnim = ObjectAnimator.ofFloat(note, "translationY", 0f, -Random.nextInt(100, 300).toFloat())
        floatAnim.duration = 3000

        // Breathing/scale animation
        val breathAnim = ObjectAnimator.ofFloat(note, "scaleX", 0.8f, 1.2f, 0.8f)
        breathAnim.duration = 1000
        breathAnim.repeatCount = ValueAnimator.INFINITE
        breathAnim.repeatMode = ValueAnimator.REVERSE

        val breathYAnim = ObjectAnimator.ofFloat(note, "scaleY", 0.8f, 1.2f, 0.8f)
        breathYAnim.duration = 1000
        breathYAnim.repeatCount = ValueAnimator.INFINITE
        breathYAnim.repeatMode = ValueAnimator.REVERSE

        // Shaking/rotation animation
        val shakeAnim = ObjectAnimator.ofFloat(note, "rotation", -15f, 15f, -15f)
        shakeAnim.duration = 800
        shakeAnim.repeatCount = ValueAnimator.INFINITE
        shakeAnim.repeatMode = ValueAnimator.REVERSE

        floatAnim.start()
        breathAnim.start()
        breathYAnim.start()
        shakeAnim.start()

        // Remove note after animation
        floatAnim.addUpdateListener {
            if (floatAnim.animatedFraction >= 0.8f) {
                note.alpha = note.alpha - 0.05f
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            musicNotesContainer.removeView(note)
            // Add new note to keep continuous animation
            addMusicNote(listOf(
                R.drawable.m_one, R.drawable.m_two, R.drawable.m_three,
                R.drawable.m_four, R.drawable.m_five, R.drawable.m_six
            ).random())
        }, 3000)
    }

    private fun setupButtonListeners() {
        replayButton.setOnClickListener {
            // Navigate back to rhythm game with same song
            val intent = Intent(this, RhythmSummaryActivity::class.java)
            intent.putExtra("SONG_TITLE", songTitle)
            startActivity(intent)
            finish()
        }

        dashboardButton.setOnClickListener {
            // Navigate to dashboard activity
            val intent = Intent(this, GameDashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart animations if needed
    }

    override fun onPause() {
        super.onPause()
        // Clean up animations if needed
    }
}