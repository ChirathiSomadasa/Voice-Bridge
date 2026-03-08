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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

/**
 * ASGuidanceAboveActivity — drag-and-drop guidance popup for ages 8-10.
 *
 *  • Appears as a centred floating dialog (not full-screen).
 *  • Shown only ONCE per login session via [hasBeenShownThisSession].
 *  • Video is centred both horizontally and vertically inside the dialog.
 */
class ASGuidanceAboveActivity : AppCompatActivity() {

    companion object {
        var hasBeenShownThisSession: Boolean = false
    }

    private lateinit var videoView: VideoView
    private lateinit var btnOk: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        styleAsDialog()
        setContentView(buildDialogLayout())
        setupVideo(R.raw.drag_and_drop)

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
        setFinishOnTouchOutside(false)
    }

    // ── Layout ─────────────────────────────────────────────────────────────

    private fun buildDialogLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER_HORIZONTAL
            setPadding(48, 48, 48, 48)
            setBackgroundResource(R.drawable.rounded_card_background)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Wrap VideoView in a FrameLayout so Gravity.CENTER applies cleanly
        videoView = VideoView(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(400, 400, Gravity.CENTER)
        }

        val videoContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 440
            ).also { it.bottomMargin = 32 }
            addView(videoView)
        }

        btnOk = Button(this).apply {
            text = "Got it! Let's Play!"
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundResource(R.drawable.rounded_button_background)
            setTextColor(Color.WHITE)
        }

        root.addView(videoContainer)
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
        val age       = intent.getIntExtra("USER_AGE", 8)
        startActivity(Intent(this, ActivitySequenceOverActivity::class.java).apply {
            putExtra("SELECTED_ROUTINE_ID", routineId)
            putExtra("USER_AGE", age)
        })
        finish()
    }

    override fun onPause()   { super.onPause();   videoView.pause() }
    override fun onResume()  { super.onResume();  videoView.start() }
    override fun onDestroy() { super.onDestroy(); videoView.stopPlayback() }
}