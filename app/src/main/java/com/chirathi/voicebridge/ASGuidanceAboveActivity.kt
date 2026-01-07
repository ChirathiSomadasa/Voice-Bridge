package com.chirathi.voicebridge

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class ASGuidanceAboveActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var btnOk: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asguidance_above)

        videoView = findViewById(R.id.videoView)
        btnOk = findViewById(R.id.btn_ok)

        // Set up video
        setupVideo()

        // Set up button click listener
        btnOk.setOnClickListener {
            val intent = Intent(this, ActivitySequenceOverActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupVideo() {
        try {
            // Get video URI from raw folder or assets
            val videoPath = "android.resource://" + packageName + "/" + R.raw.drag_and_drop

            videoView.setVideoURI(Uri.parse(videoPath))
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                videoView.start()
            }

            videoView.setOnErrorListener { mp, what, extra ->
                // If video fails, show a fallback message
                videoView.visibility = View.GONE
                true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Hide video view if there's an error
            videoView.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        videoView.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.stopPlayback()
    }
}