package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class ASequenceScoreboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var finalAlpha: Float = 0f
    private var poppedBubbles: Int = 0
    private var totalBubbles: Int = 5
    private var correctCount: Int = 0
    private var errorCount: Int = 0
    private var completedSubroutines: Int = 0
    private var previousAlpha: Float = 0f
    private var previousCorrect: Int = 0
    private var previousSubroutine: Int = -1
    private var previousError: String = "none"
    private var userSelectedRoutine: Int = 0
    private var userAge: Int = 6
    private var attemptsCount: Int = 0

    private var shouldAwardSticker: Boolean = false

    // Dynamic staircase configuration
    private var totalSteps = 1
    private var starFinalStep = 0
    private lateinit var starCharacter: ImageView
    private val stepBlocks = mutableListOf<ImageView>()
    private val stepLabels = mutableListOf<TextView>()

    // UI Elements
    private lateinit var staircaseContainer: ConstraintLayout
    private lateinit var therapeuticMessage: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnHome: Button
    private lateinit var motivationalQuote: TextView

    private var gameMode: String = "under"

    // Animation containers
    private lateinit var blockDropAnimators: MutableList<AnimatorSet>
    private var starGlowAnimator: ObjectAnimator? = null
    private var starBounceAnimator: ObjectAnimator? = null

    // Sound effects
    private lateinit var blockDropSound: MediaPlayer
    private lateinit var starBounceSound: MediaPlayer
    private lateinit var completionSound: MediaPlayer

    private val blockWidth = 140
    private val blockHeight = 40
    private val blockSpacing = 20
    private val verticalSpacing = 15

    // AI feedback state
    private var aiFeedback: TherapeuticFeedback? = null
    private var feedbackReady = false

    // ── TTS ──────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var pendingMsg = ""
    private var pendingEnc = ""

    companion object {
        private const val TAG = "ScoreboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        val extras = intent.extras
        if (extras != null) {
            Log.d(TAG, "=== ALL INTENT EXTRAS ===")
            for (key in extras.keySet()) Log.d(TAG, "$key = ${extras[key]}")
            Log.d(TAG, "========================")
        }

        getIntentData()
        Log.d(TAG, "Scoreboard: finalAlpha=$finalAlpha correct=$correctCount errors=$errorCount attempts=$attemptsCount awardSticker=$shouldAwardSticker")
        gameMode = intent.getStringExtra("GAME_MODE") ?: if (userAge >= 8) "over" else "under"
        initializeSounds()
        initializeViews()
        calculateStairCount()
        calculateStarProgress()
        buildStaircase()
        hideExtraStairs()

        // ── Initialise TTS before fetching feedback ───────────────────────────
        tts = TextToSpeech(this, this)

        fetchAIFeedback()
        startTherapeuticAnimationSequence()
        setupButtonListeners()
    }

    // =========================================================================
    // TTS
    // =========================================================================

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US)
            tts.setPitch(1.3f)       // warm and child-friendly without being shrill
            tts.setSpeechRate(0.75f) // slow enough for young listeners to follow
            isTtsReady = true
            if (pendingMsg.isNotBlank()) {
                speakFeedback(pendingMsg, pendingEnc)
                pendingMsg = ""
                pendingEnc = ""
            }
        }
    }

    /**
     * Speaks message and encouragement as two separate utterances with a
     * 700 ms silent gap so the child can absorb each sentence naturally.
     * Punctuation is preserved so the TTS engine pauses at sentence ends.
     */
    private fun speakFeedback(message: String, encouragement: String) {
        if (!isTtsReady) return
        fun clean(s: String) = s.replace(Regex("[^a-zA-Z0-9 .,!?']"), "").trim()
        tts.speak(clean(message),       TextToSpeech.QUEUE_FLUSH, null, "msg")
        tts.playSilentUtterance(700,    TextToSpeech.QUEUE_ADD,   "pause")
        tts.speak(clean(encouragement), TextToSpeech.QUEUE_ADD,   null, "enc")
    }

    // =========================================================================
    // Intent data
    // =========================================================================

    private fun getIntentData() {
        finalAlpha           = intent.getFloatExtra("FINAL_ALPHA", 0f)
        poppedBubbles        = intent.getIntExtra("POPPED_BUBBLES", 0)
        totalBubbles         = intent.getIntExtra("TOTAL_BUBBLES", 5)
        correctCount         = intent.getIntExtra("CORRECT_COUNT", 0)
        errorCount           = intent.getIntExtra("ERROR_COUNT", 0)
        completedSubroutines = intent.getIntExtra("COMPLETED_SUBROUTINES", 0)
        previousAlpha        = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        previousCorrect      = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        previousSubroutine   = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        previousError        = intent.getStringExtra("PREVIOUS_ERROR") ?: "none"
        userSelectedRoutine  = intent.getIntExtra("USER_SELECTED_ROUTINE", 0)
        userAge              = intent.getIntExtra("USER_AGE", 6)
        gameMode = intent.getStringExtra("GAME_MODE") ?: if (userAge >= 8) "over" else "under"
        attemptsCount        = intent.getIntExtra("ATTEMPTS_COUNT", 0)
        shouldAwardSticker   = intent.getBooleanExtra("SHOULD_AWARD_STICKER", false)
    }

    // =========================================================================
    // Performance tier
    // =========================================================================

    private enum class Tier { BEST, HIGH, MODERATE, LOW }

    private fun performanceTier(): Tier {
        val attempts = attemptsCount.coerceAtLeast(1)
        return when {
            errorCount == 0 && attempts <= 3 -> Tier.BEST
            errorCount <= 2 && attempts <= 3 -> Tier.HIGH
            errorCount <= 3 && attempts <= 7 -> Tier.MODERATE
            else                             -> Tier.LOW
        }
    }

    private fun calculateStairCount() {
        totalSteps = when (performanceTier()) {
            Tier.BEST            -> 5
            Tier.HIGH            -> 5
            Tier.MODERATE        -> 3
            Tier.LOW             -> 1
        }
        totalSteps    = totalSteps.coerceIn(1, 5)
        starFinalStep = totalSteps - 1
        Log.d(TAG, "Tier=${performanceTier()} stairs=$totalSteps alpha=$finalAlpha errors=$errorCount attempts=$attemptsCount")
    }

    private fun calculateStarProgress() { starFinalStep = totalSteps - 1 }

    private fun buildStaircase() {
        val themeColor = when (performanceTier()) {
            Tier.BEST,
            Tier.HIGH     -> Color.parseColor("#4CAF50")
            Tier.MODERATE -> Color.parseColor("#FF9800")
            Tier.LOW      -> Color.parseColor("#FF6B8B")
        }
        for (i in 0 until totalSteps) {
            val block = stepBlocks[i]
            block.setColorFilter(themeColor)
            block.alpha        = 0f
            block.translationY = -1000f
            block.visibility   = View.VISIBLE
            stepLabels[i].visibility = View.INVISIBLE
        }
    }

    private fun hideExtraStairs() {
        for (i in totalSteps until stepBlocks.size) {
            stepBlocks[i].visibility = View.GONE
            stepLabels[i].visibility = View.GONE
        }
    }

    // =========================================================================
    // Views
    // =========================================================================

    private fun initializeViews() {
        staircaseContainer = findViewById(R.id.staircaseContainer)
        therapeuticMessage = findViewById(R.id.therapeuticMessage)
        btnContinue        = findViewById(R.id.btnContinue)
        btnHome            = findViewById(R.id.btnHome)
        starCharacter      = findViewById(R.id.starCharacter)
        motivationalQuote  = findViewById(R.id.motivationalQuote)

        stepBlocks.apply {
            add(findViewById(R.id.stepBlock1)); add(findViewById(R.id.stepBlock2))
            add(findViewById(R.id.stepBlock3)); add(findViewById(R.id.stepBlock4))
            add(findViewById(R.id.stepBlock5))
        }
        stepLabels.apply {
            add(findViewById(R.id.stepLabel1)); add(findViewById(R.id.stepLabel2))
            add(findViewById(R.id.stepLabel3)); add(findViewById(R.id.stepLabel4))
            add(findViewById(R.id.stepLabel5))
        }
        blockDropAnimators = mutableListOf()
    }

    // =========================================================================
    // AI feedback
    // =========================================================================

    private fun fetchAIFeedback() {
        therapeuticMessage.text = "Well done"

        val apiKey = getString(R.string.groq_api_key)
        Log.d(TAG, "DIAG 1 - API key: isBlank=${apiKey.isBlank()}, length=${apiKey.length}, starts=${apiKey.take(8)}")

        val level = TherapeuticFeedbackGenerator.classify(
            correctCount = correctCount,
            errorCount   = errorCount,
            attempts     = attemptsCount.coerceAtLeast(correctCount + errorCount),
            finalAlpha   = finalAlpha
        )
        Log.d(TAG, "DIAG 2 - Performance level classified as: $level (correct=$correctCount, errors=$errorCount, attempts=$attemptsCount, alpha=$finalAlpha)")

        val routineName = when (userSelectedRoutine) {
            0    -> "Morning Routine"
            1    -> "Bedtime Routine"
            2    -> "School Routine"
            else -> "Daily Routine"
        }

        val session = SessionContext(
            activityType     = TherapeuticFeedbackGenerator.ActivityType.SEQUENCE_ORDERING,
            performanceLevel = level,
            childAge         = userAge,
            errorCount       = errorCount,
            correctCount     = correctCount,
            attempts         = attemptsCount.coerceAtLeast(correctCount + errorCount),
            routineName      = routineName,
            previousAlpha    = previousAlpha,
            sessionNumber    = completedSubroutines
        )
        Log.d(TAG, "DIAG 3 - SessionContext built: age=$userAge, routine=$routineName, session=$completedSubroutines")
        Log.d(TAG, "DIAG 4 - Launching coroutine for AI feedback...")

        lifecycleScope.launch {
            Log.d(TAG, "DIAG 5 - Inside coroutine, calling TherapeuticFeedbackGenerator.generate()")
            try {
                val feedback = TherapeuticFeedbackGenerator.generate(
                    context = this@ASequenceScoreboardActivity,
                    session = session
                )
                Log.d(TAG, "DIAG 6 - generate() returned: fromAI=${feedback.fromAI}, headline='${feedback.headline}'")
                Log.d(TAG, "DIAG 6 - message='${feedback.message}', encouragement='${feedback.encouragement}'")

                aiFeedback    = feedback
                feedbackReady = true
                applyFeedbackToUI(feedback)
                Log.d(TAG, "AI feedback (fromAI=${feedback.fromAI}): ${feedback.headline}")

            } catch (e: Exception) {
                Log.e(TAG, "DIAG ERROR - Exception during AI feedback generation: ${e::class.simpleName}: ${e.message}", e)
            }
        }
    }

    private fun applyFeedbackToUI(feedback: TherapeuticFeedback) {
        runOnUiThread {
            // Update headline
            therapeuticMessage.text  = feedback.headline
            therapeuticMessage.alpha = 0f
            therapeuticMessage.visibility = View.VISIBLE
            therapeuticMessage.animate().alpha(1f).setDuration(800).start()

            // If star is already visible, reposition the quote next to it
            if (starCharacter.visibility == View.VISIBLE) {
                showQuoteForStar()
            } else {
                motivationalQuote.text = "${feedback.message}\n\n${feedback.encouragement}"
            }

            // ── Speak feedback (message + encouragement only, no headline) ────
            if (isTtsReady) {
                speakFeedback(feedback.message, feedback.encouragement)
            } else {
                pendingMsg = feedback.message
                pendingEnc = feedback.encouragement
            }
        }
    }

    // =========================================================================
    // Animation sequence
    // =========================================================================

    private fun updateTherapeuticMessage() {
        aiFeedback?.let { therapeuticMessage.text = it.headline }
        therapeuticMessage.alpha      = 0f
        therapeuticMessage.visibility = View.VISIBLE
        therapeuticMessage.animate().alpha(1f).setDuration(800).start()
    }

    private fun startTherapeuticAnimationSequence() {
        updateTherapeuticMessage()
        Handler(Looper.getMainLooper()).postDelayed({ dropBlocksSequence() }, 1000)

        val maxWait = 1000L + (totalSteps * 400L) + 600L + 2500L + 3000L
        Handler(Looper.getMainLooper()).postDelayed({
            if (!btnContinue.isEnabled && !isFinishing) {
                Log.w(TAG, "Fallback timer fired")
                if (shouldAwardSticker && performanceTier() == Tier.BEST) {
                    navigateToStickerScreen()
                } else {
                    enableNavigationButtons()
                }
            }
        }, maxWait.coerceAtLeast(10000L))
    }

    private fun dropBlocksSequence() {
        blockDropAnimators.clear()
        for (i in 0 until totalSteps) Handler().postDelayed({ animateBlockDrop(i) }, i * 400L)
        Handler().postDelayed({ introduceStar() }, (totalSteps * 400L) + 600)
    }

    private fun animateBlockDrop(stepIndex: Int) {
        val block   = stepBlocks[stepIndex]
        val baseY   = 200f
        val targetY = baseY - (stepIndex * (blockHeight + verticalSpacing)).toFloat()
        val targetX = 20f + (stepIndex * blockSpacing).toFloat()

        val dropAnim  = ObjectAnimator.ofFloat(block, "translationY", -1000f, targetY).apply {
            duration = 800; interpolator = BounceInterpolator()
        }
        val slideAnim = ObjectAnimator.ofFloat(block, "translationX", targetX).apply { duration = 600 }
        val fadeAnim  = ObjectAnimator.ofFloat(block, "alpha", 0f, 1f).apply { duration = 400 }

        val animSet = AnimatorSet()
        animSet.playTogether(dropAnim, slideAnim, fadeAnim)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                blockDropSound.seekTo(0); blockDropSound.setVolume(0.4f, 0.4f); blockDropSound.start()
            }
            override fun onAnimationEnd(animation: Animator) {
                ObjectAnimator.ofFloat(block, "translationY", targetY - 5, targetY).apply {
                    duration = 300; interpolator = BounceInterpolator()
                }.start()
            }
        })
        animSet.start()
        blockDropAnimators.add(animSet)
    }

    private fun introduceStar() {
        if (starFinalStep < 0 || starFinalStep >= stepBlocks.size) return
        val targetBlock = stepBlocks[starFinalStep]
        val finalX = targetBlock.x + (targetBlock.width / 4)
        val finalY = targetBlock.y - starCharacter.height - 40f

        starCharacter.x          = finalX
        starCharacter.y          = finalY
        starCharacter.visibility = View.VISIBLE
        starCharacter.alpha      = 0f

        motivationalQuote.alpha      = 0f
        motivationalQuote.visibility = View.VISIBLE

        val appearAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(starCharacter, "alpha",    0f, 1f),
                ObjectAnimator.ofFloat(starCharacter, "scaleX",   0f, 1.2f),
                ObjectAnimator.ofFloat(starCharacter, "scaleY",   0f, 1.2f),
                ObjectAnimator.ofFloat(starCharacter, "rotation", 0f, 720f)
            )
            duration = 1000; interpolator = DecelerateInterpolator()
        }
        val bounceAnim = ObjectAnimator.ofFloat(starCharacter, "y", finalY, finalY - 50f, finalY).apply {
            duration = 600; repeatCount = 1; interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playSequentially(appearAnim, bounceAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    starBounceSound.seekTo(0); starBounceSound.start()
                }
                override fun onAnimationEnd(animation: Animator) {
                    showQuoteForStar()
                    settleStar()
                }
            })
            start()
        }
    }

    private fun showQuoteForStar() {
        val displayText = aiFeedback?.let {
            "${it.message}\n\n${it.encouragement}"
        } ?: "Loading your message…"

        motivationalQuote.text       = displayText
        motivationalQuote.visibility = View.VISIBLE
        motivationalQuote.alpha      = 0f

        motivationalQuote.post {
            val starScreenPos = IntArray(2)
            starCharacter.getLocationOnScreen(starScreenPos)

            val parentScreenPos = IntArray(2)
            (motivationalQuote.parent as View).getLocationOnScreen(parentScreenPos)

            val starLeftInParent  = (starScreenPos[0] - parentScreenPos[0]).toFloat()
            val starTopInParent   = (starScreenPos[1] - parentScreenPos[1]).toFloat()
            val starRightInParent = starLeftInParent + starCharacter.width
            val starMidYInParent  = starTopInParent + (starCharacter.height / 2f)

            val quoteW  = motivationalQuote.measuredWidth.toFloat()
            val quoteH  = motivationalQuote.measuredHeight.toFloat()
            val screenW = resources.displayMetrics.widthPixels.toFloat()
            val margin  = 24f

            var targetX: Float
            var targetY: Float

            when (totalSteps) {
                1 -> {
                    targetX = starRightInParent + margin
                    targetY = starMidYInParent - (quoteH / 2f)
                    if (targetX + quoteW > screenW - margin) {
                        targetX = starLeftInParent - quoteW - margin
                    }
                }
                3 -> {
                    targetX = starLeftInParent + (starCharacter.width / 2f) - (quoteW / 2f)
                    targetY = starTopInParent - quoteH - margin
                    if (targetY < parentScreenPos[1].toFloat()) {
                        targetY = starTopInParent + starCharacter.height + margin
                    }
                }
                else -> {
                    targetX = starLeftInParent - quoteW - margin
                    targetY = starMidYInParent - (quoteH / 2f)
                    if (targetX < margin) {
                        targetX = starRightInParent + margin
                    }
                }
            }

            val parentH = (motivationalQuote.parent as View).height.toFloat()
            targetY = targetY.coerceIn(margin, parentH - quoteH - margin)

            motivationalQuote.x = targetX
            motivationalQuote.y = targetY

            ObjectAnimator.ofFloat(motivationalQuote, "alpha", 0f, 1f)
                .apply { duration = 800; start() }
        }
    }

    private fun settleStar() {
        if (starFinalStep < 0 || starFinalStep >= stepBlocks.size) return

        for (i in 0..starFinalStep) {
            Handler(Looper.getMainLooper()).postDelayed({
                val block = stepBlocks[i]
                AnimatorSet().apply {
                    playSequentially(
                        AnimatorSet().apply {
                            playTogether(
                                ObjectAnimator.ofFloat(block, "scaleX", 1f, 1.1f),
                                ObjectAnimator.ofFloat(block, "scaleY", 1f, 1.1f)
                            ); duration = 200
                        },
                        AnimatorSet().apply {
                            playTogether(
                                ObjectAnimator.ofFloat(block, "scaleX", 1.1f, 1f),
                                ObjectAnimator.ofFloat(block, "scaleY", 1.1f, 1f)
                            ); duration = 200
                        }
                    ); start()
                }
            }, i * 150L)
        }

        completionSound.seekTo(0); completionSound.start()
        startStarGlow()

        val settleDelay = (starFinalStep * 150L) + 800L
        if (shouldAwardSticker && performanceTier() == Tier.BEST) {
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToStickerScreen()
            }, settleDelay + 1500L)
        } else {
            enableNavigationButtons()
        }
    }

    private fun startStarGlow() {
        starGlowAnimator = ObjectAnimator.ofFloat(starCharacter, "scaleX", 1.2f, 1.3f, 1.2f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; start()
        }
        ObjectAnimator.ofFloat(starCharacter, "scaleY", 1.2f, 1.3f, 1.2f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE; start()
        }
    }

    private fun enableNavigationButtons() {
        ObjectAnimator.ofFloat(btnContinue, "alpha", 0f, 1f).apply { duration = 500; start() }
        ObjectAnimator.ofFloat(btnHome,     "alpha", 0f, 1f).apply { duration = 500; start() }
        btnContinue.isEnabled = true
        btnHome.isEnabled     = true
    }

    // =========================================================================
    // Button listeners
    // =========================================================================

    private fun setupButtonListeners() {
        btnContinue.alpha     = 0f
        btnHome.alpha         = 0f
        btnContinue.isEnabled = false
        btnHome.isEnabled     = false

        btnContinue.setOnClickListener {
            ObjectAnimator.ofPropertyValuesHolder(
                btnContinue,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            ).apply {
                duration = 200
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        navigateNext { navigateToGame() }
                    }
                })
                start()
            }
        }

        btnHome.setOnClickListener {
            ObjectAnimator.ofPropertyValuesHolder(
                btnHome,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            ).apply {
                duration = 200
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        navigateNext {
                            startActivity(Intent(this@ASequenceScoreboardActivity, RoutineSelectionActivity::class.java))
                            finish()
                        }
                    }
                })
                start()
            }
        }
    }

    private fun navigateToStickerScreen() {
        if (isFinishing) return

        // 1. Stop TTS before leaving
        if (::tts.isInitialized && isTtsReady) tts.stop()

        // 2. Cancel all animators
        starGlowAnimator?.cancel()
        starBounceAnimator?.cancel()
        blockDropAnimators.forEach { it.cancel() }
        stepBlocks.forEach { it.animate().cancel() }
        starCharacter.animate().cancel()

        // 3. Release media players
        try { blockDropSound.stop() } catch (_: Exception) {}
        try { starBounceSound.stop() } catch (_: Exception) {}
        try { completionSound.stop() } catch (_: Exception) {}

        // 4. Navigate
        startActivity(Intent(this, UnlockStickerActivity::class.java).apply {
            putExtra("GAME_MODE",             gameMode)
            putExtra("FINAL_ALPHA",           finalAlpha)
            putExtra("POPPED_BUBBLES",        poppedBubbles)
            putExtra("TOTAL_BUBBLES",         totalBubbles)
            putExtra("CORRECT_COUNT",         correctCount)
            putExtra("ERROR_COUNT",           errorCount)
            putExtra("PREVIOUS_ALPHA",        previousAlpha)
            putExtra("PREVIOUS_CORRECT",      previousCorrect)
            putExtra("PREVIOUS_SUBROUTINE",   previousSubroutine)
            putExtra("PREVIOUS_ERROR",        previousError)
            putExtra("USER_SELECTED_ROUTINE", userSelectedRoutine)
            putExtra("USER_AGE",              userAge)
        })
        finish()
    }

    private fun navigateNext(destination: () -> Unit) {
        destination()
    }

    private fun navigateToGame() {
        if (::tts.isInitialized && isTtsReady) tts.stop()
        val targetActivity = if (gameMode == "over") ActivitySequenceOverActivity::class.java
        else ActivitySequenceUnderActivity::class.java
        Log.d(TAG, "Continue → ${targetActivity.simpleName}")
        startActivity(Intent(this, targetActivity).apply {
            putExtra("PREVIOUS_ALPHA",      finalAlpha)
            putExtra("PREVIOUS_CORRECT",    correctCount)
            putExtra("PREVIOUS_SUBROUTINE", previousSubroutine)
            putExtra("PREVIOUS_ERROR",      previousError)
            putExtra("SELECTED_ROUTINE_ID", userSelectedRoutine)
            putExtra("USER_AGE",            userAge)
        })
        finish()
    }

    // =========================================================================
    // Sounds init
    // =========================================================================

    private fun initializeSounds() {
        blockDropSound  = MediaPlayer.create(this, R.raw.block_drop)
        starBounceSound = MediaPlayer.create(this, R.raw.star_bounce)
        completionSound = MediaPlayer.create(this, R.raw.completion_sound)
        blockDropSound.setVolume(0.3f, 0.3f)
        starBounceSound.setVolume(0.5f, 0.5f)
        completionSound.setVolume(0.7f, 0.7f)
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onPause() {
        super.onPause()
        starGlowAnimator?.pause(); starBounceAnimator?.pause()
        blockDropAnimators.forEach { it.pause() }
        blockDropSound.pause(); starBounceSound.pause(); completionSound.pause()
    }

    override fun onResume() {
        super.onResume()
        starGlowAnimator?.resume(); starBounceAnimator?.resume()
        blockDropAnimators.forEach { it.resume() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) tts.shutdown()
        starGlowAnimator?.cancel()
        starBounceAnimator?.cancel()
        blockDropAnimators.forEach { it.cancel() }
        try { blockDropSound.release() } catch (_: Exception) {}
        try { starBounceSound.release() } catch (_: Exception) {}
        try { completionSound.release() } catch (_: Exception) {}
    }
}