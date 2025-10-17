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

        val teacherBtn = findViewById<LinearLayout>(R.id.teacher_layout)
        teacherBtn.setOnClickListener {
            val intent = Intent(this, TeacherRegistrationActivity::class.java)
            startActivity(intent)
        }

        val child = findViewById<Button>(R.id.child_btn)
        child.setOnClickListener {
            val intent = Intent(this, ChildRegistrationActivity::class.java)
            startActivity(intent)
        }
        val teacher = findViewById<Button>(R.id.teacher_btn)
        teacher.setOnClickListener {
            val intent = Intent(this, TeacherRegistrationActivity::class.java)//
            startActivity(intent)
        }
    }
}