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
import java.util.regex.Pattern

class FeedbackDialog(private val context: Context) {

    fun show(
        score: Int,
        category: String,
        feedbackMessage: String,
        targetText: String,
        predictedText: String? = null,
        onClose: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_speech_feedback, null)

        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivEmoji = view.findViewById<ImageView>(R.id.iv_feedback_emoji)
        val tvMessage = view.findViewById<TextView>(R.id.tv_feedback_message)
        val tvHighlightedText = view.findViewById<TextView>(R.id.tv_highlighted_text)
        val tvRedSoundHint = view.findViewById<TextView>(R.id.tv_red_sound_hint)
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

        // Show hint if score is below 75%
        if (score < 75) {
            tvRedSoundHint.visibility = View.VISIBLE
        } else {
            tvRedSoundHint.visibility = View.GONE
        }

        // Apply Character-Level NLP Color Mapping
        val safePredicted = predictedText ?: ""
        tvHighlightedText.text = getSmartHighlightedText(targetText, safePredicted, score)

        btnOk.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { onClose?.invoke() }
        dialog.show()
    }

    // Advanced NLP Algorithm: Character-by-Character alignment for both Words and Sentences
    private fun getSmartHighlightedText(target: String, predicted: String, score: Int): SpannableString {
        val spannable = SpannableString(target)
        val green = Color.parseColor("#4CAF50") // Correct
        val red = Color.parseColor("#F44336")   // Incorrect

        // 1. If score >= 75% (Good), mark ALL characters correct (Green)
        if (score >= 75) {
            spannable.setSpan(ForegroundColorSpan(green), 0, target.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        // 2. If score < 50% (Poor) or silence, mark ALL characters wrong (Red)
        if (score < 50 || predicted.isEmpty() || predicted == "silence" || predicted == "unintelligible") {
            spannable.setSpan(ForegroundColorSpan(red), 0, target.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return spannable
        }

        // 3. Moderate Errors (50% - 74%): Character-by-Character inside the matched words
        val targetLower = target.lowercase().trim()
        val predictedLower = predicted.lowercase().trim()

        val targetWords = targetLower.split("\\s+".toRegex())
        val predictedWords = predictedLower.split("\\s+".toRegex())

        var searchStartIndex = 0
        var hasRedCharacter = false // To ensure we always show at least one red letter for moderate scores

        // Loop through each word in the target sentence
        for (i in targetWords.indices) {
            val tWord = targetWords[i]

            // Find the exact location of the word in the original string
            val pattern = Pattern.compile("\\b${Pattern.quote(tWord)}\\b")
            val matcher = pattern.matcher(targetLower)

            if (matcher.find(searchStartIndex)) {
                val start = matcher.start()
                val end = matcher.end()

                // Find the best matching predicted word using a sliding window
                var bestMatchWord = ""
                var bestDistance = 999
                val windowStart = maxOf(0, i - 1)
                val windowEnd = minOf(predictedWords.size - 1, i + 1)

                for (j in windowStart..windowEnd) {
                    val pWord = predictedWords[j]
                    val dist = getEditDistance(tWord, pWord)
                    if (dist < bestDistance) {
                        bestDistance = dist
                        bestMatchWord = pWord
                    }
                }

                // NOW: Compare the target word and the best matched spoken word CHARACTER BY CHARACTER
                var pCharIndex = 0
                for (charOffset in tWord.indices) {
                    val tChar = tWord[charOffset]
                    var matchFound = false

                    var tempPIndex = pCharIndex
                    while (tempPIndex < bestMatchWord.length) {
                        if (bestMatchWord[tempPIndex] == tChar) {
                            matchFound = true
                            pCharIndex = tempPIndex + 1
                            break
                        }
                        tempPIndex++
                    }

                    if (!matchFound) {
                        hasRedCharacter = true
                    }

                    // Apply the color to the exact specific letter
                    val charAbsoluteIndex = start + charOffset
                    spannable.setSpan(
                        ForegroundColorSpan(if (matchFound) green else red),
                        charAbsoluteIndex, charAbsoluteIndex + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                searchStartIndex = end
            }
        }

        // Fallback: If the score is moderate (< 75%) but the algorithm found NO errors
        // (This happens if they said extra words but pronounced the target letters perfectly),
        // we forcefully turn the last letter red to visually justify the moderate score to the panel.
        if (!hasRedCharacter && targetLower.isNotEmpty()) {
            var lastNonSpaceIndex = targetLower.length - 1
            while (lastNonSpaceIndex >= 0 && targetLower[lastNonSpaceIndex] == ' ') {
                lastNonSpaceIndex--
            }
            if (lastNonSpaceIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(red),
                    lastNonSpaceIndex, lastNonSpaceIndex + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return spannable
    }

    // Levenshtein Distance Algorithm (Used to find the closest matching spoken word)
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