package com.chirathi.voicebridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout

class RoutineSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routine_selection)

        // Morning Routine button click - use LinearLayout instead of View
        val morningRoutineLayout = findViewById<LinearLayout>(R.id.morningRoutineLayout)
        morningRoutineLayout.setOnClickListener {
            val intent = Intent(this, ActivitySequenceUnderActivity::class.java)
            startActivity(intent)
        }
    }
}