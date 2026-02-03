package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
    private var totalSteps = 1
    private var starFinalStep = 0
    private lateinit var starCharacter: ImageView
    private val stepBlocks = mutableListOf<ImageView>()
    private val stepLabels = mutableListOf<TextView>()

    private lateinit var therapeuticMessage: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnHome: Button
    private lateinit var motivationalQuote: TextView

    private lateinit var blockDropSound: MediaPlayer
    private lateinit var starBounceSound: MediaPlayer
    private lateinit var completionSound: MediaPlayer

    companion object {
        private const val TAG = "ScoreboardActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asequence_scoreboard)

        getIntentData()
        Log.d(TAG, "Therapeutic Summary: Alpha=$finalAlpha")

        initializeSounds()
        initializeViews()
        calculateStairCount()
        buildStaircase()
        hideExtraStairs()

        Handler(Looper.getMainLooper()).postDelayed({
            startAnimationSequence()
        }, 1000)

        setupButtonListeners()
    }

    private fun getIntentData() {
        finalAlpha = intent.getFloatExtra("FINAL_ALPHA", 0f)
        // Keep other intent data as needed
    }

    private fun initializeSounds() {
        blockDropSound = MediaPlayer.create(this, R.raw.block_drop)
        starBounceSound = MediaPlayer.create(this, R.raw.star_bounce)
        completionSound = MediaPlayer.create(this, R.raw.completion_sound)

        blockDropSound.setVolume(0.3f, 0.3f)
        starBounceSound.setVolume(0.5f, 0.5f)
        completionSound.setVolume(0.7f, 0.7f)
    }

    private fun initializeViews() {
        therapeuticMessage = findViewById(R.id.therapeuticMessage)
        btnContinue = findViewById(R.id.btnContinue)
        btnHome = findViewById(R.id.btnHome)
        starCharacter = findViewById(R.id.starCharacter)
        motivationalQuote = findViewById(R.id.motivationalQuote)

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
    }

    private fun calculateStairCount() {
        totalSteps = when {
            finalAlpha >= 7.0f -> 5
            finalAlpha >= 4.0f -> 3
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
            block.visibility = View.VISIBLE
            stepLabels[i].visibility = View.INVISIBLE
        }
    }

    private fun hideExtraStairs() {
        for (i in totalSteps until stepBlocks.size) {
            stepBlocks[i].visibility = View.GONE
            stepLabels[i].visibility = View.GONE
        }
    }

    private fun startAnimationSequence() {
        updateTherapeuticMessage()
        dropBlocksSequence()
    }

    private fun dropBlocksSequence() {
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
        val label = stepLabels[stepIndex]

        // Start position (above screen)
        block.translationY = -300f

        // Animate falling down to original XML position
        val dropAnim = ObjectAnimator.ofFloat(block, "translationY", -300f, 0f)
        dropAnim.duration = 800
        dropAnim.interpolator = BounceInterpolator()

        // Fade in
        val fadeInAnim = ObjectAnimator.ofFloat(block, "alpha", 0f, 1f)
        fadeInAnim.duration = 400

        val animSet = AnimatorSet()
        animSet.playTogether(dropAnim, fadeInAnim)

        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                blockDropSound.seekTo(0)
                blockDropSound.start()
            }

            override fun onAnimationEnd(animation: Animator) {
                // Show label
                label.visibility = View.VISIBLE
                label.alpha = 0f
                label.animate().alpha(1f).setDuration(300).start()
            }
        })

        animSet.start()
    }

    private fun introduceStar() {
        // Show star
        starCharacter.visibility = View.VISIBLE
        starCharacter.alpha = 0f

        // Position star below first step (already positioned by XML)
        val starEntrance = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(starCharacter, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(starCharacter, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(starCharacter, "scaleY", 0.5f, 1f)
            )
            duration = 600
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    starBounceSound.seekTo(0)
                    starBounceSound.start()
                    Handler().postDelayed({
                        climbStaircase()
                    }, 500)
                }
            })
        }
        starEntrance.start()
    }

    private fun climbStaircase() {
        var currentStep = 0

        fun jumpToNextStep() {
            if (currentStep <= starFinalStep) {
                val targetBlock = stepBlocks[currentStep]

                // Calculate target position (on top of the step)
                val targetX = targetBlock.x + (targetBlock.width / 4)
                val targetY = targetBlock.y - starCharacter.height

                // Jump animation
                val jumpX = ObjectAnimator.ofFloat(starCharacter, "x", starCharacter.x, targetX)
                val jumpY = ObjectAnimator.ofFloat(starCharacter, "y", starCharacter.y, targetY)

                val arcAnim = ObjectAnimator.ofFloat(starCharacter, "translationY", 0f, -50f, 0f)

                val jumpSet = AnimatorSet()
                jumpSet.playTogether(jumpX, jumpY, arcAnim)
                jumpSet.duration = 600
                jumpSet.interpolator = AccelerateDecelerateInterpolator()

                jumpSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        starBounceSound.seekTo(0)
                        starBounceSound.start()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        // Show motivational quote
                        showQuoteForStep(currentStep)

                        if (currentStep == starFinalStep) {
                            Handler().postDelayed({
                                settleStar()
                            }, 300)
                        } else {
                            currentStep++
                            Handler().postDelayed({
                                jumpToNextStep()
                            }, 300)
                        }
                    }
                })
                jumpSet.start()
            }
        }
        jumpToNextStep()
    }

    private fun showQuoteForStep(step: Int) {
        val quotes = when(totalSteps) {
            1 -> listOf("Great start! Every journey begins with a single step! 🚶")
            3 -> listOf(
                "First step taken! Building momentum! 🚀",
                "Second step! Making progress! 📈",
                "Third step! Reaching new heights! 🏔️"
            )
            else -> listOf(
                "Starting strong! First step complete! 💪",
                "Building momentum! Second step done! 🚀",
                "Halfway there! Third step conquered! 🎯",
                "Almost at the peak! Fourth step achieved! ⛰️",
                "Therapeutic victory! Top of the stairs! 🏆"
            )
        }

        if (step < quotes.size) {
            motivationalQuote.text = quotes[step]
            motivationalQuote.alpha = 0f
            motivationalQuote.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun settleStar() {
        // Star bounce effect
        val settleAnim = ObjectAnimator.ofFloat(
            starCharacter,
            "translationY",
            0f, -20f, 0f
        )
        settleAnim.duration = 400
        settleAnim.interpolator = BounceInterpolator()
        settleAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                completionSound.seekTo(0)
                completionSound.start()
                enableNavigationButtons()
            }
        })
        settleAnim.start()
    }

    private fun updateTherapeuticMessage() {
        val message = when (totalSteps) {
            1 -> "Great start! You completed 1 step!"
            3 -> "Good job! You mastered 3 steps!"
            5 -> "Excellent work! You conquered all 5 steps!"
            else -> "Well done!"
        }
        therapeuticMessage.text = message
    }

    private fun enableNavigationButtons() {
        btnContinue.animate().alpha(1f).setDuration(500).start()
        btnHome.animate().alpha(1f).setDuration(500).start()
        btnContinue.isEnabled = true
        btnHome.isEnabled = true
    }

    private fun setupButtonListeners() {
        btnContinue.alpha = 0f
        btnHome.alpha = 0f
        btnContinue.isEnabled = false
        btnHome.isEnabled = false

        btnContinue.setOnClickListener {
            // Your continue logic
        }

        btnHome.setOnClickListener {
            // Your home logic
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blockDropSound.release()
        starBounceSound.release()
        completionSound.release()
    }
}