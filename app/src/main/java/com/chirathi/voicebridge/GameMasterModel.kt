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

data class ModelDecision(
    val emotionLevel: Int,   // 0=Basic, 1=Context, 2=Scenario
    val friendAction: Int,   // 0-9 (Virtual Friend Item)
    val sequenceAction: Int, // 0=None, 1=Visual, 2=Auditory, 3=None (for hints)
    val motivationId: Int,   // 0=Stars, 1=Quote, 2=Anim
    val rhythmComplexity: Float, // 0.0 - 1.0
    val routineAction: Int,  // 0=Morning, 1=Bed, 2=School, 3=Play
    val subRoutineRecommendation: Int, // 0=Repeat, 1=Stay, 2=Progress
    val therapeuticIntent: TherapeuticIntent // NEW: Therapeutic goal for this session
): Serializable

data class TherapeuticIntent(
    val primaryGoal: String, // "reduce_cognitive_load", "build_confidence", "improve_attention"
    val uiComplexity: Float, // 0.1-1.0 (controls UI simplicity)
    val sessionDuration: Int, // Recommended session length in minutes
    val adaptiveScaling: Boolean // Whether to adapt difficulty mid-session
): Serializable

data class BehavioralProfile(
    val jitter: Float, // Motor variance
    val hesitation: Float, // Cognitive latency
    val accuracy: Float,
    val responseTime: Float,
    val errorPattern: List<Int>, // Pattern of errors
    val engagementScore: Float,
    val frustrationLevel: Float,
    val processingSpeed: Float
): Serializable

