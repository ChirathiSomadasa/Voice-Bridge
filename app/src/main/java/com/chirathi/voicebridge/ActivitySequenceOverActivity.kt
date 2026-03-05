package com.chirathi.voicebridge

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * ActivitySequenceOverActivity — v2.0
 *
 * Uses XML layout (activity_sequence_over.xml) — same visual style as
 * ActivitySequenceUnderActivity. All UI logic unchanged.
 */
class ActivitySequenceOverActivity : AppCompatActivity() {

    // ── Model ──────────────────────────────────────────────────────────────
    private lateinit var gameMaster: GameMasterModel
    private var currentPrediction: Prediction = Prediction.defaults()

    // ── TTS ────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    // ── Shared prefs ───────────────────────────────────────────────────────
    private lateinit var prefs: SharedPreferences
    private val BUBBLE_PREFS              = "bubble_prefs"
    private val KEY_COMPLETED_SUBROUTINES = "completed_subroutines"
    private val completedSubRoutines      = mutableSetOf<Pair<Int, Int>>()

    // ── Session ────────────────────────────────────────────────────────────
    private var currentRoutineId    = 0
    private var currentSubRoutineId = 0
    private var userAge             = 8
    private var errorCount          = 0
    private var correctCount        = 0
    private var attemptsCount       = 0
    private var gameStartTime       = 0L
    private var elapsedTime         = 0L
    private val tapTimes            = mutableListOf<Long>()

    // ── Mini-game state ────────────────────────────────────────────────────
    private var miniGameTriggeredThisRound = false

    // ── Routine data ───────────────────────────────────────────────────────
    private val routineSentences = mapOf(
        0 to mapOf(
            0 to listOf(
                "wake_up"     to "Wake up",
                "make_bed"    to "Make your bed",
                "drink_water" to "Drink water"
            ),
            1 to listOf(
                "brush_teeth" to "Brush your teeth",
                "wash_face"   to "Wash your face",
                "dry_towel"   to "Dry your face"
            ),
            2 to listOf(
                "get_dressed"  to "Get dressed",
                "apply_powder" to "Put on lotion",
                "put_pajamas"  to "Put away pajamas"
            )
        )
    )

    // ── UI views (from XML) ────────────────────────────────────────────────
    private lateinit var timerTextView:            TextView
    private lateinit var gameTitleText:            TextView
    private lateinit var tvInstruction:            TextView
    private lateinit var btnStart:                 Button
    private lateinit var horizontalImagesContainer: LinearLayout
    private lateinit var dropZonesContainer:       LinearLayout
    private lateinit var verticalImagesContainer:  LinearLayout

    private lateinit var previewText1: TextView
    private lateinit var previewText2: TextView
    private lateinit var previewText3: TextView

    private lateinit var dropZone1:  LinearLayout
    private lateinit var dropZone2:  LinearLayout
    private lateinit var dropZone3:  LinearLayout
    private lateinit var dropText1:  TextView
    private lateinit var dropText2:  TextView
    private lateinit var dropText3:  TextView

    // ── Timer ──────────────────────────────────────────────────────────────
    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var isTimerRunning = false

    // ── Drag-drop state ────────────────────────────────────────────────────
    private data class SentenceItem(val id: String, val text: String)
    private var correctOrder          = listOf<SentenceItem>()
    private var shuffledOrder         = listOf<SentenceItem>()
    private val dropZoneContents      = mutableMapOf<Int, String?>()
    private val draggableCards        = mutableListOf<View>()
    private val dropZoneViews         = mutableListOf<LinearLayout>()
    private val dropTextViews         = mutableListOf<TextView>()

    companion object { private const val TAG = "ActivityOverage" }

    // ======================================================================
    // Lifecycle
    // ======================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_sequence_over)   // ← XML layout

        userAge          = intent.getIntExtra("USER_AGE", 8)
        currentRoutineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        prefs            = getSharedPreferences(BUBBLE_PREFS, Context.MODE_PRIVATE)

        gameMaster = GameMasterModel(this)
        initViews()
        initTts()
        loadCompletedSubRoutines()
        setupTimer()
        setupBackButton()
        sessionStart()
    }

    override fun onResume() {
        super.onResume()
        CalmMusicManager.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        CalmMusicManager.onActivityPause()
    }

    override fun onDestroy() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        gameMaster.close()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MiniGamesActivitySequence.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val passed = data?.getBooleanExtra(MiniGamesActivitySequence.RESULT_PASSED, false) ?: false
            if (passed) correctCount++
