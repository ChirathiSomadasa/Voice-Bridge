package com.chirathi.voicebridge

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

/**
 * ASGuide_BelowActivity — tap guidance popup (age 6-7).
 *
 *  • Appears as a centred floating dialog (not full-screen).
 *  • Shown only ONCE per login session via [hasBeenShownThisSession].
 *    RoutineSelectionActivity checks this flag and skips directly to
 *    ActivitySequenceUnderActivity on subsequent routine taps.
 *  • "Got it!" forwards SELECTED_ROUTINE_ID and USER_AGE to the game.
 */
class ASGuide_BelowActivity : AppCompatActivity() {

    companion object {
        /**
         * Set to true the first time the guide is shown.
         * Resets automatically when the process dies (new login session).
         */
        var hasBeenShownThisSession: Boolean = false

    }

    private var childId = "default"
    private var tts: android.speech.tts.TextToSpeech? = null
    private lateinit var videoView: VideoView
    private lateinit var btnOk: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        styleAsDialog()
        setContentView(buildDialogLayout())
        childId = intent.getStringExtra("CHILD_ID") ?: "default"
        setupVideo(R.raw.tap)
        initTts()

        btnOk.setOnClickListener {
            hasBeenShownThisSession = true
            navigateToGame()
        }
    }

    // ── Dialog window styling ──────────────────────────────────────────────

    private fun styleAsDialog() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.also { it.dimAmount = 0.65f }
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.CENTER)
        setFinishOnTouchOutside(false) // child must tap "Got it!"
    }
    private fun initTts() {
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language    = java.util.Locale.US
                tts?.setPitch(1.4f)
                tts?.setSpeechRate(0.80f)
                // Short delay so dialog is fully visible before speaking
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    tts?.speak(
                        "Tap the steps in order!",
                        android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                        null,
                        "guide"
                    )
                }, 600)
            }
        }
    }

    // ── Layout (built in code — no extra XML needed) ───────────────────────

    private fun buildDialogLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundResource(R.drawable.rounded_card_background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        videoView = VideoView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 480
            ).also { it.bottomMargin = 32 }
            id = View.generateViewId()
        }

        btnOk = Button(this).apply {
            text = "Got it! Let's Play!!"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.rounded_button_background)
            setTextColor(Color.WHITE)
        }

        root.addView(videoView)
        root.addView(btnOk)
        return root
    }

    // ── Video ──────────────────────────────────────────────────────────────

    private fun setupVideo(rawRes: Int) {
        try {
            val uri = Uri.parse("android.resource://$packageName/$rawRes")
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                videoView.start()
            }
            videoView.setOnErrorListener { _, _, _ ->
                videoView.visibility = View.GONE
                true
            }
        } catch (e: Exception) {
            videoView.visibility = View.GONE
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun navigateToGame() {
        val routineId = intent.getIntExtra("SELECTED_ROUTINE_ID", 0)
        val age       = intent.getIntExtra("USER_AGE", 6)
        startActivity(Intent(this, ActivitySequenceUnderActivity::class.java).apply {
            putExtra("SELECTED_ROUTINE_ID", routineId)
            putExtra("CHILD_ID", childId)
            putExtra("USER_AGE", age)
        })
        finish()
    }

    override fun onPause()   { super.onPause();   videoView.pause(); tts?.stop() }
    override fun onResume()  { super.onResume();  videoView.start() }
    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
        tts?.stop()       // ← add this
        tts?.shutdown()
    }
}