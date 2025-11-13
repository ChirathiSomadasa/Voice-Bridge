package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class PandaIntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panda_intro)


        val rootLayout = findViewById<View>(android.R.id.content)
        rootLayout.setOnClickListener {
            val intent = Intent(this, MoodMatchSevenUpActivity::class.java)
            startActivity(intent)
        }
    }
}