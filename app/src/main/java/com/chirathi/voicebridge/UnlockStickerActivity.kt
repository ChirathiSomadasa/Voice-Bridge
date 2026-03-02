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
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup

/**
 * UnlockStickerActivity — updated.
 *
 *  Changes vs original:
 *   • Uses StickerManager.awardNextSticker() — guaranteed unique sticker per session.
 *     If the pool is exhausted the screen shows a "You have all stickers!" message
 *     and falls through to the Replay / Dashboard buttons without the animation.
 *   • Updates StickerManager (counts, routine unlocks) immediately on award.
 *   • Sticker count shown in Toast and title text.
 *   • "COLLECT" button then reveals Replay / Dashboard buttons (unchanged flow).
 */
class UnlockStickerActivity : AppCompatActivity() {

    private lateinit var container    : FrameLayout
    private lateinit var giftBox      : ImageView
    private lateinit var stickerCard  : ImageView
    private lateinit var stickerReveal: ImageView
    private lateinit var collectButton: android.widget.Button
    private lateinit var titleText    : TextView
    private lateinit var unlockedText : TextView
    private lateinit var dimOverlay   : View
    private lateinit var glowOverlay  : ImageView
    private lateinit var replayButton    : android.widget.Button
    private lateinit var dashboardButton : android.widget.Button
    private lateinit var buttonsContainer: LinearLayout

    // Game data from intent
    private var finalAlpha           = 0f
    private var poppedBubbles        = 0
    private var totalBubbles         = 5
    private var correctCount         = 0
    private var errorCount           = 0
    private var completedSubroutines = 0
    private var routineCompleted     = 0
    private var previousAlpha        = 0f
    private var previousCorrect      = 0
    private var previousSubroutine   = -1
    private var previousError        = "none"
    private var userSelectedRoutine  = 0
    private var userAge              = 6

    // Awarded sticker for this session
    private var awardedSticker: StickerManager.StickerInfo? = null

    companion object {
        private const val TAG = "UnlockStickerActivity"
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e(TAG, "CRASH: ${throwable.message}")
        }

