package com.chirathi.voicebridge

import android.app.Activity
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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

class ActivitySequenceUnderActivity : AppCompatActivity() {

    // ── Model & Tracking ──────────────────────────────────────────────────────
    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    private lateinit var tracker: SessionStateTracker
    private var seqSpec: PersonalizedContentSelector.SequenceRoundSpec? = null

    // ── TTS ───────────────────────────────────────────────────────────────────
    private lateinit var textToSpeech: TextToSpeech
    private var isTtsReady = false

    // ── Sounds ────────────────────────────────────────────────────────────────
    private var bubblePopSound: MediaPlayer? = null
    private var correctSound  : MediaPlayer? = null
    private var hintSound     : MediaPlayer? = null

    // ── Session metrics ───────────────────────────────────────────────────────
    private data class TapMetrics(
        var startTime            : Long = System.currentTimeMillis(),
        var tapTimes             : MutableList<Long>          = mutableListOf(),
        var tapPositions         : MutableList<Pair<Int,Int>> = mutableListOf(),
        var hesitationTimes      : MutableList<Long>          = mutableListOf(),
        var correctCount         : Int  = 0,
        var errorCount           : Int  = 0,
        var sessionAlpha         : Float= 0f,
        var completedSubRoutines : MutableList<Pair<Int,Int>> = mutableListOf()
    )
    private val sessionMetrics = TapMetrics()

    // ── Bubble progress ───────────────────────────────────────────────────────
    private val totalBubbles  = 3
    private var poppedBubbles = 0
    private lateinit var bubbleContainer: LinearLayout
    private val bubbleViews = mutableListOf<ImageView>()

    // ── Prefs ─────────────────────────────────────────────────────────────────
    private lateinit var sharedPreferences: SharedPreferences
    private val BUBBLE_PREFS              = "bubble_prefs"
    private val KEY_POPPED_BUBBLES        = "popped_bubbles"
    private val KEY_COMPLETED_SUBROUTINES = "completed_subroutines"

    // ── Game state ────────────────────────────────────────────────────────────
    private var currentRoutineId      = 0
    private var currentSubRoutineId   = 0
    private var currentCorrectOrder   = mutableListOf<String>()
    private var userAge               = 6
    private var userSelectedRoutineId = -1
    private var attemptsCount         = 0
    private var correctAnswers        = 0
    private var isGameComplete        = false
    private val completedSubRoutines  = mutableSetOf<Pair<Int,Int>>()

    // ── Mini-game state ───────────────────────────────────────────────────────
    private var miniGameTriggeredThisRound = false

    // ── Timers ────────────────────────────────────────────────────────────────
    private var gameStartTime  = 0L
    private var elapsedTime    = 0L
    private val timerHandler   = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var isTimerRunning = false

    // Idle Timer (Triggers hint if child does nothing for 6 seconds)
    private val idleHandler = Handler(Looper.getMainLooper())
    private var idleRunnable: Runnable? = null
    private val IDLE_TIMEOUT_MS = 6000L

    // ── UI ────────────────────────────────────────────────────────────────────
    private lateinit var horizontalContainer: LinearLayout
    private lateinit var verticalContainer  : LinearLayout
    private lateinit var orderDisplay       : LinearLayout
    private lateinit var gameTitle          : TextView
    private lateinit var tvInstruction      : TextView
    private lateinit var btnStart           : Button
    private lateinit var timerTextView      : TextView
    private lateinit var hintIndicator      : ImageView
    private lateinit var visualHintOverlay  : View
    private lateinit var btnUndo            : Button
    private lateinit var btnClear           : Button
    private lateinit var tvOrder1           : TextView
    private lateinit var tvOrder2           : TextView
    private lateinit var tvOrder3           : TextView

    private val verticalImages = mutableListOf<ImageView>()
    private val selectedOrder  = mutableListOf<String>()

