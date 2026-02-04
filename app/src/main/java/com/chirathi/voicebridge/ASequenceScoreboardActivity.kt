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
    private var totalSteps = 1 // Default to 1 stair for low score
    private var starFinalStep = 0
    private lateinit var starCharacter: ImageView
    private val stepBlocks = mutableListOf<ImageView>()
    // Keeping the list definition to avoid errors, but they won't be used visually
    private val stepLabels = mutableListOf<TextView>()

    // Different motivational quotes for different levels
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

    // Stair positioning variables
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

        // TEMPORARY DEBUG: Log all intent extras
        val extras = intent.extras
        if (extras != null) {
            Log.d(TAG, "=== ALL INTENT EXTRAS ===")
            for (key in extras.keySet()) {
                Log.d(TAG, "$key = ${extras[key]}")
            }
            Log.d(TAG, "========================")
        }

        // Get therapeutic data from intent
        getIntentData()

        Log.d(TAG, "Therapeutic Summary: Alpha=$finalAlpha, Bubbles=$poppedBubbles/$totalBubbles")

        // Initialize sound effects
        initializeSounds()

        // Initialize views
        initializeViews()

        // Calculate how many stairs to show based on score
        calculateStairCount()

        // Calculate star's final position based on alpha
        calculateStarProgress()

        // Build therapeutic staircase
        buildStaircase()

        // Hide extra stairs
        hideExtraStairs()

        // Start therapeutic animation sequence
        startTherapeuticAnimationSequence()

        // Setup button listeners
        setupButtonListeners()
    }

    private fun getIntentData() {
        finalAlpha = intent.getFloatExtra("FINAL_ALPHA", 0f)
        poppedBubbles = intent.getIntExtra("POPPED_BUBBLES", 0)
        totalBubbles = intent.getIntExtra("TOTAL_BUBBLES", 5)
        correctCount = intent.getIntExtra("CORRECT_COUNT", 0)
        errorCount = intent.getIntExtra("ERROR_COUNT", 0)
        completedSubroutines = intent.getIntExtra("COMPLETED_SUBROUTINES", 0)
        previousAlpha = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        previousCorrect = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        previousSubroutine = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        previousError = intent.getStringExtra("PREVIOUS_ERROR") ?: "none"
        userSelectedRoutine = intent.getIntExtra("USER_SELECTED_ROUTINE", 0)
        userAge = intent.getIntExtra("USER_AGE", 6)

        Log.d(TAG, "Received from intent - FinalAlpha: $finalAlpha, ErrorCount: $errorCount")
    }

    private fun initializeSounds() {
        blockDropSound = MediaPlayer.create(this, R.raw.block_drop)
        starBounceSound = MediaPlayer.create(this, R.raw.star_bounce)
        completionSound = MediaPlayer.create(this, R.raw.completion_sound)

        // Adjust volumes for therapeutic experience
        blockDropSound.setVolume(0.3f, 0.3f)
        starBounceSound.setVolume(0.5f, 0.5f)
        completionSound.setVolume(0.7f, 0.7f)
    }

    private fun initializeViews() {
        staircaseContainer = findViewById(R.id.staircaseContainer)
        therapeuticMessage = findViewById(R.id.therapeuticMessage)
        btnContinue = findViewById(R.id.btnContinue)
        btnHome = findViewById(R.id.btnHome)
        starCharacter = findViewById(R.id.starCharacter)
        motivationalQuote = findViewById(R.id.motivationalQuote)

        // Initialize all possible step blocks and labels
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

        // Initialize animation list
        blockDropAnimators = mutableListOf()
    }

    private fun calculateStairCount() {
        // Get performance data from intent
        val avgResponseTime = intent.getLongExtra("AVG_RESPONSE_TIME", 0L)
        val attempts = intent.getIntExtra("ATTEMPTS_COUNT", 0)
        val errors = intent.getIntExtra("ERROR_COUNT", 0)

        Log.d(TAG, "Stair calculation - Alpha: $finalAlpha, Time: ${avgResponseTime}ms, Attempts: $attempts, Errors: $errors")

        // FIXED: More realistic staircase logic
        totalSteps = when {
            // 5 STAIRS: EXCELLENT - First try success or very high performance
            (attempts == 1 && errors == 0 && finalAlpha >= 7.0f) -> {
                Log.d(TAG, "Condition: 5 stairs - First try mastery")
                5
            }
            (finalAlpha >= 8.0f && errors <= 1) -> {
                Log.d(TAG, "Condition: 5 stairs - Excellent performance")
                5
            }

            // 3 STAIRS: MODERATE - Made good progress
            (finalAlpha >= 5.0f && errors <= 2) -> {
                Log.d(TAG, "Condition: 3 stairs - Good progress")
                3
            }
            (finalAlpha >= 3.0f && attempts <= 2 && errors <= 3) -> {
                Log.d(TAG, "Condition: 3 stairs - Completed with some help")
                3
            }

            // 1 STAIR: NEEDS PRACTICE - Struggled or low performance
            else -> {
                Log.d(TAG, "Condition: 1 stair - Needs more practice")
                1
            }
        }

        // Debug evaluation
        Log.d(TAG, "=== DEBUG EVALUATION ===")
        Log.d(TAG, "Attempts == 1 && errors == 0 && finalAlpha >= 7.0: ${attempts == 1 && errors == 0 && finalAlpha >= 7.0f}")
        Log.d(TAG, "finalAlpha >= 8.0 && errors <= 1: ${finalAlpha >= 8.0f && errors <= 1}")
        Log.d(TAG, "finalAlpha >= 5.0 && errors <= 2: ${finalAlpha >= 5.0f && errors <= 2}")
        Log.d(TAG, "finalAlpha >= 3.0 && attempts <= 2 && errors <= 3: ${finalAlpha >= 3.0f && attempts <= 2 && errors <= 3}")
        Log.d(TAG, "========================")

        // Safety check: Ensure totalSteps is valid
        totalSteps = totalSteps.coerceIn(1, 5)

        starFinalStep = totalSteps - 1
        Log.d(TAG, "Final decision: $totalSteps steps, Star at step: $starFinalStep")
    }

    private fun buildStaircase() {
        val themeColor = when(totalSteps) {
            1 -> Color.parseColor("#FF6B8B") // Pink
            3 -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#4CAF50") // Green
        }

        for (i in 0 until totalSteps) {
            val block = stepBlocks[i]
            block.setColorFilter(themeColor)
            block.alpha = 0f
            block.translationY = -1000f // Fall from top
            block.visibility = View.VISIBLE
            stepLabels[i].visibility = View.INVISIBLE
        }
    }

    private fun calculateStarProgress() {
        starFinalStep = totalSteps - 1
    }

    private fun updateTherapeuticMessage() {
        val message = when (totalSteps) {
            1 -> "Great start!"
            3 -> "Good job!"
            5 -> "Excellent work!"
            else -> "Well done!"
        }
        therapeuticMessage.text = message
        therapeuticMessage.alpha = 0f
        therapeuticMessage.visibility = View.VISIBLE

        // Animate the therapeutic message
        therapeuticMessage.animate()
            .alpha(1f)
            .setDuration(800)
            .start()
    }

    private fun hideExtraStairs() {
        for (i in totalSteps until stepBlocks.size) {
            stepBlocks[i].visibility = View.GONE
            stepLabels[i].visibility = View.GONE
        }
    }

    private fun startTherapeuticAnimationSequence() {
        updateTherapeuticMessage()
        Handler(Looper.getMainLooper()).postDelayed({
            dropBlocksSequence()
        }, 1000)
    }

    private fun dropBlocksSequence() {
        blockDropAnimators.clear()

        for (i in 0 until totalSteps) {
            Handler().postDelayed({
                animateBlockDrop(i)
            }, i * 400L)
        }

        Handler().postDelayed({
            introduceStar()
        }, (totalSteps * 400L) + 600)
    }

    private fun animateBlockDrop(stepIndex: Int) {
        val block = stepBlocks[stepIndex]

        // Reduced from 600f to 200f to move the whole staircase higher up
        val baseY = 200f
        val targetY = baseY - (stepIndex * (blockHeight + verticalSpacing)).toFloat()
        val targetX = 20f + (stepIndex * blockSpacing).toFloat()

        val dropAnim = ObjectAnimator.ofFloat(block, "translationY", -1000f, targetY)
        dropAnim.duration = 800
        dropAnim.interpolator = BounceInterpolator()

        val slideAnim = ObjectAnimator.ofFloat(block, "translationX", targetX)
        slideAnim.duration = 600

        val fadeInAnim = ObjectAnimator.ofFloat(block, "alpha", 0f, 1f)
        fadeInAnim.duration = 400

        val animSet = AnimatorSet()
        animSet.playTogether(dropAnim, slideAnim, fadeInAnim)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                blockDropSound.seekTo(0)
                blockDropSound.setVolume(0.4f, 0.4f)
                blockDropSound.start()
            }
            override fun onAnimationEnd(animation: Animator) {
                val bounceAnim = ObjectAnimator.ofFloat(block, "translationY", targetY - 5, targetY)
                bounceAnim.duration = 300
                bounceAnim.interpolator = BounceInterpolator()
                bounceAnim.start()
            }
        })
        animSet.start()
        blockDropAnimators.add(animSet)
    }

    private fun introduceStar() {
        if (starFinalStep < 0 || starFinalStep >= stepBlocks.size) return
        val targetBlock = stepBlocks[starFinalStep]

        // Position the star
        val finalX = targetBlock.x + (targetBlock.width / 4)
        val finalY = targetBlock.y - starCharacter.height - 40f

        starCharacter.x = finalX
        starCharacter.y = finalY
        starCharacter.visibility = View.VISIBLE
        starCharacter.alpha = 0f

        // Ensure motivational quote is properly initialized
        motivationalQuote.alpha = 0f
        motivationalQuote.visibility = View.VISIBLE

        // Animation Phase A: Appear and Swirl in place
        val appearAnim = AnimatorSet()
        appearAnim.playTogether(
            ObjectAnimator.ofFloat(starCharacter, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(starCharacter, "scaleX", 0f, 1.2f),
            ObjectAnimator.ofFloat(starCharacter, "scaleY", 0f, 1.2f),
            ObjectAnimator.ofFloat(starCharacter, "rotation", 0f, 720f)
        )
        appearAnim.duration = 1000
        appearAnim.interpolator = DecelerateInterpolator()

        // Animation Phase B: Bounce Twice in place
        val bounceAnim = ObjectAnimator.ofFloat(starCharacter, "y", finalY, finalY - 50f, finalY)
        bounceAnim.duration = 600
        bounceAnim.repeatCount = 1
        bounceAnim.interpolator = AccelerateDecelerateInterpolator()

        val fullSequence = AnimatorSet()
        fullSequence.playSequentially(appearAnim, bounceAnim)

        fullSequence.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                starBounceSound.seekTo(0)
                starBounceSound.start()
            }

            override fun onAnimationEnd(animation: Animator) {
                // Show quote immediately after star animation
                Handler(Looper.getMainLooper()).postDelayed({
                    showQuoteForStep(starFinalStep)
                }, 300)

                // Finalize state
                settleStar()
            }
        })

        fullSequence.start()
    }

    private fun settleStar() {
        if (starFinalStep >= 0 && starFinalStep < stepBlocks.size) {
            // Highlight all reached steps with a sparkle
            for (i in 0..starFinalStep) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val block = stepBlocks[i]
                    val sparkleAnim = AnimatorSet()
                    sparkleAnim.playTogether(
                        ObjectAnimator.ofFloat(block, "scaleX", 1f, 1.1f),
                        ObjectAnimator.ofFloat(block, "scaleY", 1f, 1.1f)
                    )
                    sparkleAnim.duration = 200

                    val resetAnim = AnimatorSet()
                    resetAnim.playTogether(
                        ObjectAnimator.ofFloat(block, "scaleX", 1.1f, 1f),
                        ObjectAnimator.ofFloat(block, "scaleY", 1.1f, 1f)
                    )
                    resetAnim.duration = 200

                    val sequence = AnimatorSet()
                    sequence.playSequentially(sparkleAnim, resetAnim)
                    sequence.start()
                }, i * 150L)
            }

            // Play completion sound
            completionSound.seekTo(0)
            completionSound.start()

            // Start the idle breathing animation
            startStarGlow()

            // Allow user to leave
            enableNavigationButtons()
        }
    }

    private fun showQuoteForStep(step: Int) {
        val quotes = motivationalQuotes[totalSteps] ?: return

        // Use step index to get the appropriate quote
        val quoteIndex = min(step, quotes.size - 1)
        motivationalQuote.text = quotes[quoteIndex]
        motivationalQuote.visibility = View.VISIBLE
        motivationalQuote.alpha = 0f

        // Wait for layout to complete before positioning
        motivationalQuote.post {
            // Position the quote above the star
            val quoteHeight = motivationalQuote.measuredHeight
            val quoteWidth = motivationalQuote.measuredWidth
            val starWidth = starCharacter.width

            // Center the quote horizontally relative to the star
            val centerX = starCharacter.x + (starWidth / 2) - (quoteWidth / 2)

            // Position the quote above the star with some spacing
            val quotePosY = starCharacter.y - quoteHeight - 20f

            motivationalQuote.x = centerX
            motivationalQuote.y = quotePosY

            Log.d(TAG, "Quote positioning - Star: (${starCharacter.x}, ${starCharacter.y}), Quote: ($centerX, $quotePosY)")

            val fadeIn = ObjectAnimator.ofFloat(motivationalQuote, "alpha", 0f, 1f)
            fadeIn.duration = 800
            fadeIn.start()

            Log.d(TAG, "Showing quote: '${quotes[quoteIndex]}' at step $step (totalSteps: $totalSteps)")
        }
    }

    private fun startStarGlow() {
        // Continuous glowing animation
        starGlowAnimator = ObjectAnimator.ofFloat(starCharacter, "scaleX", 1.2f, 1.3f, 1.2f)
        starGlowAnimator?.duration = 1000
        starGlowAnimator?.repeatCount = ValueAnimator.INFINITE
        starGlowAnimator?.repeatMode = ValueAnimator.REVERSE
        starGlowAnimator?.start()

        val glowY = ObjectAnimator.ofFloat(starCharacter, "scaleY", 1.2f, 1.3f, 1.2f)
        glowY.duration = 1000
        glowY.repeatCount = ValueAnimator.INFINITE
        glowY.repeatMode = ValueAnimator.REVERSE
        glowY.start()
    }

    private fun enableNavigationButtons() {
        val continueFade = ObjectAnimator.ofFloat(btnContinue, "alpha", 0f, 1f)
        continueFade.duration = 500
        continueFade.start()

        val homeFade = ObjectAnimator.ofFloat(btnHome, "alpha", 0f, 1f)
        homeFade.duration = 500
        homeFade.start()

        btnContinue.isEnabled = true
        btnHome.isEnabled = true
    }

    private fun setupButtonListeners() {
        btnContinue.alpha = 0f
        btnHome.alpha = 0f
        btnContinue.isEnabled = false
        btnHome.isEnabled = false

        btnContinue.setOnClickListener {
            val pressAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnContinue,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            )
            pressAnim.duration = 200
            pressAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val intent = Intent(this@ASequenceScoreboardActivity, ActivitySequenceUnderActivity::class.java)
                    intent.putExtra("PREVIOUS_ALPHA", finalAlpha)
                    intent.putExtra("PREVIOUS_CORRECT", correctCount)
                    intent.putExtra("PREVIOUS_SUBROUTINE", previousSubroutine)
                    intent.putExtra("PREVIOUS_ERROR", previousError)
                    intent.putExtra("SELECTED_ROUTINE_ID", userSelectedRoutine)
                    intent.putExtra("USER_AGE", userAge)
                    startActivity(intent)
                    finish()
                }
            })
            pressAnim.start()
        }

        btnHome.setOnClickListener {
            val pressAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnHome,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            )
            pressAnim.duration = 200
            pressAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val intent = Intent(this@ASequenceScoreboardActivity, RoutineSelectionActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            })
            pressAnim.start()
        }
    }

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