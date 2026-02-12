package com.chirathi.voicebridge

import android.content.Context
import com.chirathi.voicebridge.ml.GameMaster
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.Serializable
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

// =========== DATA CLASSES ===========
data class DistractorSet(
    val phonetic: List<String>,  // Rhyming words
    val semantic: List<String>,  // Similar meaning
    val visual: List<String>,    // Similar appearance
    val random: List<String>     // No association
)

data class ModelDecision(
    val emotionLevel: Int,   // 0=Basic, 1=Context, 2=Scenario
    val friendAction: Int,   // 0-9 (Virtual Friend Item)
    val sequenceAction: Int, // 0=None, 1=Visual, 2=Auditory, 3=None (for hints)
    val motivationId: Int,   // 0=Stars, 1=Quote, 2=Anim
    val rhythmComplexity: Float, // 0.0 - 1.0
    val routineAction: Int,  // 0=Morning, 1=Bed, 2=School, 3=Play
    val subRoutineRecommendation: Int, // 0=Repeat, 1=Stay, 2=Progress
    val therapeuticIntent: TherapeuticIntent,
    val recommendedLevel: Int = 1, // 1-4 difficulty level
    val distractorStrategy: DistractorStrategy // NEW: How to choose wrong options
): Serializable

data class DistractorStrategy(
    val type: String, // "random", "phonetic", "semantic", "visual", "mixed"
    val difficulty: Float, // 0.1-1.0
    val confidenceThreshold: Float, // 0.0-1.0
    val useDiagnosticDistractors: Boolean // Whether to use targeted distractors
): Serializable

data class TherapeuticIntent(
    val primaryGoal: String, // "reduce_cognitive_load", "build_confidence", "improve_attention", "challenge_mastery"
    val uiComplexity: Float, // 0.1-1.0 (controls UI simplicity)
    val sessionDuration: Int, // Recommended session length in minutes
    val adaptiveScaling: Boolean, // Whether to adapt difficulty mid-session
    val recommendedLevel: Int = 1,
    val distractorProfile: String = "balanced" // "easy", "balanced", "challenging"
): Serializable

data class BehavioralProfile(
    val jitter: Float, // Motor variance
    val hesitation: Float, // Cognitive latency
    val accuracy: Float,
    val responseTime: Float,
    val errorPattern: List<Int>, // Pattern of errors (1=phonetic, 2=semantic, 3=visual, 4=random)
    val engagementScore: Float,
    val frustrationLevel: Float,
    val processingSpeed: Float,
    val currentDifficultyLevel: Int = 1,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val dominantErrorType: Int = 0, // NEW: What kind of errors they make most
    val learningRate: Float = 0.5f // NEW: How fast they improve
): Serializable

data class SessionMetrics(
    val startTime: Long,
    var interactionCount: Int = 0,
    var correctCount: Int = 0,
    var errorCount: Int = 0,
    val avgResponseTime: Float = 0f,
    val behavioralProfile: BehavioralProfile? = null,
    val therapeuticIntent: TherapeuticIntent? = null,
    val levelProgression: List<Int> = emptyList(), // NEW: Track level changes
    val strategyChanges: List<String> = emptyList() // NEW: Track strategy changes
): Serializable

class GameMasterModel(context: Context) {
    private var interpreter: Interpreter? = null
    private val TAG = "GameMasterModel"

    // Maps to store which index corresponds to which output head
    private var idxEmo = -1
    private var idxFrn = -1
    private var idxSeq = -1
    private var idxRhy = -1
    private var idxRtn = -1
    private var idxMot = -1
    private var idxSub = -1
    private var idxIntent = -1
    private var idxLevel = -1 // NEW: Model predicts difficulty level
    private var idxStrategy = -1 // NEW: Model predicts distractor strategy

    // Behavioral tracking
    private val sessionMetrics = SessionMetrics(System.currentTimeMillis())
    private val responseTimes = mutableListOf<Long>()
    private val touchCoordinates = mutableListOf<Pair<Float, Float>>()
    private val errorPatterns = mutableListOf<Int>()
    private val levelHistory = mutableListOf<Int>()
    private var engagementStartTime: Long = 0

    // Therapeutic profile cache
    private var currentTherapeuticIntent: TherapeuticIntent? = null
    private var currentBehavioralProfile: BehavioralProfile? = null
    private var currentDistractorStrategy: DistractorStrategy? = null

