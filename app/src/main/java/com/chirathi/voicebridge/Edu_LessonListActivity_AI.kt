package com.chirathi.voicebridge

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.repository.AIRepository
import kotlinx.coroutines.launch

/**
 * Enhanced Lesson List Activity with AI Recommendations
 * Shows therapy tasks recommended by AI based on child's profile
 */
class Edu_LessonListActivity_AI : AppCompatActivity() {
    
    private val aiRepository = AIRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_lesson_list)
        
        // Get data from intent
        val ageGroup = intent.getStringExtra("AGE_GROUP") ?: "6"
        val disorderType = intent.getStringExtra("DISORDER_TYPE") ?: "Stuttering"
        val isAiRecommended = intent.getBooleanExtra("IS_AI_RECOMMENDED", false)
        
        // UI Elements
        val backButton: ImageView = findViewById(R.id.back)
        val titleText: TextView = findViewById(R.id.tvListTitle)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = findViewById(R.id.rvLessons)

        // Setup
        backButton.setOnClickListener { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Update title based on source
        if (isAiRecommended) {
            titleText.text = "🤖 AI Recommended Therapy Tasks"
        } else {
            titleText.text = "Select a Lesson"
        }
        
        // Load AI recommendations
        loadAIRecommendations(
            age = ageGroup.toIntOrNull() ?: 6,
            disorder = disorderType,
            progressBar = progressBar,
            recyclerView = recyclerView
        )
    }
    
    /**
     * Load therapy task recommendations from AI API
     */
    private fun loadAIRecommendations(
        age: Int,
        disorder: String,
        progressBar: ProgressBar,
        recyclerView: RecyclerView
    ) {
        // Show loading
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                // Call AI API to get recommendations
                val tasks = aiRepository.getRecommendationsByAge(
                    age = age,
                    disorder = disorder
                )
                
                // Hide loading
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                if (tasks.isNotEmpty()) {
                    // SUCCESS: Display AI recommendations
                    Toast.makeText(
                        this@Edu_LessonListActivity_AI,
                        "✓ Found ${tasks.size} AI-recommended tasks",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Create adapter with AI tasks
                    val adapter = AITaskAdapter(tasks) { selectedTask ->
                        // When user clicks a task, show details
                        showTaskDetails(selectedTask)
                    }
                    recyclerView.adapter = adapter
                    
                    // Print to Logcat for debugging
                    tasks.forEach { task ->
                        println("🤖 AI Task: ${task.title}")
                        println("   Description: ${task.description}")
                        println("   Duration: ${task.duration}")
                        println("   Materials: ${task.materials}")
                        println("   Tips: ${task.tips}")
                        println("---")
                    }
                    
                } else {
                    // No recommendations found
                    Toast.makeText(
                        this@Edu_LessonListActivity_AI,
                        "No recommendations found for age $age, disorder: $disorder",
                        Toast.LENGTH_LONG
                    ).show()
                }
                
            } catch (e: Exception) {
                // Error handling
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                Toast.makeText(
                    this@Edu_LessonListActivity_AI,
                    "Error loading AI recommendations: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Show detailed information about selected task
     */
    private fun showTaskDetails(task: com.chirathi.voicebridge.api.models.TherapyTask) {
        // Create a dialog or new activity to show full task details
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📚 ${task.title}")
            .setMessage("""
                ${task.description}
                
                👥 Age Group: ${task.ageGroup}
                🎯 Disorder: ${task.disorder}
                ⏱️ Duration: ${task.duration}
                
                📝 Activity:
                ${task.activity}
                
                🛠️ Materials Needed:
                ${task.materials}
                
                💡 Tips:
                ${task.tips}
            """.trimIndent())
            .setPositiveButton("Start Activity") { dialog, _ ->
                // Launch the actual therapy activity
                Toast.makeText(this, "Starting: ${task.title}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Close", null)
            .create()
        
        dialog.show()
    }
}

/**
 * RecyclerView Adapter for AI Therapy Tasks
 */
class AITaskAdapter(
    private val tasks: List<com.chirathi.voicebridge.api.models.TherapyTask>,
    private val onTaskClick: (com.chirathi.voicebridge.api.models.TherapyTask) -> Unit
) : RecyclerView.Adapter<AITaskAdapter.TaskViewHolder>() {
    
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // You can reuse your existing item_lesson.xml layout
        // or create a new layout for AI tasks
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): TaskViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return TaskViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        
        // Bind data to views
        // You'll need to cast itemView's children to appropriate types
        // Example:
        // holder.itemView.findViewById<TextView>(R.id.tvTitle).text = task.title
        // holder.itemView.findViewById<TextView>(R.id.tvDescription).text = task.description
        
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }
    
    override fun getItemCount() = tasks.size
}
