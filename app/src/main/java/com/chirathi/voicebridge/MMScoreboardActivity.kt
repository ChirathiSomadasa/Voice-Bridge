package com.chirathi.voicebridge

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class MMScoreboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Views
    private lateinit var btnPlayAgain: Button
    private lateinit var btnDashboard: Button
    private lateinit var btnUnlockGift: CardView
    private lateinit var giftBoxIcon: ImageView
    private lateinit var titleWon: TextView
    private lateinit var performanceText: TextView
    private lateinit var starLeft: ImageView
    private lateinit var starMiddle: ImageView
    private lateinit var starRight: ImageView

    // Data
    private var correctAnswers = 0
    private var motivationId = 0
    private var unlockGift = false
    private var alphaScore = 0f
    private var avgResponseTime = 0f
    private var avgTaps = 0

    // Tools
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mmscoreboard)

        // Debug logging
        val receivedCorrect = intent.getIntExtra("CORRECT_ANSWERS", -1)
        val receivedMotivation = intent.getIntExtra("MOTIVATION_ID", -1)
        Log.e("MMScoreboard", "DEBUG DATA: Correct=$receivedCorrect, Motivation=$receivedMotivation")

        // 1. Get Data Passed from Game
        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        motivationId = intent.getIntExtra("MOTIVATION_ID", 0)
        unlockGift = intent.getBooleanExtra("UNLOCK_GIFT", false)

        // Load performance metrics from Intent
        alphaScore = intent.getFloatExtra("AVG_ALPHA", 0.0f)
        avgResponseTime = intent.getFloatExtra("AVG_RESPONSE_TIME", 0.0f)
        avgTaps = intent.getIntExtra("AVG_TAPS", 0)

        Log.e("MMScoreboard", "Performance Metrics - Alpha: $alphaScore, " +
                "Response Time: $avgResponseTime, Taps: $avgTaps")

        // 2. Init Tools
        tts = TextToSpeech(this, this)
        initViews()

        // 3. EXECUTE LOGIC with PERFORMANCE metrics
        setupUIWithPerformance()

        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initViews() {
        titleWon = findViewById(R.id.titleWon)
        performanceText = findViewById(R.id.performanceText)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnDashboard = findViewById(R.id.btnDashboard)
        btnUnlockGift = findViewById(R.id.btnUnlockGift)
        giftBoxIcon = findViewById(R.id.giftBoxIcon)
        starLeft = findViewById(R.id.starLeft)
        starMiddle = findViewById(R.id.starMiddle)
        starRight = findViewById(R.id.starRight)
    }

    private fun calculateStarsFromPerformance(alpha: Float, responseTime: Float, taps: Int): Int {
        // Higher alpha = better performance
        // Lower response time = faster/better
        // Lower taps = less frustration

        var starScore = 0

        // Alpha-based scoring (achievement index)
        when {
            alpha > 8.0f -> starScore += 3  // Excellent performance
            alpha > 5.0f -> starScore += 2  // Good performance
            alpha > 2.0f -> starScore += 1  // Okay performance
        }

        // Response time scoring (faster is better)
        when {
            responseTime < 1500 -> starScore += 2  // Very fast
            responseTime < 2500 -> starScore += 1  // Average speed
            // > 2500 gets no bonus
        }

        // Frustration scoring (fewer taps is better)
        when {
            taps <= 2 -> starScore += 2    // Very calm
            taps <= 4 -> starScore += 1    // Somewhat calm
            // > 4 taps gets no bonus
        }

        // Convert to 1-3 star scale
        return when {
            starScore >= 5 -> 3  // 3 stars for excellent performance
            starScore >= 3 -> 2  // 2 stars for good performance
            starScore >= 1 -> 1  // 1 star for basic performance
            else -> 0            // 0 stars for poor performance
        }
    }

    private fun setupUIWithPerformance() {
        // A. Calculate stars based on PERFORMANCE metrics
        val performanceStars = calculateStarsFromPerformance(alphaScore, avgResponseTime, avgTaps)

        // B. Set Emotive Title based on PERFORMANCE
        val (mainTitle, colorRes) = getPerformanceResult(alphaScore)
        titleWon.text = mainTitle
        titleWon.setTextColor(resources.getColor(colorRes, theme))

        // C. Use performance stars
        animateStars(performanceStars)

        // D. EXECUTE MOTIVATION STRATEGY (The AI Decision)
        Handler(Looper.getMainLooper()).postDelayed({
            when (motivationId) {
                0 -> runStrategyStars(performanceStars)
                1 -> runStrategyPerformanceQuote()
                2 -> runStrategyAnimation()
                else -> runStrategyStars(performanceStars)
            }
        }, 500)

        // E. Gift Logic (Model Driven)
        if (unlockGift) {
            btnUnlockGift.visibility = View.VISIBLE
            btnUnlockGift.alpha = 0f
            btnUnlockGift.animate().alpha(1f).setDuration(1000).start()
            startGiftPulse()
        } else {
            btnUnlockGift.visibility = View.GONE
        }
    }

    private fun getPerformanceResult(alpha: Float): Pair<String, Int> {
        return when {
            alpha > 8.0f -> Pair("Outstanding! ⭐⭐⭐", R.color.gold)
            alpha > 5.0f -> Pair("Great Job! ⭐⭐", R.color.light_green)
            alpha > 2.0f -> Pair("Good Effort! ⭐", R.color.light_blue)
            else -> Pair("Keep Practicing!", R.color.dark_orange)
        }
    }

    // --- UPDATED STRATEGIES FOR PERFORMANCE ---

    // --- STRATEGY 0: STARS (Calm) ---
    private fun runStrategyStars(starCount: Int) {
        val text = "You earned $starCount stars!"
        performanceText.text = text
        speakText(text)
    }

    // --- STRATEGY 1: QUOTE (Social) - UPDATED FOR PERFORMANCE ---
    private fun runStrategyPerformanceQuote() {
        val quote = getPerformanceQuote(alphaScore, avgResponseTime, avgTaps)
        performanceText.text = quote
        speakText(quote)
    }

    // --- STRATEGY 2: ANIMATION (High Energy) ---
    private fun runStrategyAnimation() {
        performanceText.text = "Hooray! You did it!"
        animateTitleExcitement() // Bouncing Title
        playCelebrationSound()   // Sound Effect
        speakText("Hooray! You did it!")
    }

    private fun getPerformanceQuote(alpha: Float, responseTime: Float, taps: Int): String {
        val feedback = StringBuilder()

        // Alpha feedback (achievement index)
        feedback.append(when {
            alpha > 8.0f -> "You are a Super Star!"
            alpha > 5.0f -> "You did it! Hooray! "
            alpha > 2.0f -> "You are doing great! "
            else -> "Nice try! "
        })

        // Response time feedback
        feedback.append(when {
            responseTime < 1500 -> "You're so quick! "
            responseTime < 2500 -> "Good speed! "
            else -> "Take your time next round! "
        })

        // Tap frustration feedback
        feedback.append(when {
            taps <= 2 -> "Perfect control!"
            taps <= 4 -> "Staying focused!"
            else -> "Try gentle taps next time!"
        })

        return feedback.toString()
    }

    private fun animateStars(starsToShow: Int) {
        val stars = listOf(starLeft, starMiddle, starRight)
        stars.forEachIndexed { index, star ->
            if (index < starsToShow) {
                star.setImageResource(R.drawable.star_filled)
                val anim = ScaleAnimation(0f, 1f, 0f, 1f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
                anim.duration = 600
                anim.startOffset = index * 300L
                anim.interpolator = BounceInterpolator()
                star.startAnimation(anim)
            } else {
                star.setImageResource(R.drawable.star_outline)
                star.alpha = 0.3f
            }
        }
    }

    private fun animateTitleExcitement() {
        val anim = ObjectAnimator.ofFloat(titleWon, "translationY", 0f, -20f, 0f)
        anim.duration = 500
        anim.repeatCount = 3
        anim.start()
    }

    private fun startGiftPulse() {
        val anim = ObjectAnimator.ofFloat(giftBoxIcon, "scaleX", 1f, 1.1f, 1f)
        anim.repeatCount = ValueAnimator.INFINITE
        anim.repeatMode = ValueAnimator.REVERSE
        anim.duration = 1000
        anim.start()
    }

    private fun playCelebrationSound() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.correct_sound)
            mediaPlayer?.start()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupButtons() {
        btnPlayAgain.setOnClickListener {
            val intent = Intent(this, MoodMatchSevenDownActivity::class.java)

            // Clear any flags from previous intent
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish() // Close scoreboard
        }
        btnDashboard.setOnClickListener {
            try {
                val intent = Intent(this, GameDashboardActivity::class.java)
                // Clear all activities and start fresh at dashboard
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e("MMScoreboard", "Error navigating to GameDashboardActivity: ${e.message}")
                // Fallback: Just close the app gracefully
                finishAffinity()
            }
        }
        btnUnlockGift.setOnClickListener {
            val intent = Intent(this, AllCorrectGrandPrizeActivity::class.java)
            startActivityForResult(intent, 100)
        }
    }

    // TTS
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.6f)
            tts.setSpeechRate(0.9f)
            isTtsReady = true
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            // Remove emojis for TTS
            val clean = text.replace(Regex("[^a-zA-Z0-9 ]"), "")
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        mediaPlayer?.release()
        super.onDestroy()
    }
}