    // =========== COMPREHENSIVE DIAGNOSTIC METADATA ===========
    private val diagnosticMetadata = mapOf(
        // ===== ROW ROW ROW YOUR BOAT =====
        "boat" to DistractorSet(
            phonetic = listOf("coat", "goat", "float", "moat", "note", "vote"),
            semantic = listOf("anchor", "sail", "oar", "lifejacket", "ship", "vessel"),
            visual = listOf("canoe", "kayak", "raft", "dinghy"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "stream" to DistractorSet(
            phonetic = listOf("dream", "cream", "beam", "team", "seam"),
            semantic = listOf("river", "creek", "brook", "canal", "tributary"),
            visual = listOf("wave", "waterfall", "rapids", "flow"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "dream" to DistractorSet(
            phonetic = listOf("stream", "cream", "beam", "team", "seam"),
            semantic = listOf("nightmare", "fantasy", "vision", "thought", "imagination"),
            visual = listOf("cloud", "sleep", "pillow", "moon"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "creek" to DistractorSet(
            phonetic = listOf("squeak", "beak", "leak", "peak", "weak"),
            semantic = listOf("stream", "brook", "river", "tributary", "runlet"),
            visual = listOf("water", "flow", "small river", "canal"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "mouse" to DistractorSet(
            phonetic = listOf("house", "louse", "douse", "grouse", "spouse"),
            semantic = listOf("rat", "rodent", "hamster", "gerbil", "squeak"),
            visual = listOf("cat", "cheese", "hole", "tiny"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "river" to DistractorSet(
            phonetic = listOf("shiver", "liver", "giver", "quiver", "deliver"),
            semantic = listOf("stream", "creek", "brook", "canal", "waterway"),
            visual = listOf("lake", "ocean", "sea", "pond"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "polar bear" to DistractorSet(
            phonetic = listOf("olar bear", "solar bear", "polar chair", "polar care"),
            semantic = listOf("ice bear", "white bear", "arctic bear", "ursus maritimus"),
            visual = listOf("penguin", "seal", "snow", "iceberg"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "crocodile" to DistractorSet(
            phonetic = listOf("crocodile", "clockodile", "rockodile", "mockodile"),
            semantic = listOf("alligator", "reptile", "caiman", "gavial"),
            visual = listOf("lizard", "snake", "turtle", "dinosaur"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),

        // ===== TWINKLE TWINKLE LITTLE STAR =====
        "star" to DistractorSet(
            phonetic = listOf("car", "far", "bar", "jar", "tar", "scar"),
            semantic = listOf("moon", "planet", "comet", "galaxy", "sun", "asteroid"),
            visual = listOf("diamond", "sparkle", "light", "twinkle"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "world" to DistractorSet(
            phonetic = listOf("whirled", "hurled", "curled", "swirled", "furled"),
            semantic = listOf("earth", "globe", "planet", "universe", "sphere"),
            visual = listOf("map", "globe", "atlas", "continent"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "diamond" to DistractorSet(
            phonetic = listOf("diamond", "lemon", "demon", "common", "salmon"),
            semantic = listOf("gem", "jewel", "crystal", "sparkle", "precious stone"),
            visual = listOf("square", "circle", "triangle", "oval", "rectangle"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "sun" to DistractorSet(
            phonetic = listOf("run", "fun", "bun", "gun", "nun", "done"),
            semantic = listOf("star", "solar", "light", "day", "bright"),
            visual = listOf("moon", "planet", "sky", "cloud"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "light" to DistractorSet(
            phonetic = listOf("night", "bright", "sight", "fight", "kite", "might"),
            semantic = listOf("illumination", "brightness", "glow", "shine", "radiance"),
            visual = listOf("lamp", "bulb", "candle", "flashlight"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "night" to DistractorSet(
            phonetic = listOf("light", "knight", "bite", "sight", "fight", "might"),
            semantic = listOf("evening", "dark", "midnight", "dusk", "twilight"),
            visual = listOf("moon", "stars", "dark sky", "bedtime"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "traveller" to DistractorSet(
            phonetic = listOf("traveler", "graveler", "leveler", "shoveler"),
            semantic = listOf("voyager", "explorer", "tourist", "journeyer", "wanderer"),
            visual = listOf("backpack", "compass", "map", "suitcase"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "dark blue sky" to DistractorSet(
            phonetic = listOf("dark blue sky", "park blue sky", "mark blue sky", "lark blue sky"),
            semantic = listOf("night sky", "evening", "midnight", "dusk"),
            visual = listOf("stars", "moon", "clouds", "galaxy"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "window" to DistractorSet(
            phonetic = listOf("windo", "widow", "willow", "pillow", "bellow"),
            semantic = listOf("glass", "pane", "opening", "frame", "transparent"),
            visual = listOf("door", "wall", "curtain", "house"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "eyes" to DistractorSet(
            phonetic = listOf("eyes", "lies", "pies", "ties", "dies", "flies"),
            semantic = listOf("vision", "sight", "eyeballs", "peepers", "optics"),
            visual = listOf("nose", "ears", "mouth", "face"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),

        // ===== JACK AND JILL =====
        "hill" to DistractorSet(
            phonetic = listOf("pill", "mill", "fill", "will", "bill", "dill"),
            semantic = listOf("mountain", "slope", "elevation", "climb", "peak"),
            visual = listOf("valley", "plain", "field", "land"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "water" to DistractorSet(
            phonetic = listOf("daughter", "otter", "quarter", "slaughter", "porter"),
            semantic = listOf("h2o", "liquid", "aqua", "fluid", "drink"),
            visual = listOf("river", "lake", "ocean", "rain"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        ),
        "crown" to DistractorSet(
            phonetic = listOf("brown", "clown", "drown", "frown", "gown"),
            semantic = listOf("tiara", "headpiece", "royalty", "king", "queen"),
            visual = listOf("hat", "cap", "helmet", "headband"),
            random = listOf("apple", "ball", "car", "house", "tree", "book")
        )
    )

    // Default distractor set for keywords not in metadata
    private val defaultDistractorSet = DistractorSet(
        phonetic = listOf("coat", "goat", "float", "note"),
        semantic = listOf("item", "object", "thing", "piece"),
        visual = listOf("square", "circle", "triangle", "oval"),
        random = listOf("apple", "ball", "car", "house")
    )

    init {
        try {
            exploreAssets(context)
            val modelFile = loadModelFile(context)
            interpreter = Interpreter(modelFile)
            Log.d(TAG, "✅ Model loaded successfully")
            inspectOutputTensors()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading model: ${e.message}")
        }
    }

    // =========== MODEL-BASED DISTRACTOR GENERATION ===========

    fun getDistractorSet(keyword: String): DistractorSet {
        return diagnosticMetadata[keyword.lowercase()] ?: defaultDistractorSet
    }

    fun generateDistractorsByStrategy(
        keyword: String,
        strategy: DistractorStrategy,
        count: Int = 3
    ): List<Pair<String, Int>> {
        val distractorSet = getDistractorSet(keyword)
        val distractors = mutableListOf<Pair<String, Int>>()

        Log.d(TAG, "🎯 Generating distractors for '$keyword' with strategy: ${strategy.type}")

        when (strategy.type) {
            "random" -> {
                // All random - easiest
                distractors.addAll(getRandomDistractors(distractorSet.random, count))
            }
            "phonetic" -> {
                // Mix of phonetic and random
                val phoneticCount = (count * strategy.difficulty).toInt().coerceIn(1, count)
                val randomCount = count - phoneticCount
                distractors.addAll(getPhoneticDistractors(distractorSet.phonetic, phoneticCount))
                distractors.addAll(getRandomDistractors(distractorSet.random, randomCount))
            }
            "semantic" -> {
                // Mix of semantic and random
                val semanticCount = (count * strategy.difficulty).toInt().coerceIn(1, count)
                val randomCount = count - semanticCount
                distractors.addAll(getSemanticDistractors(distractorSet.semantic, semanticCount))
                distractors.addAll(getRandomDistractors(distractorSet.random, randomCount))
            }
            "visual" -> {
                // Mix of visual and random
                val visualCount = (count * strategy.difficulty).toInt().coerceIn(1, count)
                val randomCount = count - visualCount
                distractors.addAll(getVisualDistractors(distractorSet.visual, visualCount))
                distractors.addAll(getRandomDistractors(distractorSet.random, randomCount))
            }
            "mixed" -> {
                // Balanced mix of all types - most challenging
                val perType = (count / 3).coerceAtLeast(1)
                distractors.addAll(getPhoneticDistractors(distractorSet.phonetic, perType))
                distractors.addAll(getSemanticDistractors(distractorSet.semantic, perType))
                distractors.addAll(getVisualDistractors(distractorSet.visual, perType))
                // Fill remaining with random if needed
                if (distractors.size < count) {
                    distractors.addAll(getRandomDistractors(distractorSet.random, count - distractors.size))
                }
            }
            "diagnostic" -> {
                // Target specific weakness based on error pattern
                val dominantError = currentBehavioralProfile?.dominantErrorType ?: 4
                when (dominantError) {
                    1 -> { // Phonetic errors - practice with more phonetic distractors
                        distractors.addAll(getPhoneticDistractors(distractorSet.phonetic, 2))
                        distractors.addAll(getRandomDistractors(distractorSet.random, 1))
                    }
                    2 -> { // Semantic errors - practice with more semantic distractors
                        distractors.addAll(getSemanticDistractors(distractorSet.semantic, 2))
                        distractors.addAll(getRandomDistractors(distractorSet.random, 1))
                    }
                    3 -> { // Visual errors - practice with more visual distractors
                        distractors.addAll(getVisualDistractors(distractorSet.visual, 2))
                        distractors.addAll(getRandomDistractors(distractorSet.random, 1))
                    }
                    else -> { // Random errors - mix it up
                        distractors.addAll(getPhoneticDistractors(distractorSet.phonetic, 1))
                        distractors.addAll(getSemanticDistractors(distractorSet.semantic, 1))
                        distractors.addAll(getVisualDistractors(distractorSet.visual, 1))
                    }
                }
            }
            else -> {
                // Default to balanced
                distractors.addAll(getPhoneticDistractors(distractorSet.phonetic, 1))
                distractors.addAll(getSemanticDistractors(distractorSet.semantic, 1))
                distractors.addAll(getRandomDistractors(distractorSet.random, 1))
            }
        }

        // Shuffle and limit to count
        return distractors.shuffled().take(count)
    }

    fun generateDistractorsByLevel(
        keyword: String,
        level: Int,
        count: Int = 3
    ): List<Pair<String, Int>> {
        // Convert level to appropriate strategy
        val strategy = when (level) {
            1 -> DistractorStrategy("random", 0.3f, 0.7f, false)
            2 -> DistractorStrategy("phonetic", 0.5f, 0.6f, true)
            3 -> DistractorStrategy("mixed", 0.7f, 0.5f, true)
            4 -> DistractorStrategy("diagnostic", 0.9f, 0.4f, true)
            else -> DistractorStrategy("balanced", 0.5f, 0.6f, true)
        }

        return generateDistractorsByStrategy(keyword, strategy, count)
    }

    private fun getPhoneticDistractors(phoneticList: List<String>, count: Int): List<Pair<String, Int>> {
        return phoneticList.shuffled().take(count).map { word ->
            Pair(word, KeywordImageMapper.getImageResource(word))
        }
    }

    private fun getSemanticDistractors(semanticList: List<String>, count: Int): List<Pair<String, Int>> {
        return semanticList.shuffled().take(count).map { word ->
            Pair(word, KeywordImageMapper.getImageResource(word))
        }
    }

    private fun getVisualDistractors(visualList: List<String>, count: Int): List<Pair<String, Int>> {
        return visualList.shuffled().take(count).map { word ->
            Pair(word, KeywordImageMapper.getImageResource(word))
        }
    }

    private fun getRandomDistractors(randomList: List<String>, count: Int): List<Pair<String, Int>> {
        return randomList.shuffled().take(count).map { word ->
            Pair(word, KeywordImageMapper.getImageResource(word))
        }
    }

    // =========== MODEL-BASED PREDICTION ===========

    fun predictWithBehavioralContext(inputFeatures: FloatArray): ModelDecision {
        val profile = currentBehavioralProfile

        // Augment input features with behavioral data
        val augmentedFeatures = if (profile != null) {
            val behavioralFeatures = floatArrayOf(
                profile.jitter,
                profile.hesitation,
                profile.accuracy,
                profile.responseTime / 1000f,
                profile.frustrationLevel,
                profile.engagementScore,
                profile.processingSpeed,
                profile.currentDifficultyLevel.toFloat(),
                profile.consecutiveCorrect.toFloat(),
                profile.consecutiveWrong.toFloat(),
                profile.dominantErrorType.toFloat(),
                profile.learningRate
            )
            inputFeatures + behavioralFeatures
        } else {
            inputFeatures
        }

        val baseDecision = predict(augmentedFeatures)

        // Override with model's recommended level and strategy
        val recommendedLevel = baseDecision.recommendedLevel
        val distractorStrategy = baseDecision.distractorStrategy

        // Update therapeutic intent based on behavioral state
        val therapeuticIntent = determineTherapeuticIntent(profile, baseDecision)

        return baseDecision.copy(
            therapeuticIntent = therapeuticIntent,
            recommendedLevel = recommendedLevel,
            distractorStrategy = distractorStrategy
        )
    }

    fun predict(inputFeatures: FloatArray): ModelDecision {
        if (interpreter == null) return getDefaultDecision(inputFeatures)

        val featureCount = max(inputFeatures.size, 16)
        val paddedFeatures = if (inputFeatures.size < featureCount) {
            inputFeatures + FloatArray(featureCount - inputFeatures.size)
        } else inputFeatures

        val inputs = arrayOf(paddedFeatures)
        val outputs = mutableMapOf<Int, Any>()

        // Allocate outputs for all tensor heads
        if (idxEmo != -1) outputs[idxEmo] = Array(2) { FloatArray(3) }
        if (idxFrn != -1) outputs[idxFrn] = Array(2) { FloatArray(10) }
        if (idxSeq != -1) outputs[idxSeq] = Array(2) { FloatArray(3) }
        if (idxRhy != -1) outputs[idxRhy] = Array(2) { FloatArray(1) }
        if (idxRtn != -1) outputs[idxRtn] = Array(2) { FloatArray(4) }
        if (idxMot != -1) outputs[idxMot] = Array(2) { FloatArray(3) }
        if (idxSub != -1) outputs[idxSub] = Array(2) { FloatArray(2) }
        if (idxIntent != -1) outputs[idxIntent] = Array(2) { FloatArray(5) }
        if (idxLevel != -1) outputs[idxLevel] = Array(2) { FloatArray(4) } // 4 levels
        if (idxStrategy != -1) outputs[idxStrategy] = Array(2) { FloatArray(6) } // 6 strategies

        try {
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return getDefaultDecision(inputFeatures)
        }

        // Parse model outputs
        val emotionLevel = if (idxEmo != -1) argMax((outputs[idxEmo] as Array<FloatArray>)[0]) else 0
        val friendAction = if (idxFrn != -1) argMax((outputs[idxFrn] as Array<FloatArray>)[0]) else 0
        val sequenceAction = if (idxSeq != -1) argMax((outputs[idxSeq] as Array<FloatArray>)[0]) else 1
        val motivationId = if (idxMot != -1) argMax((outputs[idxMot] as Array<FloatArray>)[0]) else 0
        val rhythmComplexity = if (idxRhy != -1) (outputs[idxRhy] as Array<FloatArray>)[0][0].coerceIn(0f, 1f) else 0.5f
        val routineAction = if (idxRtn != -1) argMax((outputs[idxRtn] as Array<FloatArray>)[0]) else 0

        // Model-predicted level (1-4)
        val recommendedLevel = if (idxLevel != -1) {
            argMax((outputs[idxLevel] as Array<FloatArray>)[0]) + 1
        } else {
            calculateRecommendedLevel(currentBehavioralProfile?.currentDifficultyLevel ?: 1)
        }

        // Model-predicted distractor strategy
        val distractorStrategy = if (idxStrategy != -1) {
            val strategyIdx = argMax((outputs[idxStrategy] as Array<FloatArray>)[0])
            getStrategyFromIndex(strategyIdx, recommendedLevel)
        } else {
            getDefaultStrategy(recommendedLevel)
        }

        val subRoutineRecommendation = if (idxSub != -1) {
            argMax((outputs[idxSub] as Array<FloatArray>)[0])
        } else {
            calculateSubRoutineRecommendation(
                sequenceAction,
                inputFeatures.getOrNull(5) ?: 0f,
                inputFeatures.getOrNull(7)?.toInt() ?: 0
            )
        }

        val therapeuticIntent = if (idxIntent != -1) {
            val intentIdx = argMax((outputs[idxIntent] as Array<FloatArray>)[0])
            getIntentFromIndex(intentIdx, recommendedLevel)
        } else {
            TherapeuticIntent(
                "build_confidence",
                0.5f,
                10,
                true,
                recommendedLevel,
                "balanced"
            )
        }

        return ModelDecision(
            emotionLevel = emotionLevel,
            friendAction = friendAction,
            sequenceAction = sequenceAction,
            motivationId = motivationId,
            rhythmComplexity = rhythmComplexity,
            routineAction = routineAction,
            subRoutineRecommendation = subRoutineRecommendation,
            therapeuticIntent = therapeuticIntent,
            recommendedLevel = recommendedLevel,
            distractorStrategy = distractorStrategy
        )
    }

    private fun getStrategyFromIndex(index: Int, level: Int): DistractorStrategy {
        return when(index) {
            0 -> DistractorStrategy("random", 0.3f, 0.7f, false)
            1 -> DistractorStrategy("phonetic", 0.5f, 0.6f, true)
            2 -> DistractorStrategy("semantic", 0.6f, 0.5f, true)
            3 -> DistractorStrategy("visual", 0.6f, 0.5f, true)
            4 -> DistractorStrategy("mixed", 0.8f, 0.4f, true)
            5 -> DistractorStrategy("diagnostic", 0.9f, 0.3f, true)
            else -> getDefaultStrategy(level)
        }
    }

    private fun getDefaultStrategy(level: Int): DistractorStrategy {
        return when(level) {
            1 -> DistractorStrategy("random", 0.3f, 0.7f, false)
            2 -> DistractorStrategy("phonetic", 0.5f, 0.6f, true)
            3 -> DistractorStrategy("mixed", 0.7f, 0.5f, true)
            4 -> DistractorStrategy("diagnostic", 0.9f, 0.4f, true)
            else -> DistractorStrategy("balanced", 0.5f, 0.6f, true)
        }
    }

    private fun getIntentFromIndex(index: Int, level: Int): TherapeuticIntent {
        return when(index) {
            0 -> TherapeuticIntent("reduce_cognitive_load", 0.3f, 5, true, 1, "easy")
            1 -> TherapeuticIntent("build_confidence", 0.5f, 10, true, 2, "easy")
            2 -> TherapeuticIntent("improve_attention", 0.7f, 12, true, 3, "balanced")
            3 -> TherapeuticIntent("challenge_mastery", 0.9f, 15, true, 4, "challenging")
            else -> TherapeuticIntent("maintain_progress", 0.6f, 10, false, level, "balanced")
        }
    }

    // =========== BEHAVIORAL ANALYSIS ===========

    fun trackResponse(startTime: Long, isCorrect: Boolean, selectedOption: String, correctOption: String, currentLevel: Int = 1) {
        val responseTime = System.currentTimeMillis() - startTime
        responseTimes.add(responseTime)

        if (isCorrect) {
            sessionMetrics.correctCount++
            currentBehavioralProfile = currentBehavioralProfile?.copy(
                consecutiveCorrect = (currentBehavioralProfile?.consecutiveCorrect ?: 0) + 1,
                consecutiveWrong = 0
            )
        } else {
            sessionMetrics.errorCount++
            val errorType = classifyErrorType(selectedOption, correctOption)
            errorPatterns.add(errorType)
            currentBehavioralProfile = currentBehavioralProfile?.copy(
                consecutiveWrong = (currentBehavioralProfile?.consecutiveWrong ?: 0) + 1,
                consecutiveCorrect = 0
            )
        }

        sessionMetrics.interactionCount++
        updateBehavioralProfile()

        // Track level history
        if (currentBehavioralProfile?.currentDifficultyLevel != currentLevel) {
            levelHistory.add(currentLevel)
            sessionMetrics.levelProgression.plus(currentLevel)
        }
    }

    private fun classifyErrorType(selected: String, correct: String): Int {
        val selectedLower = selected.lowercase()
        val correctLower = correct.lowercase()
        val distractorSet = diagnosticMetadata[correctLower]

        return when {
            distractorSet?.phonetic?.contains(selectedLower) == true -> 1 // Phonetic error
            distractorSet?.semantic?.contains(selectedLower) == true -> 2 // Semantic error
            distractorSet?.visual?.contains(selectedLower) == true -> 3 // Visual error
            selectedLower.length == correctLower.length -> 3 // Potential visual error
            selectedLower.soundsLike(correctLower) -> 1 // Phonetic error
            else -> 4 // Random error
        }
    }

    private fun calculateDominantErrorType(): Int {
        if (errorPatterns.size < 3) return 4 // Not enough data

        val recentErrors = errorPatterns.takeLast(10)
        val counts = IntArray(5)
        recentErrors.forEach { counts[it]++ }

        var maxCount = 0
        var dominantType = 4
        for (i in 1..4) {
            if (counts[i] > maxCount) {
                maxCount = counts[i]
                dominantType = i
            }
        }
        return dominantType
    }

    private fun calculateLearningRate(): Float {
        if (sessionMetrics.interactionCount < 5) return 0.5f

        // Compare first half vs second half accuracy
        val halfPoint = sessionMetrics.interactionCount / 2
        val firstHalfCorrect = sessionMetrics.correctCount.coerceAtMost(halfPoint)
        val secondHalfCorrect = (sessionMetrics.correctCount - firstHalfCorrect).coerceAtLeast(0)

        val firstHalfAccuracy = firstHalfCorrect.toFloat() / halfPoint
        val secondHalfAccuracy = secondHalfCorrect.toFloat() / (sessionMetrics.interactionCount - halfPoint)

        return (secondHalfAccuracy - firstHalfAccuracy).coerceIn(0f, 1f)
    }

    private fun String.soundsLike(other: String): Boolean {
        if (this.length != other.length) return false
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        val thisVowels = this.filter { it.toLowerCase() in vowels }
        val otherVowels = other.filter { it.toLowerCase() in vowels }
        return thisVowels == otherVowels
    }

    private fun updateBehavioralProfile() {
        if (responseTimes.isEmpty()) return

        val avgRT = responseTimes.average().toFloat()
        val accuracy = if (sessionMetrics.interactionCount > 0) {
            sessionMetrics.correctCount.toFloat() / sessionMetrics.interactionCount
        } else 0f

        val hesitation = calculateHesitation()
        val jitter = calculateJitter()
        val engagementScore = calculateEngagementScore()
        val frustrationLevel = calculateFrustrationLevel()
        val processingSpeed = 1000f / avgRT
        val dominantErrorType = calculateDominantErrorType()
        val learningRate = calculateLearningRate()

        currentBehavioralProfile = currentBehavioralProfile?.copy(
            jitter = jitter,
            hesitation = hesitation,
            accuracy = accuracy,
            responseTime = avgRT,
            errorPattern = errorPatterns.takeLast(10),
            engagementScore = engagementScore,
            frustrationLevel = frustrationLevel,
            processingSpeed = processingSpeed,
            dominantErrorType = dominantErrorType,
            learningRate = learningRate
        ) ?: BehavioralProfile(
            jitter = jitter,
            hesitation = hesitation,
            accuracy = accuracy,
            responseTime = avgRT,
            errorPattern = errorPatterns.takeLast(10),
            engagementScore = engagementScore,
            frustrationLevel = frustrationLevel,
            processingSpeed = processingSpeed,
            currentDifficultyLevel = 1,
            consecutiveCorrect = 0,
            consecutiveWrong = 0,
            dominantErrorType = dominantErrorType,
            learningRate = learningRate
        )
    }

    private fun calculateJitter(): Float {
        if (touchCoordinates.size < 3) return 0f
        val variances = mutableListOf<Float>()
        for (i in 1 until touchCoordinates.size) {
            val (x1, y1) = touchCoordinates[i-1]
            val (x2, y2) = touchCoordinates[i]
            val distance = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
            variances.add(distance)
        }
        val mean = variances.average().toFloat()
        val variance = variances.map { (it - mean).pow(2) }.average().toFloat()
        return sqrt(variance)
    }

    private fun calculateHesitation(): Float {
        if (responseTimes.size < 2) return 0f
        val mean = responseTimes.average()
        val variance = responseTimes.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat() / mean.toFloat()
    }

    private fun calculateEngagementScore(): Float {
        val sessionDuration = System.currentTimeMillis() - engagementStartTime
        val interactionsPerMinute = (sessionMetrics.interactionCount.toFloat() / (sessionDuration / 60000f))
        val accuracyWeight = sessionMetrics.correctCount.toFloat() / maxOf(1, sessionMetrics.interactionCount)
        return (interactionsPerMinute * 0.6f + accuracyWeight * 0.4f).coerceIn(0f, 1f)
    }

    private fun calculateFrustrationLevel(): Float {
        if (errorPatterns.isEmpty()) return 0f
        var consecutiveErrors = 0
        var maxConsecutive = 0
        for (i in max(0, errorPatterns.size - 5) until errorPatterns.size) {
            if (errorPatterns[i] > 0) {
                consecutiveErrors++
                maxConsecutive = maxOf(maxConsecutive, consecutiveErrors)
            } else {
                consecutiveErrors = 0
            }
        }
        return (maxConsecutive.toFloat() / 5f).coerceIn(0f, 1f)
    }

    fun calculateRecommendedLevel(currentLevel: Int): Int {
        val profile = currentBehavioralProfile ?: return currentLevel
        var recommendedLevel = currentLevel

        // Increase level after 3 consecutive correct answers
        if (profile.consecutiveCorrect >= 3 && currentLevel < 4) {
            recommendedLevel = currentLevel + 1
        }

        // Decrease level after 2 consecutive wrong answers
        if (profile.consecutiveWrong >= 2 && currentLevel > 1) {
            recommendedLevel = currentLevel - 1
        }

        // Performance-based adjustments
        if (profile.accuracy > 0.8f && currentLevel < 4) {
            recommendedLevel = currentLevel + 1
        }
        if (profile.accuracy < 0.4f && currentLevel > 1) {
            recommendedLevel = currentLevel - 1
        }

        // Learning rate adjustment
        if (profile.learningRate > 0.3f && currentLevel < 4) {
            recommendedLevel = currentLevel + 1
        }

        return recommendedLevel
    }

    private fun determineTherapeuticIntent(
        profile: BehavioralProfile?,
        baseDecision: ModelDecision
    ): TherapeuticIntent {
        val profile = profile ?: return TherapeuticIntent(
            primaryGoal = "build_confidence",
            uiComplexity = 0.5f,
            sessionDuration = 10,
            adaptiveScaling = true,
            recommendedLevel = baseDecision.recommendedLevel,
            distractorProfile = "balanced"
        )

        return when {
            profile.frustrationLevel > 0.7f -> TherapeuticIntent(
                primaryGoal = "reduce_frustration",
                uiComplexity = 0.3f,
                sessionDuration = 5,
                adaptiveScaling = true,
                recommendedLevel = maxOf(1, baseDecision.recommendedLevel - 1),
                distractorProfile = "easy"
            )
            profile.accuracy < 0.4f -> TherapeuticIntent(
                primaryGoal = "build_confidence",
                uiComplexity = 0.4f,
                sessionDuration = 8,
                adaptiveScaling = true,
                recommendedLevel = maxOf(1, baseDecision.recommendedLevel - 1),
                distractorProfile = "easy"
            )
            profile.engagementScore < 0.3f -> TherapeuticIntent(
                primaryGoal = "improve_attention",
                uiComplexity = 0.6f,
                sessionDuration = 12,
                adaptiveScaling = true,
                recommendedLevel = baseDecision.recommendedLevel,
                distractorProfile = "balanced"
            )
            profile.accuracy > 0.9f && profile.consecutiveCorrect >= 5 -> TherapeuticIntent(
                primaryGoal = "challenge_mastery",
                uiComplexity = 0.9f,
                sessionDuration = 15,
                adaptiveScaling = true,
                recommendedLevel = minOf(4, baseDecision.recommendedLevel + 1),
                distractorProfile = "challenging"
            )
            else -> TherapeuticIntent(
                primaryGoal = "maintain_progress",
                uiComplexity = 0.7f,
                sessionDuration = 10,
                adaptiveScaling = true,
                recommendedLevel = baseDecision.recommendedLevel,
                distractorProfile = "balanced"
            )
        }
    }

    private fun calculateSubRoutineRecommendation(sequenceAction: Int, accuracy: Float, errorCount: Int): Int {
        return when {
            errorCount >= 3 || accuracy < 30f -> 0 // Repeat
            errorCount == 0 && accuracy >= 80f -> 2 // Progress
            else -> 1 // Stay
        }
    }

    // =========== GETTERS ===========

    fun getRecommendedLevel(): Int {
        return calculateRecommendedLevel(currentBehavioralProfile?.currentDifficultyLevel ?: 1)
    }

    fun getRecommendedStrategy(): DistractorStrategy {
        return currentDistractorStrategy ?: getDefaultStrategy(getRecommendedLevel())
    }

    fun getConsecutiveCorrect(): Int {
        return currentBehavioralProfile?.consecutiveCorrect ?: 0
    }

    fun getConsecutiveWrong(): Int {
        return currentBehavioralProfile?.consecutiveWrong ?: 0
    }

    fun getDominantErrorType(): Int {
        return currentBehavioralProfile?.dominantErrorType ?: 4
    }

    fun getIndividualizedLatencyBuffer(): Long {
        val profile = currentBehavioralProfile ?: return 3000L
        return (profile.responseTime * 1.5f).toLong().coerceIn(2000L, 10000L)
    }

    fun getSessionSummary(): SessionMetrics {
        return sessionMetrics.copy(
            behavioralProfile = currentBehavioralProfile,
            therapeuticIntent = currentTherapeuticIntent,
            levelProgression = levelHistory,
            strategyChanges = emptyList() // TODO: Track strategy changes
        )
    }

    fun resetSession() {
        responseTimes.clear()
        touchCoordinates.clear()
        errorPatterns.clear()
        levelHistory.clear()
        engagementStartTime = System.currentTimeMillis()
        currentBehavioralProfile = null
        currentTherapeuticIntent = null
        currentDistractorStrategy = null
        sessionMetrics.interactionCount = 0
        sessionMetrics.correctCount = 0
        sessionMetrics.errorCount = 0
    }

    // =========== DEFAULT DECISION ===========

    private fun getDefaultDecision(inputFeatures: FloatArray): ModelDecision {
        val age = inputFeatures.getOrNull(0)?.toInt() ?: 6
        val accuracy = inputFeatures.getOrNull(5) ?: 0f

        val sequenceAction = when {
            age < 7 || accuracy < 50f -> 2
            else -> 1
        }

        val subRoutineRec = when {
            accuracy < 40f -> 0
            accuracy > 80f -> 2
            else -> 1
        }

        val recommendedLevel = 1
        val defaultStrategy = getDefaultStrategy(recommendedLevel)

        return ModelDecision(
            emotionLevel = 0,
            friendAction = 0,
            sequenceAction = sequenceAction,
            motivationId = 0,
            rhythmComplexity = 0.5f,
            routineAction = 0,
            subRoutineRecommendation = subRoutineRec,
            therapeuticIntent = TherapeuticIntent(
                primaryGoal = "build_confidence",
                uiComplexity = 0.5f,
                sessionDuration = 10,
                adaptiveScaling = true,
                recommendedLevel = recommendedLevel,
                distractorProfile = "balanced"
            ),
            recommendedLevel = recommendedLevel,
            distractorStrategy = defaultStrategy
        )
    }

    private fun argMax(array: FloatArray): Int {
        var maxIdx = 0
        var maxVal = array[0]
        for (i in 1 until array.size) {
            if (array[i] > maxVal) {
                maxVal = array[i]
                maxIdx = i
            }
        }
        return maxIdx
    }

    fun trackTouch(x: Float, y: Float, timestamp: Long) {
        touchCoordinates.add(Pair(x, y))
        if (touchCoordinates.size > 20) {
            touchCoordinates.removeAt(0)
        }
    }

    // =========== MODEL LOADING ===========

    private fun exploreAssets(context: Context) {
        try {
            Log.d(TAG, "🔍 === EXPLORING ASSETS FOLDER ===")
            val rootAssets = context.assets.list("")
            Log.d(TAG, "📁 Root assets: ${rootAssets?.joinToString() ?: "empty"}")
            if (rootAssets?.contains("ml") == true) {
                val mlAssets = context.assets.list("ml")
                Log.d(TAG, "📁 ml/ folder contents: ${mlAssets?.joinToString() ?: "empty"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error exploring assets: ${e.message}")
        }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val possiblePaths = listOf(
            "game_master.tflite",
            "ml/game_master.tflite"
        )
        for (path in possiblePaths) {
            try {
                val fileDescriptor = context.assets.openFd(path)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                Log.d(TAG, "Found model at: $path")
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            } catch (e: Exception) {
                // Continue searching
            }
        }
        throw FileNotFoundException("Model file not found in Assets. Checked: $possiblePaths")
    }

    private fun inspectOutputTensors() {
        if (interpreter == null) return
        val count = interpreter!!.outputTensorCount
        val size3Indices = mutableListOf<Int>()
        val size4Indices = mutableListOf<Int>() // For level
        val size6Indices = mutableListOf<Int>() // For strategy

        idxEmo = -1; idxFrn = -1; idxSeq = -1; idxRhy = -1
        idxRtn = -1; idxMot = -1; idxSub = -1; idxIntent = -1
        idxLevel = -1; idxStrategy = -1

        for (i in 0 until count) {
            val tensor = interpreter!!.getOutputTensor(i)
            val shape = tensor.shape()
            val size = if (shape.size > 1) shape[1] else 0
            Log.d(TAG, "Tensor $i: Shape ${shape.contentToString()}")

            when (size) {
                10 -> idxFrn = i
                4 -> {
                    // Could be routine OR level
                    if (idxRtn == -1) idxRtn = i else idxLevel = i
                    size4Indices.add(i)
                }
                1 -> idxRhy = i
                2 -> idxSub = i
                5 -> idxIntent = i
                6 -> {
                    idxStrategy = i
                    size6Indices.add(i)
                }
                3 -> size3Indices.add(i)
            }
        }

        size3Indices.sort()
        if (size3Indices.size >= 3) {
            idxEmo = size3Indices[0]
            idxSeq = size3Indices[1]
            idxMot = size3Indices[2]
        }

        Log.d(TAG, "Mapped: Rtn=$idxRtn, Frn=$idxFrn, Sub=$idxSub, Intent=$idxIntent, Level=$idxLevel, Strategy=$idxStrategy")
    }
}