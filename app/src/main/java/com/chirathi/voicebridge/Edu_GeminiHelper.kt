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

//    private fun buildTherapyPrompt(task: TherapyTask): String = """
//        You are a friendly and encouraging speech therapist for children.
//        Summarize the therapy task below in 1-2 short, positive sentences that a parent can read to the child.
//        Avoid medical jargon and keep it formal.
//
//        TASK INPUT:
//        Title/Activity: ${task.title ?: task.activity.take(80)}
//        Age Group: ${task.ageGroup}
//        Disorder: ${task.disorder}
//        Goal: ${task.goal}
//        Materials: ${task.materials ?: "None"}
//        Tips: ${task.tips ?: "None"}
//        Description: ${task.description ?: task.activity}
//
//        OUTPUT:
//            - Start with one or two inviting sentence explaining the activity purpose for home practice.
//            - Then provide 1-3 numbered lines: STEP 1:, STEP 2:, STEP 3: (as needed).
//            - Highlight the goal, keep it playful, under 1020 words total.
//            - One friendly emoji is OK; avoid bullets other than the STEP numbering.
//            - display these - Age Group: ${task.ageGroup} Disorder: ${task.disorder}
//            - Goal: ${task.goal}
//    """.trimIndent()

    private fun buildTherapyPrompt(task: TherapyTask): String = """
        You are a speech therapist helping a child aged 6–10 with Speech Sound Disorder (SSD).
        Write VERY SIMPLE instructions the child can understand easily.

        Rules:
        • Use short sentences (max 8 words).
        • Use very simple words.
        • Sound friendly and playful.
        • Avoid long explanations.Avoid medical jargon.
        • Use only 4 or 6 steps. simplified each steps content. Each step must be easy to follow.
        • Use simple 2 or 3 emojis only. like happy face, clapping hands, etc. Avoid complex emojis.
        • these ${task.title ?: task.activity} should be for home practice that easily do in home with parents help.

        Activity Information:
        Activity: ${task.title ?: task.activity}
        Goal: ${task.goal}
        Age Group: ${task.ageGroup}
        Disorder: ${task.disorder}
        Materials: ${task.materials ?: "None"}
        Tips: ${task.tips ?: "None"}
        Description: ${task.description ?: task.activity}
        child matches ${task.score} the above information with the activity and goal.

        Output format:
        display these - ${task.title ?: task.activity}
        short description of the activity in 1-2 sentences. for parent to read to child. Avoid complex sentences.
        STEP 1: 
        STEP 2: 
        STEP 3: 
        STEP 4:
        STEP 5:
        STEP 6:
        Tools & Materials : bullet marks mention tools, materials needed for the activity if any according to ${task.title ?: task.activity}, and how to use them in simple terms.
        
        Goal: Help the child practice ${task.goal}. after one line space place the goal in simple words. like "Goal: Practice saying 's' sound". Avoid complex sentences.
        Next Steps: Provide what the parent can do after the activity to improve the child. like "Next Steps: how to go on forward with this activity.
        
        don't add any * mark or - mark in the steps, just write STEP 1: and then the instruction. Avoid any extra formatting. Keep it very simple and clear for a child to understand.
    """.trimIndent()

    private fun fallback(task: TherapyTask): String {
        val goalText = if (!task.goal.isNullOrBlank()) task.goal else "your speech skills"
        return "Let's try this activity together! It will help with $goalText. We'll go step by step and have fun."
    }
}
