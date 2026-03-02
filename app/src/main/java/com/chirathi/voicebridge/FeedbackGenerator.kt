package com.chirathi.voicebridge

import com.google.ai.client.generativeai.GenerativeModel

object FeedbackGenerator {

    private val apiKey = BuildConfig.GEMINI_FEEDBACK_API_KEY

    // This is a suspend function, meaning it can safely do long-running tasks like network calls
    suspend fun getDynamicFeedback(score: Int, category: String): String {
        return try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )

            val prompt = """
                You are a friendly and encouraging speech therapist for children. 
                A child just completed a speech pronunciation task and got a score of $score% (Category: $category).
                Write a single, short, and highly encouraging sentence to motivate the child.
                DO NOT mention the specific word they tried to say. 
                DO NOT use complex words. Keep it fun!
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            // Get the generated text, or fall back to hardcoded text if the response is null
            var generatedText = response.text?.trim() ?: getHardcodedFallback(category)

            // Remove extra quotes if Gemini includes them
            generatedText.replace("\"", "")

        } catch (e: Exception) {
            e.printStackTrace()
            getHardcodedFallback(category)
        }
    }

    // Fallback logic to ensure the app never crashes or shows an empty dialog if offline
    private fun getHardcodedFallback(category: String): String {
        return when (category) {
            "good" -> "Great job! Your pronunciation is very clear!"
            "moderate" -> "Nice try! Practice a little more."
            else -> "Let's practice again together!"
        }
    }
}