    // ── Routines ──────────────────────────────────────────────────────────────
    private val routines = mapOf(
        0 to mapOf(
            0 to Triple(listOf("rtn0_sub1_wake",   "rtn0_sub1_bed",    "rtn0_sub1_drink"),  1, "Wake up routine"),
            1 to Triple(listOf("rtn0_sub2_brush",  "rtn0_sub2_wash",   "rtn0_sub2_dry"),    2, "Hygiene sequence"),
            2 to Triple(listOf("rtn0_sub3_change", "rtn0_sub3_cream",  "rtn0_sub3_wash"),   3, "Getting dressed")
        ),
        1 to mapOf(
            0 to Triple(listOf("rtn1_sub1_wash",   "rtn1_sub1_sit",    "rtn1_sub1_napkin"), 1, "Before eating"),
            1 to Triple(listOf("rtn1_sub2_eat",    "rtn1_sub2_wipe",   "rtn1_sub2_wash"),   2, "Mealtime")
        ),
        2 to mapOf(
            0 to Triple(listOf("rtn2_sub1_books",  "rtn2_sub1_lunch",  "rtn2_sub1_pack"),   1, "Pack for school")
        )
    )

    companion object {
        private const val TAG = "ActivitySequenceUnder"
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_under)

        sharedPreferences     = getSharedPreferences(BUBBLE_PREFS, Context.MODE_PRIVATE)
        userSelectedRoutineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        userAge               = intent.getIntExtra("USER_AGE", 6)

        tracker = SessionStateTracker(ageGroup = userAge)
        loadCompletedSubRoutines()

        gameMaster = GameMasterModel(this)

