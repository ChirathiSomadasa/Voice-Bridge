package com.chirathi.voicebridge

import kotlin.math.max
import kotlin.math.min

/**
 * PersonalizedContentSelector — Single source of truth for all content decisions.
 */
object PersonalizedContentSelector {

    // ── Layout types ────────────────────────────────────────────────────────

    enum class MoodLayout {
        TWO_BUTTON,   // 2 large image buttons
        FOUR_BUTTON,  // 4 grid buttons
        SCENARIO      // scenario image + 4 emotion choices
    }

    // ── Content pool descriptors ─────────────────────────────────────────────

    private data class EmotionPool(
        val correctPool:     List<String>,
        val distractorTiers: Map<Int, List<String>>
    )

    // ── Age 6-7 pools ────────────────────────────────────────────────────────

    private val A6_T0 = EmotionPool(
        correctPool = listOf(
            "mood_lvl0_shared_happy",
            "mood_lvl0_shared_sad",
            "mood_lvl0_shared_angry",
            "mood_lvl0_shared_bored",
            "mood_lvl0_shared_shy"
        ),
        distractorTiers = mapOf(
            0 to listOf("mood_lvl0_shared_bored",   "mood_lvl0_shared_shy"),
            1 to listOf("mood_lvl0_shared_happy",   "mood_lvl0_shared_bored"),
            2 to listOf("mood_lvl0_shared_sad",     "mood_lvl0_shared_angry"),
            3 to listOf("mood_lvl0_shared_sad",     "mood_lvl0_shared_shy"),
            4 to listOf("mood_lvl0_shared_sad",     "mood_lvl0_shared_angry", "mood_lvl0_shared_bored")
        )
    )

    private val A6_T1 = EmotionPool(
        correctPool = listOf(
            "mood_lvl1_shared_scared",
            "mood_lvl1_shared_proud",
            "mood_lvl1_shared_sleepy"
        ),
        distractorTiers = mapOf(
            0 to listOf("mood_lvl0_shared_bored",  "mood_lvl0_shared_sad"),
            1 to listOf("mood_lvl1_shared_scared", "mood_lvl0_shared_sad"),
            2 to listOf("mood_lvl1_shared_scared", "mood_lvl0_shared_angry"),
            3 to listOf("mood_lvl0_shared_sad",    "mood_lvl1_shared_scared"),
            4 to listOf("mood_lvl0_shared_sad", "mood_lvl0_shared_angry", "mood_lvl1_shared_scared")
        )
    )

    private val A6_SCENARIOS = listOf(
        "mood_lvl2_age6_birthday"         to "mood_lvl0_shared_happy",
        "mood_lvl2_age6_dropped_icecream" to "mood_lvl0_shared_sad",
        "mood_lvl2_age6_broken_robot"     to "mood_lvl0_shared_angry",
        "mood_lvl2_age6_dog_barking"      to "mood_lvl1_shared_scared",
        "mood_lvl2_age6_bored"            to "mood_lvl0_shared_bored"
    )

    // ── Age 8-10 pools ───────────────────────────────────────────────────────

    private val A8_T0 = EmotionPool(
        correctPool = listOf(
            "mood_lvl0_shared_happy",
            "mood_lvl0_age8_cheerful",
            "mood_lvl0_shared_bored",
            "mood_lvl0_age8_surprise",
            "mood_lvl0_age8_tired"
        ),
        distractorTiers = mapOf(
            0 to listOf("mood_lvl0_shared_bored",  "mood_lvl0_age8_tired"),
            1 to listOf("mood_lvl0_shared_happy",  "mood_lvl0_age8_cheerful"),
            2 to listOf("mood_lvl0_shared_happy",  "mood_lvl0_age8_cheerful", "mood_lvl0_age8_surprise"),
            3 to listOf("mood_lvl0_age8_tired",    "mood_lvl0_shared_bored"),
            4 to listOf("mood_lvl0_shared_happy",  "mood_lvl0_age8_cheerful", "mood_lvl0_age8_surprise")
        )
    )

