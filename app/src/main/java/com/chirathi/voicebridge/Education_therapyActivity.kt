package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


class Education_therapyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_therapy)

        val btnLevel1: Button = findViewById(R.id.btn_level1)
        val btnLevel2: Button = findViewById(R.id.btn_level2)
        val btnLevel3: Button = findViewById(R.id.btn_level3)
        val btnLevel4: Button = findViewById(R.id.btn_level4)
        val backButton: ImageView = findViewById(R.id.back)


        btnLevel1.setOnClickListener {
            startActivity(Intent(this, Education_age4_6_Activity::class.java))
        }

        btnLevel2.setOnClickListener {
            startActivity(Intent(this, Education_age6_10_Activity::class.java))
        }

        btnLevel3.setOnClickListener {
            startActivity(Intent(this, Education_age6_10_Activity::class.java))
        }

        btnLevel4.setOnClickListener {
            startActivity(Intent(this, Education_age6_10_Activity::class.java))
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}
