package com.chirathi.voicebridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * MiniGamesActivitySequence — updated mini-games.
 *
 * GAME 0 — What's Missing?
 * Shows steps 1-3 with ONE hidden behind "?".
 * THREE option cards (2 wrong distractors + 1 correct). optionD hidden.
 *
 * GAME 1 — Connect the Mesh
 * Shows 3 step images arranged in a triangle.
 * Dashed lines connect them initially (mesh look).
 * Child taps steps in correct order — each correct tap turns the
 * connecting line solid green and locks that node.
 * Wrong tap: red flash + shake. No penalty skipping.
 */
class MiniGamesActivitySequence : AppCompatActivity() {

    companion object {
        const val TAG                  = "MiniGame"
        const val EXTRA_TYPE           = "MINI_GAME_TYPE"
        const val EXTRA_ROUTINE_ID     = "ROUTINE_ID"
        const val EXTRA_SUB_ROUTINE_ID = "SUB_ROUTINE_ID"
        const val EXTRA_USER_AGE       = "USER_AGE"
        const val EXTRA_HIDDEN_INDEX   = "HIDDEN_STEP_INDEX"
        const val RESULT_PASSED        = "MINI_GAME_PASSED"
        const val RESULT_SCORE         = "MINI_GAME_SCORE"
        const val REQUEST_CODE         = 888
        const val TYPE_WHATS_MISSING   = 0
    }

    private data class Step(val id: String, val drawableRes: Int, val label: String)

    private val routineSteps = mapOf(
        0 to mapOf(
            0 to listOf(
                Step("wake_up",     R.drawable.seq_rtn0_sub1_1_wakeup, "Wake Up"),
                Step("make_bed",    R.drawable.seq_rtn0_sub1_2_bed,    "Make Bed"),
                Step("drink_water", R.drawable.seq_rtn0_sub1_3_drink,  "Drink Water")
            ),
            1 to listOf(
                Step("brush_teeth", R.drawable.seq_rtn0_sub2_1_brush,  "Brush Teeth"),
                Step("wash_face",   R.drawable.seq_rtn0_sub2_2_wash,   "Wash Face"),
                Step("dry_towel",   R.drawable.seq_rtn0_sub2_3_dry,    "Dry Face")
            ),
            2 to listOf(
                Step("get_dressed",  R.drawable.seq_rtn0_sub3_1_change, "Get Dressed"),
                Step("apply_powder", R.drawable.seq_rtn0_sub3_2_cream,  "Put on Lotion"),
                Step("put_pajamas",  R.drawable.seq_rtn0_sub3_3_wash,   "Put Away Pajamas")
            )
        )
    )

    private var miniGameType = TYPE_WHATS_MISSING
    private var routineId    = 0
    private var subRoutineId = 0
    private var userAge      = 6
    private var modelHiddenIndex = -1
    private var steps        = listOf<Step>()
    private var mistakeCount = 0
    private var tts          : TextToSpeech? = null

    // Views from XML
    private lateinit var miniGameTitle      : TextView
    private lateinit var miniGameInstruction: TextView
    private lateinit var progressSection    : LinearLayout
    private lateinit var progressDotText    : TextView
    private lateinit var optionSectionLabel : TextView
    private lateinit var optionsGrid        : GridLayout
    private lateinit var btnSkipMiniGame    : Button
    private lateinit var feedbackOverlay    : FrameLayout
    private lateinit var feedbackEmoji      : ImageView
    private lateinit var feedbackMessage    : TextView
    private lateinit var sequenceRow        : LinearLayout

    private lateinit var seqCard1: FrameLayout; private lateinit var seqCard2: FrameLayout; private lateinit var seqCard3: FrameLayout
    private lateinit var seqImg1 : ImageView;   private lateinit var seqImg2 : ImageView;   private lateinit var seqImg3 : ImageView
    private lateinit var seqBadge1: TextView;   private lateinit var seqBadge2: TextView;   private lateinit var seqBadge3: TextView
    private lateinit var seqQuestion1: TextView;private lateinit var seqQuestion2: TextView;private lateinit var seqQuestion3: TextView

