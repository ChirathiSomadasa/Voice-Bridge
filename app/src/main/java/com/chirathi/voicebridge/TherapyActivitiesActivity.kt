package com.chirathi.voicebridge

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chirathi.voicebridge.api.models.RecommendTherapyRequest
import com.chirathi.voicebridge.api.models.RecommendTherapyItem
import com.chirathi.voicebridge.repository.AIRepository
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import com.chirathi.voicebridge.api.models.*

class TherapyActivitiesActivity : AppCompatActivity() {

    private val repo = AIRepository()
    private val geminiHelper = Edu_GeminiHelper()   // uses BuildConfig key
    private lateinit var progress: ProgressBar
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TherapyActivityAdapter
    private lateinit var backButton: ImageView

    // cache intent values so they’re available inside onItemClicked
    private var age: Int = 8  // default to middle of 6-10 range
    private var disorder: String = ""
    private var severity: String = ""
    private var communication: String = ""
    private var attention: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edu_sub_lesson_detail)

        // read extras once
        age = intent.getIntExtra("AGE", 6)
        disorder = intent.getStringExtra("DISORDER").orEmpty()
        severity = intent.getStringExtra("SEVERITY").orEmpty()
        communication = intent.getStringExtra("COMM").orEmpty()
        attention = intent.getStringExtra("ATTN").orEmpty()

        progress = findViewById(R.id.progressTherapyActivities)
        recycler = findViewById(R.id.rvTherapyActivities)

        adapter = TherapyActivityAdapter(
            onDetailsClick = { item -> onItemClicked(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadActivities()

        // Setup back button
        backButton.setOnClickListener { finish() }
    }

    private fun loadActivities() {
        progress.visibility = View.VISIBLE
        recycler.visibility = View.GONE

        lifecycleScope.launch {
            val items = repo.getTherapyRecommendations(
                RecommendTherapyRequest(
                    age = age,
                    disorder = disorder,
                    severity = severity,
                    communication = communication,
                    attention = attention
                )
            )

            progress.visibility = View.GONE
            recycler.visibility = View.VISIBLE

            if (items.isEmpty()) {
                Toast.makeText(this@TherapyActivitiesActivity, "No activities returned", Toast.LENGTH_LONG).show()
            } else {
                adapter.submitList(items)
            }
        }
    }


    private fun onItemClicked(item: RecommendTherapyItem) {
        // Build a minimal TherapyTask for Gemini
        val task = TherapyTask(
            activity = item.activity,
            ageGroup = "Age $age",
            disorder = disorder.ifBlank { "Speech" },
            goal = "Improve $severity $disorder".trim(),
            score = item.score
        )
        val taskTitle = item.activity.take(50).split(":").firstOrNull() ?: "Therapy Task"
        val matchScore = item.score
        val previewMsg = """
                📝 Activity:
                ${item.activity}

                👥 Age Group: $age
                🎯 Best for: ${disorder.ifBlank { "Speech" }}
                ⏱️ Duration: "15-20 minutes"}

                🎯 Smart Goal:
                Improve $severity $disorder

                ${if (item.score != null) "⭐ AI Match: ${(item.score * 100).toInt()}%" else ""}
            """.trimIndent()

        val loading = ProgressBar(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Generating details...")
            .setView(loading)
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch {
            val details = try {
                geminiHelper.generateTherapyDetail(task)
            } catch (e: Exception) {
                "Could not generate details: ${e.message}"
            } finally {
                dialog.dismiss()
            }
            val messageToShow = if (!details.isNullOrBlank()) details.trim() else previewMsg
            val boldTargets = listOf("Goal", "Tools & Materials", "Next Step", "STEP 1:", "STEP 2:", "STEP 3:", "STEP 4:", "STEP 5:", "STEP 6:")
            val spannable = SpannableString(messageToShow).apply {
                boldTargets.forEach { target ->
                    val start = messageToShow.indexOf(target)
                    if (start >= 0) {
                        setSpan(
                            StyleSpan(Typeface.BOLD),
                            start,
                            start + target.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            AlertDialog.Builder(this@TherapyActivitiesActivity)
                .setTitle("📚 $taskTitle")
                .setMessage(spannable)
                .setPositiveButton("Close", null)
                .show()
        }
    }


    // --- Adapter defined inside this file ---
    private class TherapyActivityAdapter(
        private val onDetailsClick: (RecommendTherapyItem) -> Unit
    ) : ListAdapter<RecommendTherapyItem, TherapyActivityAdapter.VH>(Diff) {

        object Diff : DiffUtil.ItemCallback<RecommendTherapyItem>() {
            override fun areItemsTheSame(oldItem: RecommendTherapyItem, newItem: RecommendTherapyItem) =
                oldItem.activity == newItem.activity
            override fun areContentsTheSame(oldItem: RecommendTherapyItem, newItem: RecommendTherapyItem) =
                oldItem == newItem
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val title: TextView = view.findViewById(R.id.tvActivityTitle)
            private val score: TextView = view.findViewById(R.id.tvActivityScore)
            private val btnDetails: View = view.findViewById(R.id.btnActivityDetails)

            fun bind(item: RecommendTherapyItem) {
                title.text = item.activity
                score.text = item.score?.let { "${(it * 100).toInt()}% match" } ?: "—"

                btnDetails.setOnClickListener {
                    val pos = absoluteAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onDetailsClick(getItem(pos))
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_therapy_activity, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))
    }
}