package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import android.net.Uri

/**
 * FeedbackPopupDialog
 *
 * FIX: "Tap anywhere to continue" was silently broken because:
 *
 *   1. window?.decorView?.setOnClickListener  — the decor view is at the
 *      BOTTOM of the view hierarchy.  VideoView (and any other child view)
 *      consumes the MotionEvent before it can bubble down to the decor,
 *      so the click listener was never called.
 *
 *   2. VideoView intercepts all touch events by default and never passes
 *      them to its parent.
 *
 * Correct fix: override dispatchTouchEvent() on the Dialog itself.
 * dispatchTouchEvent() is called BEFORE any child view sees the event,
 * so it reliably catches every finger-down no matter where on the screen
 * the user taps — VideoView included.
 */
class FeedbackPopupDialog(
    context: Context,
    private val isGoodFeedback: Boolean,
    private val correctAnswers: Int,
    private val totalRounds: Int,
    private val score: Int,
    private val onComplete: () -> Unit
) : Dialog(context) {

    companion object { private const val TAG = "FeedbackPopup" }

    private lateinit var videoView:   VideoView
    private lateinit var titleText:   TextView
    private lateinit var progressBar: ProgressBar

    private val handler          = Handler(Looper.getMainLooper())
    private val feedbackDuration = 8_000L
    private var progressRunnable: Runnable? = null
    private var isDone           = false

    private var isTtsShutdown = false

    private var tts: android.speech.tts.TextToSpeech? = null

    // ── Intercept every touch at the Dialog level ─────────────────────────────
    //
    // dispatchTouchEvent() is the very first method called when a touch event
    // enters a Window.  It runs before any child view's onTouchEvent, so
    // VideoView cannot swallow it here.
    //
    // We only act on ACTION_DOWN (the initial finger press) so a single tap
    // triggers exactly once — no double-fire from ACTION_UP.
    //
    // super.dispatchTouchEvent() is still called so the dialog's own
    // internal gesture handling (ripples, etc.) keeps working normally.

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            completeFeedback()
        }
        return super.dispatchTouchEvent(ev)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_feedback_popup_dialog)

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        videoView   = findViewById(R.id.feedbackVideo)
        titleText   = findViewById(R.id.title)
        progressBar = findViewById(R.id.progressBar)

        titleText.text = if (isGoodFeedback)
            "Wow! You got them!"
        else
            "You are learning. Keep going!"

        // NOTE: the old window?.decorView?.setOnClickListener is removed.
        // dispatchTouchEvent above handles all taps more reliably.

        setupVideo()
    }

    private fun initTts() {
        if (isGoodFeedback) return  // ← only speak for bad feedback
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language    = java.util.Locale.US
                tts?.setPitch(1.4f)
                tts?.setSpeechRate(0.80f)
                handler.postDelayed({
                    tts?.speak(
                        "You are learning. Keep going!",
                        android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                        null,
                        "encourage"
                    )
                }, 400)
            }
        }
    }

    private fun shutdownTts() {
        if (isTtsShutdown) return
        isTtsShutdown = true
        tts?.stop()
        tts?.shutdown()
    }

    private fun setupVideo() {
        try {
            val videoRes = if (isGoodFeedback) R.raw.feedback_good else R.raw.feedback_bad
            val uri = Uri.parse("android.resource://${context.packageName}/$videoRes")
            videoView.setVideoURI(uri)
            videoView.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "Video error what=$what extra=$extra")
                startTimer()
                true
            }
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                videoView.start()
                startTimer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video setup error: ${e.message}")
            startTimer()
        }
        initTts()
    }

    private fun startTimer() {
        startProgressAnimation()
        handler.postDelayed({ completeFeedback() }, feedbackDuration)
    }

    private fun startProgressAnimation() {
        progressBar.progress = 0
        var progress = 0
        progressRunnable = object : Runnable {
            override fun run() {
                progress++
                progressBar.progress = progress
                if (progress < 100) handler.postDelayed(this, 80)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun completeFeedback() {
        if (isDone) return
        isDone = true                                      // guard — runs only once
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        shutdownTts()
        try { videoView.stopPlayback() } catch (_: Exception) {}
        dismiss()
        onComplete()
    }

    override fun onStop() {
        super.onStop()
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        shutdownTts()
        try { videoView.pause() } catch (_: Exception) {}
    }
}