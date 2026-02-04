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

    // Block colors based on performance
    private val highAccuracyColor = Color.parseColor("#4CAF50")  // Green
    private val mediumAccuracyColor = Color.parseColor("#FF9800") // Orange
    private val lowAccuracyColor = Color.parseColor("#FF6B8B")   // Pink

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

        Log.d(TAG, "Stair calculation - Alpha: $finalAlpha, Time: ${avgResponseTime}ms, Attempts: $attempts")

        // Determine steps based on performance (NOT just alpha)
        totalSteps = when {
            finalAlpha >= 9.0f && avgResponseTime < 6000 && attempts == 1 -> 5
            finalAlpha >= 7.0f || (finalAlpha >= 5.0f && avgResponseTime < 8000) -> 5
            finalAlpha >= 4.0f || avgResponseTime < 15000 -> 3
            else -> 1
        }

        starFinalStep = totalSteps - 1
        Log.d(TAG, "Staircase: $totalSteps steps")
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
        motivationalQuote.text = ""
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
        // val label = stepLabels[stepIndex] // Unused

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

        // Labels disabled based on request
        // label.translationY = targetY - 30
        // label.translationX = targetX + (blockWidth / 2) - 30

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

        // Position the star.
        // CHANGE: Changed +20f to -40f to position the star higher above the block.
        val finalX = targetBlock.x + (targetBlock.width / 4)
        val finalY = targetBlock.y - starCharacter.height - 40f

        starCharacter.x = finalX
        starCharacter.y = finalY
        starCharacter.visibility = View.VISIBLE
        starCharacter.alpha = 0f

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
                // CHANGE: Removed the loop that showed step labels.

                // Show quote ABOVE the settled star
                showQuoteForStep(starFinalStep)

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

    // Unused method, kept to avoid compilation errors if referenced elsewhere, but not called.
    private fun showStepLabel(stepIndex: Int) {
        if (stepIndex < stepLabels.size) {
            val label = stepLabels[stepIndex]
            label.visibility = View.VISIBLE
            label.alpha = 0f
            label.animate()
                .alpha(1f)
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun showQuoteForStep(step: Int) {
        val quotes = motivationalQuotes[totalSteps] ?: return

        if (step < quotes.size) {
            motivationalQuote.text = quotes[step]
            motivationalQuote.visibility = View.VISIBLE

            // CHANGE: Dynamic positioning to place the quote above the star.
            // We use .post() to ensure the TextView has measured its new text height.
            motivationalQuote.post {
                val spacing = 30f // Space between star and quote
                // Set Y position so the bottom of the quote is above the star
                motivationalQuote.y = starCharacter.y - motivationalQuote.height - spacing
            }

            val fadeIn = ObjectAnimator.ofFloat(motivationalQuote, "alpha", 0f, 1f)
            fadeIn.duration = 600
            fadeIn.start()
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