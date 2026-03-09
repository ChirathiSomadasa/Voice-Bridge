package com.chirathi.voicebridge

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * RMIntroActivity — v12.0 (Popup style, buffering spinner hidden, smaller size)
 *
 * CHANGES FROM v11.0
 * ──────────────────
 *  [1] BUFFERING SPINNER HIDDEN
 *      The spinning circle while the video loads is NOT part of the layout —
 *      it is drawn by the Android MediaPlayer system layer directly on top of
 *      the VideoView's SurfaceView. Hiding a ProgressBar in your own XML does
 *      nothing to it.
 *
 *      Fix: a solid white cover View is inserted programmatically into the
 *      same FrameLayout parent as the VideoView, above it in Z-order, before
 *      playback begins. This physically paints over the spinner. The instant
 *      the video is prepared and starts playing, the cover fades out (200 ms)
 *      and is removed from the hierarchy. Tap-to-skip also removes it.
 *
 *  [2] POPUP IS SMALLER
 *      Window reduced from 90 × 70 % to 82 × 56 % of the screen.
 */
class RMIntroActivity : AppCompatActivity() {

    private lateinit var videoView:        VideoView
    private lateinit var currentSongTitle: String

    /** White cover placed above the VideoView to hide the system buffering spinner. */
    private var bufferCover: View? = null

    private val updateHandler       = Handler(Looper.getMainLooper())
    private val TAG                 = "RMIntroActivity"
    private var isActivityDestroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.activity_rmintro)

        // [FIX 2] Smaller popup — 82 % wide × 56 % tall, always centred.
        window.setLayout(
            (resources.displayMetrics.widthPixels  * 0.82).toInt(),
            (resources.displayMetrics.heightPixels * 0.56).toInt()
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.also {
            it.dimAmount = 0.55f
            it.gravity   = Gravity.CENTER
        }

        isActivityDestroyed = false
        currentSongTitle    = intent.getStringExtra("SONG_TITLE") ?: "Row Row Row Your Boat"

        videoView = findViewById(R.id.videoView)

        // Hide any ProgressBar that lives in the XML layout.
        try { findViewById<ProgressBar>(R.id.progressBar)?.visibility = View.GONE }
        catch (_: Exception) {}

        // Show tap-to-skip hint if the view id exists in the layout.
        try {
            findViewById<TextView>(R.id.skipLabel)?.apply {
                text       = "TAP ANYWHERE TO SKIP  ▶"
                visibility = View.VISIBLE
            }
        } catch (_: Exception) {}

        // [FIX 1] Cover the VideoView BEFORE it starts preparing so the
        // MediaPlayer buffering spinner is never visible to the child.
        attachBufferCover()

        setupVideoPlayer()

        // Tap anywhere → skip
        val root = try { findViewById<View>(R.id.main) } catch (_: Exception) { null }
        root?.setOnClickListener     { skip() }
        videoView.setOnClickListener { skip() }
    }

    // ── [FIX 1] Buffer cover ──────────────────────────────────────────────

    /**
     * Inserts a solid white View directly above the VideoView in the same
     * FrameLayout parent. Because it is added last it sits on top in Z-order,
     * covering whatever MediaPlayer renders on the SurfaceView beneath it.
     */
    private fun attachBufferCover() {
        val parent = videoView.parent as? FrameLayout ?: return
        val cover = View(this).apply {
            setBackgroundColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        parent.addView(cover)
        bufferCover = cover
    }

    /**
     * Fades the cover out over 200 ms then removes it from the view hierarchy.
     * Called as soon as the video actually starts playing.
     */
    private fun removeBufferCover() {
        val cover = bufferCover ?: return
        bufferCover = null
        cover.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { (cover.parent as? FrameLayout)?.removeView(cover) }
            .start()
    }

    // ── Video player ──────────────────────────────────────────────────────

    private fun setupVideoPlayer() {
        try {
            videoView.setMediaController(null)
            val videoUri = Uri.parse("android.resource://$packageName/raw/bears_fun_singing_game")

            videoView.setOnPreparedListener {
                if (isActivityDestroyed) return@setOnPreparedListener
                videoView.start()
                removeBufferCover()   // reveal video; spinner is already gone
            }

            videoView.setOnCompletionListener {
                if (!isActivityDestroyed) navigateToRhythmSummary()
            }

            videoView.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "Video error $what/$extra — auto-skipping")
                removeBufferCover()
                if (!isActivityDestroyed) {
                    updateHandler.postDelayed({
                        if (!isActivityDestroyed) navigateToRhythmSummary()
                    }, 600)
                }
                true
            }

            videoView.setVideoURI(videoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Video setup failed: ${e.message}")
            removeBufferCover()
            updateHandler.postDelayed({
                if (!isActivityDestroyed) navigateToRhythmSummary()
            }, 600)
        }
    }

    // ── Navigation & lifecycle ────────────────────────────────────────────

    private fun skip() {
        if (isActivityDestroyed) return
        removeBufferCover()
        videoView.stopPlayback()
        updateHandler.removeCallbacksAndMessages(null)
        navigateToRhythmSummary()
    }

    private fun navigateToRhythmSummary() {
        if (isActivityDestroyed) return
        updateHandler.removeCallbacksAndMessages(null)
        try {
            startActivity(Intent(this, RhythmSummaryActivity::class.java).apply {
                putExtra("SONG_TITLE", currentSongTitle)
            })
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isActivityDestroyed) skip()
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityDestroyed = true
        bufferCover = null
        updateHandler.removeCallbacksAndMessages(null)
        try {
            videoView.stopPlayback()
            videoView.setOnPreparedListener(null)
            videoView.setOnCompletionListener(null)
            videoView.setOnErrorListener(null)
        } catch (_: Exception) {}
    }
}