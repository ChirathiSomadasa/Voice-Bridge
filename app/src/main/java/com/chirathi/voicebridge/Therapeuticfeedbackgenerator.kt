package com.chirathi.voicebridge

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TherapeuticFeedback(
    val headline:      String,
    val message:       String,
    val encouragement: String,
    val fromAI:        Boolean = false
)

data class SessionContext(
    val activityType:      TherapeuticFeedbackGenerator.ActivityType,
    val performanceLevel:  TherapeuticFeedbackGenerator.PerformanceLevel,
    val childAge:          Int,
    val errorCount:        Int,
    val correctCount:      Int,
    val attempts:          Int,
    val routineName:       String = "",
    val songTitle:         String = "",
    val subRoutineLabel:   String = "",
    val usedHints:         Boolean = false,
    val usedUndo:          Boolean = false,
    val miniGamePlayed:    Boolean = false,
    val avgResponseTimeMs: Long    = 0L,
    val consecutiveCorrect:Int     = 0,
    val consecutiveWrong:  Int     = 0,
    val previousAlpha:     Float   = 0f,
    val sessionNumber:     Int     = 0
)

object TherapeuticFeedbackGenerator {

    private const val TAG = "TherapeuticFeedback"

    private const val GROQ_ENDPOINT =
        "https://api.groq.com/openai/v1/chat/completions"

    enum class PerformanceLevel {
        MASTERY, STRONG, DEVELOPING, PERSISTING
    }

    enum class ActivityType(val label: String) {
        SEQUENCE_ORDERING ("putting the steps of a daily routine in the right order"),
        MOOD_MATCHING     ("looking at a face picture and choosing the right feeling"),
        RHYTHM_LISTENING  ("listening to a song and finding the matching picture word")
    }

    fun classify(
        correctCount: Int,
        errorCount:   Int,
        attempts:     Int,
        finalAlpha:   Float = 0f
    ): PerformanceLevel {
        val accuracy = if (attempts > 0) correctCount.toFloat() / attempts.coerceAtLeast(1) else 0f
        return when {
            (accuracy >= 0.90f && errorCount <= 1) || finalAlpha >= 7f -> PerformanceLevel.MASTERY
            (accuracy >= 0.65f && errorCount <= 3) || finalAlpha >= 4f -> PerformanceLevel.STRONG
            accuracy >= 0.30f || errorCount in 1..6                    -> PerformanceLevel.DEVELOPING
            else                                                        -> PerformanceLevel.PERSISTING
        }
    }

    suspend fun generate(
        context: Context,
        session: SessionContext
    ): TherapeuticFeedback = withContext(Dispatchers.IO) {

        // DIAG A — check key resolution inside generator
        val apiKey = resolveApiKey(context)
        Log.d(TAG, "DIAG A - resolveApiKey: isBlank=${apiKey.isBlank()}, length=${apiKey.length}, starts=${apiKey.take(8)}")

        if (apiKey.isBlank()) {
            Log.w(TAG, "DIAG A - No API key — falling back")
            return@withContext lastResortFallback(session.performanceLevel)
        }

        try {
            val prompt = buildPrompt(session)
            Log.d(TAG, "DIAG B - Prompt built, length=${prompt.length} chars")
            Log.d(TAG, "DIAG B - Calling Gemini for: ${session.activityType} / ${session.performanceLevel} / age ${session.childAge}")

            val startTime = System.currentTimeMillis()
            val raw = callGroq(prompt, apiKey)
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "DIAG C - Gemini responded in ${elapsed}ms, raw length=${raw.length}")
            Log.d(TAG, "DIAG C - Raw response preview: ${raw.take(300)}")

            val parsed = parseGroqResponse(raw)
            if (parsed != null) {
                Log.d(TAG, "DIAG D - Parse SUCCESS: fromAI=${parsed.fromAI}, headline='${parsed.headline}'")
                parsed
            } else {
                Log.w(TAG, "DIAG D - Parse FAILED — falling back")
                lastResortFallback(session.performanceLevel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "DIAG E - Exception: ${e::class.simpleName}: ${e.message}", e)
            lastResortFallback(session.performanceLevel)
        }
    }

    suspend fun generate(
        context:          Context,
        activityType:     ActivityType,
        performanceLevel: PerformanceLevel,
        childAge:         Int,
        errorCount:       Int,
        correctCount:     Int,
        attempts:         Int    = correctCount + errorCount,
        sessionNotes:     String = ""
    ): TherapeuticFeedback {
        val routineName = Regex("Activity:\\s*([^.]+)").find(sessionNotes)?.groupValues?.get(1)?.trim() ?: ""
        val songTitle   = Regex("song \"([^\"]+)\"").find(sessionNotes)?.groupValues?.get(1)?.trim() ?: ""

        val session = SessionContext(
            activityType     = activityType,
            performanceLevel = performanceLevel,
            childAge         = childAge,
            errorCount       = errorCount,
            correctCount     = correctCount,
            attempts         = attempts,
            routineName      = routineName,
            songTitle        = songTitle
        )
        return generate(context, session)
    }