        initViews()
        initTts()
        initSounds()
        setupTimer()
        setupBubbleProgress()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        CalmMusicManager.onActivityResume(this)
        if (verticalContainer.visibility == View.VISIBLE && !isGameComplete) {
            startIdleTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        CalmMusicManager.onActivityPause()
        cancelIdleTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        cancelIdleTimer()
        if (::textToSpeech.isInitialized) { textToSpeech.stop(); textToSpeech.shutdown() }
        bubblePopSound?.release(); correctSound?.release(); hintSound?.release()
        gameMaster.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MiniGamesActivitySequence.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val passed = data?.getBooleanExtra(MiniGamesActivitySequence.RESULT_PASSED, false) ?: false
            if (passed) sessionMetrics.correctCount++
            if (verticalContainer.visibility != View.VISIBLE) {
                transitionToVerticalLayout()
            }
        }
    }

    // =========================================================================
    // Init
    // =========================================================================

    private fun initTts() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                textToSpeech.language     = Locale.US
                textToSpeech.setSpeechRate(0.7f)
                textToSpeech.setPitch(1.2f)
            }
        }
    }

    private fun initSounds() {
        try {
            bubblePopSound = MediaPlayer.create(this, R.raw.bubble_pop)
            correctSound   = MediaPlayer.create(this, R.raw.correct_sound)
            hintSound      = MediaPlayer.create(this, R.raw.hint_sound)
        } catch (e: Exception) { Log.w(TAG, "Sound init non-fatal: ${e.message}") }
    }

    private fun setupBubbleProgress() {
        bubbleContainer = findViewById(R.id.bubbleContainer)
        bubbleContainer.removeAllViews(); bubbleViews.clear()
        poppedBubbles = sharedPreferences.getInt(KEY_POPPED_BUBBLES, 0)

        for (i in 0 until totalBubbles) {
            val b = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(60, 60).also { it.marginEnd = 10 }
                if (i < poppedBubbles) { setImageResource(R.drawable.bubble_popped); visibility = View.GONE }
                else setImageResource(R.drawable.bubble_full)
            }
            bubbleContainer.addView(b); bubbleViews.add(b)
        }
    }

    private fun loadCompletedSubRoutines() {
        val saved = sharedPreferences.getStringSet(KEY_COMPLETED_SUBROUTINES, emptySet()) ?: emptySet()
        saved.forEach { key ->
            val parts = key.split("_")
            if (parts.size == 2) {
                val r = parts[0].toIntOrNull(); val s = parts[1].toIntOrNull()
                if (r != null && s != null) completedSubRoutines.add(Pair(r, s))
            }
        }
    }

    private fun loadUserProfile() {
        sessionStart(userAge)
    }

    // =========================================================================
    // Session start
    // =========================================================================

    private fun sessionStart(age: Int) {
        currentRoutineId = if (userSelectedRoutineId != -1) userSelectedRoutineId else 0

        currentPrediction = gameMaster.predictSafe(
            childId     = ChildSession.childId,
            age         = age.toFloat(),
            accuracy    = 0.5f,
            engagement  = 0.7f,
            frustration = 0.2f
        )

        selectSubRoutine()

        val routineData = routines[currentRoutineId]?.get(currentSubRoutineId)
        if (routineData != null) {
            currentCorrectOrder = routineData.first.toMutableList()
            Handler(Looper.getMainLooper()).post { showInitialCorrectOrder() }
        } else {
            currentCorrectOrder = mutableListOf("rtn0_sub1_wake", "rtn0_sub1_bed", "rtn0_sub1_drink")
            currentSubRoutineId = 0
            Handler(Looper.getMainLooper()).post { showInitialCorrectOrder() }
        }

        updateInstruction(isInitialPhase = true)
        updateHintIndicator()
    }

    private fun selectSubRoutine() {
        val maxSub = routines[currentRoutineId]?.keys?.maxOrNull() ?: 0
        val available = (0..maxSub).filter {
            !completedSubRoutines.contains(Pair(currentRoutineId, it))
        }.toMutableList()

        if (available.isEmpty()) {
            resetAllProgress(); currentSubRoutineId = 0; return
        }

        val rec = currentPrediction.subRoutine.coerceIn(0, available.size - 1)
        currentSubRoutineId = available[rec]
    }

    private fun resetAllProgress() {
        poppedBubbles = 0; completedSubRoutines.clear()
        sharedPreferences.edit().clear().apply(); setupBubbleProgress()
    }

    // =========================================================================
    // Instruction & hints
    // =========================================================================

    private fun updateInstruction(isInitialPhase: Boolean = false) {
        val routineName = when (currentRoutineId) {
            0 -> "Morning Routine"; 1 -> "Mealtime Routine"
            2 -> "School Routine";  else -> "Daily Activity"
        }
        val subDesc = routines[currentRoutineId]?.get(currentSubRoutineId)?.third ?: ""

        runOnUiThread {
            gameTitle.text = "$routineName: $subDesc"
            val instr = if (isInitialPhase) "Look and remember the correct order"
            else                "Tap in the right order"
            tvInstruction.text = instr
            if (isTtsReady) textToSpeech.speak(instr, TextToSpeech.QUEUE_FLUSH, null, "instr")
        }
    }

    private fun updateHintIndicator() {
        runOnUiThread { hintIndicator.visibility = View.GONE }
    }

    // Idle Timer Logic
    private fun startIdleTimer() {
        cancelIdleTimer()
        if (isGameComplete) return

        idleRunnable = Runnable { deliverIdleHint() }
        idleHandler.postDelayed(idleRunnable!!, IDLE_TIMEOUT_MS)
    }

    private fun cancelIdleTimer() {
        idleRunnable?.let { idleHandler.removeCallbacks(it) }
        idleRunnable = null
    }

    private fun deliverIdleHint() {
        if (isGameComplete) return
        val expectedIndex = selectedOrder.size
        if (expectedIndex >= 3) return // Already picked all 3

        Log.d(TAG, "Child is idle. Triggering idle hint for index: $expectedIndex")
        deliverHint(expectedIndex, isIdle = true)

        // Restart the timer so it hints again if they STILL don't do anything
        startIdleTimer()
    }

    private fun deliverHint(zoneIndex: Int = -1, isIdle: Boolean = false) {
        val spec = seqSpec ?: return

        val expectedStepId = if (zoneIndex >= 0) currentCorrectOrder.getOrNull(zoneIndex) else null
        val stepName = expectedStepId?.let { displayName(it) } ?: "the next step"

        val previousStepId = if (zoneIndex > 0) currentCorrectOrder.getOrNull(zoneIndex - 1) else null
        val previousStepName = previousStepId?.let { displayName(it) }

        val routineName = when (currentRoutineId) {
            0 -> "Morning Routine"
            1 -> "Mealtime Routine"
            2 -> "School Routine"
            else -> "Daily Activity"
        }

        // Only show visual highlight on actual mistakes, not just idling
//        if (!isIdle && (spec.hintLevel >= 2 || sessionMetrics.errorCount > 1) && zoneIndex >= 0) {
//            visualHintOverlay.visibility = View.VISIBLE
//            Handler(Looper.getMainLooper()).postDelayed({ visualHintOverlay.visibility = View.GONE }, 1500)
//        }

        lifecycleScope.launch {
            val hintText = DynamicHintGenerator.generateSequenceHint(
                context = this@ActivitySequenceUnderActivity,
                targetStepName = stepName,
                previousStepName = previousStepName,
                routineName = routineName,
                childAge = userAge,
                isIdle = isIdle
            )
            if (isTtsReady) {
                textToSpeech.speak(hintText, TextToSpeech.QUEUE_FLUSH, null, "hint")
            }
        }
    }

    // =========================================================================
    // Views
    // =========================================================================

    private fun initViews() {
        horizontalContainer = findViewById(R.id.horizontal_images_container)
        verticalContainer   = findViewById(R.id.vertical_images_container)
        orderDisplay        = findViewById(R.id.order_display_container)
        gameTitle           = findViewById(R.id.game_title)
        tvInstruction       = findViewById(R.id.tv_instruction)
        btnStart            = findViewById(R.id.btn_start)
        timerTextView       = findViewById(R.id.timerTextView)
        hintIndicator       = findViewById(R.id.hintIndicator)
        visualHintOverlay   = findViewById(R.id.visualHintOverlay)
        btnUndo             = findViewById(R.id.btnUndo)
        btnClear            = findViewById(R.id.btnClear)
        tvOrder1            = findViewById(R.id.tv_order_1)
        tvOrder2            = findViewById(R.id.tv_order_2)
        tvOrder3            = findViewById(R.id.tv_order_3)

        btnStart.setOnClickListener { startGame() }
        btnUndo.setOnClickListener  { undoLastSelection() }
        btnClear.setOnClickListener { clearAllSelections() }

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            if (verticalContainer.visibility == View.VISIBLE) {
                android.app.AlertDialog.Builder(this)
                    .setMessage("Leave this game? Your progress will be lost.")
                    .setPositiveButton("Leave") { _, _ ->
                        startActivity(Intent(this, RoutineSelectionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                        finish()
                    }
                    .setNegativeButton("Stay", null)
                    .show()
            } else {
                startActivity(Intent(this, RoutineSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }

        setupInitialView()
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
    }

    private fun updateTimerDisplay() {
        runOnUiThread {
            val s = (elapsedTime / 1000) % 60
            val m = (elapsedTime / 60000) % 60
            timerTextView.text = String.format("%02d:%02d", m, s)
        }
    }

    private fun startTimer() {
        gameStartTime = System.currentTimeMillis(); elapsedTime = 0L
        isTimerRunning = true; timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false; timerHandler.removeCallbacks(timerRunnable)
        elapsedTime = System.currentTimeMillis() - gameStartTime
        updateTimerDisplay()
    }

    private fun setupInitialView() {
        runOnUiThread {
            horizontalContainer.visibility = View.VISIBLE
            btnStart.visibility            = View.VISIBLE
            timerTextView.visibility       = View.GONE
            btnUndo.visibility             = View.GONE
            btnClear.visibility            = View.GONE
            verticalContainer.visibility   = View.GONE
            orderDisplay.visibility        = View.GONE
            visualHintOverlay.visibility   = View.GONE
            resetGameState()
        }
    }

    // =========================================================================
    // Phase 1: show correct order
    // =========================================================================

    private fun showInitialCorrectOrder() {
        runOnUiThread {
            val imageViews = listOf(
                findViewById<ImageView>(R.id.horizontal_img_1),
                findViewById<ImageView>(R.id.horizontal_img_2),
                findViewById<ImageView>(R.id.horizontal_img_3)
            )
            val frameLayouts = listOf(
                findViewById<FrameLayout>(R.id.horizontal_frame_1),
                findViewById<FrameLayout>(R.id.horizontal_frame_2),
                findViewById<FrameLayout>(R.id.horizontal_frame_3)
            )
            val drawableMap = subRoutineDrawableMap(currentRoutineId, currentSubRoutineId)

            for ((index, stepId) in currentCorrectOrder.withIndex()) {
                if (index >= imageViews.size) break
                imageViews[index].setImageResource(drawableMap[stepId] ?: R.drawable.seq_rtn0_sub1_1_wakeup)
                imageViews[index].tag = stepId
                val frame = frameLayouts[index]
                removeBadgesFrom(frame)
                frame.addView(makeBadge((index + 1).toString()))
            }
        }
    }

    private fun subRoutineDrawableMap(routineId: Int, subId: Int): Map<String, Int> = when (routineId) {
        1 -> when (subId) {
            0 -> mapOf(
                "rtn1_sub1_wash"   to R.drawable.seq_rtn1_sub1_1_wash,
                "rtn1_sub1_sit"    to R.drawable.seq_rtn1_sub1_2_sit,
                "rtn1_sub1_napkin" to R.drawable.seq_rtn1_sub1_3_napkin
            )
            else -> mapOf(
                "rtn1_sub2_eat"  to R.drawable.seq_rtn1_sub2_1_eat,
                "rtn1_sub2_wipe" to R.drawable.seq_rtn1_sub2_2_wipe,
                "rtn1_sub2_wash" to R.drawable.seq_rtn1_sub2_3_wash
            )
        }
        2 -> mapOf(
            "rtn2_sub1_books" to R.drawable.seq_rtn2_sub1_1_books,
            "rtn2_sub1_lunch" to R.drawable.seq_rtn2_sub1_2_lunch,
            "rtn2_sub1_pack"  to R.drawable.seq_rtn2_sub1_3_pack
        )
        else -> when (subId) {
            1 -> mapOf(
                "rtn0_sub2_brush" to R.drawable.seq_rtn0_sub2_1_brush,
                "rtn0_sub2_wash"  to R.drawable.seq_rtn0_sub2_2_wash,
                "rtn0_sub2_dry"   to R.drawable.seq_rtn0_sub2_3_dry
            )
            2 -> mapOf(
                "rtn0_sub3_change" to R.drawable.seq_rtn0_sub3_1_change,
                "rtn0_sub3_cream"  to R.drawable.seq_rtn0_sub3_2_cream,
                "rtn0_sub3_wash"   to R.drawable.seq_rtn0_sub3_3_wash
            )
            else -> mapOf(
                "rtn0_sub1_wake"  to R.drawable.seq_rtn0_sub1_1_wakeup,
                "rtn0_sub1_bed"   to R.drawable.seq_rtn0_sub1_2_bed,
                "rtn0_sub1_drink" to R.drawable.seq_rtn0_sub1_3_drink
            )
        }
    }

    private fun makeBadge(text: String, size: Int = 65): TextView = TextView(this).apply {
        this.text = text; tag = "badge"; textSize = 20f
        setTextColor(ContextCompat.getColor(this@ActivitySequenceUnderActivity, android.R.color.white))
        setBackgroundResource(R.drawable.circle_badge)
        gravity = Gravity.CENTER; includeFontPadding = false; setPadding(0,0,0,0)
        layoutParams = FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.END; topMargin = 8; rightMargin = 8
        }
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun removeBadgesFrom(frame: FrameLayout) {
        for (i in frame.childCount - 1 downTo 0) {
            if (frame.getChildAt(i).tag == "badge") frame.removeViewAt(i)
        }
    }

    // =========================================================================
    // Game start
    // =========================================================================

    private fun resetGameState() {
        selectedOrder.clear(); isGameComplete = false
        miniGameTriggeredThisRound = false
        runOnUiThread {
            tvOrder1.text = "1. "; tvOrder2.text = "2. "; tvOrder3.text = "3. "
            timerTextView.text = "00:00"
        }
    }

    private fun startGame() {
        runOnUiThread { btnStart.visibility = View.GONE }
        updatePrediction()
        startTimer()
        transitionToVerticalLayout()
        Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 600)
    }

    private fun updatePrediction() {
        currentPrediction = gameMaster.predictSafe(
            childId            = ChildSession.childId,
            age                = userAge.toFloat(),
            accuracy           = if (attemptsCount > 0) sessionMetrics.correctCount.toFloat() / attemptsCount else 0.5f,
            engagement         = tracker.engagement,
            frustration        = tracker.frustration,
            jitter             = calculateJitter(),
            rt                 = avgReactionTime(),
            consecutiveCorrect = sessionMetrics.correctCount.toFloat(),
            consecutiveWrong   = sessionMetrics.errorCount.toFloat()
        )
        seqSpec = PersonalizedContentSelector.selectSequenceRound(userAge, currentPrediction)
        updateInstruction(isInitialPhase = false)
        updateHintIndicator()
    }

    private fun maybeLaunchMiniGame() {
        if (miniGameTriggeredThisRound) return

        if (attemptsCount == 0 || sessionMetrics.errorCount < 2) return

        val spec = seqSpec ?: return
        if (!currentPrediction.minigameTrigger && !currentPrediction.whatsMissingTrigger
            && !currentPrediction.connectDotsTrigger) return

        miniGameTriggeredThisRound = true
        startActivityForResult(
            Intent(this, MiniGamesActivitySequence::class.java).apply {
                putExtra(MiniGamesActivitySequence.EXTRA_TYPE,           spec.miniGameType)
                putExtra(MiniGamesActivitySequence.EXTRA_HIDDEN_INDEX,   spec.hiddenStepIndex)
                putExtra(MiniGamesActivitySequence.EXTRA_ROUTINE_ID,     currentRoutineId)
                putExtra(MiniGamesActivitySequence.EXTRA_SUB_ROUTINE_ID, currentSubRoutineId)
                putExtra(MiniGamesActivitySequence.EXTRA_USER_AGE,       userAge)
            },
            MiniGamesActivitySequence.REQUEST_CODE
        )
    }

    private fun transitionToVerticalLayout() {
        runOnUiThread {
            horizontalContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out))
            Handler(Looper.getMainLooper()).postDelayed({
                horizontalContainer.visibility = View.GONE
                verticalContainer.visibility   = View.VISIBLE
                orderDisplay.visibility        = View.VISIBLE
                timerTextView.visibility       = View.VISIBLE
                btnUndo.visibility             = View.VISIBLE
                btnClear.visibility            = View.VISIBLE
                btnUndo.alpha = 0f; btnClear.alpha = 0f
                btnUndo.animate().alpha(1f).setDuration(300).start()
                btnClear.animate().alpha(1f).setDuration(300).start()
                createVerticalLayout()
                verticalContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))

                // Start waiting for them to make a move
                startIdleTimer()

            }, 300)
        }
    }

    private fun createVerticalLayout() {
        runOnUiThread {
            verticalContainer.removeAllViews(); verticalImages.clear()
            val shuffled = currentCorrectOrder.shuffled()
            val drawableMap = subRoutineDrawableMap(currentRoutineId, currentSubRoutineId)

            for (stepId in shuffled) {
                val frame = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(280, 280).also { it.setMargins(0,20,0,20) }
                }
                val img = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    setImageResource(drawableMap[stepId] ?: R.drawable.seq_rtn0_sub1_1_wakeup)
                    tag = stepId; scaleType = ImageView.ScaleType.CENTER_INSIDE
                    adjustViewBounds = true; isClickable = true; setPadding(16,16,16,16)

                    setOnClickListener {
                        val tapTime = System.currentTimeMillis()
                        val hes = if (sessionMetrics.tapTimes.isNotEmpty())
                            tapTime - sessionMetrics.tapTimes.last()
                        else tapTime - sessionMetrics.startTime
                        sessionMetrics.tapTimes.add(tapTime)
                        sessionMetrics.hesitationTimes.add(hes)
                        val loc = IntArray(2); getLocationOnScreen(loc)
                        sessionMetrics.tapPositions.add(Pair(loc[0], loc[1]))
                        onImageSelected(tag.toString(), this)
                    }
                }
                frame.addView(img); verticalContainer.addView(frame); verticalImages.add(img)
            }
        }
    }

    // =========================================================================
    // Selection logic
    // =========================================================================

    private fun onImageSelected(imageType: String, imageView: ImageView) {
        if (isGameComplete || selectedOrder.contains(imageType)) return

        cancelIdleTimer() // User made a move

        selectedOrder.add(imageType)
        imageView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200)
            .withEndAction { imageView.animate().scaleX(1f).scaleY(1f).setDuration(200).start() }.start()
        try { correctSound?.seekTo(0); correctSound?.start() } catch (_: Exception) {}
        markImageAsSelected(imageView, selectedOrder.size.toString())
        updateOrderDisplay()

        if (selectedOrder.size == 3) {
            checkOrder()
        } else {
            // Still waiting for them to finish picking
            startIdleTimer()
        }
    }

    private fun markImageAsSelected(imageView: ImageView, orderNumber: String) {
        runOnUiThread {
            imageView.alpha = 0.7f; imageView.isClickable = false
            val parent = imageView.parent as? FrameLayout ?: return@runOnUiThread
            removeBadgesFrom(parent)
            parent.addView(makeBadge(orderNumber, 80).also {
                (it.layoutParams as FrameLayout.LayoutParams).apply { topMargin = 12; rightMargin = 12 }
                it.textSize = 26f
            })
        }
    }

    private fun updateOrderDisplay() {
        runOnUiThread {
            selectedOrder.forEachIndexed { index, id ->
                val name = displayName(id)
                when (index) {
                    0 -> tvOrder1.text = "1. $name"
                    1 -> tvOrder2.text = "2. $name"
                    2 -> tvOrder3.text = "3. $name"
                }
            }
        }
    }

    private fun displayName(id: String) = when (id) {
        "rtn0_sub1_wake"   -> "Wake Up"
        "rtn0_sub1_bed"    -> "Make Bed"
        "rtn0_sub1_drink"  -> "Drink Water"
        "rtn0_sub2_brush"  -> "Brush Teeth"
        "rtn0_sub2_wash"   -> "Wash Face"
        "rtn0_sub2_dry"    -> "Dry with Towel"
        "rtn0_sub3_change" -> "Get Dressed"
        "rtn0_sub3_cream"  -> "Apply Lotion"
        "rtn0_sub3_wash"   -> "Put Pajamas Away"
        "rtn1_sub1_wash"   -> "Wash Hands"
        "rtn1_sub1_sit"    -> "Sit Down"
        "rtn1_sub1_napkin" -> "Put on Napkin"
        "rtn1_sub2_eat"    -> "Eat Food"
        "rtn1_sub2_wipe"   -> "Wipe Mouth"
        "rtn1_sub2_wash"   -> "Wash Hands"
        "rtn2_sub1_books"  -> "Pack Books"
        "rtn2_sub1_lunch"  -> "Pack Lunch"
        "rtn2_sub1_pack"   -> "Pack Bag"
        else               -> "Step"
    }

    // =========================================================================
    // Check order & bubbles
    // =========================================================================

    private fun checkOrder() {
        verticalImages.forEach { it.isClickable = false }
        Handler(Looper.getMainLooper()).postDelayed({
            attemptsCount++
            if (selectedOrder == currentCorrectOrder) {
                sessionMetrics.correctCount++
                correctAnswers = 3

                tracker.update(true, 1, currentPrediction.frustrationRisk, 0)
                tracker.recordFeatureVector(gameMaster.lastFeatureVector())

                Log.d(TAG, "CORRECT!")
                popBubble()
            } else {
                sessionMetrics.errorCount++
                Log.d(TAG, "WRONG (errors=${sessionMetrics.errorCount})")

                updatePrediction()

                tracker.update(false, 1, currentPrediction.frustrationRisk, sessionMetrics.errorCount)

                var wrongIndex = -1
                for (i in 0 until 3) {
                    if (selectedOrder[i] != currentCorrectOrder[i]) {
                        wrongIndex = i
                        break
                    }
                }
                deliverHint(wrongIndex, isIdle = false)

                Toast.makeText(this, "Try again!", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ resetForRetry() }, 1500)
                Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 1800)
            }
        }, 500)
    }

    private fun popBubble() {
        isGameComplete = true
        cancelIdleTimer()
        if (poppedBubbles >= totalBubbles) return
        val bubble = bubbleViews[poppedBubbles]
        bubble.setImageResource(R.drawable.bubble_popped)
        bubble.animate().scaleX(1.5f).scaleY(1.5f).alpha(0f).setDuration(500)
            .withEndAction { bubble.visibility = View.GONE }.start()
        try { bubblePopSound?.seekTo(0); bubblePopSound?.start() } catch (_: Exception) {}
        poppedBubbles++
        sharedPreferences.edit().putInt(KEY_POPPED_BUBBLES, poppedBubbles).apply()
        markSubRoutineCompleted()
        Handler(Looper.getMainLooper()).postDelayed({ onSessionComplete() }, 1000)
    }

    private fun markSubRoutineCompleted() {
        val key = Pair(currentRoutineId, currentSubRoutineId)
        completedSubRoutines.add(key)
        sessionMetrics.completedSubRoutines.add(key)
        val set = sharedPreferences.getStringSet(KEY_COMPLETED_SUBROUTINES, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        set.add("${currentRoutineId}_${currentSubRoutineId}")
        sharedPreferences.edit().putStringSet(KEY_COMPLETED_SUBROUTINES, set).apply()
    }

    private fun resetForRetry() {
        selectedOrder.clear()
        runOnUiThread { tvOrder1.text = "1. "; tvOrder2.text = "2. "; tvOrder3.text = "3. " }
        verticalImages.forEach { img ->
            img.alpha = 1f; img.isClickable = true
            (img.parent as? FrameLayout)?.let { removeBadgesFrom(it) }
        }
        startIdleTimer()
    }

    private fun undoLastSelection() {
        if (selectedOrder.isEmpty()) {
            Toast.makeText(this, "No steps to undo", Toast.LENGTH_SHORT).show(); return
        }
        cancelIdleTimer()
        val last = selectedOrder.removeLast()
        verticalImages.firstOrNull { it.tag == last }?.let { img ->
            runOnUiThread {
                img.alpha = 1f; img.isClickable = true
                (img.parent as? FrameLayout)?.let { removeBadgesFrom(it) }
            }
        }
        updateOrderDisplay()
        startIdleTimer()
    }

    private fun clearAllSelections() {
        if (selectedOrder.isEmpty()) {
            Toast.makeText(this, "Nothing to clear", Toast.LENGTH_SHORT).show(); return
        }
        cancelIdleTimer()
        selectedOrder.forEach { id ->
            verticalImages.firstOrNull { it.tag == id }?.let { img ->
                runOnUiThread {
                    img.alpha = 1f; img.isClickable = true
                    (img.parent as? FrameLayout)?.let { removeBadgesFrom(it) }
                }
            }
        }
        selectedOrder.clear(); updateOrderDisplay()
        startIdleTimer()
    }

    // =========================================================================
    // Metric helpers
    // =========================================================================

    private fun avgReactionTime(): Float {
        if (sessionMetrics.tapTimes.size < 2) return 3000f
        return (1 until sessionMetrics.tapTimes.size)
            .map { sessionMetrics.tapTimes[it] - sessionMetrics.tapTimes[it - 1] }
            .average().toFloat()
    }

    private fun calculateJitter(): Float {
        if (sessionMetrics.tapPositions.size < 3) return 0.10f
        val dists = (1 until sessionMetrics.tapPositions.size).map { i ->
            val (x1, y1) = sessionMetrics.tapPositions[i - 1]
            val (x2, y2) = sessionMetrics.tapPositions[i]
            sqrt(((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toFloat())
        }
        val mean     = dists.average().toFloat()
        val variance = dists.map { (it - mean) * (it - mean) }.average().toFloat()
        return sqrt(variance).coerceIn(0.01f, 0.5f)
    }

    // =========================================================================
    // Session complete
    // =========================================================================

    private fun onSessionComplete() {
        stopTimer()

        val finalAlpha = gameMaster.calculateAlpha(
            accuracy     = sessionMetrics.correctCount.toFloat() / attemptsCount.coerceAtLeast(1),
            responseTime = elapsedTime.toFloat(),
            age          = userAge,
            errorCount   = sessionMetrics.errorCount
        )

        val isBestPerformance = sessionMetrics.errorCount == 0
        val stickerKey   = "sticker_won_sub_${currentRoutineId}_${currentSubRoutineId}"
        val alreadyWon   = sharedPreferences.getBoolean(stickerKey, false)
        val awardSticker = isBestPerformance && !alreadyWon && !StickerManager.isPoolExhausted(this)

        if (awardSticker) sharedPreferences.edit().putBoolean(stickerKey, true).apply()

        Log.d(TAG, "Session complete: finalAlpha=$finalAlpha awardSticker=$awardSticker")

        ChildProfileManager.updateAfterSession(
            context                = this,
            childId                = ChildSession.childId,
            ageGroup               = userAge,
            gameType               = "sequence",
            sessionAccuracy        = sessionMetrics.correctCount.toFloat() / attemptsCount.coerceAtLeast(1),
            sessionFrustration     = tracker.frustration,
            sessionEngagement      = tracker.engagement,
            sessionJitter          = calculateJitter(),
            sessionRt              = avgReactionTime(),
            sessionAlpha           = finalAlpha,
            peakConsecWrong        = tracker.peakConsecWrong,
            lastFiveFeatureVectors = tracker.getFlatHistory()
        )

        startActivity(Intent(this, ASequenceScoreboardActivity::class.java).apply {
            putExtra("GAME_MODE",             "under")
            putExtra("FINAL_ALPHA",           finalAlpha)
            putExtra("POPPED_BUBBLES",        poppedBubbles)
            putExtra("TOTAL_BUBBLES",         totalBubbles)
            putExtra("CORRECT_COUNT",         sessionMetrics.correctCount)
            putExtra("ERROR_COUNT",           sessionMetrics.errorCount)
            putExtra("ROUTINE_COMPLETED",     currentRoutineId)
            putExtra("USER_AGE",              userAge)
            putExtra("ATTEMPTS_COUNT",        attemptsCount)
            putExtra("SHOULD_AWARD_STICKER",  awardSticker)
            putExtra("USER_SELECTED_ROUTINE", userSelectedRoutineId)
            putExtra("PREVIOUS_SUBROUTINE",   currentSubRoutineId)
        })
        finish()
    }
}