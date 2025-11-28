package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.RelativeLayout
import android.widget.VideoView

class SpeechLevel2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech_level2)

        val speakBtn = findViewById<Button>(R.id.btn_level2)
        speakBtn.setOnClickListener {
            val intent = Intent(this, SpeechLevel2TaskActivity::class.java)
            startActivity(intent)
        }

        val instructionBtn = findViewById<RelativeLayout>(R.id.btn_instruction)
        instructionBtn.setOnClickListener {
            showInstructionVideo()
        }

        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            finish()
        }
    }

    private fun showInstructionVideo() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_instruction_video)

        val videoView = dialog.findViewById<VideoView>(R.id.videoView)
        val closeBtn = dialog.findViewById<ImageView>(R.id.btnClose)

        val videoPath = "android.resource://" + packageName + "/" + R.raw.speech_level2_instruction
        val uri = Uri.parse(videoPath)
        videoView.setVideoURI(uri)

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        videoView.start()

        closeBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}