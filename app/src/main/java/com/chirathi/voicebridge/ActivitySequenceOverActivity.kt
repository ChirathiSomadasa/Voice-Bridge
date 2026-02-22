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
 * ActivitySequenceOverActivity — drag-and-drop sequence game for ages 8-10.
 *
 *  Mini-game fix (same as Under activity):
 *   maybeLaunchMiniGame() now uses currentPrediction.minigameTrigger as the
 *   PRIMARY gate instead of only checking whatsMissingTrigger / connectDotsTrigger.
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

    // ── Routine data — SHORT simple sentences ─────────────────────────────
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

    // ── UI ─────────────────────────────────────────────────────────────────
    private lateinit var rootScroll : ScrollView
    private lateinit var rootLayout : LinearLayout
    private lateinit var timerTextView: TextView
    private lateinit var titleText  : TextView
    private lateinit var instrText  : TextView

    private val timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable
    private var isTimerRunning = false

    // Drag-drop state
    private data class SentenceItem(val id: String, val text: String)
    private var correctOrder     = listOf<SentenceItem>()
    private var shuffledOrder    = listOf<SentenceItem>()
    private val dropZoneContents = mutableMapOf<Int, String?>()
    private val draggableCards   = mutableListOf<View>()
    private val dropZones        = mutableListOf<LinearLayout>()

    companion object {
        private const val TAG = "ActivityOverage"
    }

    // ======================================================================
    // Lifecycle
    // ======================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userAge          = intent.getIntExtra("USER_AGE", 8)
        currentRoutineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        prefs            = getSharedPreferences(BUBBLE_PREFS, Context.MODE_PRIVATE)

        gameMaster = GameMasterModel(this)
        initTts()
        loadCompletedSubRoutines()
        buildLayout()
        setupTimer()
        sessionStart()
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
            miniGameTriggeredThisRound = false
        }
    }

    // ======================================================================
    // Layout
    // ======================================================================

    private fun buildLayout() {
        rootScroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#EEF2FF"))
        }

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        titleText = TextView(this).apply {
            textSize = 22f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#3F51B5"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        rootLayout.addView(titleText)

        instrText = TextView(this).apply {
            textSize = 16f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#555555"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }
        rootLayout.addView(instrText)

        timerTextView = TextView(this).apply {
            text     = "00:00"
            textSize = 18f
            gravity  = Gravity.CENTER
            setTextColor(Color.parseColor("#888888"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 16 }
        }
        rootLayout.addView(timerTextView)

        rootScroll.addView(rootLayout)
        setContentView(rootScroll)
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
    // Phase 1 — Show correct order
    // ======================================================================

    private fun showCorrectOrderPhase() {
        val sentences = routineSentences[currentRoutineId]?.get(currentSubRoutineId)
            ?: routineSentences[0]!![0]!!

        correctOrder = sentences.map { (id, text) -> SentenceItem(id, text) }

        val routineName = when (currentRoutineId) {
            0 -> "Morning Routine"; 1 -> "Bedtime Routine"; 2 -> "School Routine"
            else -> "Daily Routine"
        }
        titleText.text = routineName
        instrText.text = "Read and remember the order!"
        speak("Look at the steps. Remember the order!")

        while (rootLayout.childCount > 3) rootLayout.removeViewAt(3)

        for ((index, step) in correctOrder.withIndex()) {
            rootLayout.addView(buildSentenceDisplayCard(index + 1, step.text,
                Color.parseColor("#C8E6C9"), Color.parseColor("#2E7D32")))
        }

        val startBtn = Button(this).apply {
            text     = "I'm Ready! Let's Go! 🚀"
            textSize = 18f
            setBackgroundResource(R.drawable.rounded_button_background)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 32 }
        }
        startBtn.setOnClickListener { startDragDropPhase() }
        rootLayout.addView(startBtn)
    }

    private fun buildSentenceDisplayCard(number: Int, text: String, bgColor: Int, numColor: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(bgColor)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 12; it.bottomMargin = 4 }

            addView(TextView(this@ActivitySequenceOverActivity).apply {
                this.text = "$number"
                textSize  = 28f
                setTextColor(numColor)
                typeface  = Typeface.DEFAULT_BOLD
                gravity   = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(70, ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(this@ActivitySequenceOverActivity).apply {
                this.text = text
                textSize  = 18f
                typeface  = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#333333"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginStart = 16 }
            })
        }
    }

    // ======================================================================
    // Phase 2 — Drag and drop (long-press only)
    // ======================================================================

    private fun startDragDropPhase() {
        shuffledOrder = correctOrder.shuffled()
        dropZoneContents.clear()
        draggableCards.clear()
        dropZones.clear()
        errorCount    = 0
        correctCount  = 0
        attemptsCount = 0
        gameStartTime = System.currentTimeMillis()
        miniGameTriggeredThisRound = false
        startTimer()

        instrText.text = "Hold and drag each step to the right box!"
        speak("Hold a step and drag it to the right box!")

        while (rootLayout.childCount > 3) rootLayout.removeViewAt(3)

        updatePrediction()

        val zonesLabel = TextView(this).apply {
            text     = "Put steps in order:"
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 16; it.bottomMargin = 8 }
        }
        rootLayout.addView(zonesLabel)

        for (i in correctOrder.indices) {
            dropZoneContents[i] = null
            val zone = buildDropZone(i)
            dropZones.add(zone)
            rootLayout.addView(zone)
        }

        val cardsLabel = TextView(this).apply {
            text     = "Drag these steps:"
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 28; it.bottomMargin = 8 }
        }
        rootLayout.addView(cardsLabel)

        for (step in shuffledOrder) {
            val card = buildDraggableCard(step)
            draggableCards.add(card)
            rootLayout.addView(card)
        }

        Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 600)
    }

    private fun buildDropZone(index: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#E3F2FD"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 110
            ).also { it.topMargin = 10 }
            tag = index

            addView(TextView(this@ActivitySequenceOverActivity).apply {
                text     = "${index + 1}"
                textSize = 24f
                setTextColor(Color.parseColor("#1565C0"))
                typeface = Typeface.DEFAULT_BOLD
                gravity  = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(60, ViewGroup.LayoutParams.MATCH_PARENT)
            })
            addView(TextView(this@ActivitySequenceOverActivity).apply {
                text     = "Drop here…"
                textSize = 15f
                setTextColor(Color.parseColor("#90A4AE"))
                gravity  = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    .also { it.marginStart = 12 }
                tag = "placeholder"
            })

            setOnDragListener { view, event -> handleDrop(view as LinearLayout, index, event) }
        }
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
            tag = step.id
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
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginStart = 12 }
            })
        }

        // Long-press only — card stays visible during drag
        card.setOnLongClickListener { view ->
            tapTimes.add(System.currentTimeMillis())
            val clipData   = ClipData.newPlainText("sentenceId", step.id)
            val dragShadow = View.DragShadowBuilder(view)
            view.startDragAndDrop(clipData, dragShadow, view, 0)
            true
        }

        return card
    }

    private fun handleDrop(zone: LinearLayout, zoneIndex: Int, event: DragEvent): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> { zone.setBackgroundColor(Color.parseColor("#BBDEFB")); true }
            DragEvent.ACTION_DRAG_EXITED  -> { zone.setBackgroundColor(Color.parseColor("#E3F2FD")); true }
            DragEvent.ACTION_DROP -> {
                zone.setBackgroundColor(Color.parseColor("#E3F2FD"))
                val droppedView = event.localState as? View ?: return true
                val droppedId   = droppedView.tag as? String ?: return true
                val expectedId  = correctOrder.getOrNull(zoneIndex)?.id ?: return true

                attemptsCount++

                if (droppedId == expectedId) {
                    correctCount++
                    droppedView.visibility = View.GONE
                    dropZoneContents[zoneIndex] = droppedId
                    zone.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    updateDropZoneText(zone, correctOrder[zoneIndex].text)
                    speak("Well done!")

                    if (dropZoneContents.values.all { it != null }) {
                        Handler(Looper.getMainLooper()).postDelayed({ onGameSuccess() }, 500)
                    }
                } else {
                    errorCount++
                    zone.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    Handler(Looper.getMainLooper()).postDelayed({
                        zone.setBackgroundColor(Color.parseColor("#E3F2FD"))
                    }, 600)
                    speak("Try again!")

                    updatePrediction()
                    if (currentPrediction.whatsMissingTrigger) showInlineHint(zoneIndex)

                    // Attempt mini-game after each error
                    Handler(Looper.getMainLooper()).postDelayed({ maybeLaunchMiniGame() }, 800)
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> true
            else -> false
        }
    }

    private fun updateDropZoneText(zone: LinearLayout, newText: String) {
        zone.findViewWithTag<TextView>("placeholder")?.apply {
            text     = newText
            setTextColor(Color.parseColor("#2E7D32"))
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun showInlineHint(zoneIndex: Int) {
        Toast.makeText(this,
            "Hint: Box ${zoneIndex + 1} → \"${correctOrder[zoneIndex].text}\"",
            Toast.LENGTH_LONG).show()
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
    // Mini-game launcher — FIXED
    //
    //  Root cause: model returns minigameTrigger=true but
    //  whatsMissingTrigger=false AND connectDotsTrigger=false.
    //  Old code only checked the two specific triggers → always blocked.
    //  Now minigameTrigger is the PRIMARY gate.
    // ======================================================================

    private fun maybeLaunchMiniGame() {
        if (miniGameTriggeredThisRound) return

        val shouldTrigger = currentPrediction.minigameTrigger          // ← primary gate
                || currentPrediction.whatsMissingTrigger
                || currentPrediction.connectDotsTrigger

        if (!shouldTrigger) {
            Log.d(TAG, "maybeLaunchMiniGame: all triggers false — skipping")
            return
        }

        miniGameTriggeredThisRound = true

        val gameType = when {
            currentPrediction.whatsMissingTrigger -> MiniGamesActivitySequence.TYPE_WHATS_MISSING
            currentPrediction.connectDotsTrigger  -> MiniGamesActivitySequence.TYPE_CONNECT_DOTS
            else -> if (errorCount % 2 == 0)
                MiniGamesActivitySequence.TYPE_WHATS_MISSING
            else
                MiniGamesActivitySequence.TYPE_CONNECT_DOTS
        }

        Log.d(TAG, "Launching mini-game: type=$gameType " +
                "(minigameTrigger=${currentPrediction.minigameTrigger} " +
                "whatsMissing=${currentPrediction.whatsMissingTrigger} " +
                "connectDots=${currentPrediction.connectDotsTrigger})")

        startActivityForResult(
            Intent(this, MiniGamesActivitySequence::class.java).apply {
                putExtra(MiniGamesActivitySequence.EXTRA_TYPE,           gameType)
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

        val accuracy = if (attemptsCount > 0) correctCount.toFloat() / attemptsCount else 0f
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
        gameStartTime = System.currentTimeMillis()
        isTimerRunning = true
        timerHandler.post(timerRunnable)
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
        elapsedTime = System.currentTimeMillis() - gameStartTime
    }

    // ======================================================================
    // Helpers
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