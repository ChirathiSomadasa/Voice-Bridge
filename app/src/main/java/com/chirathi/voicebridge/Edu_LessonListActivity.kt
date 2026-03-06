//package com.chirathi.voicebridge
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import android.view.View
//import android.widget.ProgressBar
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.chirathi.voicebridge.data.LessonAdapter
//import com.chirathi.voicebridge.data.LessonRepository
//
//class Edu_LessonListActivity : AppCompatActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_edu_lesson_list) // You need to create this layout
//
//        val age = intent.getStringExtra("AGE_GROUP") ?: "6"
//        val subject = intent.getStringExtra("SUBJECT") ?: "General"
//
//        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
//        val recyclerView = findViewById<RecyclerView>(R.id.rvLessons)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        // Before calling Repository
//        progressBar.visibility = View.VISIBLE
//        recyclerView.visibility = View.GONE
//
//        // Fetch Data
//        LessonRepository.getLessons(
//            age = age,
//            subject = subject,
//            onResult = { lessonList ->
//                // Hide loading, show content
//                progressBar.visibility = View.GONE
//                recyclerView.visibility = View.VISIBLE
//
//                // Check if list is empty to avoid blank screen confusion
//                if (lessonList.isEmpty()) {
//                    Toast.makeText(this, "No lessons found for this subject", Toast.LENGTH_SHORT).show()
//                }
//
//                // Setup Adapter
//                val adapter = LessonAdapter(lessonList) { selectedLesson ->
//                    // Handle click (e.g., go to a Quiz Activity)
//                    val intent = Intent(this, Edu_LessonDetailActivity::class.java)
//                    intent.putExtra("lesson", selectedLesson) // Parcelable
//                    startActivity(intent)
//
//                    Toast.makeText(
//                        this,
//                        "Clicked: ${selectedLesson.lessonTitle}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                recyclerView.adapter = adapter
//            },
//            onError = { exception ->
//                progressBar.visibility = View.GONE
//                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
//            }
//        )
//    }
//}

package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.data.LessonAdapter
import com.chirathi.voicebridge.data.LessonRepository
import com.chirathi.voicebridge.data.LessonModel

class Edu_LessonListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_lesson_list)

        val age = intent.getStringExtra("AGE_GROUP") ?: "6"
        val subject = intent.getStringExtra("SUBJECT") ?: "General"

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val recyclerView = findViewById<RecyclerView>(R.id.rvLessons)
        val backButton = findViewById<View>(R.id.back)

        backButton.setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        LessonRepository.getLessons(
            age = age,
            subject = subject,
            onResult = { lessonList ->
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                if (lessonList.isEmpty()) {
                    Toast.makeText(this, "No lessons found for this subject", Toast.LENGTH_SHORT).show()
                }

                val adapter = LessonAdapter(lessonList) { selectedLesson ->
                    // Check if sub-lessons exist, navigate accordingly
                    val intent = if (selectedLesson.subLessons.isNotEmpty()) {
                        Intent(this, Edu_SubLessonDetailActivity::class.java)
                    } else {
                        Intent(this, Edu_LessonDetailActivity::class.java)
                    }

                    // Pass the complete lesson object to the next activity
                    intent.putExtra("lesson", selectedLesson)

                    startActivity(intent)
                }
                recyclerView.adapter = adapter
            },
            onError = { exception ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }
}