        try {
            setContentView(R.layout.activity_unlock_sticker)
            readIntent()
            initViews()
            setupDimOverlay()

            // ── Award sticker via StickerManager ─────────────────────────────
            awardedSticker = StickerManager.awardNextSticker(this)

            if (awardedSticker == null) {
                // Pool exhausted — skip animation, show completion message
                showPoolExhaustedState()
                return
            }

            Log.d(TAG, "Awarded sticker: ${awardedSticker!!.name}")
            Toast.makeText(this,
                "New sticker: ${awardedSticker!!.name}! (${StickerManager.stickerCount(this)} total)",
                Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({ startGiftBoxAnimation() }, 500)

        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}")
            finish()
        }
    }

    // =========================================================================
    // Intent
    // =========================================================================

    private fun readIntent() {
        finalAlpha           = intent.getFloatExtra("FINAL_ALPHA", 0f)
        poppedBubbles        = intent.getIntExtra("POPPED_BUBBLES", 0)
        totalBubbles         = intent.getIntExtra("TOTAL_BUBBLES", 5)
        correctCount         = intent.getIntExtra("CORRECT_COUNT", 0)
        errorCount           = intent.getIntExtra("ERROR_COUNT", 0)
        completedSubroutines = intent.getIntExtra("COMPLETED_SUBROUTINES", 0)
        routineCompleted     = intent.getIntExtra("ROUTINE_COMPLETED", 0)
        previousAlpha        = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        previousCorrect      = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        previousSubroutine   = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        previousError        = intent.getStringExtra("PREVIOUS_ERROR") ?: "none"
        userSelectedRoutine  = intent.getIntExtra("USER_SELECTED_ROUTINE", 0)
        userAge              = intent.getIntExtra("USER_AGE", 6)
    }

    // =========================================================================
    // Views
    // =========================================================================

    private fun initViews() {
        container       = findViewById(R.id.main)
        giftBox         = findViewById(R.id.giftBox)
        stickerCard     = findViewById(R.id.stickerCard)
        stickerReveal   = findViewById(R.id.stickerReveal)
        collectButton   = findViewById(R.id.collectButton)
        titleText       = findViewById(R.id.title)
        unlockedText    = findViewById(R.id.unlockedText)
        glowOverlay     = findViewById(R.id.glowOverlay)
        buttonsContainer = findViewById(R.id.buttonsContainer) as LinearLayout
        replayButton    = findViewById(R.id.replayButton)
        dashboardButton = findViewById(R.id.dashboardButton)

        replayButton.setOnClickListener    { onReplayClicked() }
        dashboardButton.setOnClickListener { onDashboardClicked() }

        giftBox.visibility      = View.VISIBLE
        stickerCard.visibility  = View.INVISIBLE
        stickerReveal.visibility= View.INVISIBLE
        collectButton.visibility= View.INVISIBLE
        titleText.visibility    = View.INVISIBLE
        unlockedText.visibility = View.INVISIBLE
        glowOverlay.visibility  = View.INVISIBLE

        collectButton.setOnClickListener { collectSticker() }
    }

    private fun setupDimOverlay() {
        dimOverlay = View(this).apply {
            setBackgroundColor(android.graphics.Color.argb(220, 0, 0, 0))
            alpha       = 0f
            isClickable = true
            isFocusable = true
        }
        container.addView(dimOverlay, 0, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        ObjectAnimator.ofFloat(dimOverlay, "alpha", 0f, 1f).apply {
            duration = 800; start()
        }
    }

    // =========================================================================
    // Pool-exhausted fallback
    // =========================================================================

    private fun showPoolExhaustedState() {
        giftBox.visibility = View.GONE
        titleText.text     = "You have ALL the stickers! You're amazing!"
        titleText.visibility = View.VISIBLE
        unlockedText.text    = "Nothing new today — keep playing!"
        unlockedText.visibility = View.VISIBLE
        collectButton.visibility = View.GONE
        // Show nav buttons immediately
        buttonsContainer.visibility = View.VISIBLE
        replayButton.visibility     = View.VISIBLE
        dashboardButton.visibility  = View.VISIBLE
    }

    // =========================================================================
    // Animation sequence (unchanged structure, uses awardedSticker for drawable)
    // =========================================================================

    private fun startGiftBoxAnimation() {
        val scaleIn = ObjectAnimator.ofPropertyValuesHolder(
            giftBox,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("alpha",  0f, 1f)
        ).apply { duration = 800; interpolator = OvershootInterpolator() }

        scaleIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Handler(Looper.getMainLooper()).postDelayed({ shakeAndOpenBox() }, 300)
            }
        })
        scaleIn.start()
    }

    private fun shakeAndOpenBox() {
        val shakeX = ObjectAnimator.ofFloat(giftBox, "translationX", 0f,-15f,12f,-8f,5f,0f)
            .apply { duration = 600; interpolator = DecelerateInterpolator() }
        val shakeY = ObjectAnimator.ofFloat(giftBox, "translationY", 0f,-5f,4f,-3f,0f)
            .apply { duration = 600; interpolator = DecelerateInterpolator() }
        shakeX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) { hideBoxAndRevealCard() }
        })
        AnimatorSet().apply { playTogether(shakeX, shakeY); start() }
    }

    private fun hideBoxAndRevealCard() {
        ObjectAnimator.ofFloat(giftBox, "alpha", 1f, 0f).apply { duration = 300; start() }
        Handler(Looper.getMainLooper()).postDelayed({
            stickerCard.visibility  = View.VISIBLE
            stickerCard.scaleX      = 0.5f; stickerCard.scaleY = 0.5f; stickerCard.alpha = 0f
            floatCardUpward()
        }, 300)
    }

    private fun floatCardUpward() {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(stickerCard, "alpha", 0f, 1f).also { it.duration = 400 },
                ObjectAnimator.ofPropertyValuesHolder(stickerCard,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 0.8f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 0.8f)
                ).also { it.duration = 400; it.interpolator = DecelerateInterpolator() },
                ObjectAnimator.ofFloat(stickerCard, "translationY",
                    stickerCard.translationY, stickerCard.translationY - 300f
                ).also { it.duration = 1200; it.interpolator = AccelerateDecelerateInterpolator() }
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { revealStickerWithGlow() }
            })
            start()
        }
    }

    private fun revealStickerWithGlow() {
        // Use drawable from the awarded sticker
        val drawableRes = awardedSticker?.drawableRes ?: R.drawable.sticker
        stickerReveal.setImageResource(drawableRes)

        glowOverlay.visibility = View.VISIBLE
        glowOverlay.scaleX     = 0.5f; glowOverlay.scaleY = 0.5f; glowOverlay.alpha = 0f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofPropertyValuesHolder(glowOverlay,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 2f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 2f)
                ).also { it.duration = 500 },
                ObjectAnimator.ofFloat(glowOverlay, "alpha", 0f, 0.7f, 0f).also { it.duration = 600 }
            )
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            stickerReveal.visibility = View.VISIBLE
            stickerReveal.scaleX     = 0.8f; stickerReveal.scaleY = 0.8f; stickerReveal.alpha = 0f
            stickerReveal.translationX = stickerCard.translationX
            stickerReveal.translationY = stickerCard.translationY

            ObjectAnimator.ofFloat(stickerCard, "alpha", 1f, 0f).apply { duration = 200; start() }

            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(stickerReveal, "alpha", 0f, 1f).also { it.duration = 300 },
                    ObjectAnimator.ofPropertyValuesHolder(stickerReveal,
                        android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.8f, 1.1f, 1f),
                        android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.8f, 1.1f, 1f)
                    ).also { it.duration = 400; it.interpolator = BounceInterpolator() }
                )
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { showCollectButtonAndText() }
                })
                start()
            }
        }, 200)
    }

    private fun showCollectButtonAndText() {
        val totalNow = StickerManager.stickerCount(this)
        val needFor1 = (3 - totalNow).coerceAtLeast(0)
        val needFor2 = (6 - totalNow).coerceAtLeast(0)

        unlockedText.text    = "NEW STICKER UNLOCKED!"
        unlockedText.alpha   = 0f; unlockedText.visibility = View.VISIBLE; unlockedText.translationY = 200f

        titleText.text = when {
            totalNow >= 6      -> "🎊 All routines unlocked!"
            totalNow >= 3      -> awardedSticker?.name + " collected! Collect $needFor2 more for School Routine!"
            needFor1 > 0       -> "${awardedSticker?.name} collected! Collect $needFor1 more for Bedtime Routine!"
            else               -> "${awardedSticker?.name} collected! You have $totalNow stickers!"
        }
        titleText.alpha      = 0f; titleText.visibility = View.VISIBLE; titleText.translationY = 300f

        collectButton.scaleX = 0f; collectButton.scaleY = 0f
        collectButton.visibility = View.VISIBLE; collectButton.alpha = 0f

        ObjectAnimator.ofFloat(unlockedText, "alpha", 0f, 1f).also { it.duration = 600; it.start() }
        ObjectAnimator.ofFloat(unlockedText, "translationY", 200f, 0f)
            .also { it.duration = 600; it.interpolator = DecelerateInterpolator(); it.start() }
        ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f)
            .also { it.duration = 600; it.startDelay = 200; it.start() }
        ObjectAnimator.ofFloat(titleText, "translationY", 300f, 100f)
            .also { it.duration = 600; it.startDelay = 200; it.interpolator = DecelerateInterpolator(); it.start() }

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(collectButton, "alpha", 0f, 1f).also { it.duration = 400; it.startDelay = 400 },
                ObjectAnimator.ofPropertyValuesHolder(collectButton,
                    android.animation.PropertyValuesHolder.ofFloat("scaleX", 0f, 1.2f, 1f),
                    android.animation.PropertyValuesHolder.ofFloat("scaleY", 0f, 1.2f, 1f)
                ).also { it.duration = 600; it.startDelay = 400; it.interpolator = BounceInterpolator() }
            )
            start()
        }
    }

    private fun collectSticker() {
        collectButton.isEnabled = false
        Toast.makeText(this, "Sticker Collected!", Toast.LENGTH_SHORT).show()

        val shrinkAnim = ObjectAnimator.ofPropertyValuesHolder(
            stickerReveal,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 1f, 0.8f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 1f, 0.8f)
        ).apply { duration = 200; interpolator = DecelerateInterpolator() }

        shrinkAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(stickerReveal,   "alpha", 1f, 0f).also { it.duration = 500 },
                        ObjectAnimator.ofFloat(unlockedText,    "alpha", 1f, 0f).also { it.duration = 300 },
                        ObjectAnimator.ofFloat(titleText,       "alpha", 1f, 0f).also { it.duration = 300 },
                        ObjectAnimator.ofFloat(collectButton,   "alpha", 1f, 0f).also { it.duration = 300 }
                    )
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { showReplayAndDashboardButtons() }
                    })
                    start()
                }
            }
        })
        shrinkAnim.start()
    }

    private fun showReplayAndDashboardButtons() {
        runOnUiThread {
            buttonsContainer.visibility = View.VISIBLE
            replayButton.visibility     = View.VISIBLE
            dashboardButton.visibility  = View.VISIBLE
            replayButton.alpha          = 0f; dashboardButton.alpha = 0f
            replayButton.translationY   = 50f; dashboardButton.translationY = 50f

            replayButton.animate().alpha(1f).translationY(0f).setDuration(500)
                .setInterpolator(DecelerateInterpolator()).start()
            Handler(Looper.getMainLooper()).postDelayed({
                dashboardButton.animate().alpha(1f).translationY(0f).setDuration(500)
                    .setInterpolator(DecelerateInterpolator()).start()
            }, 200)
            replayButton.isEnabled   = true
            dashboardButton.isEnabled= true
        }
    }

    // =========================================================================
    // Navigation
    // =========================================================================

    private fun onReplayClicked() {
        startActivity(Intent(this, ActivitySequenceUnderActivity::class.java).apply {
            putExtra("PREVIOUS_ALPHA",       previousAlpha)
            putExtra("PREVIOUS_CORRECT",     previousCorrect)
            putExtra("PREVIOUS_SUBROUTINE",  previousSubroutine)
            putExtra("PREVIOUS_ERROR",       previousError)
            putExtra("SELECTED_ROUTINE_ID",  userSelectedRoutine)
            putExtra("USER_AGE",             userAge)
        })
        finish()
    }

    private fun onDashboardClicked() {
        startActivity(Intent(this, GameDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
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