package com.chirathi.voicebridge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DynamicHintGenerator {
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val TAG = "DynamicHint"

    suspend fun generateSequenceHint(
        context: Context,
        targetStepName: String,
        previousStepName: String?,
        routineName: String,
        childAge: Int,
        isIdle: Boolean = false
    ): String = withContext(Dispatchers.IO) {

        val apiKey = try {
            val id = context.resources.getIdentifier("groq_api_key", "string", context.packageName)
            if (id == 0) "" else context.getString(id).trim()
        } catch (e: Exception) { "" }

        val fallback = if (targetStepName.isNotBlank()) "Think about what happens next. Is it $targetStepName?" else "Take another look at the steps."

        if (apiKey.isBlank() || targetStepName.isBlank()) return@withContext fallback

        // Strictly define the context so the AI doesn't hallucinate missing steps
        val contextLine = if (previousStepName != null) {
            "They just successfully completed: '$previousStepName'. Now they need to find the step that comes immediately AFTER that."
        } else {
            "They are looking for the VERY FIRST step of the '$routineName' routine. Do NOT ask what comes before it, because nothing comes before it. Ask them how they start the routine."
        }

        val actionLine = if (isIdle) {
            "They are hesitating and haven't made a move for a few seconds. They need to find: \"$targetStepName\". Ask a gentle question to nudge them toward it."
        } else {
            "The correct answer they need to find is: \"$targetStepName\". They just made a mistake. Guide them toward the right answer."
        }

        val prompt = """
            You are a friendly speech therapist helping a $childAge-year-old child with a sequence game about "$routineName".
            $contextLine
            $actionLine
            Write a very short, gentle hint (maximum 8 words).
            Do not say "wrong" or "no". Just ask a simple guiding question to help them figure out it is "$targetStepName".
            Make it sound encouraging.
        """.trimIndent()

        try {
            val conn = URL(GROQ_ENDPOINT).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            val body = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put("max_tokens", 25)
                put("temperature", 0.6)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content").trim()
                    .replace("\"", "")
            } else fallback
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch hint: ${e.message}")
            fallback
        }
    }
}