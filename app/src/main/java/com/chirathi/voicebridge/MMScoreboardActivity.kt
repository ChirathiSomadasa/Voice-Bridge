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

    private lateinit var btnPlayAgain:   Button
    private lateinit var btnDashboard:   Button
    private lateinit var btnUnlockGift:  CardView
    private lateinit var giftBoxIcon:    ImageView
    private lateinit var titleWon:       TextView
    private lateinit var performanceText:TextView
    private lateinit var starLeft:       ImageView
    private lateinit var starMiddle:     ImageView
    private lateinit var starRight:      ImageView

    // Game result data
    private var correctAnswers = 0
    private var totalRounds    = 5
    private var score          = 0
    private var ageGroup       = 6
    private var motivationId   = 0
    private var unlockGift     = false

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mmscoreboard)

        correctAnswers = intent.getIntExtra("CORRECT_ANSWERS", 0)
        totalRounds    = intent.getIntExtra("TOTAL_ROUNDS", 5)
        score          = intent.getIntExtra("SCORE", 0)
        ageGroup       = intent.getIntExtra("AGE_GROUP", 6)
        motivationId   = intent.getIntExtra("MOTIVATION_ID", 0)
        unlockGift     = intent.getBooleanExtra("UNLOCK_GIFT", false)

        Log.d("MMScoreboard", "correct=$correctAnswers total=$totalRounds score=$score age=$ageGroup")

        tts = TextToSpeech(this, this)
        initViews()
        setupUI()
        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }
    }

    private fun initViews() {
        titleWon       = findViewById(R.id.titleWon)
        performanceText= findViewById(R.id.performanceText)
        btnPlayAgain   = findViewById(R.id.btnPlayAgain)
        btnDashboard   = findViewById(R.id.btnDashboard)
        btnUnlockGift  = findViewById(R.id.btnUnlockGift)
        giftBoxIcon    = findViewById(R.id.giftBoxIcon)
        starLeft       = findViewById(R.id.starLeft)
        starMiddle     = findViewById(R.id.starMiddle)
        starRight      = findViewById(R.id.starRight)
    }

    // =========================================================================
    // Stars & text — driven purely by correctAnswers
    // =========================================================================

    /** 5/5 → 3 stars | 3-4/5 → 2 stars | 1-2/5 → 1 star | 0/5 → 0 stars */
    private fun starsFromCorrect(correct: Int, total: Int): Int = when {
        correct == total              -> 3
        correct >= (total * 0.6f)    -> 2
        correct >= 1                 -> 1
        else                         -> 0
    }

    private fun titleFromCorrect(correct: Int, total: Int): Pair<String, Int> = when {
        correct == total           -> Pair("Outstanding!", R.color.gold)
        correct >= (total * 0.6f)  -> Pair("Great Job!",  R.color.light_green)
        correct >= 1               -> Pair("Good Effort!", R.color.light_blue)
        else                       -> Pair("Keep Practicing!", R.color.dark_orange)
    }

    private fun messageFromCorrect(correct: Int, total: Int): String = when {
        correct == total          -> "Amazing! You got all $total right! You're a superstar!"
        correct >= (total * 0.6f) -> "Well done! You got $correct out of $total. Keep it up!"
        correct >= 1              -> "You got $correct right! Practice makes perfect!"
        else                      -> "That's okay! Every try helps you learn. Let's go again!"
    }

    // =========================================================================
    // Setup
    // =========================================================================

    private fun setupUI() {
        val stars = starsFromCorrect(correctAnswers, totalRounds)
        val (title, colorRes) = titleFromCorrect(correctAnswers, totalRounds)
        val message = messageFromCorrect(correctAnswers, totalRounds)

        titleWon.text = title
        titleWon.setTextColor(resources.getColor(colorRes, theme))

        animateStars(stars)

        Handler(Looper.getMainLooper()).postDelayed({
            when (motivationId) {
                2    -> { playCelebrationSound(); animateTitleExcitement() }
                else -> { /* default: just show message */ }
            }
            performanceText.text = message
            speakText(message)
        }, 500)

        // Gift
        if (unlockGift) {
            btnUnlockGift.visibility = View.VISIBLE
            btnUnlockGift.alpha = 0f
            btnUnlockGift.animate().alpha(1f).setDuration(1000).start()
            startGiftPulse()
        } else {
            btnUnlockGift.visibility = View.GONE
        }
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
        anim.duration = 500; anim.repeatCount = 3; anim.start()
    }

    private fun startGiftPulse() {
        val anim = ObjectAnimator.ofFloat(giftBoxIcon, "scaleX", 1f, 1.1f, 1f)
        anim.repeatCount = ValueAnimator.INFINITE
        anim.repeatMode = ValueAnimator.REVERSE
        anim.duration = 1000; anim.start()
    }

    private fun playCelebrationSound() {
        try { mediaPlayer = MediaPlayer.create(this, R.raw.correct_sound); mediaPlayer?.start() }
        catch (e: Exception) { e.printStackTrace() }
    }

    // =========================================================================
    // Buttons
    // =========================================================================

    private fun setupButtons() {
        btnPlayAgain.setOnClickListener {
            // Pass the SAME age group back so the game restores the correct mode
            startActivity(Intent(this, MoodMatchSevenDownActivity::class.java).apply {
                putExtra("AGE_GROUP", ageGroup)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }

        btnDashboard.setOnClickListener {
            try {
                startActivity(Intent(this, GameDashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Log.e("MMScoreboard", "Dashboard nav error: ${e.message}")
                finishAffinity()
            }
        }

        btnUnlockGift.setOnClickListener {
            startActivityForResult(Intent(this, AllCorrectGrandPrizeActivity::class.java), 100)
        }
    }

    // =========================================================================
    // TTS
    // =========================================================================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US); tts.setPitch(1.6f); tts.setSpeechRate(0.9f)
            isTtsReady = true
        }
    }

    private fun speakText(text: String) {
        if (isTtsReady) {
            val clean = text.replace(Regex("[^a-zA-Z0-9 !.,'?]"), "")
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        mediaPlayer?.release()
        super.onDestroy()
    }
}