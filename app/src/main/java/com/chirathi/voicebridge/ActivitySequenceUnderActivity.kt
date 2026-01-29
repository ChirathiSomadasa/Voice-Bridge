package com.chirathi.voicebridge

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
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
import java.util.Locale

class ActivitySequenceUnderActivity : AppCompatActivity() {

    // Therapeutic model instance
    private lateinit var gameMaster: GameMasterModel

    // Speech synthesis for auditory hints
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    // Sound effects for visual hints and bubbles
    private lateinit var bubblePopSound: MediaPlayer
    private lateinit var correctSound: MediaPlayer
    private lateinit var hintSound: MediaPlayer

    private var isNavigatingToSticker = false

    // Session tracking for behavioral metrics
    private data class TapMetrics(
        var startTime: Long = System.currentTimeMillis(),
        var tapTimes: MutableList<Long> = mutableListOf(),
        var tapPositions: MutableList<Pair<Int, Int>> = mutableListOf(),
        var hesitationTimes: MutableList<Long> = mutableListOf(),
        var correctCount: Int = 0,
        var errorCount: Int = 0,
        var sessionAlpha: Float = 0f,
        var completedSubRoutines: MutableList<Pair<Int, Int>> = mutableListOf()
    )

    private val sessionMetrics = TapMetrics()
    private fun getLocalAlpha(): Float = calculateFinalAlpha()

    // Bubble progress tracking
    private var totalBubbles = 5
    private var poppedBubbles = 0
    private lateinit var bubbleContainer: LinearLayout
    private val bubbleViews = mutableListOf<ImageView>()

    // Model-driven variables
    private var currentRoutineId: Int = 0
    private var currentSubRoutineId: Int = 0
    private var currentDifficultyLevel: Int = 0
    private var shouldShowHints: Boolean = false
    private var hintType: String = "none"
    private var currentCorrectOrder = mutableListOf<String>()
    private var userAge: Int = 6

    // User selection tracking
    private var userSelectedRoutineId: Int = -1

    // Visual hint tracking
    private var hintActive = false
    private var currentHintTarget: ImageView? = null

    // Performance tracking
    private var attemptsCount = 0
    private var correctAnswers = 0
    private var totalQuestions = 3
    private var isGameComplete = false

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
    private lateinit var hintIndicator: ImageView
    private lateinit var visualHintOverlay: View

    // TextViews for order display
    private lateinit var tvOrder1: TextView
    private lateinit var tvOrder2: TextView
    private lateinit var tvOrder3: TextView

    // Add this mapping at the class level:
    private val imageNameToStepName = mapOf(
        // Morning Routine Sub1
        "seq_rtn0_sub1_1_wakeup" to "Wake Up",
        "seq_rtn0_sub1_2_bed" to "Make Bed",
        "seq_rtn0_sub1_3_drink" to "Drink Water",

        // Morning Routine Sub2
        "seq_rtn0_sub2_1_brush" to "Brush Teeth",
        "seq_rtn0_sub2_2_wash" to "Wash Face",
        "seq_rtn0_sub2_3_dry" to "Dry with Towel",

        // Morning Routine Sub3
        "seq_rtn0_sub3_1_change" to "Get Dressed",
        "seq_rtn0_sub3_2_cream" to "Apply Powder",
        "seq_rtn0_sub3_3_wash" to "Put Pajamas Away"
    )

    private val imageResourceToStepId = mapOf(
        R.drawable.seq_rtn0_sub1_1_wakeup to "wake_up",
        R.drawable.seq_rtn0_sub1_2_bed to "make_bed",
        R.drawable.seq_rtn0_sub1_3_drink to "drink_water",

        R.drawable.seq_rtn0_sub2_1_brush to "brush_teeth",
        R.drawable.seq_rtn0_sub2_2_wash to "wash_face",
        R.drawable.seq_rtn0_sub2_3_dry to "dry_towel",

        R.drawable.seq_rtn0_sub3_1_change to "get_dressed",
        R.drawable.seq_rtn0_sub3_2_cream to "apply_powder",
        R.drawable.seq_rtn0_sub3_3_wash to "put_pajamas"
    )

    // Store vertical image views
    private val verticalImages = mutableListOf<ImageView>()

    // Model-driven routine definitions with therapeutic difficulty levels
    private val routines = mapOf(
        0 to mapOf( // Morning Routine (rtn0)
            0 to Triple(listOf("wake_up", "make_bed", "drink_water"), 1, "Wake up routine"),
            1 to Triple(listOf("brush_teeth", "wash_face", "dry_towel"), 2, "Hygiene sequence"),
            2 to Triple(listOf("get_dressed", "apply_powder", "put_pajamas"), 3, "Getting dressed")
        ),
        1 to mapOf( // Bedtime Routine (rtn1) - Placeholder for future
            0 to Triple(listOf("pajamas", "story", "sleep"), 2, "Wind down"),
            1 to Triple(listOf("brush_teeth_night", "wash_face_night", "toilet"), 3, "Night routine"),
            2 to Triple(listOf("lights", "tuck_in", "goodnight"), 4, "Sleep preparation")
        ),
        2 to mapOf( // School Routine (rtn2) - Placeholder for future
            0 to Triple(listOf("uniform", "shoes", "bag"), 3, "Preparation"),
            1 to Triple(listOf("breakfast", "lunchbox", "water_bottle"), 4, "Meal packing"),
            2 to Triple(listOf("bus", "class", "playground"), 5, "Going out")
        )
    )

    // Image resources mapping - ONLY morning routine for now
    private val imageResources = mapOf(
        // Morning Routine Sub1 (wake up sequence)
        "wake_up" to R.drawable.seq_rtn0_sub1_1_wakeup,
        "make_bed" to R.drawable.seq_rtn0_sub1_2_bed,
        "drink_water" to R.drawable.seq_rtn0_sub1_3_drink,

        // Morning Routine Sub2 (hygiene sequence)
        "brush_teeth" to R.drawable.seq_rtn0_sub2_1_brush,
        "wash_face" to R.drawable.seq_rtn0_sub2_2_wash,
        "dry_towel" to R.drawable.seq_rtn0_sub2_3_dry,

        // Morning Routine Sub3 (getting dressed)
        "get_dressed" to R.drawable.seq_rtn0_sub3_1_change,
        "apply_powder" to R.drawable.seq_rtn0_sub3_2_cream,
        "put_pajamas" to R.drawable.seq_rtn0_sub3_3_wash,
    )