    private fun resolveApiKey(context: Context): String = try {
        val id = context.resources.getIdentifier("groq_api_key", "string", context.packageName)
        Log.d(TAG, "DIAG A - getIdentifier result: $id (0 means not found)")
        if (id == 0) "" else context.getString(id).trim()
    } catch (e: Exception) {
        Log.e(TAG, "DIAG A - resolveApiKey exception: ${e.message}")
        ""
    }

    private fun buildPrompt(s: SessionContext): String {

        val whatHappened = when (s.performanceLevel) {
            PerformanceLevel.MASTERY    -> "got it right very quickly with almost no mistakes"
            PerformanceLevel.STRONG     -> "got it right after trying a few times"
            PerformanceLevel.DEVELOPING -> "tried many times and kept going even when it was hard"
            PerformanceLevel.PERSISTING -> "found it very hard but never stopped trying"
        }

        val headlineGuidance = when (s.performanceLevel) {
            PerformanceLevel.MASTERY    -> "Write a BIG celebratory headline. The child did perfectly. Show maximum joy and pride."
            PerformanceLevel.STRONG     -> "Write a warm, positive headline. The child did well after some tries. Show genuine praise."
            PerformanceLevel.DEVELOPING -> "Write a gentle, encouraging headline. The child tried hard. Celebrate their effort not the result."
            PerformanceLevel.PERSISTING -> "Write a very warm, supportive headline. The child struggled but kept going. Celebrate their bravery."
        }

        val messageGuidance = when (s.performanceLevel) {
            PerformanceLevel.MASTERY    -> "Celebrate what they did — they got it right and did it well. Use words like: great, well, good, did it, right."
            PerformanceLevel.STRONG     -> "Acknowledge they kept trying and got there. Use words like: tried, kept going, did it, good."
            PerformanceLevel.DEVELOPING -> "Focus on effort, not result. They tried many times — that is brave and strong. Never mention getting it wrong."
            PerformanceLevel.PERSISTING -> "Focus only on bravery. They did not give up. That is the most important thing. Never mention the result."
        }

        val encouragementGuidance = when (s.performanceLevel) {
            PerformanceLevel.MASTERY    -> "Invite them to play again RIGHT NOW with excitement. Like: 'Play again, it is so fun!' NOT 'come back tomorrow'."
            PerformanceLevel.STRONG     -> "Encourage them to play again. Like: 'Play again and do great!' NOT 'try next time'."
            PerformanceLevel.DEVELOPING -> "Gently invite them to try again. Like: 'Try again, you are brave!' NOT 'come back later'."
            PerformanceLevel.PERSISTING -> "Warmly invite them to try once more. Like: 'Try again, you are strong!' NOT 'come back tomorrow'."
        }

        val speedNote = when {
            s.avgResponseTimeMs in 1..3000   -> "The child answered quickly."
            s.avgResponseTimeMs in 3001..7000 -> "The child took a little time to think."
            s.avgResponseTimeMs > 7000        -> "The child thought carefully before each answer."
            else                              -> ""
        }

        val behaviourNotes = buildList {
            if (s.usedHints)               add("The child asked for a hint.")
            if (s.usedUndo)                add("The child changed their mind and tried again.")
            if (s.miniGamePlayed)          add("The child played a mini game during the session.")
            if (s.consecutiveCorrect >= 2) add("The child got the last answers right in a row.")
            if (s.consecutiveWrong  >= 2)  add("The child found the last part hard.")
        }.joinToString(" ")

        val progressNote = when {
            s.sessionNumber > 1 && s.previousAlpha > 0f -> when (s.performanceLevel) {
                PerformanceLevel.MASTERY    -> "This child is doing better than last time."
                PerformanceLevel.PERSISTING -> "This child found it harder than last time."
                else                        -> "This child is making progress over time."
            }
            else -> ""
        }

        val activityDetail = when {
            s.routineName.isNotBlank() && s.subRoutineLabel.isNotBlank() ->
                "${s.activityType.label}. The routine was: ${s.routineName}. The steps were: ${s.subRoutineLabel}."
            s.routineName.isNotBlank() ->
                "${s.activityType.label}. The routine was: ${s.routineName}."
            s.songTitle.isNotBlank() ->
                "${s.activityType.label}. The song was: ${s.songTitle}."
            else ->
                s.activityType.label
        }

        return """
You are a speech therapist writing a short message for a child after a therapy game.

ABOUT THIS CHILD:
- Age: ${s.childAge} years old
- Has Autism Spectrum Disorder or Down Syndrome
- English is NOT their first language
- They can only read very short, simple English words
- Seeing too many words on screen makes them feel overwhelmed

WHAT HAPPENED IN THIS SESSION:
- Activity: $activityDetail
- Performance level: ${s.performanceLevel}
- The child $whatHappened.
- $speedNote $behaviourNotes $progressNote

HOW TO WRITE FOR THIS PERFORMANCE LEVEL:
- headline:      $headlineGuidance
- message:       $messageGuidance
- encouragement: $encouragementGuidance

WRITE a JSON object with exactly these three fields:
{
  "headline":      "...",
  "message":       "...",
  "encouragement": "..."
}

STRICT RULES — every single one must be followed:

LENGTH:
  headline:      3 to 4 words only. No more, no less.
  message:       1 sentence. 6 to 8 words only. No more.
  encouragement: 1 sentence. 4 to 6 words only. No more.

LANGUAGE:
  Use ONLY words a 6 year old beginner English speaker knows.
  Allowed words include: good, well, done, try, tried, brave, strong, nice, great, go, keep, fun, play, do, did, you, it, today, are, is, so, very, right, got, kept, going, steps, put, in, the, order, routine.
  NEVER use: persistence, determination, achievement, incredible, brilliant, challenging, practise, focus, cognitive, overcome, demonstrate, resilience, accomplish, therapeutic, vocabulary, outstanding, fantastic, wonderful.

NO TIME REFERENCES:
  NEVER say: tomorrow, later, next time, come back, again soon, next session, see you.
  The child may play RIGHT NOW. Encouragement must feel immediate, not future.
  BAD: "Come back and play again." — suggests leaving, do NOT write this.
  BAD: "Try again next time." — implies waiting, do NOT write this.
  BAD: "Play again tomorrow." — implies a future time, do NOT write this.
  GOOD: "Play again right now!" — immediate and exciting.
  GOOD: "Try once more, you are brave!" — immediate invitation.

NO EMOJIS. NO NUMBERS. NEVER NEGATIVE.
  Never say: wrong, mistake, fail, bad, hard, difficult, error, not, cannot.

PERFORMANCE MUST MATCH TONE:
  MASTERY → maximum celebration, biggest praise words.
  STRONG → warm praise, acknowledge effort paid off.
  DEVELOPING → gentle praise, celebrate trying not result.
  PERSISTING → pure warmth, celebrate bravery only.

ONLY return the JSON. No markdown. No explanation. No backticks.

GOOD MASTERY example:
{"headline":"You did so well","message":"You got every step in the right order.","encouragement":"Play again right now!"}

GOOD PERSISTING example:
{"headline":"You are so brave","message":"You kept trying and that is so good.","encouragement":"Try once more, you are strong!"}
    """.trimIndent()
    }

