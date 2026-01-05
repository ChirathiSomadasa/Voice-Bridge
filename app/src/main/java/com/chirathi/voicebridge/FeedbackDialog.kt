package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackDialog(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()

    fun show(
        score: Int,
        level: Int,
        word: String,
        pronunciationType: String
    ) {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_speech_feedback, null)

        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivEmoji = view.findViewById<ImageView>(R.id.iv_feedback_emoji)
        val tvMessage = view.findViewById<TextView>(R.id.tv_feedback_message)
        val tvScore = view.findViewById<TextView>(R.id.tv_feedback_score)
        val btnOk = view.findViewById<Button>(R.id.btn_feedback_ok)

        val category = when {
            score >= 75 -> "good"
            score >= 50 -> "moderate"
            else -> "bad"
        }

        when (category) {
            "good" -> {
                ivEmoji.setImageResource(R.drawable.good_feedback)
                tvMessage.text = "Great job! Your pronunciation is very clear!"
                tvScore.setTextColor(Color.parseColor("#4CAF50"))
            }
            "moderate" -> {
                ivEmoji.setImageResource(R.drawable.moderate_feedback)
                tvMessage.text = "Nice try! Practice a little more."
                tvScore.setTextColor(Color.parseColor("#FF9800"))
            }
            else -> {
                ivEmoji.setImageResource(R.drawable.bad_feedback)
                tvMessage.text = "Let's practice again together!"
                tvScore.setTextColor(Color.parseColor("#F44336"))
            }
        }

        tvScore.text = "Score: $score%"

        btnOk.setOnClickListener { dialog.dismiss() }
        dialog.show()

        val data = hashMapOf(
            "level" to level,
            "content" to word,
            "score" to score,
            "category" to category,
            "type" to pronunciationType,
            "timestamp" to Timestamp.now()
        )

        db.collection("pronunciation_feedback").add(data)
    }
}
