package com.chirathi.voicebridge

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
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
import kotlin.math.roundToInt

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

    // Bubble progress tracking - CHANGED: 3 bubbles for 3 sub-routines
    private var totalBubbles = 3  // Changed from 5 to 3
    private var poppedBubbles = 0
    private lateinit var bubbleContainer: LinearLayout
    private val bubbleViews = mutableListOf<ImageView>()

    // SharedPreferences for bubble persistence
    private lateinit var sharedPreferences: SharedPreferences
    private val BUBBLE_PREFS = "bubble_prefs"
    private val KEY_POPPED_BUBBLES = "popped_bubbles"
    private val KEY_COMPLETED_SUBROUTINES = "completed_subroutines"

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

    // Track completed sub-routines
    private val completedSubRoutines = mutableSetOf<Pair<Int, Int>>()

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

    // Control buttons
    private lateinit var btnUndo: Button
    private lateinit var btnClear: Button

    // TextViews for order display
    private lateinit var tvOrder1: TextView
    private lateinit var tvOrder2: TextView
    private lateinit var tvOrder3: TextView

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

    // Array resources for easier access
    private val routineArrayResources = mapOf(
        Pair(0, 0) to R.array.seq_rtn0_sub1,  // rtn0, sub1 - WAKE UP ROUTINE
        Pair(0, 1) to R.array.seq_rtn0_sub2,  // rtn0, sub2
        Pair(0, 2) to R.array.seq_rtn0_sub3,  // rtn0, sub3
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_under)

        Log.d(TAG, "onCreate: Activity started")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(BUBBLE_PREFS, Context.MODE_PRIVATE)

        // Get user's routine selection from intent
        userSelectedRoutineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        Log.d(TAG, "User selected routine: $userSelectedRoutineId")

        // Load completed sub-routines from preferences
        loadCompletedSubRoutines()

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
        setupBubbleProgress() // Load bubble state from preferences

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

        // Load popped bubbles count from preferences
        poppedBubbles = sharedPreferences.getInt(KEY_POPPED_BUBBLES, 0)
        Log.d(TAG, "Loaded popped bubbles: $poppedBubbles from preferences")

        for (i in 0 until totalBubbles) {
            val bubble = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                    marginEnd = 10
                }

                // Check if this bubble should be popped
                if (i < poppedBubbles) {
                    setImageResource(R.drawable.bubble_popped)
                    visibility = View.GONE
                } else {
                    setImageResource(R.drawable.bubble_full)
                }

                tag = "bubble_$i"
            }
            bubbleContainer.addView(bubble)
            bubbleViews.add(bubble)
        }
        Log.d(TAG, "Bubble progress setup: $totalBubbles bubbles, $poppedBubbles already popped")
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

            // Save popped bubbles count to preferences
            savePoppedBubbles()

            // Save current sub-routine as completed
            markCurrentSubRoutineAsCompleted()

            // Check for sticker unlock
            Log.d(TAG, "Sub-routine completed! Checking for sticker...")
            checkStickerUnlock()
        } else {
            Log.d(TAG, "All bubbles already popped!")
            checkStickerUnlock()
        }
    }

    private fun savePoppedBubbles() {
        sharedPreferences.edit().putInt(KEY_POPPED_BUBBLES, poppedBubbles).apply()
        Log.d(TAG, "Saved popped bubbles: $poppedBubbles")
    }

    private fun markCurrentSubRoutineAsCompleted() {
        val subRoutineKey = Pair(currentRoutineId, currentSubRoutineId)
        completedSubRoutines.add(subRoutineKey)
        sessionMetrics.completedSubRoutines.add(subRoutineKey)

        // Save to preferences
        val completedSet = sharedPreferences.getStringSet(KEY_COMPLETED_SUBROUTINES, mutableSetOf()) ?: mutableSetOf()
        completedSet.add("${currentRoutineId}_${currentSubRoutineId}")
        sharedPreferences.edit().putStringSet(KEY_COMPLETED_SUBROUTINES, completedSet).apply()

        Log.d(TAG, "Marked sub-routine $currentRoutineId-$currentSubRoutineId as completed")
    }

    private fun loadCompletedSubRoutines() {
        val completedSet = sharedPreferences.getStringSet(KEY_COMPLETED_SUBROUTINES, mutableSetOf()) ?: mutableSetOf()
        completedSet.forEach { key ->
            val parts = key.split("_")
            if (parts.size == 2) {
                val routineId = parts[0].toIntOrNull()
                val subRoutineId = parts[1].toIntOrNull()
                if (routineId != null && subRoutineId != null) {
                    completedSubRoutines.add(Pair(routineId, subRoutineId))
                }
            }
        }
        Log.d(TAG, "Loaded completed sub-routines: $completedSubRoutines")
    }

    private fun isSubRoutineCompleted(routineId: Int, subRoutineId: Int): Boolean {
        return completedSubRoutines.contains(Pair(routineId, subRoutineId))
    }

    private fun onSessionComplete() {
        Log.d(TAG, "onSessionComplete called")
        stopTimer()  // Stop timer before navigation

        // Calculate final therapeutic metrics
        val finalAlpha = calculateFinalAlpha()
        val avgResponseTime = calculateAverageResponseTime()
        val totalAttempts = sessionMetrics.correctCount + sessionMetrics.errorCount
        val accuracy = if (totalAttempts > 0) {
            sessionMetrics.correctCount.toFloat() / totalAttempts
        } else 0f

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

        // Pass performance data for tier determination
        intent.putExtra("AVG_RESPONSE_TIME", avgResponseTime)
        intent.putExtra("ACCURACY", accuracy)
        intent.putExtra("ATTEMPTS_COUNT", attemptsCount)
        intent.putExtra("TOTAL_ATTEMPTS", totalAttempts)

        // Pass performance data for model decision in next session
        intent.putExtra("PREVIOUS_ALPHA", finalAlpha)
        intent.putExtra("PREVIOUS_CORRECT", sessionMetrics.correctCount)
        intent.putExtra("PREVIOUS_SUBROUTINE", currentSubRoutineId)
        intent.putExtra("PREVIOUS_ERROR", if (sessionMetrics.errorCount > 0) "positional" else "none")
        intent.putExtra("USER_SELECTED_ROUTINE", userSelectedRoutineId)
        intent.putExtra("USER_AGE", userAge)

        // Pass completed sub-routines
        val completedIds = completedSubRoutines.map { "${it.first}_${it.second}" }.toTypedArray()
        intent.putExtra("COMPLETED_SUBROUTINE_IDS", completedIds)

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
            // FIX: Default to first sub-routine of morning routine
            currentCorrectOrder = mutableListOf("wake_up", "make_bed", "drink_water")
            currentSubRoutineId = 0
            currentDifficultyLevel = 1
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
        updateInstructionBasedOnModel(decision, isInitialPhase = true)
        updateHintIndicator()
    }

    private fun selectSubRoutineBasedOnPerformance(decision: ModelDecision) {
        Log.d(TAG, "selectSubRoutineBasedOnPerformance called")

        // Get previous performance data from intent
        val previousAlpha = intent.getFloatExtra("PREVIOUS_ALPHA", 0f)
        val previousCorrect = intent.getIntExtra("PREVIOUS_CORRECT", 0)
        val previousSubRoutine = intent.getIntExtra("PREVIOUS_SUBROUTINE", -1)
        val previousError = intent.getStringExtra("PREVIOUS_ERROR")

        // Get completed sub-routines from intent
        val completedIds = intent.getStringArrayExtra("COMPLETED_SUBROUTINE_IDS")
        completedIds?.forEach { id ->
            val parts = id.split("_")
            if (parts.size == 2) {
                val routineId = parts[0].toIntOrNull()
                val subRoutineId = parts[1].toIntOrNull()
                if (routineId != null && subRoutineId != null) {
                    completedSubRoutines.add(Pair(routineId, subRoutineId))
                }
            }
        }

        Log.d(TAG, "Completed sub-routines: $completedSubRoutines")

        // Check which sub-routines are available for current routine
        val availableSubRoutines = mutableListOf<Int>()
        for (i in 0..2) { // Assuming 3 sub-routines per routine
            if (!isSubRoutineCompleted(currentRoutineId, i)) {
                availableSubRoutines.add(i)
            }
        }

        Log.d(TAG, "Available sub-routines for routine $currentRoutineId: $availableSubRoutines")

        if (availableSubRoutines.isEmpty()) {
            // All sub-routines completed, start over
            Log.d(TAG, "All sub-routines completed! Starting from beginning.")
            resetAllProgress()
            currentSubRoutineId = 0
        } else {
            // Choose next sub-routine based on performance
            currentSubRoutineId = when {
                // If first time or no previous data, start with first available
                previousSubRoutine == -1 || !availableSubRoutines.contains(previousSubRoutine) -> {
                    availableSubRoutines.first()
                }

                // EXCELLENT performance (high accuracy, few errors) → Progress to next
                previousAlpha >= 8.0f && previousCorrect >= 2 && (previousError == null || previousError == "none") -> {
                    val nextIndex = availableSubRoutines.indexOf(previousSubRoutine) + 1
                    if (nextIndex < availableSubRoutines.size) availableSubRoutines[nextIndex] else previousSubRoutine
                }

                // GOOD performance → Stay at current level
                previousAlpha >= 6.0f -> {
                    previousSubRoutine
                }

                // POOR performance → Repeat same or go back
                else -> {
                    val prevIndex = availableSubRoutines.indexOf(previousSubRoutine)
                    if (prevIndex > 0) availableSubRoutines[prevIndex - 1] else previousSubRoutine
                }
            }
        }

        Log.d(TAG, "Selected sub-routine: $currentSubRoutineId")
    }

    private fun resetAllProgress() {
        // Clear all progress
        poppedBubbles = 0
        completedSubRoutines.clear()

        // Clear preferences
        sharedPreferences.edit().clear().apply()

        // Reset bubble views
        setupBubbleProgress()

        Log.d(TAG, "All progress reset")
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
        val totalTasks = sessionMetrics.correctCount + sessionMetrics.errorCount
        val correctRate = if (totalTasks > 0) {
            sessionMetrics.correctCount.toFloat() / totalTasks * 100
        } else 0f

        // Calculate response time metrics
        val avgResponseTime = if (sessionMetrics.tapTimes.size >= 2) {
            val intervals = mutableListOf<Long>()
            for (i in 1 until sessionMetrics.tapTimes.size) {
                intervals.add(sessionMetrics.tapTimes[i] - sessionMetrics.tapTimes[i-1])
            }
            intervals.average().toLong()
        } else 1000L

        // Calculate jitter
        val jitter = calculateJitter()

        // Calculate comprehensive alpha
        val baseAlpha = correctRate / 10f // Convert percentage to 0-10 scale

        // Adjust based on response time (faster = better)
        val timeFactor = when {
            avgResponseTime < 2000 -> 1.2f  // Bonus for fast responses
            avgResponseTime < 5000 -> 1.0f  // Normal
            else -> 0.8f  // Penalty for slow responses
        }

        // Adjust based on jitter (consistent tapping = better)
        val jitterFactor = when {
            jitter < 50 -> 1.1f  // Bonus for low jitter
            jitter < 100 -> 1.0f  // Normal
            else -> 0.9f  // Penalty for high jitter
        }

        // Adjust based on errors
        val errorFactor = when (sessionMetrics.errorCount) {
            0 -> 1.2f  // Bonus for no errors
            1 -> 1.0f  // Normal
            else -> maxOf(0.8f, 1.0f - (sessionMetrics.errorCount * 0.1f))
        }

        // REMOVE THE .coerceIn(0f, 10f) - Let alpha go above 10 for excellent performance!
        val finalAlpha = (baseAlpha * timeFactor * jitterFactor * errorFactor).roundTo(1)

        Log.d(TAG, "Final Alpha: $finalAlpha (Base: $baseAlpha, " +
                "TimeFactor: $timeFactor, JitterFactor: $jitterFactor, ErrorFactor: $errorFactor)")

        return finalAlpha
    }

    // Helper extension for rounding
    fun Float.roundTo(decimals: Int): Float {
        var multiplier = 1f
        repeat(decimals) { multiplier *= 10f }
        return (this * multiplier).roundToInt() / multiplier
    }

    private fun updateInstructionBasedOnModel(decision: ModelDecision, isInitialPhase: Boolean = false) {
        Log.d(TAG, "updateInstructionBasedOnModel: Routine=$currentRoutineId, Sub=$currentSubRoutineId, Phase=${if (isInitialPhase) "initial" else "gameplay"}")

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

        // Set different instructions for initial phase (correct order display) vs gameplay phase
        val instruction = if (isInitialPhase) {
            // When showing correct order initially
            "Look and remember the correct order"
        } else {
            // When playing the jumbled game
            when (decision.sequenceAction) {
                1 -> "Tap in the right order"
                2 -> "Tap in the right order"
                else -> "Tap in the right order"
            }
        }

        runOnUiThread {
            tvInstruction.text = instruction
            // Speak the instruction if TTS is ready
            if (isTtsReady) {
                textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "instruction")
            }
        }

        Log.d(TAG, "Instruction: $instruction (Phase: ${if (isInitialPhase) "initial" else "gameplay"})")
    }

    private fun updateHintIndicator() {
        runOnUiThread {
            hintIndicator.visibility = if (shouldShowHints) View.VISIBLE else View.GONE
            val iconRes = when (hintType) {
                "visual" -> R.drawable.ic_eye_hint
                "auditory" -> R.drawable.ic_ear_hint
                else -> R.drawable.ic_no_hint
            }
            hintIndicator.setImageResource(iconRes)
            Log.d(TAG, "Hint indicator: $hintType, icon: $iconRes (visible=${hintIndicator.visibility == View.VISIBLE})")
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

            // Control buttons
            btnUndo = findViewById(R.id.btnUndo)
            btnClear = findViewById(R.id.btnClear)

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

        btnUndo.setOnClickListener {
            Log.d(TAG, "Undo button clicked")
            undoLastSelection()
        }

        btnClear.setOnClickListener {
            Log.d(TAG, "Clear button clicked")
            clearAllSelections()
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
        // Timer visibility is now handled by showControlButtons() in transitionToVerticalLayout()
        Log.d(TAG, "Timer started")
    }

    private fun setupInitialView() {
        runOnUiThread {
            try {
                horizontalContainer.visibility = View.VISIBLE
                btnStart.visibility = View.VISIBLE

                // Hide timer and control buttons in initial view
                timerTextView.visibility = View.GONE
                btnUndo.visibility = View.GONE
                btnClear.visibility = View.GONE

                // Hide gameplay elements
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
        Log.d(TAG, "showInitialCorrectOrder called - Routine: $currentRoutineId, Sub: $currentSubRoutineId")

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

                Log.d(TAG, "Showing initial correct order: $currentCorrectOrder for sub-routine $currentSubRoutineId")

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

                // Create a mapping of step IDs to drawable resources based on current sub-routine
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
                    else -> mapOf(
                        "wake_up" to R.drawable.seq_rtn0_sub1_1_wakeup,
                        "make_bed" to R.drawable.seq_rtn0_sub1_2_bed,
                        "drink_water" to R.drawable.seq_rtn0_sub1_3_drink
                    )
                }

                for ((index, stepId) in currentCorrectOrder.withIndex()) {
                    if (index < imageViews.size) {
                        try {
                            val drawableRes = stepToDrawableMap[stepId] ?: 0

                            if (drawableRes != 0) {
                                imageViews[index].setImageResource(drawableRes)
                                imageViews[index].tag = stepId
                                Log.d(TAG, "Set image $index for step: $stepId, drawable: $drawableRes")
                            } else {
                                Log.e(TAG, "No drawable resource found for stepId: $stepId in sub-routine $currentSubRoutineId")
                                // Fallback to default image
                                imageViews[index].setImageResource(R.drawable.seq_rtn0_sub1_1_wakeup)
                                imageViews[index].tag = stepId
                            }

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

                            // Create badge
                            val badgeTextView = TextView(this@ActivitySequenceUnderActivity).apply {
                                text = badgeNumber.toString()
                                textSize = 20f
                                setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))
                                setBackgroundResource(R.drawable.circle_badge)
                                gravity = Gravity.CENTER
                                tag = "badge"
                                includeFontPadding = false
                                setPadding(0, 0, 0, 0)

                                val params = FrameLayout.LayoutParams(65, 65).apply {
                                    gravity = Gravity.TOP or Gravity.END
                                    topMargin = 8
                                    rightMargin = 8
                                }
                                layoutParams = params

                                typeface = Typeface.DEFAULT_BOLD
                            }

                            frameLayout.addView(badgeTextView)

                            // Debug: Check badge dimensions after layout
                            badgeTextView.post {
                                Log.d(TAG, "Badge $badgeNumber dimensions - Width: ${badgeTextView.width}, Height: ${badgeTextView.height}")
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting image for step $index: ${e.message}")
                        }
                    }
                }

                Log.d(TAG, "Images displayed successfully for sub-routine $currentSubRoutineId")

            } catch (e: Exception) {
                Log.e(TAG, "Error in showInitialCorrectOrder: ${e.message}")
                e.printStackTrace()
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
            // Update instruction for gameplay phase
            updateInstructionForGameplay()
        }
        startTimer()
        transitionToVerticalLayout()
        Log.d(TAG, "Game started")
    }

    private fun updateInstructionForGameplay() {
        // Get updated model decision for gameplay
        val features = prepareBehavioralFeatures(
            currentAccuracy = if (attemptsCount > 0) {
                (sessionMetrics.correctCount.toFloat() / attemptsCount * 100)
            } else 0f,
            avgResponseTime = calculateAverageResponseTime(),
            jitter = calculateJitter()
        )

        val decision = gameMaster.predict(features)

        // Update instruction for gameplay phase (jumbled ordering)
        val instruction = when (decision.sequenceAction) {
            1 -> "Take your time. I'll show you where to tap!"
            2 -> "Listen carefully. I'll tell you what's next!"
            else -> "Let's order correctly. Tap in the right sequence!"
        }

        runOnUiThread {
            tvInstruction.text = instruction
            if (isTtsReady) {
                textToSpeech.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "gameplay_instruction")
            }
        }

        // Update hint type based on model decision
        hintType = when (decision.sequenceAction) {
            1 -> "visual"
            2 -> "auditory"
            else -> "none"
        }
        shouldShowHints = decision.sequenceAction > 0
        updateHintIndicator()

        Log.d(TAG, "Gameplay instruction: $instruction, Hint type: $hintType")
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

                    // Show timer and control buttons with animation
                    timerTextView.visibility = View.VISIBLE
                    btnUndo.visibility = View.VISIBLE
                    btnClear.visibility = View.VISIBLE

                    // Optional: Add fade-in animation for buttons
                    btnUndo.alpha = 0f
                    btnClear.alpha = 0f
                    btnUndo.animate().alpha(1f).setDuration(300).start()
                    btnClear.animate().alpha(1f).setDuration(300).start()

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
                    else -> mapOf(
                        "wake_up" to R.drawable.seq_rtn0_sub1_1_wakeup,
                        "make_bed" to R.drawable.seq_rtn0_sub1_2_bed,
                        "drink_water" to R.drawable.seq_rtn0_sub1_3_drink
                    )
                }

                // Now create image views for each jumbled step ID
                for ((index, stepId) in jumbledStepIds.withIndex()) {
                    try {
                        val frameLayout = FrameLayout(this).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                width = 280 // Increased to 280dp for better visibility
                                height = 280 // Increased to 280dp for better visibility
                                setMargins(0, 20, 0, 20) // Increased margins
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
                                scaleType = ImageView.ScaleType.CENTER_INSIDE
                                adjustViewBounds = true
                                Log.d(TAG, "Image $index: stepId=$stepId, drawable=$drawableRes")
                            } else {
                                Log.e(TAG, "No drawable resource found for stepId: $stepId")
                                setImageResource(R.drawable.seq_rtn0_sub1_1_wakeup)
                                tag = stepId
                                scaleType = ImageView.ScaleType.CENTER_INSIDE
                                adjustViewBounds = true
                            }

                            isClickable = true
                            setPadding(16, 16, 16, 16) // Add padding for touch area

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

                        Log.d(TAG, "Created vertical image $index with size 280dp")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating image view $index: ${e.message}")
                        e.printStackTrace()
                    }
                }

                Log.d(TAG, "Created ${verticalImages.size} image views (280dp each)")

            } catch (e: Exception) {
                Log.e(TAG, "Error in createVerticalLayout: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun undoLastSelection() {
        if (selectedOrder.isEmpty()) {
            Toast.makeText(this, "No steps to undo", Toast.LENGTH_SHORT).show()
            return
        }

        val lastSelected = selectedOrder.removeLast()
        Log.d(TAG, "Undoing last selection: $lastSelected")

        // Find and reset the corresponding image view
        verticalImages.firstOrNull { it.tag == lastSelected }?.let { imageView ->
            runOnUiThread {
                imageView.alpha = 1f
                imageView.isClickable = true

                // Remove the badge
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

        // Update order display
        updateOrderDisplay()

        Toast.makeText(this, "Undid last step", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllSelections() {
        if (selectedOrder.isEmpty()) {
            Toast.makeText(this, "No steps to clear", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Clearing all ${selectedOrder.size} selections")

        // Reset all selected images
        selectedOrder.forEach { stepId ->
            verticalImages.firstOrNull { it.tag == stepId }?.let { imageView ->
                runOnUiThread {
                    imageView.alpha = 1f
                    imageView.isClickable = true

                    // Remove the badge
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
        }

        // Clear the selection list
        selectedOrder.clear()

        // Update order display
        updateOrderDisplay()

        Toast.makeText(this, "Cleared all selections", Toast.LENGTH_SHORT).show()
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
                // Remove existing badges
                for (i in it.childCount - 1 downTo 0) {
                    val child = it.getChildAt(i)
                    if (child is TextView && child.tag == "badge") {
                        it.removeView(child)
                    }
                }

                // UPDATED BADGE - Larger for bigger images
                val badge = TextView(this@ActivitySequenceUnderActivity).apply {
                    text = orderNumber
                    tag = "badge"
                    textSize = 26f // Larger for better visibility
                    setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))
                    setBackgroundResource(R.drawable.circle_badge)
                    gravity = Gravity.CENTER

                    // Critical for proper text rendering inside circle
                    includeFontPadding = false
                    setPadding(0, 0, 0, 0)

                    // Larger size for bigger images
                    val params = FrameLayout.LayoutParams(80, 80).apply {
                        gravity = Gravity.TOP or Gravity.END
                        topMargin = 12
                        rightMargin = 12
                    }
                    layoutParams = params

                    // Make text bold for better visibility
                    typeface = Typeface.DEFAULT_BOLD

                    // Optional: Add a subtle shadow for better contrast
                    setShadowLayer(2f, 1f, 1f, ContextCompat.getColor(this@ActivitySequenceUnderActivity, R.color.pink))
                }

                it.addView(badge)

                // Debug: Verify badge placement
                badge.post {
                    Log.d(TAG, "Vertical badge $orderNumber added to ${imageView.tag}")
                    Log.d(TAG, "Badge position: (${badge.left}, ${badge.top})")
                    Log.d(TAG, "Badge visible: ${badge.visibility == View.VISIBLE}")
                    Log.d(TAG, "Badge size: ${badge.width}x${badge.height}")
                }

                Log.d(TAG, "Added badge $orderNumber to image ${imageView.tag}")
            } ?: run {
                Log.e(TAG, "Parent FrameLayout not found for image ${imageView.tag}")
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

            // Clear any remaining order lines
            when (selectedOrder.size) {
                0 -> {
                    tvOrder1.text = "1. "
                    tvOrder2.text = "2. "
                    tvOrder3.text = "3. "
                }
                1 -> {
                    tvOrder2.text = "2. "
                    tvOrder3.text = "3. "
                }
                2 -> {
                    tvOrder3.text = "3. "
                }
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

                // MARK SUB-ROUTINE AS COMPLETED (will be done in popBubble)
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
        val localAlpha = calculateFinalAlpha()
        val avgResponseTime = calculateAverageResponseTime()

        Log.d(TAG, "Checking rewards - Alpha: $localAlpha, Avg Response Time: ${avgResponseTime}ms")

        val totalAttempts = sessionMetrics.correctCount + sessionMetrics.errorCount
        val accuracy = if (totalAttempts > 0) {
            sessionMetrics.correctCount.toFloat() / totalAttempts
        } else 0f

        // Prepare features for model decision (needed for sticker type)
        val features = prepareBehavioralFeatures(
            currentAccuracy = accuracy * 100,
            avgResponseTime = avgResponseTime,
            jitter = calculateJitter()
        )

        val decision = gameMaster.predict(features)

        // UPDATED CRITERIA - More realistic for children
        when {
            // TIER 1: STICKER - Excellent performance (high alpha AND fast)
            localAlpha >= 9.0f && avgResponseTime < 6000 && attemptsCount == 1 && sessionMetrics.errorCount == 0 -> {
                Log.d(TAG, "TIER 1: Sticker - Excellent! Alpha: $localAlpha, Time: ${avgResponseTime}ms")
                Handler().postDelayed({
                    goToStickerUnlock(decision, localAlpha)
                }, 1000)
            }

            // TIER 2: 5 STEPS - Very good performance
            localAlpha >= 7.0f || (localAlpha >= 5.0f && avgResponseTime < 8000) -> {
                Log.d(TAG, "TIER 2: 5 steps - Very good! Alpha: $localAlpha, Time: ${avgResponseTime}ms")
                Handler().postDelayed({
                    onSessionComplete()
                }, 1000)
            }

            // TIER 3: 3 STEPS - Good performance
            localAlpha >= 4.0f || avgResponseTime < 15000 -> {
                Log.d(TAG, "TIER 3: 3 steps - Good! Alpha: $localAlpha, Time: ${avgResponseTime}ms")
                Handler().postDelayed({
                    onSessionComplete()
                }, 1000)
            }

            // TIER 4: 1 STEP - Basic participation
            else -> {
                Log.d(TAG, "TIER 4: 1 step - Participated! Alpha: $localAlpha, Time: ${avgResponseTime}ms")
                Handler().postDelayed({
                    onSessionComplete()
                }, 1000)
            }
        }
    }

    private fun goToStickerUnlock(decision: ModelDecision, alpha: Float) {
        // Prevent duplicate navigation
        if (isNavigatingToSticker) {
            Log.w(TAG, "Already navigating to sticker, skipping duplicate")
            return
        }

        isNavigatingToSticker = true
        Log.d(TAG, "Direct sticker unlock flow for high performance, alpha: $alpha")

        try {
            // Determine sticker type from model decision
            val stickerType = when (decision.friendAction) {
                in 1..3 -> "comfort"      // Sensory stickers
                in 4..6 -> "achievement"  // Milestone stickers
                in 7..9 -> "celebration"  // Celebration stickers
                else -> "encouragement"   // Default encouragement
            }

            Log.d(TAG, "Creating intent for UnlockStickerActivity with sticker: $stickerType")

            val intent = Intent(this, UnlockStickerActivity::class.java)

            // Pass sticker type and performance data
            intent.putExtra("STICKER_TYPE", stickerType)
            intent.putExtra("ALPHA_SCORE", alpha)
            intent.putExtra("PERFORMANCE_TIER", 1) // Tier 1 = highest

            // Pass all game data needed for navigation after sticker collection
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

            // Pass completed sub-routines
            val completedIds = completedSubRoutines.map { "${it.first}_${it.second}" }.toTypedArray()
            intent.putExtra("COMPLETED_SUBROUTINE_IDS", completedIds)

            // Pass model decision data if needed
            intent.putExtra("MODEL_DECISION_ROUTINE", decision.routineAction)
            intent.putExtra("MODEL_DECISION_SEQUENCE", decision.sequenceAction)
            intent.putExtra("MODEL_DECISION_FRIEND", decision.friendAction)
            intent.putExtra("MODEL_DECISION_MOTIVATION", decision.motivationId)

            Log.d(TAG, "Starting UnlockStickerActivity...")
            startActivity(intent)

            // Don't finish() here - let the sticker activity handle it

        } catch (e: Exception) {
            Log.e(TAG, "ERROR starting UnlockStickerActivity: ${e.message}")
            e.printStackTrace()
            isNavigatingToSticker = false  // Reset on error

            // Fallback to normal scoreboard
            Toast.makeText(this, "Couldn't show sticker. Going to scoreboard.", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({
                onSessionComplete()
            }, 1000)
        }
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

            // Ensure buttons are still visible
            btnUndo.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE
        }

        // Reset images with therapeutic consideration
        verticalImages.forEach { imageView ->
            imageView.alpha = 1f
            imageView.isClickable = true

            // Adjust for motor difficulties if needed
            if (outcome.modelDecision.sequenceAction == 1) {
                val params = imageView.layoutParams
                params.width = 280  // Keep the large size for motor support
                params.height = 280
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
            1 -> playStarsCelebration()
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