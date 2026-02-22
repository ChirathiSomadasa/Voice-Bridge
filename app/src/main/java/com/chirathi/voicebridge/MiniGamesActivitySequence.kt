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
 *  GAME 0 — What's Missing?
 *    Shows steps 1-3 with ONE hidden behind "?".
 *    THREE option cards (2 wrong distractors + 1 correct). optionD hidden.
 *
 *  GAME 1 — Connect the Mesh
 *    Shows 3 step images arranged in a triangle.
 *    Dashed lines connect them initially (mesh look).
 *    Child taps steps in correct order — each correct tap turns the
 *    connecting line solid green and locks that node.
 *    Wrong tap: red flash + shake. No penalty skipping.
 */
class MiniGamesActivitySequence : AppCompatActivity() {

    companion object {
        const val TAG                  = "MiniGame"
        const val EXTRA_TYPE           = "MINI_GAME_TYPE"
        const val EXTRA_ROUTINE_ID     = "ROUTINE_ID"
        const val EXTRA_SUB_ROUTINE_ID = "SUB_ROUTINE_ID"
        const val EXTRA_USER_AGE       = "USER_AGE"
        const val RESULT_PASSED        = "MINI_GAME_PASSED"
        const val RESULT_SCORE         = "MINI_GAME_SCORE"
        const val REQUEST_CODE         = 888
        const val TYPE_WHATS_MISSING   = 0
        const val TYPE_CONNECT_DOTS    = 1
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
    private lateinit var feedbackEmoji      : TextView
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

        miniGameType = intent.getIntExtra(EXTRA_TYPE, TYPE_WHATS_MISSING)
        routineId    = intent.getIntExtra(EXTRA_ROUTINE_ID, 0)
        subRoutineId = intent.getIntExtra(EXTRA_SUB_ROUTINE_ID, 0)
        userAge      = intent.getIntExtra(EXTRA_USER_AGE, 6)
        steps        = routineSteps[routineId]?.get(subRoutineId) ?: routineSteps[0]!![0]!!

        bindViews()
        initTts()

        btnSkipMiniGame.setOnClickListener { finishWithResult(false, 0) }

        when (miniGameType) {
            TYPE_WHATS_MISSING -> setupWhatsMissing()
            else               -> setupConnectMesh()
        }
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
        miniGameTitle.text       = "What's Missing? 🤔"
        miniGameInstruction.text = "One step is hiding! Can you find it?"
        optionSectionLabel.text  = "Tap the missing step:"
        progressSection.visibility = View.GONE

        // Hide the 4th option — we only show 3 choices
        optionD.visibility = View.GONE

        val missingIndex = steps.indices.random()
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
            speak("Yes! That's right! Great job!")
            showFeedback("🎉", "That's it! Well done!", "#4CAF50") { finishWithResult(true, 100) }
        } else {
            mistakeCount++
            speak("Not quite. Try again!")
            showFeedback("💪", "Try again!", "#FF6B6B") {
                feedbackOverlay.visibility = View.GONE
                allFrames.forEach { it.isClickable = true }
            }
        }
    }

    // =========================================================================
    // GAME 1 — Connect the Mesh
    //
    //  Steps shown in a triangle arrangement inside a FrameLayout.
    //  Dashed grey lines connect all pairs initially (mesh look).
    //  Child taps steps in correct order 1 → 2 → 3.
    //  Each correct tap: the line from previous node to this node turns solid green.
    //  Wrong tap: red flash + shake animation.
    // =========================================================================

    private fun setupConnectMesh() {
        miniGameTitle.text       = "Connect the Steps! 🔗"
        miniGameInstruction.text = "Tap the steps in order: 1 → 2 → 3"

        // Hide XML elements we're replacing
        sequenceRow.visibility        = View.GONE
        optionsGrid.visibility        = View.GONE
        optionSectionLabel.visibility = View.GONE
        progressSection.visibility    = View.VISIBLE
        progressDotText.text          = "1"

        speak("Connect the steps! Tap number 1 first, then 2, then 3!")

        // ── Build mesh container ──────────────────────────────────────────
        val meshFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(380)
            ).also { it.topMargin = dpToPx(12); it.bottomMargin = dpToPx(8) }
            setBackgroundColor(Color.parseColor("#FFF9C4"))
        }

        // Shuffle steps so order isn't obvious from position
        val shuffled    = steps.shuffled()
        val nodeSize    = dpToPx(110)

        // Triangle positions (left-top, right-top, bottom-center)
        val positions = listOf(
            Pair(dpToPx(30),  dpToPx(30)),
            Pair(dpToPx(220), dpToPx(30)),
            Pair(dpToPx(125), dpToPx(230))
        )

        // Node views
        val nodeViews      = mutableListOf<FrameLayout>()
        val nodeOrderNums  = mutableListOf<Int>()          // correct order number (1-indexed)

        // ── Canvas overlay for lines ──────────────────────────────────────
        val canvas = MeshLineView(this)
        canvas.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        // ── Build 3 nodes ─────────────────────────────────────────────────
        for ((i, step) in shuffled.withIndex()) {
            val (x, y) = positions[i]
            val correctNum = steps.indexOfFirst { it.id == step.id } + 1
            nodeOrderNums.add(correctNum)

            val nodeCard = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(nodeSize, nodeSize).also {
                    it.leftMargin = x; it.topMargin = y
                }
                setBackgroundResource(R.drawable.rounded_card_background)
                elevation = 6f
                tag = correctNum
            }

            val img = ImageView(this).apply {
                setImageResource(step.drawableRes)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
                setPadding(12, 12, 12, 12)
            }

            val badge = TextView(this).apply {
                text      = "$correctNum"
                textSize  = 16f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.circle_badge)
                gravity   = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(dpToPx(32), dpToPx(32)).also {
                    it.gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    it.topMargin = dpToPx(6); it.rightMargin = dpToPx(6)
                }
            }

            nodeCard.addView(img)
            nodeCard.addView(badge)
            meshFrame.addView(nodeCard)
            nodeViews.add(nodeCard)
        }

        meshFrame.addView(canvas)

        // Register node centre-points for line drawing (after layout)
        meshFrame.post {
            val centres = nodeViews.map { node ->
                val lp = node.layoutParams as FrameLayout.LayoutParams
                PointF(
                    (lp.leftMargin + node.width / 2).toFloat(),
                    (lp.topMargin  + node.height / 2).toFloat()
                )
            }
            // Draw all dashed background lines (mesh)
            canvas.setMeshPoints(centres)
            canvas.invalidate()
        }

        // ── Tap-to-connect logic ──────────────────────────────────────────
        var nextExpected  = 1
        var prevNodeIndex = -1     // index in nodeViews of the last-confirmed node

        for ((i, nodeCard) in nodeViews.withIndex()) {
            val orderNum = nodeOrderNums[i]
            nodeCard.setOnClickListener { card ->
                if (orderNum == nextExpected) {
                    // ✅ Correct
                    card.animate().scaleX(1.18f).scaleY(1.18f).setDuration(160)
                        .withEndAction { card.animate().scaleX(1f).scaleY(1f).setDuration(160).start() }
                        .start()
                    card.setBackgroundColor(Color.parseColor("#C8E6C9"))
                    card.isClickable = false
                    speak(shuffled[i].label)

                    // Activate line from prev to this node
                    if (prevNodeIndex >= 0) {
                        meshFrame.post {
                            val lp0 = nodeViews[prevNodeIndex].layoutParams as FrameLayout.LayoutParams
                            val lp1 = nodeViews[i].layoutParams as FrameLayout.LayoutParams
                            canvas.addSolidLine(
                                PointF((lp0.leftMargin + nodeViews[prevNodeIndex].width / 2f),
                                    (lp0.topMargin  + nodeViews[prevNodeIndex].height / 2f)),
                                PointF((lp1.leftMargin + nodeViews[i].width / 2f),
                                    (lp1.topMargin  + nodeViews[i].height / 2f))
                            )
                            canvas.invalidate()
                        }
                    }
                    prevNodeIndex = i
                    nextExpected++
                    progressDotText.text = "$nextExpected"

                    if (nextExpected > steps.size) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            speak("Amazing! You connected all the steps!")
                            showFeedback("🎉", "All connected! Amazing!", "#4CAF50") {
                                finishWithResult(true, maxOf(0, 100 - mistakeCount * 15))
                            }
                        }, 400)
                    }
                } else {
                    // ❌ Wrong node
                    mistakeCount++
                    speak("Not yet! Tap number $nextExpected first.")
                    card.animate().translationX(-14f).setDuration(70)
                        .withEndAction {
                            card.animate().translationX(14f).setDuration(70)
                                .withEndAction {
                                    card.animate().translationX(0f).setDuration(70).start()
                                }.start()
                        }.start()
                    card.setBackgroundColor(Color.parseColor("#FFCDD2"))
                    Handler(Looper.getMainLooper()).postDelayed({
                        card.setBackgroundResource(R.drawable.rounded_card_background)
                    }, 500)
                }
            }
        }

        // ── Insert mesh into scroll root ──────────────────────────────────
        val scrollChild = (findViewById<ScrollView>(R.id.miniGameScrollView))
            .getChildAt(0) as LinearLayout
        // Insert after progressSection (index varies), so add after instruction card
        val insertIdx = scrollChild.indexOfChild(progressSection) + 1
        scrollChild.addView(meshFrame, insertIdx)
    }

    // =========================================================================
    // Custom View — draws the mesh lines
    // =========================================================================

    inner class MeshLineView(ctx: Context) : View(ctx) {
        private val meshPoints  = mutableListOf<PointF>()
        private val solidLines  = mutableListOf<Pair<PointF, PointF>>()

        private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = Color.parseColor("#BDBDBD")
            strokeWidth = 4f
            style       = Paint.Style.STROKE
            pathEffect  = DashPathEffect(floatArrayOf(18f, 12f), 0f)
        }
        private val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = Color.parseColor("#4CAF50")
            strokeWidth = 7f
            style       = Paint.Style.STROKE
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = Color.parseColor("#804CAF50")
            strokeWidth = 14f
            style       = Paint.Style.STROKE
        }

        fun setMeshPoints(pts: List<PointF>) { meshPoints.clear(); meshPoints.addAll(pts) }
        fun addSolidLine(a: PointF, b: PointF) { solidLines.add(Pair(a, b)) }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Draw all dashed background lines between every pair of nodes
            for (i in meshPoints.indices) {
                for (j in i + 1 until meshPoints.size) {
                    val alreadySolid = solidLines.any { (a, b) ->
                        (similar(a, meshPoints[i]) && similar(b, meshPoints[j])) ||
                                (similar(a, meshPoints[j]) && similar(b, meshPoints[i]))
                    }
                    if (!alreadySolid) {
                        canvas.drawLine(
                            meshPoints[i].x, meshPoints[i].y,
                            meshPoints[j].x, meshPoints[j].y,
                            dashedPaint
                        )
                    }
                }
            }

            // Draw confirmed solid green lines with glow
            for ((a, b) in solidLines) {
                canvas.drawLine(a.x, a.y, b.x, b.y, glowPaint)
                canvas.drawLine(a.x, a.y, b.x, b.y, solidPaint)
            }
        }

        private fun similar(a: PointF, b: PointF) =
            Math.abs(a.x - b.x) < 5f && Math.abs(a.y - b.y) < 5f
    }

    // =========================================================================
    // Feedback overlay
    // =========================================================================

    private fun showFeedback(emoji: String, message: String, color: String, onDone: () -> Unit) {
        feedbackEmoji.text   = emoji
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