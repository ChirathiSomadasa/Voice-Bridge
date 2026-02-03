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
import androidx.core.content.ContextCompat

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
    private val stepLabels = mutableListOf<TextView>()

    // Different messages for different step levels
    private val stepMessages = mapOf(
        1 to listOf("Step 1"),
        3 to listOf("Step 1", "Step 2", "Step 3"),
        5 to listOf("Step 1", "Step 2", "Step 3", "Step 4", "Step 5")
    )

    // Different motivational quotes for different levels
    private val motivationalQuotes = mapOf(
        1 to listOf("Great start! Every journey begins with a single step! 🚶"),
        3 to listOf(
            "First step taken! Building momentum! 🚀",
            "Second step! Making progress! 📈",
            "Third step! Reaching new heights! 🏔️"
        ),
        5 to listOf(
            "Starting strong! First step complete! 💪",
            "Building momentum! Second step done! 🚀",
            "Halfway there! Third step conquered! 🎯",
            "Almost at the peak! Fourth step achieved! ⛰️",
            "Therapeutic victory! Top of the stairs! 🏆"
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
    private val blockWidth = 280 // Width of bottom block
    private val blockHeight = 60 // Height of each block
    private val blockSpacing = 60 // Horizontal spacing for stairs
    private val verticalSpacing = 60 // Vertical spacing between stairs

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
            // This should only happen if sticker logic failed
            finalAlpha >= 9.0f && avgResponseTime < 6000 && attempts == 1 -> 5

            // 5 steps for very good performance
            finalAlpha >= 7.0f || (finalAlpha >= 5.0f && avgResponseTime < 8000) -> 5

            // 3 steps for good performance
            finalAlpha >= 4.0f || avgResponseTime < 15000 -> 3

            // 1 step for basic participation
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

            // Verses are HIDDEN initially
            stepLabels[i].visibility = View.INVISIBLE
        }

        // Hide numeric values as requested
//        alphaDisplay.visibility = View.GONE
//        progressDescription.visibility = View.GONE
    }

    private fun climbStaircase() {
        var currentStep = 0

        fun jumpToNextStep() {
            if (currentStep <= starFinalStep) {
                val targetBlock = stepBlocks[currentStep]

                // Get position of the block relative to its parent
                val targetX = targetBlock.x + (targetBlock.width / 4)
                val targetY = targetBlock.y - starCharacter.height + 10f

                // Create the Arc Jump
                val jumpX = ObjectAnimator.ofFloat(starCharacter, "x", targetX)
                val jumpY = ObjectAnimator.ofFloat(starCharacter, "y", starCharacter.y - 150f, targetY)

                jumpY.interpolator = DecelerateInterpolator()
                jumpX.duration = 700

                val jumpSet = AnimatorSet()
                jumpSet.playTogether(jumpX, jumpY)

                jumpSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        starBounceSound.start()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        // IF THIS IS THE FINAL STEP, SHOW THE VERSE
                        if (currentStep == starFinalStep) {
                            showFinalVerse(currentStep)
                        }
                        currentStep++
                        Handler(Looper.getMainLooper()).postDelayed({
                            jumpToNextStep()
                        }, 300)
                    }
                })
                jumpSet.start()
            }
        }
        jumpToNextStep()
    }

    private fun showFinalVerse(stepIndex: Int) {
        val finalLabel = stepLabels[stepIndex]
        finalLabel.visibility = View.VISIBLE
        finalLabel.alpha = 0f
        finalLabel.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator())
            .start()

        // FIX: Access the message from your map using the totalSteps key
        val messages = stepMessages[totalSteps]
        if (messages != null && stepIndex < messages.size) {
            // This sets the text on the little badge above the block
            finalLabel.text = messages[stepIndex]
        }
    }

    private fun calculateStarProgress() {
        // Star climbs to the top of whatever steps are shown
        starFinalStep = totalSteps - 1  // 0-indexed, so top step is totalSteps-1
        Log.d(TAG, "Star will climb to step $starFinalStep of $totalSteps stairs")
    }

    private fun updateTherapeuticMessage() {
        val message = when (totalSteps) {
            1 -> "Great start! You completed 1 step!"
            3 -> "Good job! You mastered 3 steps!"
            5 -> "Excellent work! You conquered all 5 steps!"
            else -> "Well done!"
        }

        therapeuticMessage.text = message

        // Hide the quote initially, it will show during climbing
        motivationalQuote.text = ""
    }

    private fun getBlockColor(): Int {
        return when (totalSteps) {
            1 -> lowAccuracyColor
            3 -> mediumAccuracyColor
            5 -> highAccuracyColor
            else -> highAccuracyColor
        }
    }


    private fun hideExtraStairs() {
        // Hide stairs beyond the totalSteps count
        for (i in totalSteps until stepBlocks.size) {
            stepBlocks[i].visibility = View.GONE
            stepLabels[i].visibility = View.GONE
        }
    }

    private fun startTherapeuticAnimationSequence() {
        // Update messages before animation starts
        updateTherapeuticMessage()

        // Therapeutic animation sequence:
        // 1. Drop blocks one by one in stair pattern
        // 2. Introduce star
        // 3. Star climbs staircase step by step
        // 4. Star settles and shows final quote

        Handler(Looper.getMainLooper()).postDelayed({
            dropBlocksSequence()
        }, 1000)
    }

    private fun dropBlocksSequence() {
        // Clear previous animators
        blockDropAnimators.clear()

        for (i in 0 until totalSteps) {
            Handler().postDelayed({
                animateBlockDrop(i)
            }, i * 600L) // Staggered drop
        }

        // After blocks drop, introduce star
        Handler().postDelayed({
            introduceStar()
        }, (totalSteps * 600L) + 800)
    }

    private fun animateBlockDrop(stepIndex: Int) {
        val block = stepBlocks[stepIndex]
        val label = stepLabels[stepIndex]

        // Calculate target Y position for stair pattern
        // Each block is positioned higher than the previous one
        val targetY = (totalSteps - stepIndex - 1) * (blockHeight + verticalSpacing).toFloat()

        // Keep the X offset for stair pattern
        val targetX = stepIndex * blockSpacing.toFloat()

        // Drop animation with bounce
        val dropAnim = ObjectAnimator.ofFloat(block, "translationY", targetY)
        dropAnim.duration = 1000
        dropAnim.interpolator = BounceInterpolator()

        // X position animation (for stair offset)
        val slideAnim = ObjectAnimator.ofFloat(block, "translationX", targetX)
        slideAnim.duration = 800
        slideAnim.interpolator = DecelerateInterpolator()

        val fadeInAnim = ObjectAnimator.ofFloat(block, "alpha", 1f)
        fadeInAnim.duration = 500

        val labelFadeAnim = ObjectAnimator.ofFloat(label, "alpha", 1f)
        labelFadeAnim.duration = 600
        labelFadeAnim.startDelay = 300

        // Position label above the block
        label.translationY = targetY - 40
        label.translationX = targetX + 20

        val animSet = AnimatorSet()
        animSet.playTogether(dropAnim, slideAnim, fadeInAnim)
        animSet.play(labelFadeAnim).after(dropAnim)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // Play block drop sound
                blockDropSound.seekTo(0)
                blockDropSound.setVolume(0.4f, 0.4f)
                blockDropSound.start()
            }

            override fun onAnimationEnd(animation: Animator) {
                // Small bounce effect after landing
                val bounceAnim = ObjectAnimator.ofFloat(block, "translationY", targetY - 10, targetY)
                bounceAnim.duration = 400
                bounceAnim.interpolator = BounceInterpolator()
                bounceAnim.start()
            }
        })

        animSet.start()
        blockDropAnimators.add(animSet)
    }

    private fun introduceStar() {
        // Star entrance animation from bottom left
        val startX = -100f
        val startY = (totalSteps * (blockHeight + verticalSpacing)) + 200f

        starCharacter.translationX = startX
        starCharacter.translationY = startY

        val starEntrance = ObjectAnimator.ofPropertyValuesHolder(
            starCharacter,
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1.5f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1.5f),
            android.animation.PropertyValuesHolder.ofFloat("rotation", 0f, 360f)
        )
        starEntrance.duration = 1000
        starEntrance.interpolator = OvershootInterpolator(1.5f)
        starEntrance.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Play star entrance sound
                starBounceSound.seekTo(0)
                starBounceSound.start()

                // After entrance, start climbing
                Handler().postDelayed({
                    climbStaircase()
                }, 500)
            }
        })
        starEntrance.start()
    }

    private fun showQuoteForStep(step: Int) {
        val quotes = motivationalQuotes[totalSteps] ?: return

        if (step < quotes.size) {
            motivationalQuote.text = quotes[step]

            val fadeIn = ObjectAnimator.ofFloat(motivationalQuote, "alpha", 0f, 1f)
            fadeIn.duration = 300
            fadeIn.start()
        }
    }

    private fun settleStar() {
        // Star settles on the top step
        val topBlock = stepBlocks[starFinalStep]
        val finalX = topBlock.translationX + 40
        val finalY = topBlock.translationY - 50f

        // Final bounce and celebration
        val settleAnim = ObjectAnimator.ofFloat(starCharacter, "translationY", finalY - 20, finalY)
        settleAnim.duration = 600
        settleAnim.interpolator = BounceInterpolator()
        settleAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                startStarGlow()
                enableNavigationButtons()
            }
        })
        settleAnim.start()

        // Highlight all reached steps
        for (i in 0..starFinalStep) {
            Handler().postDelayed({
                val block = stepBlocks[i]
                val sparkleAnim = AnimatorSet()

                val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
                    block,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f)
                )
                scaleUp.duration = 200

                val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                    block,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.15f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.15f, 1f)
                )
                scaleDown.duration = 200

                sparkleAnim.playSequentially(scaleUp, scaleDown)
                sparkleAnim.start()
            }, i * 150L)
        }

        // Play completion sound
        completionSound.seekTo(0)
        completionSound.start()
    }

    private fun startStarGlow() {
        // Continuous glowing animation
        starGlowAnimator = ObjectAnimator.ofPropertyValuesHolder(
            starCharacter,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1.5f, 1.6f, 1.5f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1.5f, 1.6f, 1.5f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0.9f, 1f)
        )
        starGlowAnimator?.duration = 1200
        starGlowAnimator?.repeatCount = ValueAnimator.INFINITE
        starGlowAnimator?.repeatMode = ValueAnimator.REVERSE
        starGlowAnimator?.interpolator = AccelerateDecelerateInterpolator()
        starGlowAnimator?.start()

        // Add gentle floating movement
        starBounceAnimator = ObjectAnimator.ofFloat(
            starCharacter,
            "translationY",
            0f, -15f, 5f, -10f, 0f
        )
        starBounceAnimator?.duration = 3000
        starBounceAnimator?.repeatCount = ValueAnimator.INFINITE
        starBounceAnimator?.repeatMode = ValueAnimator.RESTART
        starBounceAnimator?.interpolator = LinearInterpolator()
        starBounceAnimator?.start()
    }

    private fun enableNavigationButtons() {
        // Fade in buttons
        val continueFade = ObjectAnimator.ofFloat(btnContinue, "alpha", 0f, 1f)
        continueFade.duration = 600
        continueFade.start()

        val homeFade = ObjectAnimator.ofFloat(btnHome, "alpha", 0f, 1f)
        homeFade.duration = 600
        homeFade.start()

        btnContinue.isEnabled = true
        btnHome.isEnabled = true
    }

    private fun setupButtonListeners() {
        // Initial disabled and hidden state
        btnContinue.alpha = 0f
        btnHome.alpha = 0f
        btnContinue.isEnabled = false
        btnHome.isEnabled = false

        btnContinue.setOnClickListener {
            // Button press animation
            val pressAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnContinue,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            )
            pressAnim.duration = 200
            pressAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Continue to next session
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
            // Button press animation
            val pressAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnHome,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.95f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.95f, 1f)
            )
            pressAnim.duration = 200
            pressAnim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Return to routine selection
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