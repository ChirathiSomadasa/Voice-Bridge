package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class UnlockStickerActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var giftBox: ImageView
    private lateinit var stickerReveal: ImageView
    private lateinit var collectButton: Button
    private lateinit var titleText: TextView
    private lateinit var unlockedText: TextView
    private lateinit var dimOverlay: View
    private lateinit var particlesContainer: FrameLayout

    // Store game data to pass back to scoreboard
    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100

    companion object {
        private val PARTICLE_COLORS = listOf(
            R.drawable.particle_circle,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock_sticker)

        // Get game data passed from scoreboard
        attempts = intent.getIntExtra("ATTEMPTS", 1)
        completionTime = intent.getLongExtra("ELAPSED_TIME", 0L)
        accuracy = intent.getIntExtra("ACCURACY", 100)

        initViews()
        setupDimOverlay()

        // Delay start to ensure views are ready
        Handler(Looper.getMainLooper()).postDelayed({
            startGiftBoxAnimation()
        }, 500)
    }

    private fun initViews() {
        container = findViewById(R.id.main)
        giftBox = findViewById(R.id.giftBox)
        stickerReveal = findViewById(R.id.stickerReveal)
        collectButton = findViewById(R.id.collectButton)
        titleText = findViewById(R.id.title)
        unlockedText = findViewById(R.id.unlockedText)
        particlesContainer = findViewById(R.id.particlesContainer)

        // Initially hide elements
        giftBox.visibility = View.VISIBLE
        stickerReveal.visibility = View.INVISIBLE
        collectButton.visibility = View.INVISIBLE
        titleText.visibility = View.INVISIBLE
        unlockedText.visibility = View.INVISIBLE

        // Set up collect button listener
        collectButton.setOnClickListener {
            collectSticker()
        }
    }

    private fun setupDimOverlay() {
        // Create dim overlay
        dimOverlay = View(this)
        dimOverlay.setBackgroundColor(Color.argb(220, 0, 0, 0))
        dimOverlay.alpha = 0f
        dimOverlay.isClickable = true
        dimOverlay.isFocusable = true

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        (container as ViewGroup).addView(dimOverlay, 0, params)

        // Fade in dim overlay
        ObjectAnimator.ofFloat(dimOverlay, "alpha", 0f, 1f).apply {
            duration = 800
            start()
        }
    }

    private fun startGiftBoxAnimation() {
        // Step 1: Scale-in animation
        val scaleIn = ObjectAnimator.ofPropertyValuesHolder(
            giftBox,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 800
            interpolator = OvershootInterpolator()
        }

        // Step 2: Progressive shake animation
        val shakeAnimator = createProgressiveShakeAnimation()

        // Step 3: Explosion and sticker reveal
        scaleIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Start progressive shaking
                giftBox.postDelayed({
                    shakeAnimator.start()
                }, 300)
            }
        })

        scaleIn.start()
    }

    private fun createProgressiveShakeAnimation(): AnimatorSet {
        val shakeSet = AnimatorSet()
        val shakes = mutableListOf<Animator>()

        // Create progressive shake sequence
        for (i in 1..5) {
            val intensity = i * 0.5f
            val duration = (i * 100L).coerceAtMost(300L)

            val shakeX = ObjectAnimator.ofFloat(
                giftBox, "translationX",
                0f, -10f * intensity, 8f * intensity, -6f * intensity, 4f * intensity, 0f
            ).apply {
                this.duration = duration
                interpolator = DecelerateInterpolator()
            }

            val shakeY = ObjectAnimator.ofFloat(
                giftBox, "translationY",
                0f, -5f * intensity, 4f * intensity, -3f * intensity, 2f * intensity, 0f
            ).apply {
                this.duration = duration
                interpolator = DecelerateInterpolator()
            }

            val scalePulse = ObjectAnimator.ofPropertyValuesHolder(
                giftBox,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1f + (0.05f * i), 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1f + (0.05f * i), 1f)
            ).apply {
                this.duration = duration
            }

            shakes.add(AnimatorSet().apply {
                playTogether(shakeX, shakeY, scalePulse)
            })
        }

        shakeSet.playSequentially(shakes)

        // After shaking, explode and reveal sticker
        shakeSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                createExplosion()
                revealSticker()
            }
        })

        return shakeSet
    }

    private fun createExplosion() {
        val particleCount = 50
        val particleSize = 20

        for (i in 0 until particleCount) {
            val particle = ImageView(this)
            // Set random particle color
            particle.setBackgroundResource(PARTICLE_COLORS[Random.nextInt(PARTICLE_COLORS.size)])
            particle.layoutParams = FrameLayout.LayoutParams(particleSize, particleSize)

            particlesContainer.addView(particle)

            // Random starting position around the gift box
            val startX = giftBox.x + giftBox.width / 2 - particleSize / 2
            val startY = giftBox.y + giftBox.height / 2 - particleSize / 2

            particle.translationX = startX
            particle.translationY = startY
            particle.alpha = 1f

            // Random angle and distance
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val distance = Random.nextFloat() * 400 + 200

            // Explosion animation
            val explodeX = ObjectAnimator.ofFloat(
                particle, "translationX",
                startX, startX + cos(angle).toFloat() * distance
            ).apply {
                duration = 800
                interpolator = AccelerateInterpolator()
            }

            val explodeY = ObjectAnimator.ofFloat(
                particle, "translationY",
                startY, startY + sin(angle).toFloat() * distance
            ).apply {
                duration = 800
                interpolator = AccelerateInterpolator()
            }

            val fadeOut = ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f).apply {
                duration = 800
            }

            val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
                particle,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0f)
            ).apply {
                duration = 800
            }

            AnimatorSet().apply {
                playTogether(explodeX, explodeY, fadeOut, scaleDown)
                start()
            }
        }

        // Hide gift box after explosion
        ObjectAnimator.ofFloat(giftBox, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }
    }

    private fun revealSticker() {
        // Show sticker with initial small size
        stickerReveal.scaleX = 0.1f
        stickerReveal.scaleY = 0.1f
        stickerReveal.alpha = 0f
        stickerReveal.visibility = View.VISIBLE

        // Store initial position
        val initialTranslationY = stickerReveal.translationY

        // Get screen height for animation calculation
        val screenHeight = resources.displayMetrics.heightPixels

        // Swirl and rotate upward animation
        val swirlUp = ObjectAnimator.ofFloat(
            stickerReveal, "translationY",
            initialTranslationY,
            -screenHeight * 0.3f
        ).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
        }

        val rotation = ObjectAnimator.ofFloat(stickerReveal, "rotation", 0f, 1440f).apply {
            duration = 1200
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.1f, 2f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.1f, 2f)
        ).apply {
            duration = 1200
            interpolator = OvershootInterpolator()
        }

        val fadeIn = ObjectAnimator.ofFloat(stickerReveal, "alpha", 0f, 1f).apply {
            duration = 600
        }

        // After swirling up, expand to full screen
        AnimatorSet().apply {
            playTogether(swirlUp, rotation, scaleUp, fadeIn)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    expandToFullScreen()
                }
            })
            start()
        }
    }

    private fun expandToFullScreen() {
        // Expand sticker to full screen reveal
        val expandAnim = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 2f, 4f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 2f, 4f)
        ).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val centerX = ObjectAnimator.ofFloat(
            stickerReveal, "translationX",
            stickerReveal.translationX, 0f
        ).apply {
            duration = 800
        }

        val centerY = ObjectAnimator.ofFloat(
            stickerReveal, "translationY",
            stickerReveal.translationY, 0f
        ).apply {
            duration = 800
        }

        AnimatorSet().apply {
            playTogether(expandAnim, centerX, centerY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    showCollectButton()
                    showTextElements()
                }
            })
            start()
        }
    }

    private fun showCollectButton() {
        collectButton.scaleX = 0f
        collectButton.scaleY = 0f
        collectButton.visibility = View.VISIBLE

        ObjectAnimator.ofPropertyValuesHolder(
            collectButton,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 600
            interpolator = BounceInterpolator()
            start()
        }
    }

    private fun showTextElements() {
        unlockedText.alpha = 0f
        titleText.alpha = 0f
        unlockedText.visibility = View.VISIBLE
        titleText.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(unlockedText, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 200
            start()
        }

        ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 400
            start()
        }
    }

    private fun collectSticker() {
        // Disable button to prevent multiple clicks
        collectButton.isEnabled = false

        // Shrink slightly
        val shrinkAnim = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 4f, 3.5f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 4f, 3.5f)
        ).apply {
            duration = 200
            interpolator = AnticipateInterpolator()
        }

        // Jump and exit animation - SIMPLIFIED VERSION
        val currentTranslationY = stickerReveal.translationY

        // Just move it far down (2000 pixels should be enough to exit screen)
        val jumpAnim = ObjectAnimator.ofFloat(
            stickerReveal, "translationY",
            currentTranslationY,
            currentTranslationY + 2000f
        ).apply {
            duration = 800
            interpolator = AccelerateInterpolator()
        }

        val rotateExit = ObjectAnimator.ofFloat(stickerReveal, "rotation", 0f, 720f).apply {
            duration = 800
        }

        val fadeOut = ObjectAnimator.ofFloat(stickerReveal, "alpha", 1f, 0f).apply {
            duration = 800
        }

        shrinkAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                AnimatorSet().apply {
                    playTogether(jumpAnim, rotateExit, fadeOut)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // Restore brightness and interaction
                            restoreScreen()
                        }
                    })
                    start()
                }
            }
        })

        // Hide other elements
        ObjectAnimator.ofFloat(collectButton, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        ObjectAnimator.ofFloat(unlockedText, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        ObjectAnimator.ofFloat(titleText, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        shrinkAnim.start()
    }

    private fun restoreScreen() {
        // Fade out dim overlay
        ObjectAnimator.ofFloat(dimOverlay, "alpha", 1f, 0f).apply {
            duration = 600
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Remove dim overlay
                    (container as ViewGroup).removeView(dimOverlay)

                    // Return to scoreboard instead of dashboard
                    Handler(Looper.getMainLooper()).postDelayed({
                        returnToScoreboard()
                    }, 300)
                }
            })
            start()
        }
    }

    private fun returnToScoreboard() {
        // Create intent to return to scoreboard with the game data
        val intent = Intent(this, ASequenceScoreboardActivity::class.java)
        intent.putExtra("ATTEMPTS", attempts)
        intent.putExtra("ELAPSED_TIME", completionTime)
        intent.putExtra("ACCURACY", accuracy)
        intent.putExtra("STICKER_ALREADY_SHOWN", true) // ADD THIS FLAG to prevent re-showing

        // Clear the back stack so user can't go back to the sticker unlock
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        // Add a subtle animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up animations
        container.clearAnimation()
        giftBox.clearAnimation()
        stickerReveal.clearAnimation()
        collectButton.clearAnimation()
    }
}