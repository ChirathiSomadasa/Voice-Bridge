

package com.chirathi.voicebridge

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "Edu_LessonList"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_lesson_list)

        val age = intent.getStringExtra("AGE_GROUP") ?: "6"
        val subject = intent.getStringExtra("SUBJECT") ?: "General"
        val disorderType  = intent.getStringExtra("DISORDER_TYPE")
        val disorderSeverity = intent.getStringExtra("DISORDER_SEVERITY")

        Log.d(TAG, "Loading lessons: age='$age', subject='$subject', disorder='$disorderType'")

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
//            disorderType = disorderType,
            onResult = { lessonList ->
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                if (lessonList.isEmpty()) {
                    Toast.makeText(this, "No lessons found for this subject", Toast.LENGTH_SHORT).show()
                    return@getLessons
                }

                val adapter = LessonAdapter(lessonList) { selectedLesson ->
//                    val intent = Intent(this, Edu_LessonDetailActivity::class.java).apply {
//                        putExtra("lesson", selectedLesson)
//                        putExtra("AGE_GROUP", age)
//                        putExtra("DISORDER_TYPE", disorderType)
//                        putExtra("DISORDER_SEVERITY", disorderSeverity)
//                        putParcelableArrayListExtra("LESSON_LIST", ArrayList(lessonList))
//                        putExtra("LESSON_INDEX", index)
//                    }
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