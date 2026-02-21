package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView

/**
 * FeedbackPopupDialog — replaces FeedbackDialogFragment.
 *
 * Usage:
 *   FeedbackPopupDialog(context, isGood, correct, total, score) {
 *       // called when popup auto-closes (8 s) or user taps it
 *       navigateToScoreboard()
 *   }.show()
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

    private val handler         = Handler(Looper.getMainLooper())
    private val feedbackDuration = 8_000L
    private var progressRunnable: Runnable? = null
    private var isDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_feedback_popup_dialog)

        // Transparent background so the rounded card shows through
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        videoView   = findViewById(R.id.feedbackVideo)
        titleText   = findViewById(R.id.title)
        progressBar = findViewById(R.id.progressBar)

        titleText.text = if (isGoodFeedback) "Wow! You got them!" else "You're learning – let's play more!"

        // Tap anywhere to skip
        window?.decorView?.setOnClickListener { completeFeedback() }

        setupVideo()
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
        isDone = true
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        try { videoView.stopPlayback() } catch (_: Exception) {}
        dismiss()
        onComplete()
    }

    override fun onStop() {
        super.onStop()
        progressRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
        try { videoView.pause() } catch (_: Exception) {}
    }
}