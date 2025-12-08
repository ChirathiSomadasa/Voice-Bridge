package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Education_age4_6_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_age46)

        findViewById<Button>(R.id.btn_subject1).setOnClickListener {
            openLevels("Math")
        }
        findViewById<Button>(R.id.btn_subject2).setOnClickListener {
            openLevels("Science")
        }
        findViewById<Button>(R.id.btn_subject3).setOnClickListener {
            openLevels("Art")
        }
        // Add more subjects as needed
    }

    private fun openLevels(subject: String) {
//        val intent = Intent(this, EducationLevelsActivity::class.java)
//        intent.putExtra("subject", subject)
//        startActivity(intent)
    }
}