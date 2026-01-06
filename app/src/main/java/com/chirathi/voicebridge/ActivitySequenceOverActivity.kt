package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.DragEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat

class ActivitySequenceOverActivity : AppCompatActivity() {

    // Define the correct order for morning routine (for ages 8-10)
    private val correctOrder = listOf(
        "Wake Up",
        "Brush Teeth",
        "Wash Face",
        "Eat Breakfast",
        "Go to School"
    )

    // Store the current order of options
    private var currentOrder = mutableListOf<String>()

    // Store references to the option layouts
    private val optionLayouts = mutableListOf<LinearLayoutCompat>()
    private val optionTexts = mutableListOf<String>()

    // UI Elements
    private lateinit var option1: LinearLayoutCompat
    private lateinit var option2: LinearLayoutCompat
    private lateinit var option3: LinearLayoutCompat
    private lateinit var option4: LinearLayoutCompat
    private lateinit var option5: LinearLayoutCompat
    private lateinit var btnCheck: Button
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_over)

        initializeViews()
        setupClickListeners()
        setupDragAndDrop()
        resetGame()
    }

    private fun initializeViews() {
        // Initialize option layouts
        option1 = findViewById(R.id.option1_layout)
        option2 = findViewById(R.id.option2_layout)
        option3 = findViewById(R.id.option3_layout)
        option4 = findViewById(R.id.option4_layout)
        option5 = findViewById(R.id.option5_layout)

        // Add to list for easy access
        optionLayouts.addAll(listOf(option1, option2, option3, option4, option5))

        // Store the original text values
        optionTexts.addAll(listOf(
            getOptionText(option1),
            getOptionText(option2),
            getOptionText(option3),
            getOptionText(option4),
            getOptionText(option5)
        ))

        // Initialize buttons
        btnCheck = findViewById(R.id.btn_check)
        btnReset = findViewById(R.id.btn_reset)
    }

    private fun getOptionText(layout: LinearLayoutCompat): String {
        // Find the TextView inside the layout and get its text
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is TextView) {
                return child.text.toString()
            }
        }
        return ""
    }

    private fun setupClickListeners() {
        // Setup sound buttons for each option
        setupSoundButton(option1, findViewById(R.id.sound_option1))
        setupSoundButton(option2, findViewById(R.id.sound_option2))
        setupSoundButton(option3, findViewById(R.id.sound_option3))
        setupSoundButton(option4, findViewById(R.id.sound_option4))
        setupSoundButton(option5, findViewById(R.id.sound_option5))

        // Check button click
        btnCheck.setOnClickListener {
            checkOrder()
        }

        // Reset button click
        btnReset.setOnClickListener {
            resetGame()
        }
    }

    private fun setupSoundButton(layout: LinearLayoutCompat, soundButton: ImageButton) {
        soundButton.setOnClickListener {
            // Get the option text
            val optionText = getOptionText(layout)
            // Play sound for the option
            speakOption(optionText)
        }
    }

    private fun speakOption(optionText: String) {
        // Implement text-to-speech here
        Toast.makeText(this, "Speaking: $optionText", Toast.LENGTH_SHORT).show()
        // Add TextToSpeech implementation here
    }

    private fun setupDragAndDrop() {
        // Make all options draggable
        optionLayouts.forEach { layout ->
            layout.setOnLongClickListener { view ->
                // Start drag operation
                val shadowBuilder = View.DragShadowBuilder(view)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    view.startDragAndDrop(null, shadowBuilder, view, 0)
                } else {
                    @Suppress("DEPRECATION")
                    view.startDrag(null, shadowBuilder, view, 0)
                }
                view.visibility = View.INVISIBLE
                true
            }

            // Setup drop target
            layout.setOnDragListener(dragListener)
        }
    }

    private val dragListener = View.OnDragListener { view, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // Change appearance when drag starts
                view.background = ContextCompat.getDrawable(this, R.drawable.drop_target_background)
                true
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                // Highlight the drop target
                view.background = ContextCompat.getDrawable(this, R.drawable.drop_target_highlight)
                true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                // Remove highlight
                view.background = ContextCompat.getDrawable(this, R.drawable.drop_target_background)
                true
            }

            DragEvent.ACTION_DROP -> {
                // Handle the drop
                val draggedView = event.localState as LinearLayoutCompat
                val targetView = view as LinearLayoutCompat

                // Don't swap if it's the same view
                if (draggedView != targetView) {
                    // Swap the contents of draggedView and targetView
                    swapOptionContents(draggedView, targetView)
                }

                // Make draggedView visible again
                draggedView.visibility = View.VISIBLE

                // Reset background
                view.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_background)
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                // Reset all backgrounds and make all views visible
                optionLayouts.forEach { layout ->
                    layout.background = ContextCompat.getDrawable(this, R.drawable.rounded_button_background)
                    layout.visibility = View.VISIBLE
                }
                true
            }

            else -> false
        }
    }

    private fun swapOptionContents(view1: LinearLayoutCompat, view2: LinearLayoutCompat) {
        // Get the text from both views
        val text1 = getOptionText(view1)
        val text2 = getOptionText(view2)

        // Swap the text in TextViews
        setOptionText(view1, text2)
        setOptionText(view2, text1)
    }

    private fun setOptionText(layout: LinearLayoutCompat, text: String) {
        // Find the TextView inside the layout and set its text
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is TextView) {
                child.text = text
                break
            }
        }
    }

    private fun checkOrder() {
        // Get current order from UI
        currentOrder.clear()
        optionLayouts.forEach { layout ->
            currentOrder.add(getOptionText(layout))
        }

        // Check if order is correct
        val isCorrect = currentOrder == correctOrder

        if (isCorrect) {
            // Show success
            playSuccessAnimation()
            Toast.makeText(this, "Perfect! Correct order!", Toast.LENGTH_SHORT).show()

            // Delay before moving to scoreboard
            Handler().postDelayed({
                goToScoreboard(correctAnswers = 5)
            }, 2000)
        } else {
            // Show error
            playErrorAnimation()
            Toast.makeText(this, "Wrong order! Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playSuccessAnimation() {
        // Highlight all options in green
        optionLayouts.forEach { layout ->
            layout.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))

            // Add checkmark animation or effect
            layout.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    layout.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
        }
    }

    private fun playErrorAnimation() {
        // Shake animation for wrong order
        optionLayouts.forEach { layout ->
            layout.animate()
                .translationXBy(20f)
                .setDuration(100)
                .withEndAction {
                    layout.animate()
                        .translationXBy(-40f)
                        .setDuration(100)
                        .withEndAction {
                            layout.animate()
                                .translationXBy(20f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
                .start()
        }
    }

    private fun resetGame() {
        // Reset to original order
        for (i in optionLayouts.indices) {
            setOptionText(optionLayouts[i], optionTexts[i])

            // Reset appearance
            optionLayouts[i].background = ContextCompat.getDrawable(this, R.drawable.rounded_button_background)
            optionLayouts[i].visibility = View.VISIBLE
            optionLayouts[i].scaleX = 1f
            optionLayouts[i].scaleY = 1f
            optionLayouts[i].translationX = 0f
        }

        currentOrder.clear()
        Toast.makeText(this, "Game reset!", Toast.LENGTH_SHORT).show()
    }

    private fun goToScoreboard(correctAnswers: Int) {
        val intent = Intent(this, ASequenceScoreboardActivity::class.java)
        intent.putExtra("CORRECT_ANSWERS", correctAnswers)
        intent.putExtra("TOTAL_QUESTIONS", 5)
        startActivity(intent)
        finish()
    }
}