package com.chirathi.voicebridge

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * MiniGameActivity — v3.0
 *
 * Two completely different mini-games:
 *
 * TYPE 0  FACE BUILDER
 *   Child is told an emotion to build (e.g. "Build an ANGRY face!").
 *   Three sequential steps: pick eyes → pick eyebrows → pick mouth.
 *   Each step shows 3 image options (1 correct + 2 from other emotions).
 *   After all 3 steps, the assembled face is revealed.
 *   Any wrong part is highlighted in red with the correct part shown in green
 *   and replaced after a short pause so the child learns.
 *
 * TYPE 1  FEELINGS SCENARIO
 *   A scenario image + descriptive text is shown
 *   (e.g. "It's your birthday! You are going to cut the cake!").
 *   Four full emotion face images are shown as choices.
 *   Child taps the right one. Correct = green highlight, wrong = red + correct shown.
 *
 * Uses:  activity_mini_game_face.xml   (TYPE 0)
 *        activity_mini_game_scenario.xml (TYPE 1)
 */
class MiniGameMoodMatchActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG  = "MiniGameActivity"
        const val EXTRA_TYPE   = "MINIGAME_TYPE"
        const val EXTRA_DIFF   = "MINIGAME_DIFFICULTY"
        const val EXTRA_AGE    = "AGE_GROUP"
        const val REQUEST_CODE = 777

        // Face-builder steps
        private const val STEP_EYES     = 0
        private const val STEP_EYEBROWS = 1
        private const val STEP_MOUTH    = 2
    }

    // ── Intent params ─────────────────────────────────────────────────────────
    private var gameType   = 0
    private var difficulty = 0
    private var age        = 6

    // ── TTS ───────────────────────────────────────────────────────────────────
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    // =========================================================================
    // DATA
    // =========================================================================

    // Maps emotion → the drawable name prefix for each part
    // The arrays in arrays.xml store two options each; we pick one at random for "correct"
    // and pull distractors from the other emotions.
    private val partArrayNames = mapOf(
        "eyes" to mapOf(
            "happy"    to listOf("face_eyes_happy_wide",       "face_eyes_happy_crescent"),
            "sad"      to listOf("face_eyes_sad_droopy",       "face_eyes_sad_teary"),
            "angry"    to listOf("face_eyes_angry_narrow",     "face_eyes_angry_slanted"),
            "scared"   to listOf("face_eyes_scared_wide",      "face_eyes_scared_worried")
        ),
        "eyebrows" to mapOf(
            "happy"    to listOf("face_eyebrows_happy_raised"),
            "sad"      to listOf("face_eyebrows_sad_droopy"),
            "angry"    to listOf("face_eyebrows_angry_furrowed"),
            "scared"   to listOf("face_eyebrows_scared_raised")
        ),
        "mouth" to mapOf(
            "happy"    to listOf("face_mouth_happy_smile",    "face_mouth_happy_laugh"),
            "sad"      to listOf("face_mouth_sad_frown",      "face_mouth_sad_downturn"),
            "angry"    to listOf("face_mouth_angry_grimace",  "face_mouth_angry_teeth"),
            "scared"   to listOf("face_mouth_scared_wavy",   "face_mouth_scared_open")
        )
    )

    // Emotions that have a full face reference image (mood_lvl0_shared_* or lvl1)
    private val emotionFaceDrawable = mapOf(
        "happy"    to "mood_lvl0_shared_happy",
        "sad"      to "mood_lvl0_shared_sad",
        "angry"    to "mood_lvl0_shared_angry",
        "scared"   to "mood_lvl1_shared_scared"
    )

    private val emotionDisplayName = mapOf(
        "happy"    to "Happy",
        "sad"      to "Sad",
        "angry"    to "Angry",
        "scared"   to "Scared",
        "bored"    to "Bored",
        "shy"      to "Shy",
        "proud"    to "Proud"
    )

    // Scenario data
    data class ScenarioData(
        val scenarioDrawable: String,
        val prompt: String,
        val correctEmotion: String
    )

    private val scenarios = listOf(
        ScenarioData(
            "mood_lvl2_age6_birthday",
            "It's your birthday! \nYou are going to cut the cake!\nWhat are you feeling?",
            "happy"
        ),
        ScenarioData(
            "mood_lvl2_age6_dropped_icecream",
            "Oh no! You dropped your ice cream on the floor! \nWhat are you feeling?",
            "sad"
        ),
        ScenarioData(
            "mood_lvl2_age6_broken_robot",
            "Your favourite robot toy broke into pieces! \nWhat are you feeling?",
            "sad"
        ),
        ScenarioData(
            "mood_lvl2_age6_dog_barking",
            "A big dog is barking loudly at you! \nWhat are you feeling?",
            "scared"
        ),
        ScenarioData(
            "mood_lvl2_age6_bored",
            "You have nothing to do.\nYou've been waiting for a long, long time.\nWhat are you feeling?",
            "bored"  // bored uses mood_lvl0_shared_bored
        ),
        ScenarioData(
            "story_broken_toy_panel2_breaks",
            "Oh no! Your Teddy's leg just broke! \nWhat are you feeling?",
            "sad"
        ),
        ScenarioData(
            "story_lost_toy_panel2_searching",
            "You can't find your favourite toy anywhere! \nWhat are you feeling?",
            "sad"
        ),
        ScenarioData(
            "story_new_pet_panel1_pet_store",
            "Mom says you can get a pet! \nWhat are you feeling?",
            "happy"
        ),
        ScenarioData(
            "story_school_panel2_classroom",
            "You walk into the new classroom alone. \nWhat are you feeling?",
            "proud"
        ),
        ScenarioData(
            "story_birthday_panel1_invite",
            "You just got an invitation to your friend's party! \nWhat are you feeling?",
            "happy"
        )
    )

    // =========================================================================
    // FACE BUILDER STATE
    // =========================================================================

    private var targetEmotion = ""
    private var currentStep   = STEP_EYES   // 0, 1, 2

    // What the child actually selected for each step (drawable name)
    private val selectedDrawables = arrayOfNulls<String>(3)
    // What the correct answer was for each step (drawable name)
    private val correctDrawables  = arrayOfNulls<String>(3)
    // Whether each selection was correct
    private val stepCorrect       = BooleanArray(3) { false }

    // The 3 options shown at each step: list of drawable names, shuffled
    private var currentOptions = listOf<String>()

    // ── Face builder views ────────────────────────────────────────────────────
    private lateinit var fbTitle:        TextView
    private lateinit var fbInstruction:  TextView
    private lateinit var fbProgress:     TextView
    private lateinit var fbPreviewEyes:  ImageView    // live preview of selected eyes
    private lateinit var fbPreviewBrows: ImageView    // live preview of selected eyebrows
    private lateinit var fbPreviewMouth: ImageView    // live preview of selected mouth
    private lateinit var fbPreviewBase:  ImageView    // neutral face base outline
    private lateinit var fbOpt1:         ImageView
    private lateinit var fbOpt2:         ImageView
    private lateinit var fbOpt3:         ImageView
    private lateinit var fbOptLabel1:    TextView
    private lateinit var fbOptLabel2:    TextView
    private lateinit var fbOptLabel3:    TextView
    private lateinit var fbFeedback:     TextView
    private lateinit var fbBtnDone:      Button
    private lateinit var fbSelectionRow: LinearLayout
    private lateinit var fbPreviewArea:  ConstraintLayout
    private lateinit var fbReviewArea:   LinearLayout
    private lateinit var fbRevealBase:  ImageView
    private lateinit var fbRevealBrows: ImageView
    private lateinit var fbRevealEyes:  ImageView
    private lateinit var fbRevealMouth: ImageView

    // ── Scenario views ────────────────────────────────────────────────────────
    private lateinit var scTitle:       TextView
    private lateinit var scPrompt:      TextView
    private lateinit var scImage:       ImageView
    private lateinit var scOpt1:        ImageView
    private lateinit var scOpt2:        ImageView
    private lateinit var scOpt3:        ImageView
    private lateinit var scOpt4:        ImageView
    private lateinit var scLabel1:      TextView
    private lateinit var scLabel2:      TextView
    private lateinit var scLabel3:      TextView
    private lateinit var scLabel4:      TextView
    private lateinit var scFeedback:    TextView
    private lateinit var scBtnDone:     Button
    private var scCorrectEmotion = ""

    // =========================================================================
    // Lifecycle
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameType   = intent.getIntExtra(EXTRA_TYPE, 0)
        difficulty = intent.getIntExtra(EXTRA_DIFF, 0)
        age        = intent.getIntExtra(EXTRA_AGE, 6)

        tts = TextToSpeech(this, this)

        if (gameType == 1) {
            setContentView(R.layout.activity_mini_game_scenario)
            bindScenarioViews()
            setupScenario()
        } else {
            setContentView(R.layout.activity_mini_game_face)
            bindFaceViews()
            setupFaceBuilder()
        }

        // Block back navigation — child must complete the mini-game before returning
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* intentionally empty */ }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
    }



    // =========================================================================
    // ═══════════════════════  FACE BUILDER  ══════════════════════════════════
    // =========================================================================

    private fun bindFaceViews() {
        fbTitle        = findViewById(R.id.fbTitle)
        fbInstruction  = findViewById(R.id.fbInstruction)
        fbProgress     = findViewById(R.id.fbProgress)
        fbPreviewBase  = findViewById(R.id.fbPreviewBase)
        fbPreviewEyes  = findViewById(R.id.fbPreviewEyes)
        fbPreviewBrows = findViewById(R.id.fbPreviewBrows)
        fbPreviewMouth = findViewById(R.id.fbPreviewMouth)
        fbOpt1         = findViewById(R.id.fbOpt1)
        fbOpt2         = findViewById(R.id.fbOpt2)
        fbOpt3         = findViewById(R.id.fbOpt3)
        fbOptLabel1    = findViewById(R.id.fbOptLabel1)
        fbOptLabel2    = findViewById(R.id.fbOptLabel2)
        fbOptLabel3    = findViewById(R.id.fbOptLabel3)
        fbFeedback     = findViewById(R.id.fbFeedback)
        fbRevealBase  = findViewById(R.id.fbRevealBase)
        fbRevealBrows = findViewById(R.id.fbRevealBrows)
        fbRevealEyes  = findViewById(R.id.fbRevealEyes)
        fbRevealMouth = findViewById(R.id.fbRevealMouth)
        fbBtnDone      = findViewById(R.id.fbBtnDone)
        fbSelectionRow = findViewById(R.id.fbSelectionRow)
        fbPreviewArea  = findViewById(R.id.fbPreviewArea)
        fbReviewArea   = findViewById(R.id.fbReviewArea)

        fbReviewArea.visibility = View.GONE
        fbBtnDone.visibility    = View.GONE
        fbFeedback.visibility   = View.GONE
        fbBtnDone.setOnClickListener { returnToGame() }
    }

    private fun setupFaceBuilder() {
        // Pick a random target emotion from the emotions we have face parts for
        val candidates = partArrayNames["eyes"]!!.keys.toList()
        targetEmotion = candidates.random()

        val emotionName = emotionDisplayName[targetEmotion] ?: targetEmotion.replaceFirstChar { it.uppercase() }
        fbTitle.text = "Build a $emotionName face!"
        Log.d(TAG, "FaceBuilder: target=$targetEmotion")

        // Set base face (neutral outline)
        val baseRes = resolve("face_base_neutral")
        if (baseRes != 0) fbPreviewBase.setImageResource(baseRes)

        // Hide part previews until selected
        fbPreviewEyes.visibility  = View.INVISIBLE
        fbPreviewBrows.visibility = View.INVISIBLE
        fbPreviewMouth.visibility = View.INVISIBLE

        showFaceStep(STEP_EYES)
    }

    /**
     * Displays the 3 option images for the current step (eyes / eyebrows / mouth).
     */
    private fun showFaceStep(step: Int) {
        currentStep = step

        val partName = when (step) {
            STEP_EYES     -> "eyes"
            STEP_EYEBROWS -> "eyebrows"
            else          -> "mouth"
        }
        val stepLabel = when (step) {
            STEP_EYES     -> "eyes"
            STEP_EYEBROWS -> "eyebrows"
            else          -> "mouth"
        }

        fbInstruction.text = "Choose the $stepLabel:"
        fbProgress.text    = "Step ${step + 1} of 3"

        // Pick the correct drawable for this part + emotion
        val correctOptions = partArrayNames[partName]!![targetEmotion] ?: listOf()
        val correctDrawable = correctOptions.random()
        correctDrawables[step] = correctDrawable

        // Pick 2 distractors from OTHER emotions
        val otherEmotions = partArrayNames[partName]!!.keys.filter { it != targetEmotion }.shuffled().take(2)
        val distractor1 = partArrayNames[partName]!![otherEmotions[0]]!!.random()
        val distractor2 = partArrayNames[partName]!![otherEmotions[1]]!!.random()

        // Shuffle the 3 options
        currentOptions = listOf(correctDrawable, distractor1, distractor2).shuffled()

        // Apply to image views
        applyOptionImage(fbOpt1, currentOptions[0])
        applyOptionImage(fbOpt2, currentOptions[1])
        applyOptionImage(fbOpt3, currentOptions[2])

        // Clear labels
        listOf(fbOptLabel1, fbOptLabel2, fbOptLabel3).forEach { it.text = "" }

        // Set click listeners
        fbOpt1.setOnClickListener { onFacePartSelected(fbOpt1, fbOptLabel1, currentOptions[0], step) }
        fbOpt2.setOnClickListener { onFacePartSelected(fbOpt2, fbOptLabel2, currentOptions[1], step) }
        fbOpt3.setOnClickListener { onFacePartSelected(fbOpt3, fbOptLabel3, currentOptions[2], step) }

        // Announce
        speak("Choose the $stepLabel for a ${emotionDisplayName[targetEmotion]} face!")
    }

    /**
     * Called when the child taps one of the three part options.
     */
    private fun onFacePartSelected(tappedView: ImageView, label: TextView, drawableName: String, step: Int) {
        // Disable all three so child can't re-tap
        listOf(fbOpt1, fbOpt2, fbOpt3).forEach { it.isClickable = false }

        selectedDrawables[step] = drawableName
        val isCorrect = drawableName == correctDrawables[step]
        stepCorrect[step] = isCorrect

        // Highlight tapped option
        val highlightColor = if (isCorrect) R.color.green else android.R.color.holo_red_light
        tappedView.setBackgroundColor(ContextCompat.getColor(this, highlightColor))
        label.text      = if (isCorrect) "✓" else "✗"
        label.setTextColor(ContextCompat.getColor(this, highlightColor))

        // Update the live face preview with the selected part
        updateFacePreview(step, drawableName)

        Handler(Looper.getMainLooper()).postDelayed({
            if (step < STEP_MOUTH) {
                showFaceStep(step + 1)
            } else {
                showFaceReview()
            }
        }, 700)
    }

    /**
     * Places the selected part drawable into the face preview overlay.
     */
    private fun updateFacePreview(step: Int, drawableName: String) {
        val iv = when (step) {
            STEP_EYES     -> { fbPreviewEyes.visibility  = View.VISIBLE; fbPreviewEyes }
            STEP_EYEBROWS -> { fbPreviewBrows.visibility = View.VISIBLE; fbPreviewBrows }
            else          -> { fbPreviewMouth.visibility = View.VISIBLE; fbPreviewMouth }
        }
        val res = resolve(drawableName)
        if (res != 0) iv.setImageResource(res)
    }

    /**
     * After all 3 parts chosen: hide selection row, show the full reference face,
     * then give per-part feedback and replace wrong parts.
     */
    private fun showFaceReview() {
        fbSelectionRow.visibility = View.GONE
        fbInstruction.text        = "Here is the face you built!"
        fbProgress.text           = ""

        // Show review container
        fbReviewArea.visibility = View.VISIBLE

        // ── Assemble the CORRECT face from parts in the reveal panel ─────────
        val partNames = listOf("eyes", "eyebrows", "mouth")
        val revealViews = listOf(fbRevealEyes, fbRevealBrows, fbRevealMouth)

        // Pick one correct drawable per part and populate the reveal face
        for (step in 0..2) {
            val partKey     = when (step) { 0 -> "eyes"; 1 -> "eyebrows"; else -> "mouth" }
            val correctName = correctDrawables[step]
                ?: partArrayNames[partKey]!![targetEmotion]!!.first()
            val res = resolve(correctName)
            if (res != 0) {
                revealViews[step].setImageResource(res)
                revealViews[step].visibility = View.VISIBLE
            }
        }

        val emotionName = emotionDisplayName[targetEmotion] ?: targetEmotion

        // ── Build feedback and fix wrong parts in the child's preview ────────
        var allCorrect = true
        val feedbackLines = mutableListOf<String>()

        for (step in 0..2) {
            if (!stepCorrect[step]) {
                allCorrect = false
                feedbackLines.add("Look at the correct face above to see the ${emotionName} ${partNames[step]}!")
            }
        }

        fbFeedback.visibility = View.VISIBLE
        if (allCorrect) {
            fbFeedback.text = "Perfect! You built a great $emotionName face!"
            fbFeedback.setTextColor(ContextCompat.getColor(this, R.color.green))
            speak("Perfect! You built a great $emotionName face!")
        } else {
            fbFeedback.text = feedbackLines.joinToString("\n") { "• $it" }
            fbFeedback.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            speak("Good try! Watch how the $emotionName face should look!")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            fbBtnDone.visibility = View.VISIBLE
        }, 1200)
    }

    // =========================================================================
    // ═══════════════════════  SCENARIO GAME  ═════════════════════════════════
    // =========================================================================

    private fun bindScenarioViews() {
        scTitle    = findViewById(R.id.scTitle)
        scPrompt   = findViewById(R.id.scPrompt)
        scImage    = findViewById(R.id.scImage)
        scOpt1     = findViewById(R.id.scOpt1)
        scOpt2     = findViewById(R.id.scOpt2)
        scOpt3     = findViewById(R.id.scOpt3)
        scOpt4     = findViewById(R.id.scOpt4)
        scLabel1   = findViewById(R.id.scLabel1)
        scLabel2   = findViewById(R.id.scLabel2)
        scLabel3   = findViewById(R.id.scLabel3)
        scLabel4   = findViewById(R.id.scLabel4)
        scFeedback = findViewById(R.id.scFeedback)
        scBtnDone  = findViewById(R.id.scBtnDone)

        scFeedback.visibility = View.INVISIBLE
        scBtnDone.visibility  = View.GONE
        scBtnDone.setOnClickListener { returnToGame() }
    }

    private fun setupScenario() {
        val scenario     = scenarios.random()
        scCorrectEmotion = scenario.correctEmotion

        scTitle.text  = "How would you feel?"
        scPrompt.text = scenario.prompt

        // Scenario illustration
        val scRes = resolve(scenario.scenarioDrawable)
        if (scRes != 0) scImage.setImageResource(scRes)
        else scImage.setImageResource(R.drawable.panda_confused)

        // Build 4 emotion choices: correct + 3 distractors
        // Each option is shown as a face IMAGE (not just a text button)
        val allEmotions = listOf("happy", "sad", "angry", "scared", "bored", "shy")
        val distractors = allEmotions.filter { it != scenario.correctEmotion }.shuffled().take(3)
        val options     = (listOf(scenario.correctEmotion) + distractors).shuffled()

        val optViews  = listOf(scOpt1, scOpt2, scOpt3, scOpt4)
        val lblViews  = listOf(scLabel1, scLabel2, scLabel3, scLabel4)

        options.forEachIndexed { i, emotion ->
            // Set face image
            val faceDrawable = when (emotion) {
                "happy"    -> "mood_lvl0_shared_happy"
                "sad"      -> "mood_lvl0_shared_sad"
                "angry"    -> "mood_lvl0_shared_angry"
                "scared"   -> "mood_lvl1_shared_scared"
                "bored"    -> "mood_lvl0_shared_bored"
                "shy"      -> "mood_lvl0_shared_shy"
                "proud"    -> "mood_lvl1_shared_proud"
                "surprised"-> "mood_lvl0_age8_surprise"
                else       -> "mood_lvl0_shared_happy"
            }
            val res = resolve(faceDrawable)
            if (res != 0) optViews[i].setImageResource(res)
            optViews[i].tag  = emotion
            lblViews[i].text = emotionDisplayName[emotion] ?: emotion.replaceFirstChar { it.uppercase() }
            optViews[i].setOnClickListener { onScenarioAnswerTapped(optViews[i], lblViews[i], emotion) }
        }

        speak(scenario.prompt)
        Log.d(TAG, "Scenario: correct=${scenario.correctEmotion} prompt=${scenario.prompt}")
    }

    private var scenarioAnswered = false

    private fun onScenarioAnswerTapped(tapped: ImageView, label: TextView, emotion: String) {
        if (scenarioAnswered) return
        scenarioAnswered = true

        val optViews = listOf(scOpt1, scOpt2, scOpt3, scOpt4)
        val lblViews = listOf(scLabel1, scLabel2, scLabel3, scLabel4)

        // Disable all
        optViews.forEach { it.isClickable = false }

        val isCorrect = emotion == scCorrectEmotion

        if (isCorrect) {
            tapped.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
            label.setTextColor(ContextCompat.getColor(this, R.color.green))
            label.text = "✓ " + label.text.toString()
            scFeedback.text = " That's right — you would feel ${emotionDisplayName[scCorrectEmotion]}!"
            scFeedback.setTextColor(ContextCompat.getColor(this, R.color.green))
            speak("That's right! You would feel ${emotionDisplayName[scCorrectEmotion]}!")
        } else {
            tapped.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            label.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            label.text = "✗ " + label.text.toString()

            // Highlight the correct one in green
            optViews.forEachIndexed { i, iv ->
                if (iv.tag == scCorrectEmotion) {
                    iv.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
                    lblViews[i].setTextColor(ContextCompat.getColor(this, R.color.green))
                    lblViews[i].text = "✓ " + lblViews[i].text.toString()
                }
            }
            val correctName = emotionDisplayName[scCorrectEmotion] ?: scCorrectEmotion
            scFeedback.text = "You would feel $correctName! Good try"
            scFeedback.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            speak("You would feel $correctName! Good try!")
        }

        scFeedback.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            scBtnDone.visibility = View.VISIBLE
        }, 700)
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private fun returnToGame() {
        setResult(RESULT_OK)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun applyOptionImage(iv: ImageView, drawableName: String) {
        val res = resolve(drawableName)
        if (res != 0) iv.setImageResource(res)
        else {
            // Fallback: show a panda placeholder so the slot isn't blank
            iv.setImageResource(R.drawable.panda_confused)
        }
        iv.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
    }

    private fun resolve(name: String): Int =
        if (name.isEmpty()) 0
        else resources.getIdentifier(name, "drawable", packageName)

    private fun speak(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mg")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language    = Locale.US
            tts.setPitch(1.7f)
            tts.setSpeechRate(0.85f)
            ttsReady = true
        }
    }
}