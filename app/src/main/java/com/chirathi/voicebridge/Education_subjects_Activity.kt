//package com.chirathi.voicebridge
//
//import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//
//class Education_subjects_Activity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_education_subjects)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//    }
//}

package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class Education_subjects_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_subjects) // existing layout with root id @+id/main

        val ageGroup = intent.getStringExtra("AGE_GROUP") ?: "4-6"
        val root = findViewById<ConstraintLayout>(R.id.main)

        // Title
        val title = TextView(this).apply {
            textSize = 22f
            text = when (ageGroup) {
                "4-6" -> "Subjects — Age 4–6"
                "6-10" -> "Subjects — Age 6–10"
                "10-12" -> "Subjects — Age 10–12"
                else -> "Subjects"
            }
            setPadding(24, 24, 24, 12)
        }

        // Scrollable list container
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 12, 24, 24)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Example subject lists per age group
        val subjects = when (ageGroup) {
            "4-6" -> listOf("Phonics", "Speech Games", "Basic Math")
            "6-10" -> listOf("Reading", "Writing", "Math", "Science")
            "10-12" -> listOf("Language", "Math Advanced", "Comprehension")
            else -> listOf("General")
        }

        // Create buttons for each subject
        subjects.forEach { subj ->
            val btn = Button(this).apply {
                text = subj
                textSize = 18f
                isAllCaps = false
                setOnClickListener {
                    // Start level or subject detail activity and pass age + subject
                    startActivity(
                        Intent(this@Education_subjects_Activity, Education_age6_10_Activity::class.java) // replace with proper target
                            .putExtra("AGE_GROUP", ageGroup)
                            .putExtra("SUBJECT", subj)
                    )
                }
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 12, 0, 0)
                layoutParams = lp
            }
            list.addView(btn)
        }

        scroll.addView(list)

        // Add views to root (clear any placeholder content if needed)
        root.removeAllViews()
        root.addView(title)
        root.addView(scroll)

        // optional: restore layout params or constraints if you want ConstraintLayout positioning
    }
}
