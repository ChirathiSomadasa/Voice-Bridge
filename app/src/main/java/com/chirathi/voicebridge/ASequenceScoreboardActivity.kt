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
import android.util.Log
import android.view.View
import android.view.animation.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.min

class ASequenceScoreboardActivity : AppCompatActivity() {

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

    // Dynamic staircase configuration
    private var totalSteps = 1
    private var starFinalStep = 0
    private lateinit var starCharacter: ImageView
    private val stepBlocks = mutableListOf<ImageView>()
    private val stepLabels = mutableListOf<TextView>()

    private val motivationalQuotes = mapOf(
        1 to listOf("Good start","You can do it","Keep trying","We are working together"),
        3 to listOf(
            "Good job",
            "Good trying",
            "I like your focus",
            "Getting closer now",
            "We are almost there",
        ),
        5 to listOf(
            "You did it",
            "Well done",
            "You are a hero",
            "A great job today",
        )
    )

    // UI Elements
    private lateinit var staircaseContainer: ConstraintLayout
    private lateinit var therapeuticMessage: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnHome: Button
    private lateinit var motivationalQuote: TextView

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

    companion object {
        private const val TAG = "ScoreboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        val extras = intent.extras
        if (extras != null) {
            Log.d(TAG, "=== ALL INTENT EXTRAS ===")
            for (key in extras.keySet()) {
                Log.d(TAG, "$key = ${extras[key]}")
            }
            Log.d(TAG, "========================")
        }

        getIntentData()

        Log.d(TAG, "Therapeutic Summary: Alpha=$finalAlpha, Bubbles=$poppedBubbles/$totalBubbles, Age=$userAge")

        initializeSounds()
        initializeViews()
        calculateStairCount()
        calculateStarProgress()
        buildStaircase()
        hideExtraStairs()
        startTherapeuticAnimationSequence()
        setupButtonListeners()
    }

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