    // Array resources for easier access
    private val routineArrayResources = mapOf(
        Pair(0, 0) to R.array.seq_rtn0_sub1,  // rtn0, sub1
        Pair(0, 1) to R.array.seq_rtn0_sub2,  // rtn0, sub2
        Pair(0, 2) to R.array.seq_rtn0_sub3,  // rtn0, sub3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_under)

        Log.d(TAG, "onCreate: Activity started")

        // Get user's routine selection from intent
        userSelectedRoutineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        Log.d(TAG, "User selected routine: $userSelectedRoutineId")

        // Initialize therapeutic model
        gameMaster = GameMasterModel(this)
        Log.d(TAG, "GameMaster model initialized")

        // Initialize UI FIRST
        initViews()

        // Initialize TTS for auditory hints
        initializeTextToSpeech()

        // Initialize sound effects
        initializeSounds()

        setupTimer()
        setupBubbleProgress()

        // Load user profile for age
        loadUserProfile()
        // Note: sessionStart() is called from loadUserProfile()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech.language = Locale.US
                textToSpeech.setSpeechRate(0.7f)
                textToSpeech.setPitch(1.2f)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun initializeSounds() {
        bubblePopSound = MediaPlayer.create(this, R.raw.bubble_pop)
        correctSound = MediaPlayer.create(this, R.raw.correct_sound)
        hintSound = MediaPlayer.create(this, R.raw.hint_sound)
        Log.d(TAG, "Sound effects initialized")
    }

    private fun setupBubbleProgress() {
        bubbleContainer = findViewById(R.id.bubbleContainer)
        bubbleContainer.removeAllViews()
        bubbleViews.clear()

        for (i in 0 until totalBubbles) {
            val bubble = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                    marginEnd = 10
                }
                setImageResource(R.drawable.bubble_full)
                tag = "bubble_$i"
            }
            bubbleContainer.addView(bubble)
            bubbleViews.add(bubble)
        }
        Log.d(TAG, "Bubble progress setup: $totalBubbles bubbles")
    }

    private fun popBubble() {
        if (poppedBubbles < totalBubbles) {
            val bubble = bubbleViews[poppedBubbles]

            Log.d(TAG, "Popping bubble ${poppedBubbles + 1}/$totalBubbles")

            // Change to popped state
            bubble.setImageResource(R.drawable.bubble_popped)

            // Pop animation
            bubble.animate()
                .scaleX(1.5f)
                .scaleY(1.5f)
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    bubble.visibility = View.GONE
                }
                .start()

            // Play pop sound
            bubblePopSound.seekTo(0)
            bubblePopSound.start()

            poppedBubbles++

            // Check for sticker unlock
            Log.d(TAG, "Sub-routine completed! Checking for sticker...")
            checkStickerUnlock()

            // IMPORTANT: DON'T schedule onSessionComplete here anymore
            // The sticker activity will handle navigation, OR checkStickerUnlock()
            // will schedule it if no sticker is unlocked
        }
    }

    private fun onSessionComplete() {
        Log.d(TAG, "onSessionComplete called")
        stopTimer()  // Stop timer before navigation

        // Calculate final therapeutic metrics
        val finalAlpha = calculateFinalAlpha()
        sessionMetrics.sessionAlpha = finalAlpha

        Log.d(TAG, "Final Alpha: $finalAlpha")
        Log.d(TAG, "Correct Count: ${sessionMetrics.correctCount}")
        Log.d(TAG, "Error Count: ${sessionMetrics.errorCount}")
        Log.d(TAG, "Completed Sub-routines: ${sessionMetrics.completedSubRoutines.size}")

        // Navigate to therapeutic star staircase
        val intent = Intent(this, ASequenceScoreboardActivity::class.java)
        intent.putExtra("FINAL_ALPHA", finalAlpha)
        intent.putExtra("POPPED_BUBBLES", poppedBubbles)
        intent.putExtra("TOTAL_BUBBLES", totalBubbles)
        intent.putExtra("CORRECT_COUNT", sessionMetrics.correctCount)
        intent.putExtra("ERROR_COUNT", sessionMetrics.errorCount)
        intent.putExtra("COMPLETED_SUBROUTINES", sessionMetrics.completedSubRoutines.size)
        intent.putExtra("ROUTINE_COMPLETED", currentRoutineId)

        // Pass performance data for model decision in next session
        intent.putExtra("PREVIOUS_ALPHA", finalAlpha)
        intent.putExtra("PREVIOUS_CORRECT", sessionMetrics.correctCount)
        intent.putExtra("PREVIOUS_SUBROUTINE", currentSubRoutineId)
        intent.putExtra("PREVIOUS_ERROR", if (sessionMetrics.errorCount > 0) "positional" else "none")

        startActivity(intent)
        finish()
    }

    private fun loadUserProfile() {
        userAge = intent.getIntExtra("USER_AGE", 6)
        Log.d(TAG, "User age loaded: $userAge")
        sessionStart(userAge)
    }

    private fun sessionStart(userAge: Int) {
        Log.d(TAG, "sessionStart: Age=$userAge, SelectedRoutine=$userSelectedRoutineId")

        // Set routine based on user selection
        if (userSelectedRoutineId != -1) {
            currentRoutineId = userSelectedRoutineId
            Log.d(TAG, "Using user-selected routine: $currentRoutineId")
        } else {
            currentRoutineId = 0 // Default to morning routine
            Log.d(TAG, "No user selection, defaulting to routine 0")
        }

        // Prepare initial features for model to select SUB-ROUTINE
        val initialFeatures = prepareBehavioralFeatures(isInitial = true)

        // Get model decision for this session (mainly for sub-routine selection)
        val decision = gameMaster.predict(initialFeatures)
        Log.d(TAG, "Model decision received: ${decision.routineAction}, ${decision.sequenceAction}")

        // MODEL DECIDES: Which sub-routine within the selected routine
        selectSubRoutineBasedOnPerformance(decision)

        // Get the correct order for selected routine and sub-routine
        val routineData = routines[currentRoutineId]?.get(currentSubRoutineId)
        if (routineData != null) {
            currentCorrectOrder = routineData.first.toMutableList()
            currentDifficultyLevel = routineData.second
            Log.d(TAG, "Routine data loaded: ${routineData.third}, Difficulty: $currentDifficultyLevel")
            Log.d(TAG, "Correct order: $currentCorrectOrder")

            // Now we have currentCorrectOrder, we can show initial images
            // Use Handler to ensure UI thread safety
            Handler(Looper.getMainLooper()).post {
                showInitialCorrectOrder()
            }
        } else {
            Log.e(TAG, "No routine data found for routine=$currentRoutineId, sub=$currentSubRoutineId")
            currentCorrectOrder = mutableListOf("wake_up", "make_bed", "drink_water") // Default fallback
            Handler(Looper.getMainLooper()).post {
                showInitialCorrectOrder()
            }
        }

        shouldShowHints = decision.sequenceAction > 0
        hintType = when (decision.sequenceAction) {
            1 -> "visual"
            2 -> "auditory"
            else -> "none"
        }

        Log.d(TAG, "Hints: $shouldShowHints, Type: $hintType")

        // Update UI based on model decision
        updateInstructionBasedOnModel(decision)
        updateHintIndicator()
    }

    private fun selectSubRoutineBasedOnPerformance(decision: ModelDecision) {
        Log.d(TAG, "selectSubRoutineBasedOnPerformance called")

        val previousAlpha = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        val previousPerformance = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        val previousSubRoutine = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        val previousError = intent.getStringExtra("PREVIOUS_ERROR")

        Log.d(TAG, "Previous: Alpha=$previousAlpha, Correct=$previousPerformance, " +
                "SubRoutine=$previousSubRoutine, Error=$previousError")

        // Use the model's sub-routine recommendation
        currentSubRoutineId = when (decision.subRoutineRecommendation) {
            0 -> { // Repeat same sub-routine
                if (previousSubRoutine != -1) {
                    Log.d(TAG, "Model recommends: Repeat sub-routine $previousSubRoutine")
                    previousSubRoutine
                } else {
                    Log.d(TAG, "No previous sub-routine, defaulting to 0")
                    0
                }
            }
            1 -> { // Stay at current (if progressing, stay at next)
                val currentOrNext = if (previousSubRoutine != -1) previousSubRoutine else 0
                Log.d(TAG, "Model recommends: Stay at sub-routine $currentOrNext")
                currentOrNext
            }
            2 -> { // Progress to next sub-routine
                val nextSub = if (previousSubRoutine != -1) ((previousSubRoutine + 1) % 3) else 0
                Log.d(TAG, "Model recommends: Progress to sub-routine $nextSub")
                nextSub
            }
            else -> {
                Log.w(TAG, "Unknown recommendation, defaulting to 0")
                0
            }
        }.coerceIn(0, 2)

        Log.d(TAG, "Selected sub-routine: $currentSubRoutineId")
    }

    private fun prepareBehavioralFeatures(
        isInitial: Boolean = false,
        currentAccuracy: Float = 0f,
        avgResponseTime: Long = 0L,
        jitter: Float = 0f
    ): FloatArray {
        val rapidTaps = sessionMetrics.tapTimes.size.toFloat()
        val hesitation = if (sessionMetrics.hesitationTimes.isNotEmpty()) {
            sessionMetrics.hesitationTimes.average().toFloat()
        } else 0f

        // Calculate Achievement Index (alpha)
        val alpha = if (isInitial) {
            5.0f // Default starting alpha
        } else {
            calculateAlpha(
                accuracy = currentAccuracy,
                responseTime = avgResponseTime,
                age = userAge,
                errorCount = sessionMetrics.errorCount,
                difficulty = currentDifficultyLevel
            )
        }

        Log.d(TAG, "Behavioral features - Alpha: $alpha, Hesitation: $hesitation, " +
                "RapidTaps: $rapidTaps, Accuracy: $currentAccuracy")

        return floatArrayOf(
            userAge.toFloat(),          // Feature 0: Age
            hesitation,                 // Feature 1: Hesitation
            rapidTaps,                  // Feature 2: Rapid Taps
            jitter,                     // Feature 3: Jitter
            avgResponseTime.toFloat(),  // Feature 4: Response Time
            currentAccuracy,            // Feature 5: Accuracy
            currentDifficultyLevel.toFloat(), // Feature 6: Difficulty Weight
            alpha                       // Feature 7: Achievement Index
        )
    }

    private fun calculateAlpha(
        accuracy: Float,
        responseTime: Long,
        age: Int,
        errorCount: Int,
        difficulty: Int
    ): Float {
        // Therapeutic alpha calculation with difficulty adjustment
        val ageWeight = 1.0f + ((age - 6) * 0.1f)
        val errorPenalty = if (errorCount > 0) (errorCount * 0.15f) else 0f
        val difficultyBonus = difficulty * 0.2f

        val baseAlpha = (accuracy * 10 * ageWeight) - errorPenalty + difficultyBonus

        // Handle log calculation safely
        val responseSeconds = responseTime / 1000f
        val timeFactor = if (responseSeconds > 0) {
            kotlin.math.ln(responseSeconds + 1)
        } else {
            1.0f
        }

        val safeTimeFactor = if (timeFactor <= 0) 1.0f else timeFactor
        val finalAlpha = (baseAlpha / safeTimeFactor).coerceIn(0f, 10f)

        Log.d(TAG, "Alpha calculation: Base=$baseAlpha, TimeFactor=$safeTimeFactor, Final=$finalAlpha")

        return finalAlpha
    }

    private fun calculateFinalAlpha(): Float {
        val totalTasks = attemptsCount
        val correctRate = if (totalTasks > 0) {
            sessionMetrics.correctCount.toFloat() / totalTasks * 100
        } else 0f

        val avgResponseTime = if (sessionMetrics.tapTimes.size >= 2) {
            val totalTime = sessionMetrics.tapTimes.last() - sessionMetrics.tapTimes.first()
            totalTime / sessionMetrics.tapTimes.size
        } else 0L

        val finalAlpha = calculateAlpha(
            accuracy = correctRate,
            responseTime = avgResponseTime,
            age = userAge,
            errorCount = sessionMetrics.errorCount,
            difficulty = currentDifficultyLevel
        )

        Log.d(TAG, "Final Alpha calculation: CorrectRate=$correctRate, " +
                "AvgResponse=$avgResponseTime, Errors=${sessionMetrics.errorCount}, Alpha=$finalAlpha")

        return finalAlpha
    }

    private fun updateInstructionBasedOnModel(decision: ModelDecision) {
        Log.d(TAG, "updateInstructionBasedOnModel: Routine=$currentRoutineId, Sub=$currentSubRoutineId")

        // Set routine name based on currentRoutineId
        val routineName = when (currentRoutineId) {
            0 -> "Morning Routine"
            1 -> "Bedtime Routine"
            2 -> "School Routine"
            else -> "Daily Activity"
        }

        // Get sub-routine description
        val subRoutineDesc = routines[currentRoutineId]?.get(currentSubRoutineId)?.third ?: ""
        val title = "$routineName: $subRoutineDesc"

        // Run on UI thread
        runOnUiThread {
            gameTitle.text = title
        }

        Log.d(TAG, "Title set: $title")

        // Set therapeutic instruction
        val instruction = when (decision.sequenceAction) {
            1 -> "Take your time. I'll show you where to tap!"
            2 -> "Listen carefully. I'll tell you what's next!"
            else -> "Remember the order. You can do it!"
        }

        runOnUiThread {
            tvInstruction.text = instruction
        }

        Log.d(TAG, "Instruction: $instruction")
    }

    private fun updateHintIndicator() {
        runOnUiThread {
            hintIndicator.visibility = if (shouldShowHints) View.VISIBLE else View.GONE
            hintIndicator.setImageResource(
                when (hintType) {
                    "visual" -> R.drawable.ic_eye_hint
                    "auditory" -> R.drawable.ic_ear_hint
                    else -> R.drawable.ic_no_hint
                }
            )
            Log.d(TAG, "Hint indicator: $hintType (visible=${hintIndicator.visibility == View.VISIBLE})")
        }
    }

    private fun initViews() {
        Log.d(TAG, "Initializing views...")

        try {
            horizontalContainer = findViewById(R.id.horizontal_images_container)
            verticalContainer = findViewById(R.id.vertical_images_container)
            orderDisplay = findViewById(R.id.order_display_container)
            gameTitle = findViewById(R.id.game_title)
            tvInstruction = findViewById(R.id.tv_instruction)
            btnStart = findViewById(R.id.btn_start)
            timerTextView = findViewById(R.id.timerTextView)
            hintIndicator = findViewById(R.id.hintIndicator)
            visualHintOverlay = findViewById(R.id.visualHintOverlay)

            tvOrder1 = findViewById(R.id.tv_order_1)
            tvOrder2 = findViewById(R.id.tv_order_2)
            tvOrder3 = findViewById(R.id.tv_order_3)

            Log.d(TAG, "All views found successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error finding views: ${e.message}")
            e.printStackTrace()
            // Try to recover or show error
            Toast.makeText(this, "Error initializing game. Please restart.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        btnStart.setOnClickListener {
            Log.d(TAG, "Start button clicked")
            startGame()
        }

        // Setup the initial view state
        setupInitialView()

        Log.d(TAG, "Views initialized")
    }

    private fun setupTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    elapsedTime = System.currentTimeMillis() - gameStartTime
                    updateTimerDisplay()
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        Log.d(TAG, "Timer setup complete")
    }

    private fun updateTimerDisplay() {
        runOnUiThread {
            val seconds = (elapsedTime / 1000) % 60
            val minutes = (elapsedTime / (1000 * 60)) % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun startTimer() {
        gameStartTime = System.currentTimeMillis()
        elapsedTime = 0L
        isTimerRunning = true
        timerHandler.post(timerRunnable)
        runOnUiThread {
            timerTextView.visibility = View.VISIBLE
        }
        Log.d(TAG, "Timer started")
    }

    private fun setupInitialView() {
        runOnUiThread {
            try {
                horizontalContainer.visibility = View.VISIBLE
                btnStart.visibility = View.VISIBLE
                timerTextView.visibility = View.GONE
                verticalContainer.visibility = View.GONE
                orderDisplay.visibility = View.GONE
                visualHintOverlay.visibility = View.GONE

                resetGameState()
                Log.d(TAG, "Initial view setup complete")
            } catch (e: Exception) {
                Log.e(TAG, "Error in setupInitialView: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showInitialCorrectOrder() {
        Log.d(TAG, "showInitialCorrectOrder called")

        runOnUiThread {
            try {
                // Check if everything is properly initialized
                if (!this::horizontalContainer.isInitialized) {
                    Log.e(TAG, "horizontalContainer is not initialized!")
                    return@runOnUiThread
                }

                if (currentCorrectOrder.isEmpty()) {
                    Log.e(TAG, "currentCorrectOrder is empty")
                    return@runOnUiThread
                }

                Log.d(TAG, "Showing initial correct order: $currentCorrectOrder")

                // Use the existing placeholder ImageViews instead of creating new ones
                val imageViews = listOf(
                    findViewById<ImageView>(R.id.horizontal_img_1),
                    findViewById<ImageView>(R.id.horizontal_img_2),
                    findViewById<ImageView>(R.id.horizontal_img_3)
                )

                // Also get the frame layouts if you want to add badges
                val frameLayouts = listOf(
                    findViewById<FrameLayout>(R.id.horizontal_frame_1),
                    findViewById<FrameLayout>(R.id.horizontal_frame_2),
                    findViewById<FrameLayout>(R.id.horizontal_frame_3)
                )

                for ((index, stepId) in currentCorrectOrder.withIndex()) {
                    if (index < imageViews.size) {
                        try {
                            val drawableRes = when (stepId) {
                                "wake_up" -> R.drawable.seq_rtn0_sub1_1_wakeup
                                "make_bed" -> R.drawable.seq_rtn0_sub1_2_bed
                                "drink_water" -> R.drawable.seq_rtn0_sub1_3_drink
                                else -> R.drawable.seq_rtn0_sub1_1_wakeup
                            }

                            imageViews[index].setImageResource(drawableRes)
                            imageViews[index].tag = stepId

                            // Add badge to frame layout
                            val frameLayout = frameLayouts[index]
                            val badgeNumber = index + 1

                            // Remove existing badge if any
                            for (i in frameLayout.childCount - 1 downTo 0) {
                                val child = frameLayout.getChildAt(i)
                                if (child is TextView && child.tag == "badge") {
                                    frameLayout.removeView(child)
                                }
                            }

                            val badgeTextView = TextView(this@ActivitySequenceUnderActivity).apply {
                                text = badgeNumber.toString()
                                textSize = 14f
                                setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))
                                setBackgroundResource(R.drawable.circle_badge)
                                gravity = Gravity.CENTER
                                tag = "badge"

                                val params = FrameLayout.LayoutParams(40, 40).apply {
                                    gravity = Gravity.TOP or Gravity.END
                                    topMargin = 5
                                    rightMargin = 5
                                }
                                layoutParams = params
                            }

                            frameLayout.addView(badgeTextView)

                            Log.d(TAG, "Set image $badgeNumber for step: $stepId")

                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting image for step $index: ${e.message}")
                        }
                    }
                }

                Log.d(TAG, "Images displayed using existing placeholders")

            } catch (e: Exception) {
                Log.e(TAG, "Error in showInitialCorrectOrder: ${e.message}")
            }
        }
    }

    private fun resetGameState() {
        selectedOrder.clear()
        isGameComplete = false

        runOnUiThread {
            tvOrder1.text = "1. "
            tvOrder2.text = "2. "
            tvOrder3.text = "3. "
            timerTextView.text = "00:00"
        }
        Log.d(TAG, "Game state reset")
    }

    private fun startGame() {
        runOnUiThread {
            btnStart.visibility = View.GONE
        }
        startTimer()
        transitionToVerticalLayout()
        Log.d(TAG, "Game started")
    }

    private fun transitionToVerticalLayout() {
        Log.d(TAG, "Transitioning to vertical layout")

        runOnUiThread {
            try {
                val fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
                horizontalContainer.startAnimation(fadeOut)

                Handler().postDelayed({
                    horizontalContainer.visibility = View.GONE
                    verticalContainer.visibility = View.VISIBLE
                    orderDisplay.visibility = View.VISIBLE

                    createVerticalLayout()

                    // Show initial hint only if NOT first attempt
                    if (shouldShowHints && attemptsCount > 0) {
                        Log.d(TAG, "Showing initial hint (attempts=$attemptsCount)")
                        showInitialHint()
                    } else {
                        Log.d(TAG, "No initial hint (first attempt or hints disabled)")
                    }

                    val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
                    verticalContainer.startAnimation(fadeIn)
                    Log.d(TAG, "Vertical layout created and visible")
                }, 300)
            } catch (e: Exception) {
                Log.e(TAG, "Error in transitionToVerticalLayout: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun createVerticalLayout() {
        Log.d(TAG, "Creating vertical layout with correct order: $currentCorrectOrder")

        runOnUiThread {
            try {
                verticalContainer.removeAllViews()
                verticalImages.clear()

                // Create jumbled order of step IDs
                val jumbledStepIds = currentCorrectOrder.shuffled()
                Log.d(TAG, "Jumbled order: $jumbledStepIds")

                // Load images from array resource
                val arrayResId = routineArrayResources[Pair(currentRoutineId, currentSubRoutineId)]
                if (arrayResId == null) {
                    Log.e(TAG, "No array resource found for routine=$currentRoutineId, sub=$currentSubRoutineId")
                    return@runOnUiThread
                }

                val imageArray = resources.obtainTypedArray(arrayResId)

                // Create a mapping of step ID to drawable resource
                val stepToDrawableMap = when (currentSubRoutineId) {
                    0 -> mapOf(
                        "wake_up" to R.drawable.seq_rtn0_sub1_1_wakeup,
                        "make_bed" to R.drawable.seq_rtn0_sub1_2_bed,
                        "drink_water" to R.drawable.seq_rtn0_sub1_3_drink
                    )
                    1 -> mapOf(
                        "brush_teeth" to R.drawable.seq_rtn0_sub2_1_brush,
                        "wash_face" to R.drawable.seq_rtn0_sub2_2_wash,
                        "dry_towel" to R.drawable.seq_rtn0_sub2_3_dry
                    )
                    2 -> mapOf(
                        "get_dressed" to R.drawable.seq_rtn0_sub3_1_change,
                        "apply_powder" to R.drawable.seq_rtn0_sub3_2_cream,
                        "put_pajamas" to R.drawable.seq_rtn0_sub3_3_wash
                    )
                    else -> emptyMap()
                }

                // Now create image views for each jumbled step ID
                for ((index, stepId) in jumbledStepIds.withIndex()) {
                    try {
                        val frameLayout = FrameLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                width = 180
                                height = 180
                                setMargins(0, 16, 0, 16)
                            }
                        }

                        val imageView = ImageView(this).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )

                            // Get the drawable resource for this step ID
                            val drawableRes = stepToDrawableMap[stepId] ?: 0
                            if (drawableRes != 0) {
                                setImageResource(drawableRes)
                                tag = stepId  // Tag with the step ID
                                Log.d(TAG, "Image $index: stepId=$stepId, drawable=$drawableRes")
                            } else {
                                Log.e(TAG, "No drawable resource found for stepId: $stepId")
                                setImageResource(R.drawable.seq_rtn0_sub1_1_wakeup)
                                tag = stepId
                            }

                            isClickable = true

                            // Add visual hint capability
                            if (hintType == "visual" && shouldShowHints) {
                                setOnLongClickListener {
                                    Log.d(TAG, "Long click on image: ${tag}")
                                    showVisualHintForImage(this)
                                    true
                                }
                            }

                            setOnClickListener {
                                Log.d(TAG, "Image clicked: ${tag}")
                                val tapTime = System.currentTimeMillis()
                                val hesitation = if (sessionMetrics.tapTimes.isNotEmpty()) {
                                    tapTime - sessionMetrics.tapTimes.last()
                                } else tapTime - sessionMetrics.startTime

                                sessionMetrics.tapTimes.add(tapTime)
                                sessionMetrics.hesitationTimes.add(hesitation)

                                val location = IntArray(2)
                                getLocationOnScreen(location)
                                sessionMetrics.tapPositions.add(Pair(location[0], location[1]))

                                onImageSelected(tag.toString(), this)
                            }
                        }

                        frameLayout.addView(imageView)
                        verticalContainer.addView(frameLayout)
                        verticalImages.add(imageView)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating image view $index: ${e.message}")
                        e.printStackTrace()
                    }
                }

                imageArray.recycle()
                Log.d(TAG, "Created ${verticalImages.size} image views")
            } catch (e: Exception) {
                Log.e(TAG, "Error in createVerticalLayout: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun showInitialHint() {
        // Only show hint after first incorrect attempt
        if (attemptsCount > 0 && shouldShowHints) {
            when (hintType) {
                "visual" -> {
                    Log.d(TAG, "Showing visual hint for step 0")
                    Handler().postDelayed({
                        showVisualHintForStep(0)
                    }, 1500)
                }
                "auditory" -> {
                    Log.d(TAG, "Giving auditory hint for step 0")
                    Handler().postDelayed({
                        giveAuditoryHintForStep(0)
                    }, 1500)
                }
            }
        }
    }

    // VISUAL HINT IMPLEMENTATION
    private fun showVisualHintForStep(stepIndex: Int) {
        if (stepIndex < currentCorrectOrder.size) {
            val correctImageId = currentCorrectOrder[stepIndex]
            Log.d(TAG, "Showing visual hint for step $stepIndex: $correctImageId")

            verticalImages.firstOrNull { it.tag == correctImageId }?.let { targetImage ->
                showVisualHintForImage(targetImage)
            } ?: Log.e(TAG, "Target image not found for hint: $correctImageId")
        }
    }

    private fun showVisualHintForImage(targetImage: ImageView) {
        Log.d(TAG, "Activating visual hint for image: ${targetImage.tag}")

        hintActive = true
        currentHintTarget = targetImage

        // Highlight the image with a glowing circle
        visualHintOverlay.visibility = View.VISIBLE
        val location = IntArray(2)
        targetImage.getLocationOnScreen(location)

        // Position overlay circle around the image
        visualHintOverlay.x = (location[0] - 30).toFloat()
        visualHintOverlay.y = (location[1] - 30).toFloat()

        // Pulse animation
        visualHintOverlay.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0.8f)
            .setDuration(500)
            .withEndAction {
                visualHintOverlay.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(0.6f)
                    .setDuration(500)
                    .start()
            }
            .start()

        // Play hint sound
        hintSound.seekTo(0)
        hintSound.start()

        // Auto-remove hint after 3 seconds
        Handler().postDelayed({
            removeVisualHint()
        }, 3000)
    }

    private fun removeVisualHint() {
        Log.d(TAG, "Removing visual hint")
        hintActive = false
        currentHintTarget = null
        visualHintOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                visualHintOverlay.visibility = View.GONE
            }
            .start()
    }

    // AUDITORY HINT IMPLEMENTATION
    private fun giveAuditoryHintForStep(stepIndex: Int) {
        if (stepIndex < currentCorrectOrder.size && isTtsReady) {
            val stepId = currentCorrectOrder[stepIndex]
            val stepName = getStepName(stepId)
            val hintPhrase = when (stepIndex) {
                0 -> "First, we need to $stepName"
                1 -> "Next, we should $stepName"
                2 -> "Then, we $stepName"
                else -> "We need to $stepName"
            }

            Log.d(TAG, "Giving auditory hint: $hintPhrase")
            textToSpeech.speak(hintPhrase, TextToSpeech.QUEUE_FLUSH, null, "hint_$stepIndex")

            // Also show text hint
            Toast.makeText(this, hintPhrase, Toast.LENGTH_LONG).show()
        }
    }

    private fun getStepName(stepId: String): String {
        return when (stepId) {
            // Morning Routine
            "wake_up" -> "wake up"
            "make_bed" -> "make the bed"
            "drink_water" -> "drink water"
            "brush_teeth" -> "brush teeth"
            "wash_face" -> "wash face"
            "dry_towel" -> "dry with towel"
            "get_dressed" -> "get dressed"
            "apply_powder" -> "apply powder"
            "put_pajamas" -> "put pajamas away"

            // Default fallback
            else -> {
                Log.w(TAG, "Unknown step ID: $stepId")
                "do this step"
            }
        }
    }

    private val selectedOrder = mutableListOf<String>()

    private fun onImageSelected(imageType: String, imageView: ImageView) {
        if (isGameComplete || selectedOrder.contains(imageType)) {
            Log.d(TAG, "Image selection ignored: gameComplete=$isGameComplete, alreadySelected=${selectedOrder.contains(imageType)}")
            return
        }

        Log.d(TAG, "Processing image selection: $imageType (${selectedOrder.size + 1}/3)")

        // Remove visual hint if active
        if (hintActive && currentHintTarget == imageView) {
            removeVisualHint()
        }

        selectedOrder.add(imageType)
        Log.d(TAG, "Current selection: $selectedOrder")

        // Therapeutic bounce animation
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

        // Play correct sound
        correctSound.seekTo(0)
        correctSound.start()

        markImageAsSelected(imageView, selectedOrder.size.toString())
        updateOrderDisplay()

        // Give next hint if needed (only after 3 seconds delay and not on first attempt)
        if (shouldShowHints && selectedOrder.size < currentCorrectOrder.size && attemptsCount > 0) {
            Log.d(TAG, "Scheduling hint for next step in 3 seconds")
            Handler().postDelayed({
                if (selectedOrder.size < currentCorrectOrder.size) {
                    when (hintType) {
                        "visual" -> showVisualHintForStep(selectedOrder.size)
                        "auditory" -> giveAuditoryHintForStep(selectedOrder.size)
                    }
                }
            }, 3000)
        }

        if (selectedOrder.size == 3) {
            Log.d(TAG, "All 3 images selected, checking order...")
            checkOrder()
        }
    }

    private fun markImageAsSelected(imageView: ImageView, orderNumber: String) {
        runOnUiThread {
            imageView.alpha = 0.7f
            imageView.isClickable = false
            Log.d(TAG, "Marking image ${imageView.tag} as selected with badge: $orderNumber")

            val parent = imageView.parent as? FrameLayout
            parent?.let {
                for (i in it.childCount - 1 downTo 0) {
                    val child = it.getChildAt(i)
                    if (child is TextView && child.tag == "badge") {
                        it.removeView(child)
                    }
                }

                val badge = TextView(this).apply {
                    text = orderNumber
                    tag = "badge"
                    textSize = 20f
                    setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))

                    setBackgroundResource(R.drawable.circle_badge)

                    gravity = Gravity.CENTER
                    val params = FrameLayout.LayoutParams(50, 50).apply {
                        gravity = Gravity.TOP or Gravity.END
                        topMargin = 10
                        rightMargin = 10
                    }
                    layoutParams = params
                }

                it.addView(badge)
            }
        }
    }

    private fun updateOrderDisplay() {
        Log.d(TAG, "Updating order display: $selectedOrder")

        runOnUiThread {
            selectedOrder.forEachIndexed { index, imageType ->
                // Get the step name from the mapping
                val stepName = getDisplayNameForStep(imageType)

                when (index) {
                    0 -> tvOrder1.text = "1. $stepName"
                    1 -> tvOrder2.text = "2. $stepName"
                    2 -> tvOrder3.text = "3. $stepName"
                }
                Log.d(TAG, "Order $index: $imageType -> $stepName")
            }
        }
    }

    private fun getDisplayNameForStep(stepId: String): String {
        return when (stepId) {
            "wake_up" -> "Wake Up"
            "make_bed" -> "Make Bed"
            "drink_water" -> "Drink Water"
            "brush_teeth" -> "Brush Teeth"
            "wash_face" -> "Wash Face"
            "dry_towel" -> "Dry with Towel"
            "get_dressed" -> "Get Dressed"
            "apply_powder" -> "Apply Powder"
            "put_pajamas" -> "Put Pajamas Away"
            else -> {
                Log.w(TAG, "Unknown step ID in display: $stepId")
                "Step"  // Fallback
            }
        }
    }

    private fun checkOrder() {
        Log.d(TAG, "=== CHECKING ORDER ===")
        Log.d(TAG, "Selected order: $selectedOrder")
        Log.d(TAG, "Correct order: $currentCorrectOrder")

        verticalImages.forEach { it.isClickable = false }

        Handler().postDelayed({
            val isCorrect = selectedOrder == currentCorrectOrder
            attemptsCount++

            Log.d(TAG, "Attempt $attemptsCount: ${if (isCorrect) "CORRECT" else "INCORRECT"}")
            Log.d(TAG, "Comparison: ${selectedOrder.map { it }} vs ${currentCorrectOrder.map { it }}")

            // Update session metrics
            if (isCorrect) {
                sessionMetrics.correctCount++
                correctAnswers = 3

                // MARK SUB-ROUTINE AS COMPLETED
                sessionMetrics.completedSubRoutines.add(Pair(currentRoutineId, currentSubRoutineId))
                Log.d(TAG, "Sub-routine completed: $currentRoutineId-$currentSubRoutineId")

                // POP BUBBLE FOR PROGRESS (this will trigger scoreboard)
                popBubble()

            } else {
                sessionMetrics.errorCount++
                Log.d(TAG, "Error count: ${sessionMetrics.errorCount}")
            }

            // Get therapeutic decision
            val therapeuticDecision = analyzeTherapeuticOutcome(isCorrect)

            if (isCorrect) {
                playSuccessAnimation(therapeuticDecision)
                isGameComplete = true
                stopTimer()

                // Scoreboard will be shown by popBubble() after animation
            } else {
                playErrorAnimation(therapeuticDecision)

                Handler().postDelayed({
                    resetForRetry(therapeuticDecision)
                }, 1500)
            }
        }, 500)
    }

    private fun checkStickerUnlock() {
        // Calculate local alpha
        val localAlpha = calculateFinalAlpha()
        Log.d(TAG, "Checking sticker unlock with localAlpha: $localAlpha")

        val features = prepareBehavioralFeatures(
            currentAccuracy = (sessionMetrics.correctCount.toFloat() / attemptsCount * 100),
            avgResponseTime = calculateAverageResponseTime(),
            jitter = calculateJitter()
        )

        val decision = gameMaster.predict(features)

        // MORE RESTRICTIVE STICKER UNLOCK CRITERIA
        val shouldUnlockSticker = when {
            // 1. Excellent performance: Alpha >= 9.0 AND first attempt correct AND no errors
            localAlpha >= 9.0f && attemptsCount == 1 && sessionMetrics.errorCount == 0 -> {
                Log.d(TAG, "Excellent performance - Unlocking sticker")
                true
            }
            // 2. Good performance: Alpha >= 8.0 AND errorCount <= 1 AND completed multiple sub-routines
            localAlpha >= 8.0f && sessionMetrics.errorCount <= 1 && sessionMetrics.completedSubRoutines.size >= 2 -> {
                Log.d(TAG, "Good performance with multiple routines - Unlocking sticker")
                true
            }
            // 3. Milestone: Completed 5 sub-routines with good performance
            sessionMetrics.completedSubRoutines.size >= 5 && sessionMetrics.errorCount <= 2 -> {
                Log.d(TAG, "Milestone reached - Unlocking sticker")
                true
            }
            else -> {
                Log.d(TAG, "No sticker unlock this time")
                false
            }
        }

        if (shouldUnlockSticker) {
            // Navigate to sticker unlock activity
            goToStickerUnlock(decision, localAlpha)
        } else {
            // No sticker, go directly to scoreboard after a delay
            Log.d(TAG, "No sticker unlocked, going to scoreboard")
            Handler().postDelayed({
                onSessionComplete()
            }, 1000) // Shorter delay since no sticker animation
        }
        Log.d(TAG, "Should show sticker: $shouldUnlockSticker, Attempts: $attemptsCount, Alpha: $localAlpha, Errors: ${sessionMetrics.errorCount}")
    }



    private fun goToStickerUnlock(decision: ModelDecision, alpha: Float) {
        // Prevent duplicate navigation
        if (isNavigatingToSticker) {
            Log.w(TAG, "Already navigating to sticker, skipping duplicate")
            return
        }

        isNavigatingToSticker = true
        Log.d(TAG, "goToStickerUnlock called")

        try {
            val stickerType = when (decision.friendAction) {
                in 1..3 -> "comfort"    // Sensory stickers
                in 4..6 -> "achievement" // Milestone stickers
                in 7..9 -> "celebration" // Celebration stickers
                else -> "encouragement"  // Default encouragement
            }

            Log.d(TAG, "Creating intent for UnlockStickerActivity with sticker: $stickerType")

            val intent = Intent(this, UnlockStickerActivity::class.java)

            // Pass sticker type
            intent.putExtra("STICKER_TYPE", stickerType)
            intent.putExtra("ALPHA_SCORE", alpha)

            // Pass game data needed for navigation after sticker collection
            intent.putExtra("FINAL_ALPHA", calculateFinalAlpha())
            intent.putExtra("POPPED_BUBBLES", poppedBubbles)
            intent.putExtra("TOTAL_BUBBLES", totalBubbles)
            intent.putExtra("CORRECT_COUNT", sessionMetrics.correctCount)
            intent.putExtra("ERROR_COUNT", sessionMetrics.errorCount)
            intent.putExtra("COMPLETED_SUBROUTINES", sessionMetrics.completedSubRoutines.size)
            intent.putExtra("ROUTINE_COMPLETED", currentRoutineId)
            intent.putExtra("PREVIOUS_ALPHA", calculateFinalAlpha())
            intent.putExtra("PREVIOUS_CORRECT", sessionMetrics.correctCount)
            intent.putExtra("PREVIOUS_SUBROUTINE", currentSubRoutineId)
            intent.putExtra("PREVIOUS_ERROR", if (sessionMetrics.errorCount > 0) "positional" else "none")
            intent.putExtra("USER_SELECTED_ROUTINE", userSelectedRoutineId)
            intent.putExtra("USER_AGE", userAge)

            Log.d(TAG, "Starting UnlockStickerActivity...")
            startActivity(intent)
            Log.d(TAG, "UnlockStickerActivity started successfully")

            // Don't call finish() here - let the animation complete first
            // The sticker activity will handle finishing this activity

        } catch (e: Exception) {
            Log.e(TAG, "ERROR starting UnlockStickerActivity: ${e.message}")
            e.printStackTrace()
            isNavigatingToSticker = false  // Reset on error

            // Fallback to scoreboard
            Toast.makeText(this, "Couldn't show sticker. Going to scoreboard.", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({
                onSessionComplete()
            }, 1000)
        }
    }

    private fun goToRoutineSelection() {
        Log.d(TAG, "Going to Routine Selection")

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, RoutineSelectionActivity::class.java)
            // Pass any needed data
            intent.putExtra("SHOW_UNLOCKED_ROUTINES", true)
            intent.putExtra("COMPLETED_SUBROUTINES", sessionMetrics.completedSubRoutines.size)
            intent.putExtra("FINAL_ALPHA", calculateFinalAlpha())
            startActivity(intent)
            finish()
        }, 1000) // Small delay for smooth transition
    }

    private fun unlockTherapeuticSticker(decision: ModelDecision, alpha: Float) {
        val stickerType = when (decision.friendAction) {
            in 1..3 -> "comfort"    // Sensory stickers
            in 4..6 -> "achievement" // Milestone stickers
            in 7..9 -> "celebration" // Celebration stickers
            else -> "encouragement"  // Default encouragement
        }

        Log.d(TAG, "Unlocking sticker: $stickerType with alpha: $alpha")

        // Show sticker unlock animation
        showStickerUnlockAnimation(stickerType, alpha)

        // Save sticker unlock to profile
        saveStickerUnlock(stickerType, currentRoutineId, alpha)
    }

    private fun showStickerUnlockAnimation(stickerType: String, alpha: Float) {
        Log.d(TAG, "Showing sticker animation: $stickerType")

        // Create sticker reveal animation
        val stickerView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(200, 200).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(
                when (stickerType) {
                    "comfort" -> R.drawable.happy_emotion
                    "achievement" -> R.drawable.bottle
                    "celebration" -> R.drawable.sanitations
                    else -> R.drawable.bad
                }
            )
            scaleX = 0f
            scaleY = 0f
            this.alpha = 0f
        }

        val rootView = findViewById<FrameLayout>(R.id.rootContainer)
        rootView.addView(stickerView)

        // Animate sticker reveal
        stickerView.animate()
            .scaleX(1.5f)
            .scaleY(1.5f)
            .alpha(1f)
            .rotationBy(360f)
            .setDuration(1000)
            .withEndAction {
                stickerView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(500)
                    .withEndAction {
                        Handler().postDelayed({
                            stickerView.animate()
                                .alpha(0f)
                                .setDuration(500)
                                .withEndAction {
                                    rootView.removeView(stickerView)
                                    Log.d(TAG, "Sticker animation completed")
                                }
                                .start()
                        }, 2000)
                    }
                    .start()
            }
            .start()

        // Show congratulatory message based on alpha
        val message = when {
            alpha >= 8.0 -> "Therapeutic breakthrough! 🎯"
            alpha >= 6.0 -> "Amazing progress! 🌟"
            alpha >= 4.0 -> "Great work! New sticker! 🏆"
            else -> "You earned a sticker! 🎉"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d(TAG, "Sticker message: $message")
    }

    private fun saveStickerUnlock(stickerType: String, routineId: Int, alpha: Float) {
        Log.d(TAG, "Saving sticker: $stickerType for routine $routineId with alpha $alpha")
        // In real implementation, save to Firebase/SharedPreferences
    }

    private fun calculateAverageResponseTime(): Long {
        return if (sessionMetrics.tapTimes.size >= 2) {
            val totalTime = sessionMetrics.tapTimes.last() - sessionMetrics.tapTimes.first()
            totalTime / sessionMetrics.tapTimes.size
        } else 0L
    }

    private fun calculateJitter(): Float {
        if (sessionMetrics.tapPositions.size < 2) return 0f

        val xValues = sessionMetrics.tapPositions.map { it.first.toFloat() }
        val yValues = sessionMetrics.tapPositions.map { it.second.toFloat() }

        val xVariance = calculateVariance(xValues)
        val yVariance = calculateVariance(yValues)

        return (xVariance + yVariance) / 2
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun analyzeTherapeuticOutcome(isCorrect: Boolean): TherapeuticOutcome {
        val errorType = if (!isCorrect) {
            diagnoseErrorType(selectedOrder, currentCorrectOrder)
        } else "none"

        Log.d(TAG, "Analyzing outcome: correct=$isCorrect, errorType=$errorType")

        val features = prepareBehavioralFeatures(
            currentAccuracy = (sessionMetrics.correctCount.toFloat() / attemptsCount * 100),
            avgResponseTime = calculateAverageResponseTime(),
            jitter = calculateJitter()
        )

        val modelDecision = gameMaster.predict(features)
        Log.d(TAG, "Model decision for outcome: ${modelDecision.routineAction}, ${modelDecision.sequenceAction}")

        return TherapeuticOutcome(
            isCorrect = isCorrect,
            modelDecision = modelDecision,
            errorType = errorType,
            recommendation = generateRecommendation(modelDecision, errorType),
            nextHintLevel = determineNextHintLevel(modelDecision, errorType)
        )
    }

    private fun diagnoseErrorType(selected: List<String>, correct: List<String>): String {
        val errorType = if (selected.toSet() == correct.toSet()) {
            "positional"
        } else {
            "conceptual"
        }
        Log.d(TAG, "Diagnosed error type: $errorType")
        return errorType
    }

    private fun generateRecommendation(decision: ModelDecision, errorType: String): String {
        return when {
            errorType == "positional" -> "Try focusing on the order"
            errorType == "conceptual" -> "Let's review what each step means"
            decision.sequenceAction == 1 -> "Take your time with each tap"
            decision.sequenceAction == 2 -> "Listen carefully to the hints"
            else -> "Great effort! Keep going"
        }.also { Log.d(TAG, "Generated recommendation: $it") }
    }

    private fun determineNextHintLevel(decision: ModelDecision, errorType: String): Int {
        return when {
            errorType == "positional" -> 2  // Stronger visual hints
            errorType == "conceptual" -> 3  // Auditory + visual
            decision.sequenceAction > 0 -> 1  // Keep hints
            else -> 0  // No hints needed
        }.also { Log.d(TAG, "Next hint level: $it") }
    }

    data class TherapeuticOutcome(
        val isCorrect: Boolean,
        val modelDecision: ModelDecision,
        val errorType: String,
        val recommendation: String,
        val nextHintLevel: Int
    )

    private fun resetForRetry(outcome: TherapeuticOutcome) {
        Log.d(TAG, "Resetting for retry with outcome: ${outcome.errorType}")

        selectedOrder.clear()
        runOnUiThread {
            tvOrder1.text = "1. "
            tvOrder2.text = "2. "
            tvOrder3.text = "3. "
        }

        // Reset images with therapeutic consideration
        verticalImages.forEach { imageView ->
            imageView.alpha = 1f
            imageView.isClickable = true

            // Adjust for motor difficulties if needed
            if (outcome.modelDecision.sequenceAction == 1) {
                val params = imageView.layoutParams
                params.width = 200  // Larger for motor support
                params.height = 200
                imageView.layoutParams = params
            }

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

        // Apply therapeutic intervention
        applyPostErrorIntervention(outcome)
        Log.d(TAG, "Reset complete, ready for retry")
    }

    private fun applyPostErrorIntervention(outcome: TherapeuticOutcome) {
        Log.d(TAG, "Applying post-error intervention: ${outcome.errorType}")

        when (outcome.errorType) {
            "positional" -> {
                // Show visual sequence hint
                showVisualSequenceHint()
            }
            "conceptual" -> {
                // Give auditory explanation
                giveConceptualExplanation()
            }
        }

        if (outcome.recommendation.isNotEmpty()) {
            runOnUiThread {
                tvInstruction.text = outcome.recommendation
            }
            Log.d(TAG, "Updated instruction: ${outcome.recommendation}")
        }
    }

    private fun showVisualSequenceHint() {
        Log.d(TAG, "Showing visual sequence hint")

        // Show all steps in correct order briefly
        Handler().postDelayed({
            currentCorrectOrder.forEachIndexed { index, stepId ->
                Handler().postDelayed({
                    verticalImages.firstOrNull { it.tag == stepId }?.let { imageView ->
                        imageView.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .setDuration(300)
                            .withEndAction {
                                imageView.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(300)
                                    .start()
                            }
                            .start()
                    }
                }, index * 500L)
            }
        }, 500)

        Toast.makeText(this, "Watch the correct order", Toast.LENGTH_SHORT).show()
    }

    private fun giveConceptualExplanation() {
        Log.d(TAG, "Giving conceptual explanation")

        if (isTtsReady) {
            val explanation = buildString {
                append("Let's think about this. ")
                currentCorrectOrder.forEachIndexed { index, stepId ->
                    when (index) {
                        0 -> append("First we ${getStepName(stepId)}. ")
                        1 -> append("Then we ${getStepName(stepId)}. ")
                        2 -> append("Finally we ${getStepName(stepId)}. ")
                    }
                }
                append("Does that make sense?")
            }

            textToSpeech.speak(explanation, TextToSpeech.QUEUE_FLUSH, null, "explanation")
            Log.d(TAG, "Conceptual explanation spoken")
        }
    }

    private fun playSuccessAnimation(outcome: TherapeuticOutcome) {
        Log.d(TAG, "Playing success animation")

        // Celebration based on model's motivation decision
        when (outcome.modelDecision.motivationId) {
            0 -> playStarsCelebration()  // Simple stars
            1 -> playQuoteCelebration(outcome.modelDecision)  // Motivational quote
            2 -> playFullCelebration()  // Full animation
        }
    }

    private fun playStarsCelebration() {
        Log.d(TAG, "Playing stars celebration")

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
    }

    private fun playQuoteCelebration(decision: ModelDecision) {
        // Calculate local alpha for quote
        val localAlpha = calculateFinalAlpha()
        Log.d(TAG, "Playing quote celebration with localAlpha: $localAlpha")

        val quote = when (localAlpha) {
            in 7.0f..10.0f -> "Therapeutic excellence! You're growing! 🌱"
            in 4.0f..6.9f -> "Great therapeutic work! Progress made! 📈"
            else -> "Nice therapeutic effort! Every try helps! 💫"
        }

        if (isTtsReady) {
            textToSpeech.speak(quote, TextToSpeech.QUEUE_FLUSH, null, "celebration")
            Log.d(TAG, "Quote spoken: $quote")
        }

        Toast.makeText(this, quote, Toast.LENGTH_LONG).show()
        playStarsCelebration()
    }

    private fun playFullCelebration() {
        Log.d(TAG, "Playing full celebration")

        verticalImages.forEachIndexed { index, imageView ->
            Handler().postDelayed({
                imageView.animate()
                    .rotationBy(360f)
                    .scaleX(1.8f)
                    .scaleY(1.8f)
                    .setDuration(1000)
                    .withEndAction {
                        imageView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(500)
                            .start()
                    }
                    .start()
            }, index * 200L)
        }
    }

    private fun playErrorAnimation(outcome: TherapeuticOutcome) {
        Log.d(TAG, "Playing error animation for: ${outcome.errorType}")

        // Therapeutic error feedback
        val errorMessage = when (outcome.errorType) {
            "positional" -> "Almost! Let's check the order together"
            "conceptual" -> "Let's think about what each step means"
            else -> "That's okay! Try again"
        }

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

        val fullMessage = "$errorMessage. ${outcome.recommendation}"
        Toast.makeText(this, fullMessage, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Error message: $fullMessage")
    }

    private fun prepareNextChallenge() {
        Log.d(TAG, "Preparing next challenge...")
        Log.d(TAG, "Current: Routine=$currentRoutineId, Sub=$currentSubRoutineId")
        Log.d(TAG, "Completed sub-routines: ${sessionMetrics.completedSubRoutines}")

        // Determine next sub-routine (cycle through 0, 1, 2)
        val nextSubRoutine = (currentSubRoutineId + 1) % 3

        Log.d(TAG, "Next sub-routine: $nextSubRoutine")

        // Update for next round
        currentSubRoutineId = nextSubRoutine

        // Get new correct order
        val routineData = routines[currentRoutineId]?.get(currentSubRoutineId)
        if (routineData != null) {
            currentCorrectOrder = routineData.first.toMutableList()
            currentDifficultyLevel = routineData.second
            Log.d(TAG, "New routine data: ${routineData.third}, Difficulty: $currentDifficultyLevel")
            Log.d(TAG, "New correct order: $currentCorrectOrder")
        } else {
            Log.e(TAG, "No routine data found for next challenge")
            // Fallback to first sub-routine
            currentSubRoutineId = 0
            currentCorrectOrder = mutableListOf("wake_up", "make_bed", "drink_water")
        }

        // Reset for new challenge
        resetGameState()
        selectedOrder.clear()
        runOnUiThread {
            verticalContainer.removeAllViews()
        }
        verticalImages.clear()

        // Create new layout
        createVerticalLayout()

        // Update instruction
        val routineName = when (currentRoutineId) {
            0 -> "Morning Routine"
            1 -> "Bedtime Routine"
            2 -> "School Routine"
            else -> "Daily Activity"
        }

        val subRoutineDesc = routines[currentRoutineId]?.get(currentSubRoutineId)?.third ?: ""
        runOnUiThread {
            gameTitle.text = "$routineName: $subRoutineDesc"
            tvInstruction.text = "New challenge! Remember the order"
        }

        Log.d(TAG, "New challenge ready: $routineName - $subRoutineDesc")
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        elapsedTime = System.currentTimeMillis() - gameStartTime
        updateTimerDisplay()
        Log.d(TAG, "Timer stopped. Total time: ${elapsedTime}ms")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        isNavigatingToSticker = false

        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
            Log.d(TAG, "TTS shutdown")
        }

        bubblePopSound.release()
        correctSound.release()
        hintSound.release()
        Log.d(TAG, "Sound resources released")
    }

    companion object {
        private const val TAG = "TherapeuticSequence"
    }
}