    private val A8_T1 = EmotionPool(
        correctPool = listOf(
            "mood_lvl1_age8_anxious",
            "mood_lvl1_age8_greedy",
            "mood_lvl1_age8_jealous",
            "mood_lvl1_age8_curious",
            "mood_lvl1_age8_disgusted"
        ),
        distractorTiers = mapOf(
            0 to listOf("mood_lvl0_shared_bored",   "mood_lvl0_shared_happy"),
            1 to listOf("mood_lvl1_age8_anxious",   "mood_lvl1_shared_scared"),
            2 to listOf("mood_lvl1_age8_anxious",   "mood_lvl1_age8_jealous"),
            3 to listOf("mood_lvl1_age8_anxious",   "mood_lvl1_age8_disgusted"),
            4 to listOf("mood_lvl1_age8_anxious",   "mood_lvl1_age8_jealous", "mood_lvl1_age8_disgusted")
        )
    )

    private val A8_SCENARIOS = listOf(
        "mood_lvl2_age8_proud"        to "mood_lvl1_shared_proud",
        "mood_lvl2_age8_jealous"      to "mood_lvl1_age8_jealous",
        "mood_lvl2_age8_anxious"      to "mood_lvl1_age8_anxious",
        "mood_lvl2_age8_greedy"       to "mood_lvl1_age8_greedy",
        "mood_lvl2_age8_rotten_apple" to "mood_lvl1_age8_disgusted"
    )

    // ── Mood Match public API ────────────────────────────────────────────────

    data class MoodRoundSpec(
        val correctEmotionDrawable: String,
        val correctEmotionWord:     String,
        val distractors:            List<String>,
        val distractorWords:        List<String>,
        val numberOfOptions:        Int,
        val layout:                 MoodLayout,
        val hintLevel:              Int,
        val isScenario:             Boolean,
        val scenarioImageDrawable:  String = "",
        val timePressureMs:         Long,
        val difficultyLabel:        String
    )

    /**
     * @param difficultyState: 0=2-opt Easy, 1=2-opt Med, 2=4-opt Easy, 3=4-opt Med, 4=4-opt Hard (Synonyms)
     */
    fun selectMoodRound(
        ageGroup:           Int,
        prediction:         Prediction,
        difficultyState:    Int,
        recentEmotions:     Set<String>,
        sessionEmotions: Set<String>
    ): MoodRoundSpec {

        val isYoung = ageGroup <= 7
        val isScenario = (prediction.optimalDifficulty == 3 && difficultyState >= 2)

        // Strict mapping to difficulty state
        val pool = when(difficultyState) {
            0, 2 -> if (isYoung) A6_T0 else A8_T0
            else -> if (isYoung) A6_T1 else A8_T1
        }

        // Layout options directly tied to State Matrix
        val nOptions = if (difficultyState >= 2) 4 else 2
        val useSynonyms = (difficultyState == 4)

        //  1. Correct emotion (Avoid repeating recent ones)
        val correct: String
        val scenarioImage: String

        if (isScenario) {
            val scenariosToUse = if (isYoung) A6_SCENARIOS else A8_SCENARIOS
            var available = scenariosToUse.filter { it.second !in sessionEmotions }

            val betterAvailable = available.filter { it.second !in recentEmotions }
            if (betterAvailable.isNotEmpty()) available = betterAvailable

            // If pool is totally empty (shouldn't happen with 5 rounds), fallback to anything but the last one
            if (available.isEmpty()) available = scenariosToUse.filter { it.second != recentEmotions.lastOrNull() }

            val pair = available.random()
            scenarioImage = pair.first
            correct = pair.second
        } else {
            var available = pool.correctPool.filter { it !in sessionEmotions }
            val betterAvailable = available.filter { it !in recentEmotions }

            if (available.isEmpty()) {
                available = pool.correctPool.filter { it !in sessionEmotions }
            }
            if (available.isEmpty()) available = pool.correctPool

            correct = available.random()
            scenarioImage = ""
        }

        // ── 2. Unique Distractors & Vocabulary ─
        val dtype = prediction.distractor.coerceIn(0, 4)
        val dPool = (pool.distractorTiers[dtype] ?: pool.distractorTiers[0]!!).distinct()

        val correctBase = correct.split("_").last().lowercase()
        val distractors = buildUniqueDistractorList(dPool, pool.correctPool, correctBase, nOptions - 1)

        // Track used words so we never have "Tired" and "Exhausted" in the same round
        val usedWords = mutableSetOf<String>()
        val correctWord = resolveEmotionWord(correctBase, useSynonyms, nOptions, usedWords)
        usedWords.add(correctWord)

        val distWords = distractors.map { dist ->
            val base = dist.split("_").last().lowercase()
            val word = resolveEmotionWord(base, useSynonyms, nOptions, usedWords)
            usedWords.add(word)
            word
        }

//        val layout = if (isScenario) MoodLayout.SCENARIO else if (nOptions == 4) MoodLayout.FOUR_BUTTON else MoodLayout.TWO_BUTTON
        val layout = when {
            isScenario -> MoodLayout.SCENARIO
            nOptions == 4 -> MoodLayout.FOUR_BUTTON
            else -> MoodLayout.TWO_BUTTON
        }

        val timePressureMs = when (prediction.timePressure) {
            1 -> prediction.latencyBufferMs + 3000L
            2 -> prediction.latencyBufferMs
            else -> 0L
        }

        return MoodRoundSpec(
            correctEmotionDrawable = correct,
            correctEmotionWord     = correctWord,
            distractors            = distractors,
            distractorWords        = distWords,
            numberOfOptions        = nOptions,
            layout                 = layout,
            hintLevel              = prediction.optimalHint,
            isScenario             = isScenario,
            scenarioImageDrawable  = scenarioImage,
            timePressureMs         = timePressureMs,
            difficultyLabel        = "State:$difficultyState-dist$dtype"
        )
    }

