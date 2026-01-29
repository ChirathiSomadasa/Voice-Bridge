package com.chirathi.voicebridge

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class ModelDecision(
    val emotionLevel: Int,   // 0=Basic, 1=Context, 2=Scenario
    val friendAction: Int,   // 0-9 (Virtual Friend Item)
    val sequenceAction: Int, // 0=None, 1=Visual, 2=Auditory, 3=None (for hints)
    val motivationId: Int,   // 0=Stars, 1=Quote, 2=Anim
    val rhythmComplexity: Float, // 0.0 - 1.0
    val routineAction: Int,  // 0=Morning, 1=Bed, 2=School, 3=Play
    val subRoutineRecommendation: Int // 0=Repeat, 1=Stay, 2=Progress
)

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
    private var idxSub = -1  // NEW: for sub-routine recommendation

    init {
        try {
            val modelFile = loadModelFile(context, "game_master.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelFile, options)

            Log.d(TAG, "Model loaded. Inspecting output tensors...")
            inspectOutputTensors()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun inspectOutputTensors() {
        if (interpreter == null) return

        val count = interpreter!!.outputTensorCount
        Log.d(TAG, "Total output tensors: $count")

        val size3Indices = mutableListOf<Int>()
        val size4Indices = mutableListOf<Int>()

        for (i in 0 until count) {
            val tensor = interpreter!!.getOutputTensor(i)
            val shape = tensor.shape()

            // Log all tensor info
            Log.d(TAG, "Tensor $i: Shape ${shape.contentToString()}, DataType: ${tensor.dataType()}, Name: ${tensor.name()}")

            when {
                shape.size == 2 && shape[1] == 10 -> {
                    idxFrn = i // Friend Action (Size 10)
                    Log.d(TAG, "Assigned idxFrn = $i (Friend Action)")
                }
                shape.size == 2 && shape[1] == 4 -> {
                    idxRtn = i // Routine Action (Size 4)
                    Log.d(TAG, "Assigned idxRtn = $i (Routine Action)")
                }
                shape.size == 2 && shape[1] == 3 -> {
                    size3Indices.add(i) // Emotion, Sequence, Motivation (Size 3)
                    Log.d(TAG, "Added size3 tensor at index $i")
                }
                shape.size == 2 && shape[1] == 1 -> {
                    idxRhy = i // Rhythm Complexity (Size 1)
                    Log.d(TAG, "Assigned idxRhy = $i (Rhythm Complexity)")
                }
                shape.size == 2 && shape[1] == 2 -> {
                    idxSub = i // Sub-routine recommendation (Size 2: 0=Repeat, 1=Progress)
                    Log.d(TAG, "Assigned idxSub = $i (Sub-routine Recommendation)")
                }
            }
        }

        // Sort size3 indices for consistent assignment
        size3Indices.sort()

        // Assign based on common patterns
        if (size3Indices.size >= 3) {
            // Pattern 1: [emotion, sequence, motivation] - most common
            idxEmo = size3Indices[0]
            idxSeq = size3Indices[1]
            idxMot = size3Indices[2]

            Log.d(TAG, "Assigned size3 tensors: Emo=$idxEmo, Seq=$idxSeq, Mot=$idxMot")
        } else if (size3Indices.size == 2) {
            // Fallback if only 2 size3 tensors
            idxEmo = size3Indices[0]
            idxSeq = size3Indices[1]
            Log.d(TAG, "Only 2 size3 tensors: Emo=$idxEmo, Seq=$idxSeq")
        }

        // Log final assignments
        Log.d(TAG, "Final assignments:")
        Log.d(TAG, "  Emotion: $idxEmo, Friend: $idxFrn, Sequence: $idxSeq")
        Log.d(TAG, "  Motivation: $idxMot, Rhythm: $idxRhy, Routine: $idxRtn")
        Log.d(TAG, "  Sub-routine: $idxSub")
    }

    fun predict(inputFeatures: FloatArray): ModelDecision {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter is null, returning default decision")
            return getDefaultDecision(inputFeatures)
        }

        // Validate input
        if (inputFeatures.size != 8) {
            Log.e(TAG, "Input features size ${inputFeatures.size} != 8")
            return getDefaultDecision(inputFeatures)
        }

        // Log input for debugging
        Log.d(TAG, "Input features: ${inputFeatures.joinToString(", ")}")

        // 1. Prepare Inputs
        val inputs = Array(1) { inputFeatures }

        // 2. Prepare Outputs based on discovered indices
        val outputs = mutableMapOf<Int, Any>()

        // Prepare all possible output arrays
        if (idxEmo != -1) outputs[idxEmo] = Array(1) { FloatArray(3) }
        if (idxFrn != -1) outputs[idxFrn] = Array(1) { FloatArray(10) }
        if (idxSeq != -1) outputs[idxSeq] = Array(1) { FloatArray(3) }
        if (idxRhy != -1) outputs[idxRhy] = Array(1) { FloatArray(1) }
        if (idxRtn != -1) outputs[idxRtn] = Array(1) { FloatArray(4) }
        if (idxMot != -1) outputs[idxMot] = Array(1) { FloatArray(3) }
        if (idxSub != -1) outputs[idxSub] = Array(1) { FloatArray(2) }

        // 3. Run Inference
        try {
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputs), outputs)
            Log.d(TAG, "Inference successful")
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed: ${e.message}")
            e.printStackTrace()
            return getDefaultDecision(inputFeatures)
        }

        // 4. Parse Results with null safety
        val emotionLevel = if (idxEmo != -1) argMax((outputs[idxEmo] as Array<FloatArray>)[0]) else 0
        val friendAction = if (idxFrn != -1) argMax((outputs[idxFrn] as Array<FloatArray>)[0]) else 0
        val sequenceAction = if (idxSeq != -1) argMax((outputs[idxSeq] as Array<FloatArray>)[0]) else 0
        val motivationId = if (idxMot != -1) argMax((outputs[idxMot] as Array<FloatArray>)[0]) else 0
        val rhythmComplexity = if (idxRhy != -1) (outputs[idxRhy] as Array<FloatArray>)[0][0].coerceIn(0f, 1f) else 0.5f
        val routineAction = if (idxRtn != -1) argMax((outputs[idxRtn] as Array<FloatArray>)[0]) else 0

        // NEW: Calculate sub-routine recommendation based on performance
        val subRoutineRecommendation = calculateSubRoutineRecommendation(
            sequenceAction = sequenceAction,
            accuracy = inputFeatures[5], // Accuracy from features
            errorCount = inputFeatures[7].toInt() // Assuming error count is feature 7
        )

        return ModelDecision(
            emotionLevel,
            friendAction,
            sequenceAction,
            motivationId,
            rhythmComplexity,
            routineAction,
            subRoutineRecommendation
        )
    }

    /**
     * Calculate sub-routine recommendation based on performance
     * Returns: 0=Repeat same sub-routine, 1=Stay at current, 2=Progress to next
     */
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

    /**
     * Default decision when model fails
     */
    private fun getDefaultDecision(inputFeatures: FloatArray): ModelDecision {
        val age = inputFeatures[0].toInt()
        val accuracy = inputFeatures[5]

        // Simple heuristic based on age and accuracy
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
            subRoutineRecommendation = subRoutineRec
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
}