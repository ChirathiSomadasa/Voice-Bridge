package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.*

class FeedbackDialog(private val context: Context) {

    fun show(
        score: Int,
        category: String,
        feedbackMessage: String
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

        when (category) {
            "good" -> {
                ivEmoji.setImageResource(R.drawable.good_feedback)
                tvScore.setTextColor(Color.parseColor("#4CAF50"))
            }
            "moderate" -> {
                ivEmoji.setImageResource(R.drawable.moderate_feedback)
                tvScore.setTextColor(Color.parseColor("#FF9800"))
            }
            else -> {
                ivEmoji.setImageResource(R.drawable.bad_feedback)
                tvScore.setTextColor(Color.parseColor("#F44336"))
            }
        }

        tvScore.text = "Score: $score%"

        // Display the generated AI feedback message instantly without delay
        tvMessage.text = feedbackMessage

        btnOk.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}