    // Only 3 option cards used now (optionD hidden for What's Missing)
    private lateinit var optionA: FrameLayout;  private lateinit var optionB: FrameLayout
    private lateinit var optionC: FrameLayout;  private lateinit var optionD: FrameLayout
    private lateinit var optionAImg: ImageView; private lateinit var optionBImg: ImageView
    private lateinit var optionCImg: ImageView
    private lateinit var optionALabel: TextView;private lateinit var optionBLabel: TextView
    private lateinit var optionCLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mini_game_activity_sequence)

        miniGameType     = intent.getIntExtra(EXTRA_TYPE, TYPE_WHATS_MISSING)
        routineId        = intent.getIntExtra(EXTRA_ROUTINE_ID, 0)
        subRoutineId     = intent.getIntExtra(EXTRA_SUB_ROUTINE_ID, 0)
        userAge          = intent.getIntExtra(EXTRA_USER_AGE, 6)
        modelHiddenIndex = intent.getIntExtra(EXTRA_HIDDEN_INDEX, -1)
        steps            = routineSteps[routineId]?.get(subRoutineId) ?: routineSteps[0]!![0]!!

        bindViews()
        initTts()

        btnSkipMiniGame.setOnClickListener { finishWithResult(false, 0) }
        setupWhatsMissing()
    }

    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }

    private fun bindViews() {
        miniGameTitle       = findViewById(R.id.miniGameTitle)
        miniGameInstruction = findViewById(R.id.miniGameInstruction)
        progressSection     = findViewById(R.id.progressSection)
        progressDotText     = findViewById(R.id.progressDotText)
        optionSectionLabel  = findViewById(R.id.optionSectionLabel)
        optionsGrid         = findViewById(R.id.optionsGrid)
        btnSkipMiniGame     = findViewById(R.id.btnSkipMiniGame)
        feedbackOverlay     = findViewById(R.id.feedbackOverlay)
        feedbackEmoji       = findViewById(R.id.feedbackEmoji)
        feedbackMessage     = findViewById(R.id.feedbackMessage)
        sequenceRow         = findViewById(R.id.sequenceRow)

        seqCard1 = findViewById(R.id.seqCard1); seqCard2 = findViewById(R.id.seqCard2); seqCard3 = findViewById(R.id.seqCard3)
        seqImg1  = findViewById(R.id.seqImg1);  seqImg2  = findViewById(R.id.seqImg2);  seqImg3  = findViewById(R.id.seqImg3)
        seqBadge1= findViewById(R.id.seqBadge1);seqBadge2= findViewById(R.id.seqBadge2);seqBadge3= findViewById(R.id.seqBadge3)
        seqQuestion1=findViewById(R.id.seqQuestion1);seqQuestion2=findViewById(R.id.seqQuestion2);seqQuestion3=findViewById(R.id.seqQuestion3)

        optionA   = findViewById(R.id.optionA);    optionB   = findViewById(R.id.optionB)
        optionC   = findViewById(R.id.optionC);    optionD   = findViewById(R.id.optionD)
        optionAImg= findViewById(R.id.optionAImg); optionBImg= findViewById(R.id.optionBImg)
        optionCImg= findViewById(R.id.optionCImg)
        optionALabel=findViewById(R.id.optionALabel);optionBLabel=findViewById(R.id.optionBLabel)
        optionCLabel=findViewById(R.id.optionCLabel)
    }

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US; tts?.setSpeechRate(0.75f); tts?.setPitch(1.2f)
            }
        }
    }

    private fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mg") }

    // =========================================================================
    // GAME 0 — What's Missing? (3 options: 2 wrong + 1 correct)
    // =========================================================================

    private fun setupWhatsMissing() {
        miniGameTitle.text       = "What's Missing?"
        miniGameInstruction.text = "One step is hiding! Can you find it?"
        optionSectionLabel.text  = "Tap the missing step:"
        progressSection.visibility = View.GONE

        // Hide the 4th option — we only show 3 choices
        optionD.visibility = View.GONE

        val missingIndex = if (modelHiddenIndex in steps.indices) {
            modelHiddenIndex
        } else {
            steps.indices.random()
        }
        val missingStep  = steps[missingIndex]
        speak("One step is hiding! Which one is it?")

        // ── Sequence row with "?" on hidden card ──────────────────────────
        val seqImgs   = listOf(seqImg1,      seqImg2,      seqImg3)
        val seqBadges = listOf(seqBadge1,    seqBadge2,    seqBadge3)
        val seqQs     = listOf(seqQuestion1, seqQuestion2, seqQuestion3)

        for ((i, step) in steps.withIndex()) {
            seqImgs[i].setImageResource(step.drawableRes)
            if (i == missingIndex) {
                seqImgs[i].visibility   = View.GONE
                seqBadges[i].visibility = View.GONE
                seqQs[i].visibility     = View.VISIBLE
            } else {
                seqImgs[i].visibility   = View.VISIBLE
                seqBadges[i].text       = "${i + 1}"
                seqBadges[i].visibility = View.VISIBLE
                seqQs[i].visibility     = View.GONE
            }
        }

        // ── Build 3 options: 2 distractors + 1 correct, shuffled ─────────
        val distractors = steps.filter { it.id != missingStep.id }.shuffled().take(2)
        val options     = (distractors + missingStep).shuffled()   // always 3

        val frames = listOf(optionA, optionB, optionC)
        val imgs   = listOf(optionAImg, optionBImg, optionCImg)
        val labels = listOf(optionALabel, optionBLabel, optionCLabel)

        for ((i, option) in options.withIndex()) {
            imgs[i].setImageResource(option.drawableRes)
            labels[i].text = option.label
            val f = frames[i]
            f.setOnClickListener {
                f.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction { f.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }.start()
                handleWhatsMissingAnswer(option, missingStep, frames)
            }
        }
    }

    private fun handleWhatsMissingAnswer(chosen: Step, correct: Step, allFrames: List<FrameLayout>) {
        allFrames.forEach { it.isClickable = false }
        if (chosen.id == correct.id) {
            speak("Great job!")
            showFeedback("", "Well done!", "#4CAF50") { finishWithResult(true, 100) }
        } else {
            mistakeCount++
            speak("Try again!")
            showFeedback("", "Try again!", "#FF6B6B") {
                feedbackOverlay.visibility = View.GONE
                allFrames.forEach { it.isClickable = true }
            }
        }
    }

    // =========================================================================
    // Feedback overlay
    // =========================================================================

    private fun showFeedback(emoji: String, message: String, color: String, onDone: () -> Unit) {
        // Set icon image instead of emoji text
        if (color == "#4CAF50") {
            feedbackEmoji.setImageResource(R.drawable.correct_answer)  // same as RhythmSummary
        } else {
            feedbackEmoji.setImageResource(R.drawable.delete)           // same as RhythmSummary
        }
        feedbackMessage.text = message
        feedbackMessage.setTextColor(Color.parseColor(color))
        feedbackOverlay.visibility = View.VISIBLE
        feedbackOverlay.alpha      = 0f
        feedbackOverlay.animate().alpha(1f).setDuration(300).withEndAction {
            Handler(Looper.getMainLooper()).postDelayed({ onDone() }, 1400)
        }.start()
    }

    private fun finishWithResult(passed: Boolean, score: Int) {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(RESULT_PASSED, passed); putExtra(RESULT_SCORE, score)
        })
        finish()
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}