    // Resolves words and prevents duplicate conceptual synonyms in the same round
    private fun resolveEmotionWord(baseEmotion: String, useSynonym: Boolean, nOptions: Int, usedWords: Set<String>): String {
        if (!useSynonym || nOptions < 4) {
            val standard = baseEmotion.replaceFirstChar { it.uppercase() }
            return standard
        }

        val options = when (baseEmotion) {
            "happy", "cheerful" -> listOf("Glad", "Joyful", "Cheerful", "Delighted", "Pleased")
            "sad"    -> listOf("unhappy", "Upset", "Gloomy", "Sorrowful")
            "angry"  -> listOf("Mad", "Fuming", "Furious", "Raging")
            "scared", "anxious" -> listOf("Afraid", "Fearful", "Nervous", "Worried")
            "bored"  -> listOf("Fed-up", "Uninterested", "Weary")
            "shy"    -> listOf("Coy", "Timid")
            "proud"  -> listOf("Proud", "Honored", "Prideful")
            "sleepy", "tired" -> listOf("Exhausted", "Drowsy", "Tired")
            "surprise" -> listOf("Amazed", "Shocked", "Startled")
            "jealous" -> listOf("Envious")
            "disgusted" -> listOf("grossed", "displeased")
            "greedy" -> listOf("Selfish")
            "curious" -> listOf( "Interested")
            else -> listOf(baseEmotion.replaceFirstChar { it.uppercase() })
        }

        // Return a distinct synonym that hasn't been placed on the screen yet
        return options.shuffled().firstOrNull { it !in usedWords } ?: options.first()
    }

