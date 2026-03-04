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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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

    // AI feedback
    private var pendingSpeak: String? = null
    private var friendAction   = 0

    companion object { private const val TAG = "MMScoreboard" }

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
        friendAction = intent.getIntExtra("FRIEND_ACTION", 0)

        Log.d(TAG, "correct=$correctAnswers total=$totalRounds ageGroup=$ageGroup")

        tts = TextToSpeech(this, this)
        initViews()
        setupButtons()

        // ── Trigger AI feedback then build the rest of the UI ─────────────────
        setupUIWithAIFeedback()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }
    }

    private fun initViews() {
        titleWon        = findViewById(R.id.titleWon)
        performanceText = findViewById(R.id.performanceText)
        btnPlayAgain    = findViewById(R.id.btnPlayAgain)
        btnDashboard    = findViewById(R.id.btnDashboard)
        btnUnlockGift   = findViewById(R.id.btnUnlockGift)
        giftBoxIcon     = findViewById(R.id.giftBoxIcon)
        starLeft        = findViewById(R.id.starLeft)
        starMiddle      = findViewById(R.id.starMiddle)
        starRight       = findViewById(R.id.starRight)
    }

    // =========================================================================
    // AI-driven UI setup
    // =========================================================================

    /**
     * Stars are kept (they're symbolic shapes, not numbers) but the text
     * surfaces — titleWon and performanceText — are now AI-generated.
     *
     * No score, ratio, or count ever appears on screen.
     */
    private fun setupUIWithAIFeedback() {
        // Show stars immediately (visual, not numeric)
        val stars = starsFromCorrect(correctAnswers, totalRounds)
        animateStars(stars)

        // Celebration sound for high performance
        if (stars == 3) { playCelebrationSound(); animateTitleExcitement() }

        // Gift visibility
        if (unlockGift) {
            btnUnlockGift.visibility = View.VISIBLE
            btnUnlockGift.alpha = 0f
            btnUnlockGift.animate().alpha(1f).setDuration(1000).start()
            startGiftPulse()
        } else {
            btnUnlockGift.visibility = View.GONE
        }

        // Loading placeholder — no number, just warmth
        titleWon.text       = "Great work today!"
        performanceText.text = "Getting your message…"

        // Classify performance for AI prompt
        val level = TherapeuticFeedbackGenerator.classify(
            correctCount = correctAnswers,
            errorCount   = totalRounds - correctAnswers,
            attempts     = totalRounds
        )

        val session = SessionContext(
            activityType     = TherapeuticFeedbackGenerator.ActivityType.MOOD_MATCHING,
            performanceLevel = level,
            childAge         = ageGroup,
            errorCount       = totalRounds - correctAnswers,
            correctCount     = correctAnswers,
            attempts         = totalRounds
        )

        lifecycleScope.launch {
            val feedback = TherapeuticFeedbackGenerator.generate(
                context = this@MMScoreboardActivity,
                session = session
            )
            applyFeedback(feedback)
        }
    }

    private fun applyFeedback(feedback: TherapeuticFeedback) {
        runOnUiThread {
            // Headline title (coloured by performance tier but contains no number)
            titleWon.text = feedback.headline
            titleWon.setTextColor(resources.getColor(titleColorRes(correctAnswers, totalRounds), theme))
            titleWon.alpha = 0f
            titleWon.animate().alpha(1f).setDuration(700).start()

            // Body message
            val fullText = "${feedback.message}\n\n${feedback.encouragement}"
            performanceText.text  = fullText
            performanceText.alpha = 0f
            Handler(Looper.getMainLooper()).postDelayed({
                performanceText.animate().alpha(1f).setDuration(600).start()
            }, 400)

            // Speak the message when TTS is ready
            val spokenText = "${feedback.message} ${feedback.encouragement}"
            if (isTtsReady) {
                speakFeedback(feedback.message, feedback.encouragement)
            } else {
                // Store both parts separated by a delimiter for deferred speaking
                pendingSpeak = "${feedback.message}|||${feedback.encouragement}"
            }

            Log.d(TAG, "AI feedback applied (fromAI=${feedback.fromAI}): ${feedback.headline}")
        }
    }

    // =========================================================================
    // Stars — symbolic only, no number labels
    // =========================================================================

    /** Maps performance to star count (shape indicator, not a score display). */
    private fun starsFromCorrect(correct: Int, total: Int): Int = when {
        correct == total           -> 3
        correct >= (total * 0.6f)  -> 2
        correct >= 1               -> 1
        else                       -> 0
    }

    /** Returns the colour resource for the headline — matches existing system colours. */
    private fun titleColorRes(correct: Int, total: Int): Int = when {
        correct == total           -> R.color.gold
        correct >= (total * 0.6f)  -> R.color.light_green
        correct >= 1               -> R.color.light_blue
        else                       -> R.color.dark_orange
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
        anim.repeatCount = ValueAnimator.INFINITE; anim.repeatMode = ValueAnimator.REVERSE
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
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Dashboard nav error: ${e.message}"); finishAffinity()
            }
        }

        // FIXED — passes friendAction and ageGroup
        btnUnlockGift.setOnClickListener {
            // Stop scoreboard TTS before launching gift screen
            if (::tts.isInitialized && isTtsReady) {
                tts.stop()
            }
            startActivityForResult(
                Intent(this, AllCorrectGrandPrizeActivity::class.java).apply {
                    putExtra("FRIEND_ACTION", friendAction)
                    putExtra("AGE_GROUP",     ageGroup)
                },
                100
            )
        }
    }

    // =========================================================================
    // TTS
    // =========================================================================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.3f)        // slightly less chipmunk, clearer for children
            tts.setSpeechRate(0.75f)
            isTtsReady = true
            pendingSpeak?.let {
                val parts = it.split("|||")
                if (parts.size == 2) speakFeedback(parts[0], parts[1])
                else speakFeedback(it, "")   // safety fallback
                pendingSpeak = null
            }
        }
    }

    // REMOVE the old speakText() entirely and replace with:

    private fun speakFeedback(message: String, encouragement: String) {
        if (!isTtsReady) return

        // Keep periods and commas — they signal natural pauses to the TTS engine
        fun clean(s: String) = s.replace(Regex("[^a-zA-Z0-9 .,!?']"), "").trim()

        val cleanMsg = clean(message)
        val cleanEnc = clean(encouragement)

        // Speak the message first (flush any previous speech)
        tts.speak(cleanMsg, TextToSpeech.QUEUE_FLUSH, null, "msg")

        // Add ~700 ms silence so the child can absorb the message before encouragement
        tts.playSilentUtterance(700, TextToSpeech.QUEUE_ADD, "pause")

        // Then speak the encouragement
        tts.speak(cleanEnc, TextToSpeech.QUEUE_ADD, null, "enc")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        mediaPlayer?.release()
        super.onDestroy()
    }
}