    private fun callGroq(prompt: String, apiKey: String): String {
        Log.d(TAG, "DIAG B - Opening connection to Groq endpoint...")
        val conn = URL(GROQ_ENDPOINT).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput       = true
            conn.connectTimeout = 12_000
            conn.readTimeout    = 25_000

            val body = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put("max_tokens", 120)
                put("temperature", 0.85)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            Log.d(TAG, "DIAG B - Writing request body, size=${body.toString().length} bytes")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            Log.d(TAG, "DIAG C - HTTP response code: $code")

            if (code == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                Log.d(TAG, "DIAG C - Response received, length=${response.length}")
                response
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                Log.e(TAG, "DIAG C - Groq HTTP $code error body: $err")
                throw Exception("HTTP $code: $err")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseGroqResponse(raw: String): TherapeuticFeedback? = try {
        val text = JSONObject(raw)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        Log.d(TAG, "DIAG D - Extracted text from response: '$text'")

        val obj = JSONObject(text)
        TherapeuticFeedback(
            headline      = obj.getString("headline").clean(),
            message       = obj.getString("message").clean(),
            encouragement = obj.getString("encouragement").clean(),
            fromAI        = true
        )
    } catch (e: Exception) {
        Log.w(TAG, "DIAG D - parseGroqResponse failed: ${e.message}")
        null
    }

    private fun String.clean(): String =
        this
            .replace(Regex("\\b\\d+\\s*/\\s*\\d+\\b"), "")
            .replace(Regex("\\b\\d+\\s*%"), "")
            .replace(Regex("[\\p{So}\\p{Sm}\\uFE0F\\u20D0-\\u20FF" +
                    "\\u2600-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

    private fun lastResortFallback(level: PerformanceLevel): TherapeuticFeedback = when (level) {
        PerformanceLevel.MASTERY    -> TherapeuticFeedback(
            headline      = "You did so well",
            message       = "That was great work today.",
            encouragement = "Come back and play again."
        )
        PerformanceLevel.STRONG     -> TherapeuticFeedback(
            headline      = "Good job today",
            message       = "You tried hard and did well.",
            encouragement = "Keep going next time."
        )
        PerformanceLevel.DEVELOPING -> TherapeuticFeedback(
            headline      = "You kept going",
            message       = "You are brave and strong.",
            encouragement = "Try again next time."
        )
        PerformanceLevel.PERSISTING -> TherapeuticFeedback(
            headline      = "You are brave",
            message       = "You tried and that is good.",
            encouragement = "Come back and try again."
        )
    }
}