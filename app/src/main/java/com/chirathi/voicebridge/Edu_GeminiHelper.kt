// kotlin
package com.chirathi.voicebridge

import android.util.Log
import com.chirathi.voicebridge.api.models.TherapyTask
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Edu_GeminiHelper(private val apiKey: String = BuildConfig.GEMINI_Therapy_API_KEY) {

    companion object { private const val TAG = "Edu_GeminiHelper" }

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun generateTherapyDetail(task: TherapyTask): String = withContext(Dispatchers.IO) {
        try {
            val prompt = buildTherapyPrompt(task)
            val response = model.generateContent(prompt)
            val generated = response.text?.trim().orEmpty().replace("\"", "")
            if (generated.isNotBlank()) generated else fallback(task)
        } catch (e: Exception) {
            Log.e(TAG, "generateTherapyDetail failed", e)
            fallback(task)
        }
    }

    private fun buildTherapyPrompt(task: TherapyTask): String = """
        You are a friendly and encouraging speech therapist for children.
        Summarize the therapy task below in 1-2 short, positive sentences that a parent can read to the child.
        Avoid medical jargon and keep it formal.

        TASK INPUT:
        Title/Activity: ${task.title ?: task.activity.take(80)}
        Age Group: ${task.ageGroup}
        Disorder: ${task.disorder}
        Goal: ${task.goal}
        Materials: ${task.materials ?: "None"}
        Tips: ${task.tips ?: "None"}
        Description: ${task.description ?: task.activity}

        OUTPUT:
            - Start with one or two inviting sentence explaining the activity purpose for home practice.
            - Then provide 1-3 numbered lines: STEP 1:, STEP 2:, STEP 3: (as needed).
            - Highlight the goal, keep it playful, under 1020 words total.
            - One friendly emoji is OK; avoid bullets other than the STEP numbering.
            - display these - Age Group: ${task.ageGroup} Disorder: ${task.disorder}
            - Goal: ${task.goal}
    """.trimIndent()

    private fun fallback(task: TherapyTask): String {
        val goalText = if (!task.goal.isNullOrBlank()) task.goal else "your speech skills"
        return "Let's try this activity together! It will help with $goalText. We'll go step by step and have fun."
    }
}
