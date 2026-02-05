package com.chirathi.voicebridge

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup

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

    // XML-defined buttons and container - FIXED: Use LinearLayout
    private lateinit var replayButton: Button
    private lateinit var dashboardButton: Button
    private lateinit var buttonsContainer: LinearLayout  // Changed from ViewGroup to LinearLayout

    // Store game data to pass back
    private var finalAlpha: Float = 0f
    private var poppedBubbles: Int = 0
    private var totalBubbles: Int = 5
    private var correctCount: Int = 0
    private var errorCount: Int = 0
    private var completedSubroutines: Int = 0
    private var routineCompleted: Int = 0
    private var previousAlpha: Float = 0f
    private var previousCorrect: Int = 0
    private var previousSubroutine: Int = -1
    private var previousError: String = "none"
    private var userSelectedRoutine: Int = 0
    private var userAge: Int = 6
    private var stickerType: String = "comfort"

    companion object {
        private const val TAG = "UnlockStickerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "UnlockStickerActivity.onCreate() called")

        // Add crash handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CRASH in thread $thread: ${throwable.message}")
            throwable.printStackTrace()
        }

        try {
            setContentView(R.layout.activity_unlock_sticker)
            Log.d(TAG, "Layout set successfully")

            // Get game data passed from game activity
            getGameDataFromIntent()

            initViews()
            setupDimOverlay()

            // Delay start to ensure views are ready
            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "Starting gift box animation")
                startGiftBoxAnimation()
            }, 500)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            finish()
        }
    }

    private fun getGameDataFromIntent() {
        stickerType = intent.getStringExtra("STICKER_TYPE") ?: "comfort"
        finalAlpha = intent.getFloatExtra("FINAL_ALPHA", 0f)
        poppedBubbles = intent.getIntExtra("POPPED_BUBBLES", 0)
        totalBubbles = intent.getIntExtra("TOTAL_BUBBLES", 5)
        correctCount = intent.getIntExtra("CORRECT_COUNT", 0)
        errorCount = intent.getIntExtra("ERROR_COUNT", 0)
        completedSubroutines = intent.getIntExtra("COMPLETED_SUBROUTINES", 0)
        routineCompleted = intent.getIntExtra("ROUTINE_COMPLETED", 0)
        previousAlpha = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        previousCorrect = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        previousSubroutine = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        previousError = intent.getStringExtra("PREVIOUS_ERROR") ?: "none"
        userSelectedRoutine = intent.getIntExtra("USER_SELECTED_ROUTINE", 0)
        userAge = intent.getIntExtra("USER_AGE", 6)

        Log.d(TAG, "Received game data: StickerType=$stickerType, Alpha=$finalAlpha")
    }

    private fun initViews() {
        container = findViewById(R.id.main)
        giftBox = findViewById(R.id.giftBox)
        stickerCard = findViewById(R.id.stickerCard)
        stickerReveal = findViewById(R.id.stickerReveal)
        collectButton = findViewById(R.id.collectButton)
        titleText = findViewById(R.id.title)
        unlockedText = findViewById(R.id.unlockedText)
        glowOverlay = findViewById(R.id.glowOverlay)

        // Initialize XML-defined buttons and container
        buttonsContainer = findViewById(R.id.buttonsContainer) as LinearLayout  // Explicit cast
        replayButton = findViewById(R.id.replayButton)
        dashboardButton = findViewById(R.id.dashboardButton)

        // Set up button click listeners
        replayButton.setOnClickListener { onReplayClicked() }
        dashboardButton.setOnClickListener { onDashboardClicked() }

        // Initially hide elements (buttons are already "gone" in XML)
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
        dimOverlay.setBackgroundColor(android.graphics.Color.argb(220, 0, 0, 0))
        dimOverlay.alpha = 0f
        dimOverlay.isClickable = true
        dimOverlay.isFocusable = true

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        container.addView(dimOverlay, 0, params)

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

    private fun checkRoutineUnlock(stickerCount: Int) {
        val sharedPrefs = getSharedPreferences("sticker_progress", MODE_PRIVATE)

        when (stickerCount) {
            3 -> unlockRoutine(1, "Bedtime Routine", stickerCount) // Unlock bedtime after 3 stickers
            6 -> unlockRoutine(2, "School Routine", stickerCount)  // Unlock school after 6 stickers
        }
    }

    private fun unlockRoutine(routineId: Int, routineName: String, stickerCount: Int) {
        val sharedPrefs = getSharedPreferences("unlocked_routines", MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("routine_$routineId", true)
        editor.apply()

        // Show unlock message
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this,
                "🎉 $routineName Unlocked!",
                Toast.LENGTH_LONG
            ).show()
        }

        // Update title text
        val nextStickersNeeded = when (routineId) {
            1 -> 3 // Need 3 more stickers for school routine (total 6)
            2 -> 0 // All routines unlocked
            else -> 0
        }

        runOnUiThread {
            if (nextStickersNeeded > 0) {
                titleText.text = "🎉 $routineName Unlocked!\nCollect $nextStickersNeeded more stickers for next routine!"
            } else {
                titleText.text = "🎉 All Routines Unlocked!\nYou're a star! ⭐"
            }
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
        // Set sticker image based on type
        val stickerDrawable = when (stickerType) {
            "comfort" -> R.drawable.sticker_unlock_three
            "achievement" -> R.drawable.sticker_unlock_four
            "celebration" -> R.drawable.sticker_unlock_thirteen
            "encouragement" -> R.drawable.sticker_unlock_twelve
            else -> R.drawable.sticker
        }

        stickerReveal.setImageResource(stickerDrawable)

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
                        // After sticker reveal, show collect button and text
                        showCollectButtonAndText()
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

        // Show title text
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
        Log.d(TAG, "collectSticker called")

        // Disable button to prevent multiple clicks
        collectButton.isEnabled = false

        // 1. First show collected message
        Toast.makeText(
            this@UnlockStickerActivity,
            "Sticker Collected! 🎉",
            Toast.LENGTH_SHORT
        ).show()

        // 2. Shrink sticker
        val shrinkAnim = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.8f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.8f)
        ).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }

        // 3. Fade out sticker and other elements
        val fadeOutSticker = ObjectAnimator.ofFloat(stickerReveal, "alpha", 1f, 0f).apply {
            duration = 500
        }

        val fadeOutText = ObjectAnimator.ofFloat(unlockedText, "alpha", 1f, 0f).apply {
            duration = 300
        }

        val fadeOutTitle = ObjectAnimator.ofFloat(titleText, "alpha", 1f, 0f).apply {
            duration = 300
        }

        val fadeOutCollectButton = ObjectAnimator.ofFloat(collectButton, "alpha", 1f, 0f).apply {
            duration = 300
        }

        shrinkAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Start fade out animations
                AnimatorSet().apply {
                    playTogether(fadeOutSticker, fadeOutText, fadeOutTitle, fadeOutCollectButton)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // After everything fades out, show replay and dashboard buttons
                            showReplayAndDashboardButtons()
                        }
                    })
                    start()
                }
            }
        })

        shrinkAnim.start()
    }

    private fun showReplayAndDashboardButtons() {
        Log.d(TAG, "showReplayAndDashboardButtons called")

        runOnUiThread {
            // Show the container and buttons
            buttonsContainer.visibility = View.VISIBLE
            replayButton.visibility = View.VISIBLE
            dashboardButton.visibility = View.VISIBLE

            // Reset positions
            replayButton.alpha = 0f
            dashboardButton.alpha = 0f
            replayButton.translationY = 50f
            dashboardButton.translationY = 50f

            // Animate replay button
            replayButton.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()

            // Animate dashboard button with delay
            Handler().postDelayed({
                dashboardButton.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }, 200)

            // Make buttons clickable
            replayButton.isEnabled = true
            dashboardButton.isEnabled = true
        }
    }

    private fun onReplayClicked() {
        Log.d(TAG, "Replay button clicked")

        // Same as replay in scoreboard - start new game session
        val intent = Intent(this, ActivitySequenceUnderActivity::class.java)

        // Pass previous performance data for model decision
        intent.putExtra("PREVIOUS_ALPHA", previousAlpha)
        intent.putExtra("PREVIOUS_CORRECT", previousCorrect)
        intent.putExtra("PREVIOUS_SUBROUTINE", previousSubroutine)
        intent.putExtra("PREVIOUS_ERROR", previousError)

        // Pass user selection
        intent.putExtra("SELECTED_ROUTINE_ID", userSelectedRoutine)
        intent.putExtra("USER_AGE", userAge)

        startActivity(intent)
        finish()
    }

    private fun onDashboardClicked() {
        Log.d(TAG, "Dashboard button clicked")

        // Navigate to dashboard
        val intent = Intent(this, GameDashboardActivity::class.java)

        // Clear back stack
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
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