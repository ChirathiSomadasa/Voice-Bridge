package com.chirathi.voicebridge

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.RelativeLayout
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ViewObject1Activity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_view_object1)


        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        // Take Photo button functionality
//        val takePhotoBtn = findViewById<Button>(R.id.take_photo)
//        takePhotoBtn.setOnClickListener {
//            val intent = Intent(this, QuickWordsActivity::class.java)
//            startActivity(intent)
//        }

        // object detection button functionality
        val objectDetectionBtn = findViewById<Button>(R.id.detect_object)
        objectDetectionBtn.setOnClickListener {
            val intent = Intent(this, ViewObjectsActivity::class.java)
            startActivity(intent)
        }

        val instructionBtn = findViewById<RelativeLayout>(R.id.btn_instruction_o)
        instructionBtn.setOnClickListener {
            showInstructionVideo()
        }
    }

    private fun showInstructionVideo() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_instruction_video)

        val videoView = dialog.findViewById<VideoView>(R.id.videoView)
        val closeBtn = dialog.findViewById<ImageView>(R.id.btnClose)

        val videoPath = "android.resource://" + packageName + "/" + R.raw.object_detection_video
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