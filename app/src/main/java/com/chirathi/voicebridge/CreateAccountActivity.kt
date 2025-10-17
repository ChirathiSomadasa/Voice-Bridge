package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

class CreateAccountActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val childBtn = findViewById<LinearLayout>(R.id.child_layout)
        childBtn.setOnClickListener {
            val intent = Intent(this, ChildRegistrationActivity::class.java)
            startActivity(intent)
        }
    }
}