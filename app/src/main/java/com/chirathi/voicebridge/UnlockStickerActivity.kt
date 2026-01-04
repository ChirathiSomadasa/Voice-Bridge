package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UnlockStickerActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var giftBox: ImageView
    private lateinit var stickerCard: ImageView  // Blank card
    private lateinit var stickerReveal: ImageView  // Actual sticker
    private lateinit var collectButton: Button
    private lateinit var titleText: TextView
    private lateinit var unlockedText: TextView
    private lateinit var dimOverlay: View
    private lateinit var glowOverlay: ImageView

    // Store game data to pass back to scoreboard
    private var attempts = 0
    private var completionTime = 0L
    private var accuracy = 100

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
        stickerCard = findViewById(R.id.stickerCard)  // Blank white/gray card
        stickerReveal = findViewById(R.id.stickerReveal)  // Actual sticker image
        collectButton = findViewById(R.id.collectButton)
        titleText = findViewById(R.id.title)
        unlockedText = findViewById(R.id.unlockedText)
        glowOverlay = findViewById(R.id.glowOverlay)

        // Initially hide elements
        giftBox.visibility = View.VISIBLE
        stickerCard.visibility = View.INVISIBLE
        stickerReveal.visibility = View.INVISIBLE
        collectButton.visibility = View.INVISIBLE
        titleText.visibility = View.INVISIBLE
        unlockedText.visibility = View.INVISIBLE
        glowOverlay.visibility = View.INVISIBLE

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
        // 1. Scale-in animation for gift box
        val scaleIn = ObjectAnimator.ofPropertyValuesHolder(
            giftBox,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        ).apply {
            duration = 800
            interpolator = OvershootInterpolator()
        }

        scaleIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Start shaking animation after scale-in
                Handler(Looper.getMainLooper()).postDelayed({
                    shakeAndOpenBox()
                }, 300)
            }
        })

        scaleIn.start()
    }

    private fun shakeAndOpenBox() {
        // Simple shake animation (left-right shake)
        val shakeX = ObjectAnimator.ofFloat(
            giftBox, "translationX",
            0f, -15f, 12f, -8f, 5f, 0f
        ).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val shakeY = ObjectAnimator.ofFloat(
            giftBox, "translationY",
            0f, -5f, 4f, -3f, 0f
        ).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        val shakeScale = ObjectAnimator.ofPropertyValuesHolder(
            giftBox,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.05f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.05f, 1f)
        ).apply {
            duration = 600
        }

        shakeX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // After shaking, hide box and reveal card
                hideBoxAndRevealCard()
            }
        })

        AnimatorSet().apply {
            playTogether(shakeX, shakeY, shakeScale)
            start()
        }
    }

    private fun hideBoxAndRevealCard() {
        // 1. Hide gift box
        ObjectAnimator.ofFloat(giftBox, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        // 2. Show blank sticker card at box position
        Handler(Looper.getMainLooper()).postDelayed({
            stickerCard.visibility = View.VISIBLE
            stickerCard.scaleX = 0.5f
            stickerCard.scaleY = 0.5f
            stickerCard.alpha = 0f
            stickerCard.translationX = giftBox.translationX
            stickerCard.translationY = giftBox.translationY

            // Make card float upward
            floatCardUpward()
        }, 300)
    }

    private fun floatCardUpward() {
        // 1. Fade in and scale up card
        val fadeIn = ObjectAnimator.ofFloat(stickerCard, "alpha", 0f, 1f).apply {
            duration = 400
        }

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            stickerCard,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 0.8f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 0.8f)
        ).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
        }

        // 2. Float upward
        val floatUp = ObjectAnimator.ofFloat(
            stickerCard, "translationY",
            stickerCard.translationY,
            stickerCard.translationY - 300f
        ).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleUp, floatUp)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // After floating up, reveal sticker with glow
                    revealStickerWithGlow()
                }
            })
            start()
        }
    }

    private fun revealStickerWithGlow() {
        // 1. Show glow effect
        glowOverlay.visibility = View.VISIBLE
        glowOverlay.scaleX = 0.5f
        glowOverlay.scaleY = 0.5f
        glowOverlay.alpha = 0f

        val glowExpand = ObjectAnimator.ofPropertyValuesHolder(
            glowOverlay,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 2f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 2f)
        ).apply {
            duration = 500
        }

        val glowFade = ObjectAnimator.ofFloat(glowOverlay, "alpha", 0f, 0.7f, 0f).apply {
            duration = 600
        }

        // 2. Hide blank card and show actual sticker
        Handler(Looper.getMainLooper()).postDelayed({
            stickerReveal.visibility = View.VISIBLE
            stickerReveal.scaleX = 0.8f
            stickerReveal.scaleY = 0.8f
            stickerReveal.alpha = 0f
            stickerReveal.translationX = stickerCard.translationX
            stickerReveal.translationY = stickerCard.translationY

            // Hide blank card
            ObjectAnimator.ofFloat(stickerCard, "alpha", 1f, 0f).apply {
                duration = 200
                start()
            }

            // Show sticker with pop effect
            val stickerFadeIn = ObjectAnimator.ofFloat(stickerReveal, "alpha", 0f, 1f).apply {
                duration = 300
            }

            val stickerPop = ObjectAnimator.ofPropertyValuesHolder(
                stickerReveal,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.1f, 1f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.1f, 1f)
            ).apply {
                duration = 400
                interpolator = BounceInterpolator()
            }

            AnimatorSet().apply {
                playTogether(stickerFadeIn, stickerPop)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // After sticker reveal, enlarge it
                        enlargeSticker()
                    }
                })
                start()
            }
        }, 200)

        // Start glow animation
        AnimatorSet().apply {
            playTogether(glowExpand, glowFade)
            start()
        }
    }

    private fun enlargeSticker() {
        // Enlarge sticker to center of screen
        val enlarge = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 3f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 3f)
        ).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val centerX = ObjectAnimator.ofFloat(
            stickerReveal, "translationX",
            stickerReveal.translationX, 0f
        ).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        val centerY = ObjectAnimator.ofFloat(
            stickerReveal, "translationY",
            stickerReveal.translationY, 0f
        ).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }

        AnimatorSet().apply {
            playTogether(enlarge, centerX, centerY)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Show collect button and text
                    showCollectButtonAndText()
                }
            })
            start()
        }
    }

    private fun showCollectButtonAndText() {
        // Show "NEW STICKER UNLOCKED!" text
        unlockedText.text = "NEW STICKER UNLOCKED!"
        unlockedText.alpha = 0f
        unlockedText.visibility = View.VISIBLE
        unlockedText.translationY = 200f

        val textFadeIn = ObjectAnimator.ofFloat(unlockedText, "alpha", 0f, 1f).apply {
            duration = 600
        }

        val textFloatUp = ObjectAnimator.ofFloat(unlockedText, "translationY", 200f, 0f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
        }

        // Show "Collect 2 More to Unlock New Routine!" text
        titleText.text = "Collect 2 More to \nUnlock New Routine!"
        titleText.alpha = 0f
        titleText.visibility = View.VISIBLE
        titleText.translationY = 300f

        val titleFadeIn = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).apply {
            duration = 600
            startDelay = 200
        }

        val titleFloatUp = ObjectAnimator.ofFloat(titleText, "translationY", 300f, 100f).apply {
            duration = 600
            startDelay = 200
            interpolator = DecelerateInterpolator()
        }

        // Show collect button
        collectButton.scaleX = 0f
        collectButton.scaleY = 0f
        collectButton.visibility = View.VISIBLE
        collectButton.alpha = 0f

        val buttonFadeIn = ObjectAnimator.ofFloat(collectButton, "alpha", 0f, 1f).apply {
            duration = 400
            startDelay = 400
        }

        val buttonScale = ObjectAnimator.ofPropertyValuesHolder(
            collectButton,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f)
        ).apply {
            duration = 600
            startDelay = 400
            interpolator = BounceInterpolator()
        }

        // Start all text animations
        AnimatorSet().apply {
            playTogether(textFadeIn, textFloatUp)
            start()
        }

        AnimatorSet().apply {
            playTogether(titleFadeIn, titleFloatUp)
            start()
        }

        AnimatorSet().apply {
            playTogether(buttonFadeIn, buttonScale)
            start()
        }
    }

    private fun collectSticker() {
        // Disable button to prevent multiple clicks
        collectButton.isEnabled = false

        // 1. Shrink sticker slightly
        val shrinkAnim = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 3f, 2.5f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 3f, 2.5f)
        ).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }

        // 2. Jump down and exit - FIXED: Use screen height instead of view height
        val screenHeight = resources.displayMetrics.heightPixels
        val jumpDown = ObjectAnimator.ofFloat(
            stickerReveal, "translationY",
            stickerReveal.translationY,
            screenHeight.toFloat() + 500f  // Add extra 500px to ensure it goes off screen
        ).apply {
            duration = 800
            interpolator = AccelerateInterpolator()
        }

        val fadeOut = ObjectAnimator.ofFloat(stickerReveal, "alpha", 1f, 0f).apply {
            duration = 800
        }

        // 3. Hide text elements
        ObjectAnimator.ofFloat(unlockedText, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        ObjectAnimator.ofFloat(titleText, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        ObjectAnimator.ofFloat(collectButton, "alpha", 1f, 0f).apply {
            duration = 300
            start()
        }

        shrinkAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                AnimatorSet().apply {
                    playTogether(jumpDown, fadeOut)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            restoreScreen()
                        }
                    })
                    start()
                }
            }
        })

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

                    // Return to scoreboard
                    Handler(Looper.getMainLooper()).postDelayed({
                        returnToScoreboard()
                    }, 300)
                }
            })
            start()
        }
    }

    private fun returnToScoreboard() {
        val intent = Intent(this, ASequenceScoreboardActivity::class.java)
        intent.putExtra("ATTEMPTS", attempts)
        intent.putExtra("ELAPSED_TIME", completionTime)
        intent.putExtra("ACCURACY", accuracy)
        intent.putExtra("STICKER_ALREADY_SHOWN", true)

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        container.clearAnimation()
        giftBox.clearAnimation()
        stickerReveal.clearAnimation()
        collectButton.clearAnimation()
    }
}