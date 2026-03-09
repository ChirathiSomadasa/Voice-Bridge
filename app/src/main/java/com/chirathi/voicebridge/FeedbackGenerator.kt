package com.chirathi.voicebridge

import com.google.ai.client.generativeai.GenerativeModel

object FeedbackGenerator {

    private val apiKey = BuildConfig.GEMINI_FEEDBACK_API_KEY

    // Added the 'targetText' parameter to receive the specific letter, word, or sentence the child practiced
    suspend fun getDynamicFeedback(score: Int, category: String, taskType: String, targetText: String): String {
        return try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )

            // If the task is a sentence, we omit the text because it is too long to include in a short prompt.
            // For letters and words, we include the exact text to make the feedback more personalized.
            val specificTarget = if (taskType == "sentence") {
                "a sentence"
            } else {
                "the $taskType '$targetText'"
            }

            // The prompt is tailored to be highly empathetic, fun, and relatable for a child
            val prompt = """
                You are a very kind, fun, and encouraging speech therapist for little kids.
                A child just practiced saying $specificTarget and got a score of $score% (Category: $category).
                
                Follow these rules based on their category:
                - If category is 'good': Celebrate their success enthusiastically! Mention $specificTarget if possible.
                - If category is 'moderate': Tell them they are almost there and doing a great job!
                - If category is 'bad' or 'poor': Gently comfort them. Tell them that $specificTarget can be a little tricky, but you believe in them and they should try again!
                
                Write EXACTLY ONE short, highly encouraging sentence speaking directly to the child.
                DO NOT use complex words. Keep it magical, fun, and sweet!
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)

            var generatedText = response.text?.trim() ?: getHardcodedFallback(category)

            // Remove any quotation marks that Gemini might automatically add to the generated sentence
            generatedText = generatedText.replace("\"", "")

            generatedText

        } catch (e: Exception) {
            e.printStackTrace()
            // If the network fails or the API quota is reached, use the safe fallback
            getHardcodedFallback(category)
        }
    }

    // Fallback logic to ensure the app never crashes or shows an empty dialog if offline
    private fun getHardcodedFallback(category: String): String {
        return when (category) {
            "good" -> "Great job! Your voice is so clear!"
            "moderate" -> "Nice try! You are getting so close, let's practice a little more."
            else -> "That was a tricky one! Don't worry, let's try again together with a big smile!"
        }
    }
}