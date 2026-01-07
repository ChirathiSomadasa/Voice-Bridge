package com.chirathi.voicebridge

import android.R.attr.textStyle
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlin.rem

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

class Education_subjects_Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_education_subjects) // existing layout with root id @+id/main

        val ageGroup = intent.getStringExtra("AGE_GROUP") ?: "6"
        val title = findViewById<TextView>(R.id.tvTitle)
        val subjectsContainer = findViewById<LinearLayout>(R.id.subjectsContainer)

        val backButton: ImageView = findViewById(R.id.back)

        title.text = when (ageGroup) {
            "6" -> "Select Subject (Age 6)"
            "7" -> "Select Subject (Age 7)"
            "8" -> "Select Subject (Age 8)"
            "9" -> "Select Subject (Age 9)"
            "10" -> "Select Subject (Age 10)"
            else -> "Select Subject"
        }


        // Example subject lists per age group
        val subjects = when (ageGroup) {
            "6" -> listOf("General", "Speech", "Reading", "English", "Basic Math")
            "7" -> listOf("Math", "Science", "Reading", "English")
            "8" -> listOf("Math", "Science", "Reading", "English")
            "9" -> listOf("Math", "Science", "Reading", "English")
            "10" -> listOf("Math Advanced", "Science", "English")
            else -> listOf("General")
        }

//        subjectsContainer.removeAllViews()
//        subjects.forEach { subj ->
//            val btn = Button(this).apply {
//                text = subj
//                textSize = 20f
//                isAllCaps = false
//                setOnClickListener {
//                    // Handle subject click, e.g., open levels
//                    val intent = Intent(this@Education_subjects_Activity, LessonListActivity::class.java)
//                    intent.putExtra("AGE_GROUP", ageGroup)
//                    intent.putExtra("SUBJECT", subj)
//                    startActivity(intent)
//                }
//            }
//            subjectsContainer.addView(btn)
//        }


        subjects.forEachIndexed { index, subj ->
            val btn = Button(this, null, 0, R.style.SolidGreenButton).apply {
                text = subj
                textSize = 20f
                isAllCaps = false
                setTextColor(resources.getColor(R.color.white, theme))
                setTypeface(typeface, android.graphics.Typeface.BOLD)

                // Center text horizontally & vertically
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                // Optional: ensure equal horizontal padding (so text is visually centered)
                setPadding(16.dp, 0, 16.dp, 0)

                // Alternate colors if you want, or keep all green
                val bgColor = when (index % 3) {
                    0 -> R.color.green
                    1 -> R.color.pink
                    else -> R.color.dark_orange
                }
                backgroundTintList = resources.getColorStateList(bgColor, theme)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    55.dp
                ).apply {
                    topMargin = if (index == 0) 40.dp else 20.dp
                }

                // ADD THIS CLICK LISTENER HERE:
                setOnClickListener {
                    val intent = Intent(this@Education_subjects_Activity, Edu_LessonListActivity::class.java)
                    intent.putExtra("AGE_GROUP", ageGroup)
                    intent.putExtra("SUBJECT", subj)
                    startActivity(intent)
                }
            }
            subjectsContainer.addView(btn)
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}
