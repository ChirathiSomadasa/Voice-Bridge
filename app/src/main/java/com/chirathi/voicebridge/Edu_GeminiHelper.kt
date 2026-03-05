package com.chirathi.voicebridge

import android.util.Log
import com.chirathi.voicebridge.data.AnswerType
import com.chirathi.voicebridge.data.MatchPairModel
import com.chirathi.voicebridge.data.OptionModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Edu_GeminiHelper(private val apiKey: String) {

    companion object { private const val TAG = "GeminiHelper" }

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-2.5-flash",

        )

    suspend fun generateLessonContent(
        age: String,
        subject: String,
        disorderType: String?,
        severity: String?,
        lessonTitle: String,
        lessonHint: String
    ): LessonContent = withContext(Dispatchers.IO) {
        try {
            val prompt = buildLessonPrompt(age, subject, disorderType, severity, lessonTitle, lessonHint)
            Log.d("GEMINI_PROMPT", prompt)
            val response = model.generateContent(prompt)
            Log.d("GEMINI_RAW", response.toString())
            val responseText = response.text ?: throw Exception("Empty response from AI")
            parseLessonResponse(responseText, lessonHint)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating lesson content: ${e.message}", e)
            LessonContent(
                content = lessonHint,
                question = "What did you learn?",
                answer = "Review the lesson",
                answerType = AnswerType.TEXT,
                options = emptyList(),
                matchPairs = emptyList(),
                howToSteps = listOf("Read carefully", "Think about it", "Answer in your own words")
            )
        }
    }

    suspend fun validateAnswer(
        question: String,
        correctAnswer: String,
        userAnswer: String,
        age: String,
        disorderType: String?
    ): AnswerValidation = withContext(Dispatchers.IO) {
        try {
            val prompt = buildValidationPrompt(question, correctAnswer, userAnswer, age, disorderType)
            val response = model.generateContent(prompt)
            val responseText = response.text ?: throw Exception("Empty validation response")
            parseValidationResponse(responseText)
        } catch (e: Exception) {
            Log.e(TAG, "Error validating answer: ${e.message}", e)
            val isCorrect = userAnswer.trim().equals(correctAnswer.trim(), ignoreCase = true)
            AnswerValidation(
                isCorrect = isCorrect,
                feedback = if (isCorrect) "Great job!" else "Try again!",
                encouragement = "Keep learning!",
                hint = "Think about: $correctAnswer"
            )
        }
    }

    suspend fun generateHint(
        question: String,
        correctAnswer: String,
        age: String,
        disorderType: String?,
        severity: String?
    ): String = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are a special education teacher helping a child aged $age with ${disorderType ?: "learning needs"} (${severity ?: "moderate"} severity).

                Question: $question
                Correct Answer: $correctAnswer

                Provide a gentle, encouraging hint (not the full answer) that helps the child think through the problem.
                Use simple language, positive reinforcement, and visual or sensory cues if appropriate.
                Keep it under 50 words.
            """.trimIndent()

            model.generateContent(prompt).text ?: "Think about what you learned. You can do this!"
        } catch (e: Exception) {
            Log.e(TAG, "Error generating hint: ${e.message}", e)
            "Let's think about this together. Take your time!"
        }
    }

    private fun buildLessonPrompt(
        age: String,
        subject: String,
        disorderType: String?,
        severity: String?,
        lessonTitle: String,
        lessonHint: String
    ): String = """
        You are an expert special education teacher creating a single short lesson.

        STUDENT PROFILE:
        - Age: $age years old
        - Disorder: ${disorderType ?: "General learning needs"}
        - Severity: ${severity ?: "Moderate"}
        - Subject: $subject
        - Lesson: ${lessonTitle.ifBlank { lessonHint }}

        BASE CONTENT (hint/topic): $lessonHint

        TASK:
        Adapt this into a very short, child-friendly lesson. Choose an appropriate answer type:
        - TEXT (free short answer),
        - MCQ (multiple choice with 3-4 options),
        - DRAW (child draws),
        - MATCH (pairs to match).

        FORMAT EXACTLY:
        CONTENT: [3-4 simple sentences]
        QUESTION: [single clear question]
        ANSWER: [concise correct answer]
        ANSWER_TYPE: [TEXT|MCQ|DRAW|MATCH]
        OPTIONS: [for MCQ only, each line: id|text|true/false]
        MATCH_PAIRS: [for MATCH only, each line: left->right]
        STEPS: [3-4 simple steps separated by |]

        Keep it encouraging and positive.
    """.trimIndent()

//    private fun parseLessonResponse(responseText: String, baseContent: String): LessonContent {
//        val content = responseText.substringAfter("CONTENT:", "").substringBefore("QUESTION:", "").trim()
//        val question = responseText.substringAfter("QUESTION:", "").substringBefore("ANSWER:", "").trim()
//        val answer = responseText.substringAfter("ANSWER:", "").substringBefore("ANSWER_TYPE:", "").trim()
//        val answerTypeRaw = responseText.substringAfter("ANSWER_TYPE:", "").substringBefore("OPTIONS:", "").substringBefore("MATCH_PAIRS:", "").substringBefore("STEPS:", "").trim()
//        val stepsText = responseText.substringAfter("STEPS:", "").trim()
//
//        val answerType = when (answerTypeRaw.uppercase()) {
//            "MCQ" -> AnswerType.MCQ
//            "DRAW" -> AnswerType.DRAW
//            "MATCH" -> AnswerType.MATCH
//            else -> AnswerType.TEXT
//        }
//
//        val optionsText = responseText.substringAfter("OPTIONS:", "").substringBefore("MATCH_PAIRS:", "").substringBefore("STEPS:", "")
//        val options = optionsText
//            .lines()
//            .mapNotNull { line ->
//                val parts = line.split("|")
//                if (parts.size >= 3) {
//                    OptionModel(
//                        id = parts[0].trim(),
//                        text = parts[1].trim(),
//                        isCorrect = parts[2].trim().equals("true", ignoreCase = true)
//                    )
//                } else null
//            }
//
//        val matchText = responseText.substringAfter("MATCH_PAIRS:", "").substringBefore("STEPS:", "")
//        val matchPairs = matchText
//            .lines()
//            .mapNotNull { line ->
//                val parts = line.split("->")
//                if (parts.size == 2) {
//                    MatchPairModel(parts[0].trim(), parts[1].trim())
//                } else null
//            }
//
//        val steps = stepsText.split("|").map { it.trim() }.filter { it.isNotEmpty() }
//
//        if (content.isEmpty() || question.isEmpty() || answer.isEmpty()) {
//            throw Exception("Incomplete response parsing")
//        }
//
//        return LessonContent(
//            content = content,
//            question = question,
//            answer = answer,
//            answerType = answerType,
//            options = if (answerType == AnswerType.MCQ) options else emptyList(),
//            matchPairs = if (answerType == AnswerType.MATCH) matchPairs else emptyList(),
//            howToSteps = steps.ifEmpty { listOf("Read carefully", "Think about it", "Answer") }
//        )
//    }
//
//    private fun buildValidationPrompt(
//        question: String,
//        correctAnswer: String,
//        userAnswer: String,
//        age: String,
//        disorderType: String?
//    ): String = """
//        You are a kind, patient special education teacher working with a $age-year-old child with ${disorderType ?: "learning needs"}.
//
//        Question: $question
//        Expected Answer: $correctAnswer
//        Child's Answer: $userAnswer
//
//        TASK:
//        Evaluate if the child's answer is correct (conceptual match is enough).
//        Provide encouraging feedback appropriate for a child with special needs.
//
//        FORMAT EXACTLY AS:
//        CORRECT: [YES or NO]
//        FEEDBACK: [One encouraging sentence]
//        ENCOURAGEMENT: [Positive reinforcement]
//        HINT: [If wrong, a gentle hint]
//    """.trimIndent()
//
//    private fun parseValidationResponse(responseText: String): AnswerValidation {
//        val isCorrect = responseText.substringAfter("CORRECT:", "").substringBefore("FEEDBACK:", "").trim()
//            .equals("YES", ignoreCase = true)
//        val feedback = responseText.substringAfter("FEEDBACK:", "").substringBefore("ENCOURAGEMENT:", "").trim()
//        val encouragement = responseText.substringAfter("ENCOURAGEMENT:", "").substringBefore("HINT:", "").trim()
//        val hint = responseText.substringAfter("HINT:", "").trim()
//
//        if (feedback.isEmpty() || encouragement.isEmpty()) {
//            throw Exception("Incomplete validation parsing")
//        }
//
//        return AnswerValidation(isCorrect, feedback, encouragement, hint)
//    }
//}
//
//data class LessonContent(
//    val content: String,
//    val question: String,
//    val answer: String,
//    val answerType: AnswerType,
//    val options: List<OptionModel>,
//    val matchPairs: List<MatchPairModel>,
//    val howToSteps: List<String>
//)
//
//data class AnswerValidation(
//    val isCorrect: Boolean,
//    val feedback: String,
//    val encouragement: String,
//    val hint: String
//)

    private fun parseLessonResponse(responseText: String, fallbackContent: String): LessonContent {
        val content = responseText.substringAfter("CONTENT:", "").substringBefore("QUESTION:", "").trim()
        val question = responseText.substringAfter("QUESTION:", "").substringBefore("ANSWER:", "").trim()
        val answer = responseText.substringAfter("ANSWER:", "").substringBefore("ANSWER_TYPE:", "").trim()
        val answerTypeRaw = responseText.substringAfter("ANSWER_TYPE:", "").substringBefore("OPTIONS:", "").substringBefore("MATCH_PAIRS:", "").substringBefore("STEPS:", "").trim()
        val stepsText = responseText.substringAfter("STEPS:", "").trim()

        val answerType = when (answerTypeRaw.uppercase()) {
            "MCQ" -> AnswerType.MCQ
            "DRAW" -> AnswerType.DRAW
            "MATCH" -> AnswerType.MATCH
            else -> AnswerType.TEXT
        }

        val optionsText = responseText.substringAfter("OPTIONS:", "").substringBefore("MATCH_PAIRS:", "").substringBefore("STEPS:", "")
        val options = optionsText
            .lines()
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    OptionModel(
                        id = parts[0].trim(),
                        text = parts[1].trim(),
                        isCorrect = parts[2].trim().equals("true", ignoreCase = true)
                    )
                } else null
            }

        val matchText = responseText.substringAfter("MATCH_PAIRS:", "").substringBefore("STEPS:", "")
        val matchPairs = matchText
            .lines()
            .mapNotNull { line ->
                val parts = line.split("->")
                if (parts.size == 2) MatchPairModel(parts[0].trim(), parts[1].trim()) else null
            }

        val steps = stepsText.split("|").map { it.trim() }.filter { it.isNotEmpty() }

        if (content.isEmpty() || question.isEmpty() || answer.isEmpty()) {
            throw Exception("Incomplete response parsing")
        }

        return LessonContent(
            content = content.ifBlank { fallbackContent },
            question = question,
            answer = answer,
            answerType = answerType,
            options = if (answerType == AnswerType.MCQ) options else emptyList(),
            matchPairs = if (answerType == AnswerType.MATCH) matchPairs else emptyList(),
            howToSteps = steps.ifEmpty { listOf("Read carefully", "Think about it", "Answer") }
        )
    }

    private fun buildValidationPrompt(
        question: String,
        correctAnswer: String,
        userAnswer: String,
        age: String,
        disorderType: String?
    ): String = """
        You are a kind, patient special education teacher working with a $age-year-old child with ${disorderType ?: "learning needs"}.

        Question: $question
        Expected Answer: $correctAnswer
        Child's Answer: $userAnswer

        TASK:
        Evaluate if the child's answer is correct (conceptual match is enough).
        Provide encouraging feedback appropriate for a child with special needs.

        FORMAT EXACTLY AS:
        CORRECT: [YES or NO]
        FEEDBACK: [One encouraging sentence]
        ENCOURAGEMENT: [Positive reinforcement]
        HINT: [If wrong, a gentle hint]
    """.trimIndent()

    private fun parseValidationResponse(responseText: String): AnswerValidation {
        val isCorrect = responseText.substringAfter("CORRECT:", "").substringBefore("FEEDBACK:", "").trim()
            .equals("YES", ignoreCase = true)
        val feedback = responseText.substringAfter("FEEDBACK:", "").substringBefore("ENCOURAGEMENT:", "").trim()
        val encouragement = responseText.substringAfter("ENCOURAGEMENT:", "").substringBefore("HINT:", "").trim()
        val hint = responseText.substringAfter("HINT:", "").trim()

        if (feedback.isEmpty() || encouragement.isEmpty()) throw Exception("Incomplete validation parsing")

        return AnswerValidation(isCorrect, feedback, encouragement, hint)
    }
}

data class LessonContent(
    val content: String,
    val question: String,
    val answer: String,
    val answerType: AnswerType,
    val options: List<OptionModel>,
    val matchPairs: List<MatchPairModel>,
    val howToSteps: List<String>
)

data class AnswerValidation(
    val isCorrect: Boolean,
    val feedback: String,
    val encouragement: String,
    val hint: String
)