data class SessionMetrics(
    val startTime: Long,
    var interactionCount: Int = 0,
    var correctCount: Int = 0,
    var errorCount: Int = 0,
    val avgResponseTime: Float = 0f,
    val behavioralProfile: BehavioralProfile? = null,
    val therapeuticIntent: TherapeuticIntent? = null
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
    private var idxIntent = -1  // NEW: Therapeutic intent head

    // Behavioral tracking
    private val sessionMetrics = SessionMetrics(System.currentTimeMillis())
    private val responseTimes = mutableListOf<Long>()
    private val touchCoordinates = mutableListOf<Pair<Float, Float>>()
    private val errorPatterns = mutableListOf<Int>()
    private var engagementStartTime: Long = 0

    // Therapeutic profile cache
    private var currentTherapeuticIntent: TherapeuticIntent? = null
    private var currentBehavioralProfile: BehavioralProfile? = null

    // Metadata for diagnostic distractors
    private val diagnosticMetadata = mapOf(
        // Phonetic distractors (for auditory discrimination)
        "boat" to listOf("coat", "goat", "float", "moat"),
        "star" to listOf("car", "far", "bar", "jar"),
        "hill" to listOf("pill", "mill", "fill", "will"),
        "water" to listOf("daughter", "otter", "quarter", "slaughter"),

        // Semantic distractors (for category understanding)
        "boat" to listOf("anchor", "sail", "oar", "lifejacket"),
        "star" to listOf("moon", "planet", "comet", "galaxy"),
        "river" to listOf("stream", "creek", "brook", "canal"),
        "sun" to listOf("light", "heat", "day", "sky"),

        // Visual distractors (for shape/color discrimination)
        "circle" to listOf("square", "triangle", "oval", "diamond"),
        "red" to listOf("blue", "green", "yellow", "orange"),

        // Emotional distractors (for emotion recognition)
        "happy" to listOf("sad", "angry", "surprised", "scared")
    )

    init {
        try {
            // Load strictly from assets (where Android puts the file)
            exploreAssets(context)
            val modelFile = loadModelFile(context)
            interpreter = Interpreter(modelFile)
            Log.d(TAG, "✅ Model loaded successfully")
            inspectOutputTensors()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading model: ${e.message}")
        }
    }

    // NEW: Load directly from app/src/main/ml/game_master.tflite
    private fun loadModelFileFromPath(context: Context): MappedByteBuffer {
        try {
            // Get the file from the app's source directory
            val modelPath = context.filesDir.parent + "/src/main/ml/game_master.tflite"
            val file = File(modelPath)

            if (file.exists()) {
                Log.d(TAG, "✅ Loading model from: $modelPath")
                val inputStream = FileInputStream(file)
                val fileChannel = inputStream.channel
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load from path: ${e.message}")
        }

        // Fallback - try to copy from assets if available
        return loadModelFile(context)
    }


    private fun loadModelFile(context: Context): MappedByteBuffer {
        // Android build system flattens 'ml' folder into the root of assets
        // or keeps it as 'ml/'. We check both to be safe.
        val possiblePaths = listOf(
            "game_master.tflite",       // Standard location (Root of assets)
            "ml/game_master.tflite"     // Explicit subfolder
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

    private fun exploreAssets(context: Context) {
        try {
            Log.d(TAG, "🔍 === EXPLORING ASSETS FOLDER ===")

            // List root of assets
            val rootAssets = context.assets.list("")
            Log.d(TAG, "📁 Root assets: ${rootAssets?.joinToString() ?: "empty"}")

            // Check if 'ml' folder exists
            if (rootAssets?.contains("ml") == true) {
                val mlAssets = context.assets.list("ml")
                Log.d(TAG, "📁 ml/ folder contents: ${mlAssets?.joinToString() ?: "empty"}")
            }

            // Also check if file exists directly in root
            val gameMasterInRoot = rootAssets?.contains("game_master.tflite") == true
            Log.d(TAG, "📄 game_master.tflite in root: $gameMasterInRoot")

        } catch (e: Exception) {
            Log.e(TAG, "Error exploring assets: ${e.message}")
        }
    }

    private fun inspectOutputTensors() {
        if (interpreter == null) return

        val count = interpreter!!.outputTensorCount
        val size3Indices = mutableListOf<Int>()

        // Reset indices
        idxEmo = -1; idxFrn = -1; idxSeq = -1; idxRhy = -1
        idxRtn = -1; idxMot = -1; idxSub = -1; idxIntent = -1

        for (i in 0 until count) {
            val tensor = interpreter!!.getOutputTensor(i)
            val shape = tensor.shape()
            val size = if (shape.size > 1) shape[1] else 0

            Log.d(TAG, "Tensor $i: Shape ${shape.contentToString()}")

            when (size) {
                10 -> idxFrn = i
                4 -> idxRtn = i
                1 -> idxRhy = i
                2 -> idxSub = i
                5 -> idxIntent = i
                3 -> size3Indices.add(i)
            }
        }

        size3Indices.sort()
        if (size3Indices.size >= 3) {
            idxEmo = size3Indices[0]
            idxSeq = size3Indices[1]
            idxMot = size3Indices[2]
        }

        Log.d(TAG, "Mapped: Rtn=$idxRtn, Frn=$idxFrn, Sub=$idxSub, Intent=$idxIntent")
    }

    // =========== BEHAVIORAL FEATURE EXTRACTOR ===========

    fun trackTouch(x: Float, y: Float, timestamp: Long) {
        touchCoordinates.add(Pair(x, y))

        // Calculate motor jitter (variance in movement)
        if (touchCoordinates.size >= 3) {
            val jitter = calculateJitter()
            Log.d(TAG, "Motor Jitter: $jitter")
        }
    }

    fun trackResponse(startTime: Long, isCorrect: Boolean, selectedOption: String, correctOption: String) {
        val responseTime = System.currentTimeMillis() - startTime
        responseTimes.add(responseTime)

        if (isCorrect) {
            sessionMetrics.correctCount++
        } else {
            sessionMetrics.errorCount++
            // Record error type based on semantic similarity
            val errorType = classifyErrorType(selectedOption, correctOption)
            errorPatterns.add(errorType)
        }

        sessionMetrics.interactionCount++

        // Update behavioral profile in real-time
        updateBehavioralProfile()

        // Check if therapeutic intervention is needed
        if (shouldTriggerTherapeuticIntervention()) {
            Log.d(TAG, "⚠️ Therapeutic intervention triggered!")
            // Could trigger UI simplification or confidence builder mode
        }
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

    private fun classifyErrorType(selected: String, correct: String): Int {
        return when {
            // Phonetic error (similar sounding)
            selected in (diagnosticMetadata[correct]?.filter {
                it.soundsLike(selected)
            } ?: emptyList()) -> 1

            // Semantic error (related meaning)
            selected in (diagnosticMetadata[correct] ?: emptyList()) -> 2

            // Visual error (similar appearance)
            selected.length == correct.length -> 3

            // Random error
            else -> 4
        }
    }

    private fun String.soundsLike(other: String): Boolean {
        // Simple phonetic similarity check (in production, use proper algorithm)
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
        val processingSpeed = 1000f / avgRT // items per second

        currentBehavioralProfile = BehavioralProfile(
            jitter = jitter,
            hesitation = hesitation,
            accuracy = accuracy,
            responseTime = avgRT,
            errorPattern = errorPatterns.takeLast(5), // Recent pattern
            engagementScore = engagementScore,
            frustrationLevel = frustrationLevel,
            processingSpeed = processingSpeed
        )

        Log.d(TAG, "Updated Behavioral Profile:")
        Log.d(TAG, "  Accuracy: $accuracy, Avg RT: ${avgRT}ms")
        Log.d(TAG, "  Jitter: $jitter, Hesitation: $hesitation")
        Log.d(TAG, "  Frustration: $frustrationLevel, Engagement: $engagementScore")
    }

    private fun calculateHesitation(): Float {
        if (responseTimes.size < 2) return 0f
        val mean = responseTimes.average()
        val variance = responseTimes.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat() / mean.toFloat() // Coefficient of variation
    }

    private fun calculateEngagementScore(): Float {
        val sessionDuration = System.currentTimeMillis() - engagementStartTime
        val interactionsPerMinute = (sessionMetrics.interactionCount.toFloat() / (sessionDuration / 60000f))
        val accuracyWeight = sessionMetrics.correctCount.toFloat() / maxOf(1, sessionMetrics.interactionCount)

        return (interactionsPerMinute * 0.6f + accuracyWeight * 0.4f).coerceIn(0f, 1f)
    }

    private fun calculateFrustrationLevel(): Float {
        if (errorPatterns.isEmpty()) return 0f

        // Consecutive errors indicate frustration
        var consecutiveErrors = 0
        var maxConsecutive = 0
        for (i in max(0, errorPatterns.size - 5) until errorPatterns.size) {
            if (errorPatterns[i] > 0) { // Error occurred
                consecutiveErrors++
                maxConsecutive = maxOf(maxConsecutive, consecutiveErrors)
            } else {
                consecutiveErrors = 0
            }
        }

        return (maxConsecutive.toFloat() / 5f).coerceIn(0f, 1f)
    }

    private fun shouldTriggerTherapeuticIntervention(): Boolean {
        val profile = currentBehavioralProfile ?: return false

        // Trigger intervention if:
        // 1. Frustration is high
        // 2. Accuracy is low
        // 3. Response time is increasing
        return profile.frustrationLevel > 0.7f ||
                profile.accuracy < 0.3f ||
                (responseTimes.size >= 3 && responseTimes.takeLast(3).average() > responseTimes.dropLast(3).average() * 1.5)
    }

    // =========== DYNAMIC PROFILE INTERCEPTOR ===========

    fun predictWithBehavioralContext(inputFeatures: FloatArray): ModelDecision {
        val profile = currentBehavioralProfile

        // Augment input features with behavioral data if available
        val augmentedFeatures = if (profile != null) {
            val behavioralFeatures = floatArrayOf(
                profile.jitter,
                profile.hesitation,
                profile.accuracy,
                profile.responseTime / 1000f, // Convert to seconds
                profile.frustrationLevel,
                profile.engagementScore,
                profile.processingSpeed
            )
            inputFeatures + behavioralFeatures
        } else {
            inputFeatures
        }

        val baseDecision = predict(augmentedFeatures)

        // Override therapeutic intent based on behavioral state
        val therapeuticIntent = determineTherapeuticIntent(profile, baseDecision)

        return baseDecision.copy(therapeuticIntent = therapeuticIntent)
    }

    private fun determineTherapeuticIntent(
        profile: BehavioralProfile?,
        baseDecision: ModelDecision
    ): TherapeuticIntent {
        val profile = profile ?: return TherapeuticIntent(
            primaryGoal = "build_confidence",
            uiComplexity = 0.5f,
            sessionDuration = 10,
            adaptiveScaling = true
        )

        return when {
            profile.frustrationLevel > 0.7f -> TherapeuticIntent(
                primaryGoal = "reduce_frustration",
                uiComplexity = 0.3f, // Very simple UI
                sessionDuration = 5, // Short session
                adaptiveScaling = true
            )

            profile.accuracy < 0.4f -> TherapeuticIntent(
                primaryGoal = "build_confidence",
                uiComplexity = 0.4f,
                sessionDuration = 8,
                adaptiveScaling = true
            )

            profile.engagementScore < 0.3f -> TherapeuticIntent(
                primaryGoal = "improve_attention",
                uiComplexity = 0.6f, // Slightly more engaging
                sessionDuration = 12,
                adaptiveScaling = true
            )

            else -> TherapeuticIntent(
                primaryGoal = "maintain_progress",
                uiComplexity = 0.7f,
                sessionDuration = 15,
                adaptiveScaling = true
            )
        }
    }

    fun getIndividualizedLatencyBuffer(): Long {
        val profile = currentBehavioralProfile ?: return 3000L // Default 3 seconds

        // Buffer = Average RT + 50% for processing variability
        val buffer = profile.responseTime * 1.5f
        return buffer.toLong().coerceIn(2000L, 10000L) // Min 2s, Max 10s
    }

    fun getDiagnosticDistractors(keyword: String, count: Int = 3): List<Pair<String, Int>> {
        val distractors = mutableListOf<Pair<String, Int>>()
        val profile = currentBehavioralProfile

        // Select distractors based on therapeutic need
        val distractorTypes = when (profile?.errorPattern?.lastOrNull()) {
            1 -> "phonetic" // Child struggles with similar sounds
            2 -> "semantic" // Child struggles with meanings
            3 -> "visual"   // Child struggles with shapes
            else -> "mixed"  // General challenge
        }

        val availableDistractors = diagnosticMetadata[keyword] ?: emptyList()

        when (distractorTypes) {
            "phonetic" -> {
                // Use phonetic distractors for auditory discrimination
                distractors.addAll(availableDistractors.filter { it.soundsLike(keyword) }
                    .take(count).map { Pair(it, getFallbackImageForWord(it)) })
            }
            "semantic" -> {
                // Use semantic distractors for category understanding
                distractors.addAll(availableDistractors.take(count)
                    .map { Pair(it, getFallbackImageForWord(it)) })
            }
            else -> {
                // Mixed challenge
                distractors.addAll(availableDistractors.shuffled().take(count)
                    .map { Pair(it, getFallbackImageForWord(it)) })
            }
        }


        // Fill remaining slots with generic distractors if needed
        while (distractors.size < count) {
            distractors.add(Pair("item_${distractors.size}", android.R.drawable.ic_menu_help))
        }

        return distractors
    }

    private fun getFallbackImageForWord(word: String): Int {
        return when (word.lowercase()) {
            // ===== BOAT PHONETIC DISTRACTORS =====
            "coat" -> R.drawable.coat
            "goat" -> R.drawable.goat
            "note" -> R.drawable.note

            // ===== CREEK PHONETIC DISTRACTORS =====
//            "car" -> R.drawable.car_image
//            "jar" -> R.drawable.jar_image
//            "bar" -> R.drawable.bar_image
//            "far" -> R.drawable.far_image
//
//            // ===== HILL PHONETIC DISTRACTORS =====
//            "pill" -> R.drawable.pill_image
//            "mill" -> R.drawable.mill_image
//            "fill" -> R.drawable.fill_image
//            "will" -> R.drawable.will_image
//
//            // ===== WATER PHONETIC DISTRACTORS =====
//            "daughter" -> R.drawable.daughter_image
//            "otter" -> R.drawable.otter_image
//            "quarter" -> R.drawable.quarter_image
//            "slaughter" -> R.drawable.slaughter_image
//
//            // ===== BOAT SEMANTIC DISTRACTORS =====
//            "anchor" -> R.drawable.anchor_image
//            "sail" -> R.drawable.sail_image
//            "oar" -> R.drawable.oar_image
//            "lifejacket" -> R.drawable.lifejacket_image
//
//            // ===== STAR SEMANTIC DISTRACTORS =====
//            "moon" -> R.drawable.moon_image
//            "planet" -> R.drawable.planet_image
//            "comet" -> R.drawable.comet_image
//            "galaxy" -> R.drawable.galaxy_image
//
//            // ===== RIVER SEMANTIC DISTRACTORS =====
//            "stream" -> R.drawable.rhy_song0_stream  // You have this
//            "creek" -> R.drawable.creek_image
//            "brook" -> R.drawable.brook_image
//            "canal" -> R.drawable.canal_image
//
//            // ===== SUN SEMANTIC DISTRACTORS =====
//            "light" -> R.drawable.rhy_song1_light  // You have this
//            "heat" -> R.drawable.heat_image
//            "day" -> R.drawable.day_image
//            "sky" -> R.drawable.sky_image
//
//            // ===== SHAPES =====
//            "square" -> R.drawable.square_image
//            "triangle" -> R.drawable.triangle_image
//            "oval" -> R.drawable.oval_image
//            "diamond" -> R.drawable.rhy_song1_diamond  // You have this
//
//            // ===== COLORS =====
//            "blue" -> R.drawable.blue_image
//            "green" -> R.drawable.green_image
//            "yellow" -> R.drawable.yellow_image
//            "orange" -> R.drawable.orange_image
//
//            // ===== EMOTIONS =====
//            "sad" -> R.drawable.sad_image
//            "angry" -> R.drawable.angry_image
//            "surprised" -> R.drawable.surprised_image
//            "scared" -> R.drawable.scared_image

            // Generic fallback
            else -> android.R.drawable.ic_menu_help
        }
    }

    // =========== ORIGINAL PREDICT FUNCTION (UPDATED) ===========

    fun predict(inputFeatures: FloatArray): ModelDecision {
        if (interpreter == null) return getDefaultDecision(inputFeatures)

        // 1. Prepare Inputs
        val featureCount = max(inputFeatures.size, 8)
        val paddedFeatures = if (inputFeatures.size < featureCount) {
            inputFeatures + FloatArray(featureCount - inputFeatures.size)
        } else inputFeatures

        val inputs = arrayOf(paddedFeatures)
        val outputs = mutableMapOf<Int, Any>()

        // 2. Allocate Outputs (Only for existing tensors)
        if (idxEmo != -1) outputs[idxEmo] = Array(1) { FloatArray(3) }
        if (idxFrn != -1) outputs[idxFrn] = Array(1) { FloatArray(10) }
        if (idxSeq != -1) outputs[idxSeq] = Array(1) { FloatArray(3) }
        if (idxRhy != -1) outputs[idxRhy] = Array(1) { FloatArray(1) }
        if (idxRtn != -1) outputs[idxRtn] = Array(1) { FloatArray(4) }
        if (idxMot != -1) outputs[idxMot] = Array(1) { FloatArray(3) }

        // These are currently -1, but this logic ensures future compatibility
        if (idxSub != -1) outputs[idxSub] = Array(1) { FloatArray(2) }
        if (idxIntent != -1) outputs[idxIntent] = Array(1) { FloatArray(5) }

        // 3. Run Inference
        try {
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            return getDefaultDecision(inputFeatures)
        }

        // 4. Parse Results
        val emotionLevel = if (idxEmo != -1) argMax((outputs[idxEmo] as Array<FloatArray>)[0]) else 0
        val friendAction = if (idxFrn != -1) argMax((outputs[idxFrn] as Array<FloatArray>)[0]) else 0
        val sequenceAction = if (idxSeq != -1) argMax((outputs[idxSeq] as Array<FloatArray>)[0]) else 1
        val motivationId = if (idxMot != -1) argMax((outputs[idxMot] as Array<FloatArray>)[0]) else 0
        val rhythmComplexity = if (idxRhy != -1) (outputs[idxRhy] as Array<FloatArray>)[0][0].coerceIn(0f, 1f) else 0.5f
        val routineAction = if (idxRtn != -1) argMax((outputs[idxRtn] as Array<FloatArray>)[0]) else 0

        // 5. Smart Fallback for Missing Tensors

        // Sub-routine: Use model if available, else calculate manually
        val subRoutineRecommendation = if (idxSub != -1) {
            argMax((outputs[idxSub] as Array<FloatArray>)[0])
        } else {
            calculateSubRoutineRecommendation(
                sequenceAction,
                inputFeatures.getOrNull(5) ?: 0f,
                inputFeatures.getOrNull(7)?.toInt() ?: 0
            )
        }

        // Therapeutic Intent: Use model if available, else use default
        val therapeuticIntent = if (idxIntent != -1) {
            val intentIdx = argMax((outputs[idxIntent] as Array<FloatArray>)[0])
            getIntentFromIndex(intentIdx)
        } else {
            TherapeuticIntent("build_confidence", 0.5f, 10, true)
        }

        return ModelDecision(
            emotionLevel, friendAction, sequenceAction, motivationId,
            rhythmComplexity, routineAction, subRoutineRecommendation, therapeuticIntent
        )
    }

    private fun getIntentFromIndex(index: Int): TherapeuticIntent {
        return when(index) {
            0 -> TherapeuticIntent("reduce_cognitive_load", 0.3f, 5, true)
            1 -> TherapeuticIntent("build_confidence", 0.5f, 10, true)
            2 -> TherapeuticIntent("improve_attention", 0.7f, 12, true)
            3 -> TherapeuticIntent("challenge_mastery", 0.9f, 15, true)
            else -> TherapeuticIntent("maintain_progress", 0.6f, 10, false)
        }
    }

    private fun calculateSubRoutineRecommendation(
        sequenceAction: Int,
        accuracy: Float,
        errorCount: Int
    ): Int {
        return when {
            // Poor performance: Many errors or low accuracy - Repeat
            errorCount >= 3 || accuracy < 30f -> {
                Log.d(TAG, "Poor performance: Repeat same sub-routine")
                0 // Repeat
            }
            // Good performance: Few errors and high accuracy - Progress
            errorCount == 0 && accuracy >= 80f -> {
                Log.d(TAG, "Excellent performance: Progress to next sub-routine")
                2 // Progress
            }
            // Medium performance - Stay at current difficulty
            else -> {
                Log.d(TAG, "Medium performance: Stay at current sub-routine")
                1 // Stay
            }
        }
    }

    private fun getDefaultDecision(inputFeatures: FloatArray): ModelDecision {
        val age = inputFeatures.getOrNull(0)?.toInt() ?: 6
        val accuracy = inputFeatures.getOrNull(5) ?: 0f

        val sequenceAction = when {
            age < 7 || accuracy < 50f -> 2 // Auditory hints for younger or struggling
            else -> 1 // Visual hints for others
        }

        val subRoutineRec = when {
            accuracy < 40f -> 0 // Repeat
            accuracy > 80f -> 2 // Progress
            else -> 1 // Stay
        }

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
                adaptiveScaling = true
            )
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

    fun getSessionSummary(): SessionMetrics {
        return sessionMetrics.copy(behavioralProfile = currentBehavioralProfile)
    }

    fun resetSession() {
        responseTimes.clear()
        touchCoordinates.clear()
        errorPatterns.clear()
        engagementStartTime = System.currentTimeMillis()
        currentBehavioralProfile = null
        currentTherapeuticIntent = null
    }
}