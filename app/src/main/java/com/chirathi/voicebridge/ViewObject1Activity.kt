package com.chirathi.voicebridge

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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
        val takePhotoBtn = findViewById<Button>(R.id.take_photo)
        takePhotoBtn.setOnClickListener {
            val intent = Intent(this, QuickWordsActivity::class.java)
            startActivity(intent)
        }

        // object detection button functionality
        val objectDetectionBtn = findViewById<Button>(R.id.detect_object)
        objectDetectionBtn.setOnClickListener {
            val intent = Intent(this, ViewObjectsActivity::class.java)
            startActivity(intent)
        }
    }
}