        Log.d(TAG, "Received from intent - FinalAlpha: $finalAlpha, ErrorCount: $errorCount, UserAge: $userAge")
    }

    private fun initializeSounds() {
        blockDropSound  = MediaPlayer.create(this, R.raw.block_drop)
        starBounceSound = MediaPlayer.create(this, R.raw.star_bounce)
        completionSound = MediaPlayer.create(this, R.raw.completion_sound)

        blockDropSound.setVolume(0.3f, 0.3f)
        starBounceSound.setVolume(0.5f, 0.5f)
        completionSound.setVolume(0.7f, 0.7f)
    }

    private fun initializeViews() {
        staircaseContainer = findViewById(R.id.staircaseContainer)
        therapeuticMessage = findViewById(R.id.therapeuticMessage)
        btnContinue        = findViewById(R.id.btnContinue)
        btnHome            = findViewById(R.id.btnHome)
        starCharacter      = findViewById(R.id.starCharacter)
        motivationalQuote  = findViewById(R.id.motivationalQuote)

        stepBlocks.apply {
            add(findViewById(R.id.stepBlock1))
            add(findViewById(R.id.stepBlock2))
            add(findViewById(R.id.stepBlock3))
            add(findViewById(R.id.stepBlock4))
            add(findViewById(R.id.stepBlock5))
        }

        stepLabels.apply {
            add(findViewById(R.id.stepLabel1))
            add(findViewById(R.id.stepLabel2))
            add(findViewById(R.id.stepLabel3))
            add(findViewById(R.id.stepLabel4))
            add(findViewById(R.id.stepLabel5))
        }

        blockDropAnimators = mutableListOf()
    }

    private fun calculateStairCount() {
        val avgResponseTime = intent.getLongExtra("AVG_RESPONSE_TIME", 0L)
        val attempts        = intent.getIntExtra("ATTEMPTS_COUNT", 0)
        val errors          = intent.getIntExtra("ERROR_COUNT", 0)

        Log.d(TAG, "Stair calculation - Alpha: $finalAlpha, Time: ${avgResponseTime}ms, Attempts: $attempts, Errors: $errors")

        totalSteps = when {
            (attempts == 1 && errors == 0 && finalAlpha >= 7.0f) -> {
                Log.d(TAG, "Condition: 5 stairs - First try mastery"); 5
            }
            (finalAlpha >= 8.0f && errors <= 1) -> {
                Log.d(TAG, "Condition: 5 stairs - Excellent performance"); 5
            }
            (finalAlpha >= 5.0f && errors <= 2) -> {
                Log.d(TAG, "Condition: 3 stairs - Good progress"); 3
            }
            (finalAlpha >= 3.0f && attempts <= 2 && errors <= 3) -> {
                Log.d(TAG, "Condition: 3 stairs - Completed with some help"); 3
            }
            else -> {
                Log.d(TAG, "Condition: 1 stair - Needs more practice"); 1
            }
        }

        totalSteps = totalSteps.coerceIn(1, 5)
        starFinalStep = totalSteps - 1
        Log.d(TAG, "Final decision: $totalSteps steps, Star at step: $starFinalStep")
    }

    private fun buildStaircase() {
        val themeColor = when (totalSteps) {
            1    -> Color.parseColor("#FF6B8B")
            3    -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#4CAF50")
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

    private fun calculateStarProgress() {
        starFinalStep = totalSteps - 1
    }

    private fun updateTherapeuticMessage() {
        val message = when (totalSteps) {
            1    -> "Great start!"
            3    -> "Good job!"
            5    -> "Excellent work!"
            else -> "Well done!"
        }
        therapeuticMessage.text       = message
        therapeuticMessage.alpha      = 0f
        therapeuticMessage.visibility = View.VISIBLE
        therapeuticMessage.animate().alpha(1f).setDuration(800).start()
    }

    private fun hideExtraStairs() {
        for (i in totalSteps until stepBlocks.size) {
            stepBlocks[i].visibility = View.GONE
            stepLabels[i].visibility = View.GONE
        }
    }

    private fun startTherapeuticAnimationSequence() {
        updateTherapeuticMessage()
        Handler(Looper.getMainLooper()).postDelayed({ dropBlocksSequence() }, 1000)
    }

    private fun dropBlocksSequence() {
        blockDropAnimators.clear()
        for (i in 0 until totalSteps) {
            Handler().postDelayed({ animateBlockDrop(i) }, i * 400L)
        }
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
                    Handler(Looper.getMainLooper()).postDelayed({ showQuoteForStep(starFinalStep) }, 300)
                    settleStar()
                }
            })
            start()
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
                    )
                    start()
                }
            }, i * 150L)
        }
        completionSound.seekTo(0); completionSound.start()
        startStarGlow()
        enableNavigationButtons()
    }

    private fun showQuoteForStep(step: Int) {
        val quotes     = motivationalQuotes[totalSteps] ?: return
        val quoteIndex = min(step, quotes.size - 1)
        motivationalQuote.text       = quotes[quoteIndex]
        motivationalQuote.visibility = View.VISIBLE
        motivationalQuote.alpha      = 0f

        motivationalQuote.post {
            val quoteHeight = motivationalQuote.measuredHeight
            val quoteWidth  = motivationalQuote.measuredWidth
            motivationalQuote.x = starCharacter.x + (starCharacter.width  / 2) - (quoteWidth  / 2)
            motivationalQuote.y = starCharacter.y - quoteHeight - 20f
            ObjectAnimator.ofFloat(motivationalQuote, "alpha", 0f, 1f).apply { duration = 800; start() }
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
    // Button listeners — route Continue to correct activity based on userAge
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
                        navigateToGame()
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
                        startActivity(Intent(this@ASequenceScoreboardActivity, RoutineSelectionActivity::class.java))
                        finish()
                    }
                })
                start()
            }
        }
    }

    /**
     * Routes the "Play Again" tap to the correct game activity based on age:
     *   Age 6-7  → ActivitySequenceUnderActivity  (tap-to-order)
     *   Age 8-10 → ActivitySequenceOverActivity   (drag-and-drop)
     *   Other    → ActivitySequenceUnderActivity  (fallback)
     */
    private fun navigateToGame() {
        val targetActivity = if (userAge in 8..10) {
            ActivitySequenceOverActivity::class.java
        } else {
            ActivitySequenceUnderActivity::class.java
        }

        Log.d(TAG, "Continue tapped — userAge=$userAge → navigating to ${targetActivity.simpleName}")

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
    // Lifecycle
    // =========================================================================

    override fun onPause() {
        super.onPause()
        starGlowAnimator?.pause()
        starBounceAnimator?.pause()
        blockDropAnimators.forEach { it.pause() }
        blockDropSound.pause()
        starBounceSound.pause()
        completionSound.pause()
    }

    override fun onResume() {
        super.onResume()
        starGlowAnimator?.resume()
        starBounceAnimator?.resume()
        blockDropAnimators.forEach { it.resume() }
    }

    override fun onDestroy() {
        super.onDestroy()
        starGlowAnimator?.cancel()
        starBounceAnimator?.cancel()
        blockDropAnimators.forEach { it.cancel() }
        blockDropSound.release()
        starBounceSound.release()
        completionSound.release()
    }
}