//            miniGameTriggeredThisRound = false
        }
    }

    // ======================================================================
    // View binding
    // ======================================================================

    private fun initViews() {
        timerTextView             = findViewById(R.id.timerTextView)
        gameTitleText             = findViewById(R.id.game_title)
        tvInstruction             = findViewById(R.id.tv_instruction)
        btnStart                  = findViewById(R.id.btn_start)
        horizontalImagesContainer = findViewById(R.id.horizontal_images_container)
        dropZonesContainer        = findViewById(R.id.drop_zones_container)
        verticalImagesContainer   = findViewById(R.id.vertical_images_container)

        previewText1 = findViewById(R.id.preview_text_1)
        previewText2 = findViewById(R.id.preview_text_2)
        previewText3 = findViewById(R.id.preview_text_3)

        dropZone1 = findViewById(R.id.drop_zone_1)
        dropZone2 = findViewById(R.id.drop_zone_2)
        dropZone3 = findViewById(R.id.drop_zone_3)
        dropText1 = findViewById(R.id.drop_text_1)
        dropText2 = findViewById(R.id.drop_text_2)
        dropText3 = findViewById(R.id.drop_text_3)

        dropZoneViews.addAll(listOf(dropZone1, dropZone2, dropZone3))
        dropTextViews.addAll(listOf(dropText1, dropText2, dropText3))
    }

    private fun setupBackButton() {
        findViewById<View>(R.id.backBtn).setOnClickListener {
            if (dropZonesContainer.visibility == View.VISIBLE) {
                // Mid drag-drop phase — warn before leaving
                android.app.AlertDialog.Builder(this)
                    .setMessage("Leave this game? Your progress will be lost.")
                    .setPositiveButton("Leave") { _, _ ->
                        stopTimer()
                        startActivity(Intent(this, RoutineSelectionActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                        finish()
                    }
                    .setNegativeButton("Stay", null)
                    .show()
            } else {
                // Still in preview phase — safe to leave
                startActivity(Intent(this, RoutineSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }
        }
    }

    // ======================================================================
    // Session
    // ======================================================================

    private fun sessionStart() {
        currentPrediction = gameMaster.predictSafe(
            childId     = 0,
            age         = userAge.toFloat(),
            accuracy    = 0.5f,
            engagement  = 0.7f,
            frustration = 0.2f
        )
        selectSubRoutine()
        showCorrectOrderPhase()
    }

    private fun selectSubRoutine() {
        val available = (0..2).filter {
            !completedSubRoutines.contains(Pair(currentRoutineId, it))
        }.toMutableList()
        if (available.isEmpty()) { resetAllProgress(); currentSubRoutineId = 0; return }
        val modelRec        = currentPrediction.subRoutine.coerceIn(0, 2)
        currentSubRoutineId = if (modelRec < available.size) available[modelRec] else available.last()
    }

    private fun resetAllProgress() {
        completedSubRoutines.clear()
        prefs.edit().clear().apply()
    }

    // ======================================================================
    // Phase 1 — Show correct order via XML preview cards
    // ======================================================================

    private fun showCorrectOrderPhase() {
        val sentences = routineSentences[currentRoutineId]?.get(currentSubRoutineId)
            ?: routineSentences[0]!![0]!!

        correctOrder = sentences.map { (id, text) -> SentenceItem(id, text) }

        gameTitleText.text = when (currentRoutineId) {
            0 -> "Morning Routine"; 1 -> "Bedtime Routine"; 2 -> "School Routine"
            else -> "Daily Routine"
        }
        tvInstruction.text = "Read and remember the order!"
        speak("Look at the steps. Remember the order!")

        // Fill the three XML preview cards
        val texts = listOf(previewText1, previewText2, previewText3)
        correctOrder.forEachIndexed { i, step ->
            if (i < texts.size) texts[i].text = step.text
        }

        // Show phase 1, hide phase 2
        horizontalImagesContainer.visibility = View.VISIBLE
        dropZonesContainer.visibility        = View.GONE
        verticalImagesContainer.visibility   = View.GONE
        btnStart.visibility                  = View.VISIBLE
        timerTextView.visibility             = View.GONE

        btnStart.setOnClickListener { startDragDropPhase() }
    }

    // ======================================================================
    // Phase 2 — Drag and drop using XML containers
    // ======================================================================

    private fun startDragDropPhase() {
        shuffledOrder = correctOrder.shuffled()
        dropZoneContents.clear()
        draggableCards.clear()
        errorCount    = 0
        correctCount  = 0
        attemptsCount = 0
        gameStartTime = System.currentTimeMillis()
        miniGameTriggeredThisRound = false
        startTimer()

        tvInstruction.text = "Hold and drag each step to the right box!"
        speak("Hold a step and drag it to the right box!")

        // Hide phase 1, show phase 2
        horizontalImagesContainer.visibility = View.GONE
        btnStart.visibility                  = View.GONE
        dropZonesContainer.visibility        = View.VISIBLE
        verticalImagesContainer.visibility   = View.VISIBLE
        timerTextView.visibility             = View.VISIBLE

        // Reset drop zones
        dropZoneContents[0] = null
        dropZoneContents[1] = null
        dropZoneContents[2] = null
        dropTextViews.forEach { tv ->
            tv.text      = "Drop here…"
            tv.setTextColor(Color.parseColor("#90A4AE"))
            tv.typeface  = Typeface.DEFAULT
        }
        dropZoneViews.forEachIndexed { i, zone ->
            zone.setBackgroundResource(R.drawable.order_display_bg)
            zone.setOnDragListener { view, event ->
                handleDrop(view as LinearLayout, i, event, dropTextViews[i])
            }
        }

        // Add draggable cards to vertical container (keep the label, remove old cards)
        while (verticalImagesContainer.childCount > 1)
            verticalImagesContainer.removeViewAt(1)

        updatePrediction()

        for (step in shuffledOrder) {
            val card = buildDraggableCard(step)
            draggableCards.add(card)
            verticalImagesContainer.addView(card)
        }

        Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 600)
    }

    private fun buildDraggableCard(step: SentenceItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#FFF9C4"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 10 }
            tag       = step.id
            elevation = 6f

            addView(TextView(this@ActivitySequenceOverActivity).apply {
                text     = "☰"
                textSize = 22f
                setTextColor(Color.parseColor("#BDBDBD"))
                gravity  = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(50, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@ActivitySequenceOverActivity).apply {
                text     = step.text
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                ).also { it.marginStart = 12 }
            })
        }

        card.setOnLongClickListener { view ->
            tapTimes.add(System.currentTimeMillis())
            val clipData   = ClipData.newPlainText("sentenceId", step.id)
            val dragShadow = View.DragShadowBuilder(view)
            view.startDragAndDrop(clipData, dragShadow, view, 0)
            true
        }
        return card
    }

    private fun handleDrop(
        zone: LinearLayout, zoneIndex: Int, event: DragEvent, dropText: TextView
    ): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> {
                zone.setBackgroundColor(Color.parseColor("#BBDEFB")); true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                zone.setBackgroundResource(R.drawable.order_display_bg); true
            }
            DragEvent.ACTION_DROP -> {
                zone.setBackgroundResource(R.drawable.order_display_bg)
                val droppedView = event.localState as? View ?: return true
                val droppedId   = droppedView.tag as? String ?: return true
                val expectedId  = correctOrder.getOrNull(zoneIndex)?.id ?: return true

                attemptsCount++

                if (droppedId == expectedId) {
                    correctCount++
                    droppedView.visibility = View.GONE
                    dropZoneContents[zoneIndex] = droppedId
                    zone.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    dropText.text      = correctOrder[zoneIndex].text
                    dropText.setTextColor(Color.parseColor("#2E7D32"))
                    dropText.typeface  = Typeface.DEFAULT_BOLD
                    speak("Well done!")

                    if (dropZoneContents.values.all { it != null }) {
                        Handler(Looper.getMainLooper()).postDelayed({ onGameSuccess() }, 500)
                    }
                } else {
                    errorCount++
                    zone.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    Handler(Looper.getMainLooper()).postDelayed({
                        zone.setBackgroundResource(R.drawable.order_display_bg)
                    }, 600)
                    speak("Try again!")
                    updatePrediction()
                    if (currentPrediction.whatsMissingTrigger) {
                        Toast.makeText(this,
                            "Hint: Box ${zoneIndex + 1} → \"${correctOrder[zoneIndex].text}\"",
                            Toast.LENGTH_LONG).show()
                    }
                    Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 800)
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> true
            else -> false
        }
    }

    private fun updatePrediction() {
        val accuracy = if (attemptsCount > 0) correctCount.toFloat() / attemptsCount else 0.5f
        currentPrediction = gameMaster.predictSafe(
            childId            = 0,
            age                = userAge.toFloat(),
            accuracy           = accuracy,
            engagement         = if (accuracy > 0.6f) 0.8f else 0.5f,
            frustration        = if (errorCount >= 2) 0.7f else 0.2f,
            consecutiveCorrect = correctCount.toFloat(),
            consecutiveWrong   = errorCount.toFloat()
        )
    }

    // ======================================================================
    // Mini-game launcher
    // ======================================================================

    private fun maybeLaunchMiniGame() {
        if (miniGameTriggeredThisRound) return
        val shouldTrigger = currentPrediction.minigameTrigger
                || currentPrediction.whatsMissingTrigger
        if (!shouldTrigger) return
        miniGameTriggeredThisRound = true

        val gameType = when {
            currentPrediction.whatsMissingTrigger -> MiniGamesActivitySequence.TYPE_WHATS_MISSING
            else -> MiniGamesActivitySequence.TYPE_WHATS_MISSING
        }

        startActivityForResult(
            Intent(this, MiniGamesActivitySequence::class.java).apply {
                putExtra(MiniGamesActivitySequence.EXTRA_TYPE,           MiniGamesActivitySequence.TYPE_WHATS_MISSING)
                putExtra(MiniGamesActivitySequence.EXTRA_ROUTINE_ID,     currentRoutineId)
                putExtra(MiniGamesActivitySequence.EXTRA_SUB_ROUTINE_ID, currentSubRoutineId)
                putExtra(MiniGamesActivitySequence.EXTRA_USER_AGE,       userAge)
            },
            MiniGamesActivitySequence.REQUEST_CODE
        )
    }

    // ======================================================================
    // Game complete
    // ======================================================================

    private fun onGameSuccess() {
        stopTimer()
        speak("Amazing! You got the right order!")
        markSubRoutineCompleted()

        val accuracy   = if (attemptsCount > 0) correctCount.toFloat() / attemptsCount else 0f
        val finalAlpha = gameMaster.calculateAlpha(
            accuracy     = accuracy,
            responseTime = elapsedTime.toFloat(),
            age          = userAge,
            errorCount   = errorCount
        )

        val isBestPerformance = accuracy >= 0.85f && errorCount <= 1
        val stickerKey  = "sub_${currentRoutineId}_${currentSubRoutineId}"
        val alreadyWon  = prefs.getBoolean("sticker_won_$stickerKey", false)
        val awardSticker = isBestPerformance && !alreadyWon && !StickerManager.isPoolExhausted(this)
        if (awardSticker) prefs.edit().putBoolean("sticker_won_$stickerKey", true).apply()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, ASequenceScoreboardActivity::class.java).apply {
                putExtra("FINAL_ALPHA",          finalAlpha)
                putExtra("POPPED_BUBBLES",       0)
                putExtra("TOTAL_BUBBLES",        0)
                putExtra("CORRECT_COUNT",        correctCount)
                putExtra("ERROR_COUNT",          errorCount)
                putExtra("ROUTINE_COMPLETED",    currentRoutineId)
                putExtra("USER_AGE",             userAge)
                putExtra("ATTEMPTS_COUNT",       attemptsCount)
                putExtra("SHOULD_AWARD_STICKER", awardSticker)
                putExtra("USER_SELECTED_ROUTINE",currentRoutineId)
                putExtra("PREVIOUS_SUBROUTINE",  currentSubRoutineId)
            })
            finish()
        }, 800)
    }

    private fun markSubRoutineCompleted() {
        val key = Pair(currentRoutineId, currentSubRoutineId)
        completedSubRoutines.add(key)
        val set = prefs.getStringSet(KEY_COMPLETED_SUBROUTINES, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        set.add("${currentRoutineId}_${currentSubRoutineId}")
        prefs.edit().putStringSet(KEY_COMPLETED_SUBROUTINES, set).apply()
    }

    // ======================================================================
    // Timer
    // ======================================================================

    private fun setupTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    elapsedTime = System.currentTimeMillis() - gameStartTime
                    val s = (elapsedTime / 1000) % 60
                    val m = (elapsedTime / 60000) % 60
                    timerTextView.text = String.format("%02d:%02d", m, s)
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun startTimer() {
        gameStartTime  = System.currentTimeMillis()
        isTimerRunning = true
        timerTextView.visibility = View.VISIBLE
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        elapsedTime = System.currentTimeMillis() - gameStartTime
    }

    // ======================================================================
    // TTS / helpers
    // ======================================================================

    private fun speak(text: String) {
        if (isTtsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "over")
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts.language     = Locale.US
                tts.setSpeechRate(0.75f)
                tts.setPitch(1.1f)
            }
        }
    }

    private fun loadCompletedSubRoutines() {
        val saved = prefs.getStringSet(KEY_COMPLETED_SUBROUTINES, emptySet()) ?: emptySet()
        saved.forEach { key ->
            val parts = key.split("_")
            if (parts.size == 2) {
                val r = parts[0].toIntOrNull(); val s = parts[1].toIntOrNull()
                if (r != null && s != null) completedSubRoutines.add(Pair(r, s))
            }
        }
    }
}