package com.chirathi.voicebridge

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.*

class FeedbackDialog(private val context: Context) {

    fun show(
        score: Int,
        category: String,
        feedbackMessage: String,
        targetText: String,
        predictedText: String? = null, // The predicted word from the backend
        onClose: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_speech_feedback, null)

        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivEmoji = view.findViewById<ImageView>(R.id.iv_feedback_emoji)
        val tvMessage = view.findViewById<TextView>(R.id.tv_feedback_message)
        val tvHighlightedText = view.findViewById<TextView>(R.id.tv_highlighted_text)
        val tvRedSoundHint = view.findViewById<TextView>(R.id.tv_red_sound_hint) // The new hint text
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
        tvMessage.text = feedbackMessage

        // Show the red sound hint only if the score is below 75%
        if (score < 75) {
            tvRedSoundHint.visibility = View.VISIBLE
        } else {
            tvRedSoundHint.visibility = View.GONE
        }

        // Colorize letters using NLP Algorithm
        val safePredicted = predictedText ?: ""
        tvHighlightedText.text = getSmartHighlightedText(targetText, safePredicted, score)

        btnOk.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { onClose?.invoke() }
        dialog.show()
    }

    //Advanced NLP Alignment Algorithm for Pediatric Speech
    private fun getSmartHighlightedText(target: String, predicted: String, score: Int): SpannableString {
        val spannable = SpannableString(target)
        val green = Color.parseColor("#4CAF50") // Correct
        val red = Color.parseColor("#F44336")   // Incorrect

        // 1. If score >= 75%, mark everything correct (Green)
        if (score >= 75) {
            spannable.setSpan(ForegroundColorSpan(green), 0, target.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        // 2. If score < 50% or completely different word , mark everything wrong (Red)
        if (score < 50 || predicted.isEmpty() || predicted == "silence" || predicted == "unintelligible") {
            spannable.setSpan(ForegroundColorSpan(red), 0, target.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        // 3. Moderate Errors (50% - 74%): Find exactly where it went wrong
        val targetLower = target.lowercase().trim()
        val predictedLower = predicted.lowercase().trim()

        if (target.contains(" ")) {
            // SENTENCE ALGORITHM (Level 3)
            val targetWords = targetLower.split("\\s+".toRegex())
            val predictedWords = predictedLower.split("\\s+".toRegex())

            var currentIndex = 0
            for (word in targetWords) {
                val start = target.lowercase().indexOf(word, currentIndex)
                if (start == -1) continue
                val end = start + word.length

                // Check if the word exists in the predicted text. (Forgive 1 spelling error for words longer than 4 chars)
                val isWordSpoken = predictedWords.any { it == word || (word.length > 4 && getEditDistance(it, word) <= 1) }

                spannable.setSpan(ForegroundColorSpan(if (isWordSpoken) green else red), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                currentIndex = end
            }
        } else {
            // WORD & LETTER ALGORITHM (Level 1 & 2)
            var pIndex = 0
            for (tIndex in targetLower.indices) {
                val tChar = targetLower[tIndex]
                var matchFound = false

                // Match letters using Longest Common Subsequence (LCS) approach
                var tempPIndex = pIndex
                while (tempPIndex < predictedLower.length) {
                    if (predictedLower[tempPIndex] == tChar) {
                        matchFound = true
                        pIndex = tempPIndex + 1
                        break
                    }
                    tempPIndex++
                }
                spannable.setSpan(ForegroundColorSpan(if (matchFound) green else red), tIndex, tIndex + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }

    // Levenshtein Distance Algorithm to calculate edit distance between two words
    private fun getEditDistance(word1: String, word2: String): Int {
        val dp = Array(word1.length + 1) { IntArray(word2.length + 1) }
        for (i in 0..word1.length) dp[i][0] = i
        for (j in 0..word2.length) dp[0][j] = j
        for (i in 1..word1.length) {
            for (j in 1..word2.length) {
                val cost = if (word1[i - 1] == word2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[word1.length][word2.length]
    }
}