package com.chirathi.voicebridge

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

    // Staircase configuration
    private val totalSteps = 5
    private var starFinalStep = 0
    private lateinit var starCharacter: ImageView
    private val stepBlocks = mutableListOf<ImageView>()
    private val stepLabels = mutableListOf<TextView>()
    private val stepMessages = listOf(
        "Step by step!",
        "Going up!",
        "Moving along!",
        "So close!",
        "We did it!"
    )

    // Therapeutic colors
    private val blockColors = listOf(
        Color.parseColor("#FF6B8B"), // Pink
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#FF6B8B"), // Pink
        Color.parseColor("#4CAF50")  // Green
    )

    // UI Elements
    private lateinit var staircaseContainer: ConstraintLayout
    private lateinit var therapeuticMessage: TextView
    private lateinit var alphaDisplay: TextView
    private lateinit var progressDescription: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnHome: Button

    // Animations
    private var starGlowAnimator: ObjectAnimator? = null
    private var starBounceAnimator: ObjectAnimator? = null
    private lateinit var blockDropAnimators: MutableList<AnimatorSet>

    // Sound effects
    private lateinit var blockDropSound: MediaPlayer
    private lateinit var starBounceSound: MediaPlayer
    private lateinit var completionSound: MediaPlayer

    companion object {
        private const val TAG = "StarStaircase"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        // Get therapeutic data
        finalAlpha = intent.getFloatExtra("FINAL_ALPHA", 0f)
        poppedBubbles = intent.getIntExtra("POPPED_BUBBLES", 0)
        totalBubbles = intent.getIntExtra("TOTAL_BUBBLES", 5)
        correctCount = intent.getIntExtra("CORRECT_COUNT", 0)
        errorCount = intent.getIntExtra("ERROR_COUNT", 0)
        completedSubroutines = intent.getIntExtra("COMPLETED_SUBROUTINES", 0)

        Log.d(TAG, "Therapeutic Summary: Alpha=$finalAlpha, Bubbles=$poppedBubbles/$totalBubbles")

        // Initialize sound effects
        initializeSounds()

        // Initialize views
        initializeViews()

        // Calculate star's final position based on alpha
        calculateStarProgress()

        // Build therapeutic staircase
        buildStaircase()

        // Start therapeutic animation sequence
        startTherapeuticAnimationSequence()

        // Setup button listeners
        setupButtonListeners()
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
        alphaDisplay = findViewById(R.id.alphaDisplay)
        progressDescription = findViewById(R.id.progressDescription)
        btnContinue = findViewById(R.id.btnContinue)
        btnHome = findViewById(R.id.btnHome)
        starCharacter = findViewById(R.id.starCharacter)

        // Initialize step blocks
        stepBlocks.add(findViewById(R.id.stepBlock1))
        stepBlocks.add(findViewById(R.id.stepBlock2))
        stepBlocks.add(findViewById(R.id.stepBlock3))
        stepBlocks.add(findViewById(R.id.stepBlock4))
        stepBlocks.add(findViewById(R.id.stepBlock5))

        // Initialize step labels
        stepLabels.add(findViewById(R.id.stepLabel1))
        stepLabels.add(findViewById(R.id.stepLabel2))
        stepLabels.add(findViewById(R.id.stepLabel3))
        stepLabels.add(findViewById(R.id.stepLabel4))
        stepLabels.add(findViewById(R.id.stepLabel5))
    }

    private fun calculateStarProgress() {
        // MODEL-DRIVEN STAR PROGRESS CALCULATION

        // Therapeutic progress considers multiple factors:
        // 1. Alpha score (primary) - 60% weight
        // 2. Bubble completion - 20% weight
        // 3. Correct/error ratio - 20% weight

        val alphaFactor = (finalAlpha / 10f) * 0.6f
        val bubbleFactor = (poppedBubbles.toFloat() / totalBubbles) * 0.2f
        val accuracyFactor = if ((correctCount + errorCount) > 0) {
            (correctCount.toFloat() / (correctCount + errorCount)) * 0.2f
        } else 0f

        val therapeuticProgress = (alphaFactor + bubbleFactor + accuracyFactor)

        // Map to staircase steps (0-4)
        starFinalStep = (therapeuticProgress * totalSteps).toInt().coerceIn(0, totalSteps - 1)

        Log.d(TAG, "Progress Factors: Alpha=$alphaFactor, Bubbles=$bubbleFactor, Accuracy=$accuracyFactor")
        Log.d(TAG, "Therapeutic Progress: $therapeuticProgress, Star Step: $starFinalStep")

        // Set therapeutic message based on progress
        updateTherapeuticMessage(therapeuticProgress)
    }

    private fun updateTherapeuticMessage(progress: Float) {
        val message = when {
            progress >= 0.8 -> "Therapeutic breakthrough! 🌟\nYou're showing excellent progress!"
            progress >= 0.6 -> "Great therapeutic session! 🎯\nSolid skills demonstrated!"
            progress >= 0.4 -> "Good therapeutic effort! 📚\nLearning and growing!"
            progress >= 0.2 -> "Therapeutic journey continues! 🌈\nEvery step matters!"
            else -> "Therapeutic exploration! 🎪\nStarting the adventure!"
        }

        therapeuticMessage.text = message

        // Update alpha display with therapeutic interpretation
        val alphaText = when {
            finalAlpha >= 8.0 -> "Excellent Therapeutic Score: ${"%.1f".format(finalAlpha)}/10"
            finalAlpha >= 6.0 -> "Good Therapeutic Score: ${"%.1f".format(finalAlpha)}/10"
            finalAlpha >= 4.0 -> "Developing Therapeutic Score: ${"%.1f".format(finalAlpha)}/10"
            else -> "Beginning Therapeutic Score: ${"%.1f".format(finalAlpha)}/10"
        }

        alphaDisplay.text = alphaText

        // Progress description
        progressDescription.text = buildString {
            append("Completed $completedSubroutines activities\n")
            append("Popped $poppedBubbles/$totalBubbles bubbles\n")
            append("${correctCount} correct, ${errorCount} learning moments")
        }
    }

    private fun buildStaircase() {
        // Set up each step block with therapeutic colors and labels
        for (i in 0 until totalSteps) {
            val block = stepBlocks[i]
            val label = stepLabels[i]

            // Set block color
            block.setColorFilter(blockColors[i])

            // Set step message
            label.text = stepMessages[i]

            // Initial hidden state for animation
            block.alpha = 0f
            block.translationY = -500f
            label.alpha = 0f

            // Set therapeutic tag
            block.tag = "step_${i + 1}"
            label.tag = "label_${i + 1}"
        }

        // Position star at bottom initially
        starCharacter.alpha = 0f
        starCharacter.translationY = 100f
    }

    private fun startTherapeuticAnimationSequence() {
        // Therapeutic animation sequence:
        // 1. Drop blocks one by one
        // 2. Introduce star
        // 3. Star climbs staircase
        // 4. Star settles and glows

        Handler(Looper.getMainLooper()).postDelayed({
            dropBlocksSequence()
        }, 500)
    }

    private fun dropBlocksSequence() {
        blockDropAnimators = mutableListOf()

        for (i in 0 until totalSteps) {
            Handler().postDelayed({
                animateBlockDrop(i)
            }, i * 600L) // Therapeutic pacing
        }

        // After blocks drop, introduce star
        Handler().postDelayed({
            introduceStar()
        }, (totalSteps * 600L) + 500)
    }

    private fun animateBlockDrop(stepIndex: Int) {
        val block = stepBlocks[stepIndex]
        val label = stepLabels[stepIndex]

        val dropAnim = ObjectAnimator.ofFloat(block, "translationY", 0f)
        dropAnim.duration = 800
        dropAnim.interpolator = BounceInterpolator()

        val fadeInAnim = ObjectAnimator.ofFloat(block, "alpha", 1f)
        fadeInAnim.duration = 400

        val labelFadeAnim = ObjectAnimator.ofFloat(label, "alpha", 1f)
        labelFadeAnim.duration = 600
        labelFadeAnim.startDelay = 300

        val animSet = AnimatorSet()
        animSet.playTogether(dropAnim, fadeInAnim)
        animSet.play(labelFadeAnim).after(dropAnim)

        animSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                // Play block drop sound
                blockDropSound.seekTo(0)
                blockDropSound.start()
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Gentle wiggle after landing
                block.animate()
                    .rotationBy(5f)
                    .setDuration(200)
                    .withEndAction {
                        block.animate()
                            .rotationBy(-5f)
                            .setDuration(200)
                            .start()
                    }
                    .start()
            }
        })

        animSet.start()
        blockDropAnimators.add(animSet)
    }

    private fun introduceStar() {
        // Star entrance animation
        starCharacter.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(800)
            .withEndAction {
                starCharacter.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .withEndAction {
                        // Start star climb after introduction
                        Handler().postDelayed({
                            climbStaircase()
                        }, 800)
                    }
                    .start()
            }
            .start()

        // Play star entrance sound
        starBounceSound.seekTo(0)
        starBounceSound.start()
    }

    private fun climbStaircase() {
        // Therapeutic climb: star bounces up each step

        var currentStep = 0

        fun climbNextStep() {
            if (currentStep <= starFinalStep) {
                // Calculate target position (above the block)
                val block = stepBlocks[currentStep]
                val targetY = block.y - 200f // Position above block

                // Bounce animation to next step
                val bounceUp = ObjectAnimator.ofFloat(starCharacter, "y", targetY)
                bounceUp.duration = 600
                bounceUp.interpolator = AccelerateDecelerateInterpolator()

                // Gentle rotation during bounce
                val rotateAnim = ObjectAnimator.ofFloat(starCharacter, "rotation", 0f, 360f)
                rotateAnim.duration = 600

                val stepAnimSet = AnimatorSet()
                stepAnimSet.playTogether(bounceUp, rotateAnim)

                stepAnimSet.addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: android.animation.Animator) {
                        // Highlight current step
                        stepBlocks[currentStep].animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(300)
                            .start()

                        stepLabels[currentStep].animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(300)
                            .start()
                    }

                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Play bounce sound
                        starBounceSound.seekTo(0)
                        starBounceSound.start()

                        // Return step to normal
                        stepBlocks[currentStep].animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()

                        stepLabels[currentStep].animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()

                        currentStep++

                        // Continue to next step or settle
                        if (currentStep <= starFinalStep) {
                            Handler().postDelayed({
                                climbNextStep()
                            }, 400)
                        } else {
                            Handler().postDelayed({
                                settleStar()
                            }, 600)
                        }
                    }
                })

                stepAnimSet.start()
            }
        }

        // Start climbing
        climbNextStep()
    }

    private fun settleStar() {
        // Star settles into therapeutic resting pose

        // Final gentle landing
        val settleY = stepBlocks[starFinalStep].y - 150f // Slightly above the block
        starCharacter.animate()
            .y(settleY)
            .setDuration(500)
            .interpolator = OvershootInterpolator(0.5f)

        // Start therapeutic glowing
        startStarGlow()

        // Play completion sound
        completionSound.seekTo(0)
        completionSound.start()

        // Show final therapeutic message
        showFinalMessage()

        // Enable buttons
        enableNavigationButtons()
    }

    private fun startStarGlow() {
        // Therapeutic glowing animation
        starGlowAnimator = ObjectAnimator.ofPropertyValuesHolder(
            starCharacter,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0.9f, 1f)
        ).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Add gentle floating
        starBounceAnimator = ObjectAnimator.ofFloat(
            starCharacter,
            "translationY",
            0f, -20f, 0f, 10f, 0f
        ).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun showFinalMessage() {
        val finalMessage = when (starFinalStep) {
            4 -> "Therapeutic achievement unlocked! 🎉\nYou reached the top!"
            3 -> "Great therapeutic progress! 🌟\nAlmost at the top!"
            2 -> "Good therapeutic session! 📚\nHalfway up the staircase!"
            1 -> "Therapeutic effort noted! 🌈\nSecond step reached!"
            else -> "Therapeutic journey begins! 🎪\nFirst step accomplished!"
        }

        therapeuticMessage.text = finalMessage

        // Add sparkle effect to reached steps
        for (i in 0..starFinalStep) {
            Handler().postDelayed({
                stepBlocks[i].animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(300)
                    .withEndAction {
                        stepBlocks[i].animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }, i * 200L)
        }
    }

    private fun enableNavigationButtons() {
        btnContinue.alpha = 1f
        btnHome.alpha = 1f
        btnContinue.isEnabled = true
        btnHome.isEnabled = true

        // Therapeutic button animations
        btnContinue.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .start()

        btnHome.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .start()
    }

    private fun setupButtonListeners() {
        // Initial disabled state
        btnContinue.alpha = 0.5f
        btnHome.alpha = 0.5f
        btnContinue.isEnabled = false
        btnHome.isEnabled = false

        btnContinue.setOnClickListener {
            btnContinue.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnContinue.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // Continue to next therapeutic session
                            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)

                            // Pass therapeutic progress to next session
                            // The model will decide whether to repeat or progress
                            intent.putExtra("PREVIOUS_ALPHA", finalAlpha)
                            intent.putExtra("PREVIOUS_CORRECT", correctCount)
                            intent.putExtra("PREVIOUS_ERROR", if (errorCount > 0) "positional" else "none")
                            intent.putExtra("CONTINUE_THERAPY", true)

                            // Keep the same user-selected routine
                            intent.putExtra("SELECTED_ROUTINE_ID", 0) // Or pass from intent
                            intent.putExtra("USER_AGE", 6) // Or pass from intent

                            startActivity(intent)
                            finish()
                        }
                        .start()
                }
                .start()
        }

        btnHome.setOnClickListener {
            btnHome.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    btnHome.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            // Return to therapeutic dashboard
                            val intent = Intent(this, GameDashboardActivity::class.java)

                            // Pass therapeutic summary
                            intent.putExtra("THERAPEUTIC_SUMMARY", true)
                            intent.putExtra("SESSION_ALPHA", finalAlpha)
                            intent.putExtra("STAR_STEP", starFinalStep)
                            intent.putExtra("BUBBLES_POPPED", poppedBubbles)

                            startActivity(intent)
                            finish()
                        }
                        .start()
                }
                .start()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause animations safely
        starGlowAnimator?.pause()
        starBounceAnimator?.pause()

        // Pause sounds
        blockDropSound.pause()
        starBounceSound.pause()
        completionSound.pause()
    }

    override fun onResume() {
        super.onResume()
        // Resume animations safely
        starGlowAnimator?.resume()
        starBounceAnimator?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up animations safely
        starGlowAnimator?.cancel()
        starBounceAnimator?.cancel()
        blockDropAnimators?.forEach { it.cancel() }

        // Release sounds
        blockDropSound.release()
        starBounceSound.release()
        completionSound.release()
    }
}