package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.cos
import kotlin.math.sin

class AllCorrectGrandPrizeActivity : AppCompatActivity() {

    private lateinit var feedbackImage: ImageView
    private lateinit var unlockedText: TextView
    private lateinit var title: TextView
    private lateinit var btnOk: Button
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var giftBoxIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_correct_grand_prize)

        feedbackImage = findViewById(R.id.feedbackImage)
        unlockedText = findViewById(R.id.unlockedText)
        title = findViewById(R.id.title)
        btnOk = findViewById(R.id.btn_ok)
        mainLayout = findViewById(R.id.main)

        // Initially hide the panda and texts
        feedbackImage.visibility = View.GONE
        unlockedText.visibility = View.GONE
        title.visibility = View.GONE
        btnOk.visibility = View.GONE

        // Add a temporary gift box overlay
        giftBoxIcon = ImageView(this)
        giftBoxIcon.setImageResource(R.drawable.giftbox)
        giftBoxIcon.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
        mainLayout.addView(giftBoxIcon)

        // Start the gift unboxing animation sequence after layout is measured
        mainLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mainLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                startGiftUnboxingAnimation()
            }
        })

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startGiftUnboxingAnimation() {
        // Center the gift box
        giftBoxIcon.x = (mainLayout.width - giftBoxIcon.width) / 2f
        giftBoxIcon.y = (mainLayout.height - giftBoxIcon.height) / 2f

        // Step 1: Initial gift box animation (shaking)
        val shakeAnim = ObjectAnimator.ofFloat(giftBoxIcon, "rotation", -10f, 10f, -10f, 10f, -5f, 5f, 0f)
        shakeAnim.duration = 1000
        shakeAnim.repeatCount = 2
        shakeAnim.interpolator = LinearInterpolator()

        // Step 2: Gift box expands and reveals panda
        val expandAnim = ObjectAnimator.ofPropertyValuesHolder(
            giftBoxIcon,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.5f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.5f, 0f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 1f, 0f)
        )
        expandAnim.duration = 800
        expandAnim.interpolator = AccelerateInterpolator()

        // Step 3: Panda reveal animation
        expandAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Remove gift box
                mainLayout.removeView(giftBoxIcon)

                // Show panda with animation
                feedbackImage.visibility = View.VISIBLE
                feedbackImage.scaleX = 0.1f
                feedbackImage.scaleY = 0.1f
                feedbackImage.alpha = 0f

                val pandaReveal = ObjectAnimator.ofPropertyValuesHolder(
                    feedbackImage,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.1f, 1.5f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.1f, 1.5f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
                )
                pandaReveal.duration = 1200
                pandaReveal.interpolator = BounceInterpolator()
                pandaReveal.start()

                // Add a little wiggle after bounce
                Handler(Looper.getMainLooper()).postDelayed({
                    val wiggleAnim = ObjectAnimator.ofFloat(feedbackImage, "rotation", -5f, 5f, -3f, 3f, 0f)
                    wiggleAnim.duration = 500
                    wiggleAnim.interpolator = LinearInterpolator()
                    wiggleAnim.start()
                }, 1300)

                // Show unlocked text with fade-in
                Handler(Looper.getMainLooper()).postDelayed({
                    unlockedText.visibility = View.VISIBLE
                    unlockedText.alpha = 0f
                    val textFadeIn = ObjectAnimator.ofFloat(unlockedText, "alpha", 0f, 1f)
                    textFadeIn.duration = 500
                    textFadeIn.start()
                }, 300)

                // Show title with fade-in
                Handler(Looper.getMainLooper()).postDelayed({
                    title.visibility = View.VISIBLE
                    title.alpha = 0f
                    val titleFadeIn = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f)
                    titleFadeIn.duration = 500
                    titleFadeIn.start()
                }, 600)

                // Show OK button with bounce
                Handler(Looper.getMainLooper()).postDelayed({
                    btnOk.visibility = View.VISIBLE
                    btnOk.scaleX = 0f
                    btnOk.scaleY = 0f
                    val btnBounce = ObjectAnimator.ofPropertyValuesHolder(
                        btnOk,
                        android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
                        android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f)
                    )
                    btnBounce.duration = 800
                    btnBounce.interpolator = BounceInterpolator()
                    btnBounce.start()

                    // Set up OK button click listener
                    btnOk.setOnClickListener {
                        animateVacuumEffect()
                    }
                }, 900)
            }
        })

        // Start animation sequence
        shakeAnim.start()
        Handler(Looper.getMainLooper()).postDelayed({
            expandAnim.start()
        }, 2100) // After 2 shakes
    }

    private fun animateVacuumEffect() {
        // Hide all other elements
        unlockedText.visibility = View.GONE
        title.visibility = View.GONE
        btnOk.visibility = View.GONE

        // Create a spinning/vortex effect in the top-right corner
        createVortexEffect()

        // Animate the panda with a spiral/sucking motion
        val spiralAnim = createSpiralAnimation()

        spiralAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Remove any vortex effects
                mainLayout.removeView(mainLayout.findViewWithTag("vortex"))

                // Return result and finish
                val resultIntent = Intent()
                resultIntent.putExtra("PRIZE_UNLOCKED", true)
                setResult(RESULT_OK, resultIntent)
                finish()

                // Smooth exit
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        })

        spiralAnim.start()
    }

    private fun createVortexEffect() {
        // Create a vortex/spinning effect in the top-right corner
        val vortex = ImageView(this)

        // Try to load vortex image, fallback to a simple drawable
        try {
            vortex.setImageResource(R.drawable.ic_vortex) // You'll need to add this drawable
        } catch (e: Exception) {
            // Fallback to a simple circle
            vortex.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
            vortex.setImageResource(android.R.drawable.ic_menu_rotate)
        }

        vortex.tag = "vortex"
        vortex.scaleType = ImageView.ScaleType.CENTER_INSIDE

        val vortexSize = 200
        val layoutParams = ConstraintLayout.LayoutParams(vortexSize, vortexSize)
        layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID

        // Set margins using setMargins() method
        layoutParams.setMargins(0, 20, 20, 0) // left, top, right, bottom

        vortex.layoutParams = layoutParams

        mainLayout.addView(vortex)

        // Make vortex spin
        val spinAnim = ObjectAnimator.ofFloat(vortex, "rotation", 0f, 360f)
        spinAnim.duration = 1000
        spinAnim.repeatCount = ValueAnimator.INFINITE
        spinAnim.interpolator = LinearInterpolator()
        spinAnim.start()

        // Pulsing effect
        val pulseAnim = ObjectAnimator.ofPropertyValuesHolder(
            vortex,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.2f, 0.8f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.2f, 0.8f)
        )
        pulseAnim.duration = 800
        pulseAnim.repeatCount = ValueAnimator.INFINITE
        pulseAnim.repeatMode = ValueAnimator.REVERSE
        pulseAnim.start()
    }

    private fun createSpiralAnimation(): android.animation.AnimatorSet {
        // Create a spiral/sucking animation
        val spiralAnim = android.animation.AnimatorSet()

        // Starting position (center)
        feedbackImage.translationX = 0f
        feedbackImage.translationY = 0f

        // Create a ValueAnimator for the spiral path
        val spiralPathAnim = ValueAnimator.ofFloat(0f, 1f)
        spiralPathAnim.duration = 2000
        spiralPathAnim.interpolator = AccelerateInterpolator()

        val centerX = feedbackImage.x + feedbackImage.width / 2f
        val centerY = feedbackImage.y + feedbackImage.height / 2f
        val targetX = mainLayout.width - 150f // Top-right corner
        val targetY = 150f

        spiralPathAnim.addUpdateListener { animator ->
            val progress = animator.animatedValue as Float

            // Spiral path calculation
            val spiralRadius = 300f * (1 - progress) // Radius decreases over time
            val spiralAngle = progress * 8f * Math.PI.toFloat() // Multiple rotations

            // Calculate spiral coordinates
            val spiralX = spiralRadius * cos(spiralAngle)
            val spiralY = spiralRadius * sin(spiralAngle)

            // Linear movement towards target
            val linearX = centerX + (targetX - centerX) * progress
            val linearY = centerY + (targetY - centerY) * progress

            // Combine spiral and linear movement
            val finalX = linearX + spiralX
            val finalY = linearY + spiralY

            feedbackImage.translationX = finalX - feedbackImage.x
            feedbackImage.translationY = finalY - feedbackImage.y

            // Scale down gradually
            val scale = 1f - progress * 0.7f // Scale down to 30% size
            feedbackImage.scaleX = scale
            feedbackImage.scaleY = scale

            // Rotate while moving
            feedbackImage.rotation = progress * 720f // Two full rotations
        }

        // Create the final "suck in" effect
        val suckInAnim = ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.3f, 0.1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.3f, 0.1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        )
        suckInAnim.duration = 300
        suckInAnim.interpolator = AccelerateInterpolator()

        // Play animations sequentially
        spiralAnim.playSequentially(spiralPathAnim, suckInAnim)

        return spiralAnim
    }

    // Alternative simpler vacuum animation (if spiral is too complex)
    private fun createSimpleVacuumAnimation(): android.animation.AnimatorSet {
        val animSet = android.animation.AnimatorSet()

        // 1. First, the panda gets excited (bounces and grows)
        val excitementAnim = ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("translationY", 0f, -50f, 0f)
        )
        excitementAnim.duration = 800
        excitementAnim.interpolator = BounceInterpolator()

        // 2. Then it gets "pulled" toward the corner with a wobbly path
        val targetX = mainLayout.width - feedbackImage.width - 50f
        val targetY = 100f

        // Create a wobbly path animation
        val wobbleAnim = ValueAnimator.ofFloat(0f, 1f)
        wobbleAnim.duration = 1500
        wobbleAnim.interpolator = AccelerateDecelerateInterpolator()

        wobbleAnim.addUpdateListener { animator ->
            val progress = animator.animatedValue as Float

            // Wobbly path calculation (sin wave)
            val wobbleAmount = 100f * (1 - progress) // Wobble decreases as we get closer
            val wobbleX = wobbleAmount * sin(progress * 8f * Math.PI.toFloat())
            val wobbleY = wobbleAmount * cos(progress * 4f * Math.PI.toFloat())

            // Main movement
            val currentX = feedbackImage.x + (targetX - feedbackImage.x) * progress
            val currentY = feedbackImage.y + (targetY - feedbackImage.y) * progress

            // Apply wobbly offset
            feedbackImage.translationX = (currentX - feedbackImage.x) + wobbleX
            feedbackImage.translationY = (currentY - feedbackImage.y) + wobbleY

            // Scale down gradually
            val scale = 1f - progress * 0.5f // Scale down to 50% size
            feedbackImage.scaleX = scale
            feedbackImage.scaleY = scale

            // Tilt effect
            feedbackImage.rotation = progress * 360f
        }

        // 3. Final "suck in" effect
        val suckAnim = ObjectAnimator.ofPropertyValuesHolder(
            feedbackImage,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 0.01f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 0.01f),
            android.animation.PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
        )
        suckAnim.duration = 400
        suckAnim.interpolator = AccelerateInterpolator()

        // Play all animations sequentially
        animSet.playSequentially(excitementAnim, wobbleAnim, suckAnim)

        return animSet
    }

    override fun onPause() {
        super.onPause()
        // Clear all animations to prevent memory leaks
        giftBoxIcon.clearAnimation()
        feedbackImage.clearAnimation()
        unlockedText.clearAnimation()
        title.clearAnimation()
        btnOk.clearAnimation()
        mainLayout.findViewWithTag<View?>("vortex")?.clearAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear all animations to prevent memory leaks
        giftBoxIcon.clearAnimation()
        feedbackImage.clearAnimation()
        unlockedText.clearAnimation()
        title.clearAnimation()
        btnOk.clearAnimation()
        mainLayout.findViewWithTag<View?>("vortex")?.clearAnimation()
    }
}