    // Guarantees no two distractors share the same base emotion as the correct answer or each other
    private fun buildUniqueDistractorList(
        dPool:       List<String>,
        correctPool: List<String>,
        correctBase: String,
        needed:      Int
    ): List<String> {
        val result = mutableListOf<String>()
        val usedBases = mutableSetOf(correctBase)

        // Combine base mappings to treat concepts like 'tired' and 'sleepy' as the same base
        fun normalizeBase(base: String): String = when(base) {
            "sleepy", "tired" -> "exhaustion_group"
            "happy", "cheerful" -> "happy_group"
            "scared", "anxious" -> "fear_group"
            else -> base
        }
        usedBases.add(normalizeBase(correctBase))

        for (d in dPool.shuffled()) {
            if (result.size >= needed) break
            val base = normalizeBase(d.split("_").last().lowercase())
            if (usedBases.add(base)) {
                result.add(d)
            }
        }

        if (result.size < needed) {
            for (ext in correctPool.shuffled()) {
                if (result.size >= needed) break
                val base = normalizeBase(ext.split("_").last().lowercase())
                if (usedBases.add(base)) {
                    result.add(ext)
                }
            }
        }
        return result
    }

    fun resolveFriendAction(modelFrustRisk: Float, tracker: SessionStateTracker): Int {
        // Enforced strictly as active co-regulation support triggered by frustration
        val isComfortNeeded = tracker.frustration >= 0.70f || modelFrustRisk >= 0.75f || tracker.peakConsecWrong >= 2

        if (!isComfortNeeded) return 0
        return listOf(1, 2, 5, 9).random()
    }

    // ── Hint spec builder ────────────────────────────────────────────────────
    data class HintSpec(val shouldSpeak: Boolean, val speechText: String, val shouldPulse: Boolean, val shouldReveal: Boolean)

    fun buildHint(hintLevel: Int, emotionName: String, isYoung: Boolean): HintSpec = when (hintLevel) {
        0 -> HintSpec(true, if (isYoung) "Look carefully..." else "Take another look...", true, false)
        1 -> HintSpec(true, if (isYoung) "Look at the face again..." else "Think about the eyes and the mouth.", true, false)
        else -> HintSpec(true, if (emotionName.isNotBlank()) "It's $emotionName!" else "Look at the glowing option!", true, true)
    }

    // STATE MACHINE: Manages exactly when to upgrade or downgrade the difficulty layout
    fun getNewDifficultyState(currentState: Int, streakFirstTryCorrect: Int, streakFirstTryWrong: Int): Int {
        var newState = currentState
        if (streakFirstTryCorrect >= 2) {
            newState = min(4, currentState + 1)
        } else if (streakFirstTryWrong >= 2) {
            newState = max(0, currentState - 1)
        }
        return newState
    }

    // ── Rhythm & Sequence logic remains same ────────────────────────────────

    private data class SongEntry(val title: String, val complexity: Float)
    private val SONGS = listOf(
        SongEntry("Row Row Row Your Boat",       0.25f),
        SongEntry("Twinkle Twinkle Little Star", 0.55f),
        SongEntry("Jack and Jill",               0.75f)
    )

    fun selectSong(rhythmComplexity: Float, playCountMap: Map<String,Int> = emptyMap()): String =
        SONGS.minByOrNull { song ->
            val complexityFit = Math.abs(song.complexity - rhythmComplexity)
            val repetitionPenalty = (playCountMap[song.title] ?: 0) * 0.15f
            complexityFit + repetitionPenalty
        }?.title ?: "Row Row Row Your Boat"

    data class SequenceRoundSpec(
        val hintLevel:       Int,
        val hiddenStepIndex: Int,
        val miniGameType:    Int,
        val timePressureMs:  Long,
        val label:           String
    )

    fun selectSequenceRound(ageGroup: Int, prediction: Prediction): SequenceRoundSpec {
        val miniGameType = when {
            ageGroup <= 7                -> 0
            prediction.connectDotsTrigger -> 1
            else                         -> 0
        }
        val timePressureMs: Long = when (prediction.timePressure) {
            1    -> prediction.latencyBufferMs + 3000L
            2    -> prediction.latencyBufferMs
            else -> 0L
        }
        return SequenceRoundSpec(
            hintLevel       = prediction.optimalHint,
            hiddenStepIndex = prediction.hiddenStepIndex.coerceIn(0, 2),
            miniGameType    = miniGameType,
            timePressureMs  = timePressureMs,
            label           = "sub${prediction.subRoutine}-h${prediction.optimalHint}-hidden${prediction.hiddenStepIndex}"
        )
    }
}