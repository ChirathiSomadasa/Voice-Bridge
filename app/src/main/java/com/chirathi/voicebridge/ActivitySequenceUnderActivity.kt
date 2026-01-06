package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ActivitySequenceUnderActivity : AppCompatActivity() {

    private val correctOrder = listOf("get_up", "brush_teeth", "wash_face")
    private var selectedOrder = mutableListOf<String>()
    private var isGameComplete = false
    private var attemptsCount = 0
    private var correctAnswers = 0
    private var totalQuestions = 3

    // Timer variables
    private var gameStartTime: Long = 0L
    private var elapsedTime: Long = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var isTimerRunning = false

    // UI Elements
    private lateinit var horizontalContainer: LinearLayout
    private lateinit var verticalContainer: LinearLayout
    private lateinit var orderDisplay: LinearLayout
    private lateinit var gameTitle: TextView
    private lateinit var tvInstruction: TextView
    private lateinit var btnStart: Button
    private lateinit var timerTextView: TextView

    // TextViews for order display
    private lateinit var tvOrder1: TextView
    private lateinit var tvOrder2: TextView
    private lateinit var tvOrder3: TextView

    // Original horizontal images
    private lateinit var imgGetUp: ImageView
    private lateinit var imgBrushTeeth: ImageView
    private lateinit var imgWashFace: ImageView

    // Store vertical image views
    private val verticalImages = mutableListOf<ImageView>()

    // Image resources
    private val imageResources = mapOf(
        "get_up" to R.drawable.img_get_up,
        "brush_teeth" to R.drawable.img_brush_teeth,
        "wash_face" to R.drawable.img_wash_face
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_under)

        initViews()
        setupInitialView()
        setupTimer()
    }

    private fun initViews() {
        // Initialize views
        horizontalContainer = findViewById(R.id.horizontal_images_container)
        verticalContainer = findViewById(R.id.vertical_images_container)
        orderDisplay = findViewById(R.id.order_display_container)
        gameTitle = findViewById(R.id.game_title)
        tvInstruction = findViewById(R.id.tv_instruction)
        btnStart = findViewById(R.id.btn_start)
        timerTextView = findViewById(R.id.timerTextView)

        tvOrder1 = findViewById(R.id.tv_order_1)
        tvOrder2 = findViewById(R.id.tv_order_2)
        tvOrder3 = findViewById(R.id.tv_order_3)

        imgGetUp = findViewById(R.id.img_get_up)
        imgBrushTeeth = findViewById(R.id.img_brush_teeth)
        imgWashFace = findViewById(R.id.img_wash_face)

        // Set tags for identification
        imgGetUp.tag = "get_up"
        imgBrushTeeth.tag = "brush_teeth"
        imgWashFace.tag = "wash_face"

        // Start button listener
        btnStart.setOnClickListener {
            startGame()
        }
    }

    private fun setupTimer() {
        // Initialize timer runnable
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    elapsedTime = System.currentTimeMillis() - gameStartTime
                    updateTimerDisplay()
                    timerHandler.postDelayed(this, 1000) // Update every second
                }
            }
        }
    }

    private fun updateTimerDisplay() {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / (1000 * 60)) % 60

        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun startTimer() {
        gameStartTime = System.currentTimeMillis()
        elapsedTime = 0L
        isTimerRunning = true
        timerHandler.post(timerRunnable)
        timerTextView.visibility = View.VISIBLE
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        elapsedTime = System.currentTimeMillis() - gameStartTime
        updateTimerDisplay()
    }

    private fun setupInitialView() {
        // Show horizontal layout and start button
        horizontalContainer.visibility = View.VISIBLE
        btnStart.visibility = View.VISIBLE
        timerTextView.visibility = View.GONE // Hide timer initially

        // Hide vertical layout and other elements
        verticalContainer.visibility = View.GONE
        orderDisplay.visibility = View.GONE
        tvInstruction.visibility = View.GONE

        // Reset game state
        resetGameState()

        // Update game title
        gameTitle.text = "Remember the correct order"

        // Update instruction
        tvInstruction.text = "Study the sequence, then click Start"
        tvInstruction.visibility = View.VISIBLE
    }

    private fun startGame() {
        // Hide start button
        btnStart.visibility = View.GONE

        // Start the timer
        startTimer()

        // Transition to vertical layout
        transitionToVerticalLayout()
    }

    private fun transitionToVerticalLayout() {
        // Animate horizontal layout fading out
        val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        horizontalContainer.startAnimation(fadeOut)

        Handler().postDelayed({
            // Hide horizontal layout
            horizontalContainer.visibility = View.GONE

            // Show vertical layout and other UI elements
            verticalContainer.visibility = View.VISIBLE
            orderDisplay.visibility = View.VISIBLE
            tvInstruction.visibility = View.VISIBLE

            // Update game title and instruction
            gameTitle.text = "Arrange in Correct Sequence"
            tvInstruction.text = "Tap the images in correct order"

            // Create vertical layout with jumbled images
            createVerticalLayout()

            // Animate vertical layout fading in
            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            verticalContainer.startAnimation(fadeIn)

        }, 300)
    }

    private fun createVerticalLayout() {
        // Clear previous vertical layout
        verticalContainer.removeAllViews()
        verticalImages.clear()

        // Create a shuffled/jumbled order
        val jumbledOrder = correctOrder.shuffled()

        // Add each image to vertical layout
        jumbledOrder.forEachIndexed { index, imageType ->
            // Create FrameLayout container
            val frameLayout = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    width = 150
                    height = 150
                    setMargins(0, 16, 0, 16)
                }
            }

            // Create ImageView
            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setImageResource(imageResources[imageType] ?: R.drawable.img_get_up)
                tag = imageType
                isClickable = true

                setOnClickListener {
                    onImageSelected(imageType, this)
                }
            }

            frameLayout.addView(imageView)
            verticalContainer.addView(frameLayout)

            // Store reference
            verticalImages.add(imageView)
        }
    }

    private fun onImageSelected(imageType: String, imageView: ImageView) {
        if (isGameComplete || selectedOrder.contains(imageType)) return

        // Add to selected order
        selectedOrder.add(imageType)

        // Play bounce animation
        imageView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                imageView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()

        // Mark as selected
        markImageAsSelected(imageView, selectedOrder.size.toString())

        // Update order display
        updateOrderDisplay()

        // Check if all images selected
        if (selectedOrder.size == 3) {
            checkOrder()
        }
    }

    private fun markImageAsSelected(imageView: ImageView, orderNumber: String) {
        imageView.alpha = 0.7f
        imageView.isClickable = false

        // Add badge
        val parent = imageView.parent as? FrameLayout
        parent?.let {
            // Remove existing badges
            for (i in it.childCount - 1 downTo 0) {
                val child = it.getChildAt(i)
                if (child is TextView && child.tag == "badge") {
                    it.removeView(child)
                }
            }

            // Add new badge
            val badge = TextView(this).apply {
                text = orderNumber
                tag = "badge"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))

                try {
                    setBackgroundResource(R.drawable.badge_background)
                } catch (e: Exception) {
                    setBackgroundColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.holo_red_dark))
                }

                gravity = Gravity.CENTER

                val params = FrameLayout.LayoutParams(36, 36).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = 5
                    rightMargin = 5
                }
                layoutParams = params
            }

            it.addView(badge)
        }
    }

    private fun updateOrderDisplay() {
        selectedOrder.forEachIndexed { index, imageType ->
            val stepName = when (imageType) {
                "get_up" -> "Get Up"
                "brush_teeth" -> "Brush Teeth"
                "wash_face" -> "Wash Face"
                else -> "Unknown"
            }

            when (index) {
                0 -> tvOrder1.text = "1. $stepName"
                1 -> tvOrder2.text = "2. $stepName"
                2 -> tvOrder3.text = "3. $stepName"
            }
        }
    }

    private fun checkOrder() {
        // Disable all images during check
        verticalImages.forEach { it.isClickable = false }

        Handler().postDelayed({
            val isCorrect = selectedOrder == correctOrder

            // Increment attempts counter
            attemptsCount++

            if (isCorrect) {
                // Correct order
                correctAnswers = 3
                playSuccessAnimation()
                isGameComplete = true

                // Stop the timer
                stopTimer()

                Handler().postDelayed({
                    goToScoreboard()
                }, 2000)
            } else {
                // Wrong order
                playErrorAnimation()

                Handler().postDelayed({
                    // Reset for retry
                    resetForRetry()
                }, 1500)
            }
        }, 500)
    }

    private fun resetForRetry() {
        selectedOrder.clear()

        // Clear order display
        tvOrder1.text = "1. "
        tvOrder2.text = "2. "
        tvOrder3.text = "3. "

        // Reset vertical images
        verticalImages.forEach { imageView ->
            imageView.alpha = 1f
            imageView.isClickable = true

            // Remove badges
            val parent = imageView.parent as? FrameLayout
            parent?.let {
                for (i in it.childCount - 1 downTo 0) {
                    val child = it.getChildAt(i)
                    if (child is TextView && child.tag == "badge") {
                        it.removeView(child)
                    }
                }
            }
        }
    }

    private fun resetGameState() {
        selectedOrder.clear()
        isGameComplete = false
        attemptsCount = 0
        correctAnswers = 0

        // Clear order display
        tvOrder1.text = "1. "
        tvOrder2.text = "2. "
        tvOrder3.text = "3. "

        // Reset timer display
        timerTextView.text = "00:00"
    }

    private fun playSuccessAnimation() {
        verticalImages.forEach { imageView ->
            imageView.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(500)
                .withEndAction {
                    imageView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(500)
                        .start()
                }
                .start()
        }

        Toast.makeText(this, "Perfect! Correct order!", Toast.LENGTH_SHORT).show()
    }

    private fun playErrorAnimation() {
        verticalImages.forEach { imageView ->
            imageView.animate()
                .translationXBy(20f)
                .setDuration(100)
                .withEndAction {
                    imageView.animate()
                        .translationXBy(-40f)
                        .setDuration(100)
                        .withEndAction {
                            imageView.animate()
                                .translationXBy(20f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
                .start()
        }

        Toast.makeText(this, "Wrong order! Try again.", Toast.LENGTH_SHORT).show()
    }

    private fun goToScoreboard() {
        val intent = Intent(this, ASequenceScoreboardActivity::class.java)
        intent.putExtra("CORRECT_ANSWERS", correctAnswers)
        intent.putExtra("TOTAL_QUESTIONS", totalQuestions)
        intent.putExtra("ATTEMPTS", attemptsCount) // Send actual attempts count
        intent.putExtra("ELAPSED_TIME", elapsedTime) // Send elapsed time
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop timer to prevent memory leaks
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }
}