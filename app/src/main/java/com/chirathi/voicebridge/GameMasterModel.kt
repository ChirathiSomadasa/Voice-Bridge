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
    val sequenceAction: Int, // 0=None, 1=Motor, 2=Cognitive
    val motivationId: Int,   // 0=Stars, 1=Quote, 2=Anim
    val rhythmComplexity: Float // 0.0 - 1.0
)

class GameMasterModel(context: Context) {
    private var interpreter: Interpreter? = null
    private val TAG = "GameMasterModel"

    // Maps to store which index corresponds to which output head
    private var idxEmo = 0
    private var idxFrn = 1
    private var idxSeq = 2
    private var idxRhy = 3
    private var idxRtn = 4
    private var idxMot = 5

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

    //
    // DYNAMICALLY FIND INDICES BASED ON SHAPE AND NAME
    private fun inspectOutputTensors() {
        if (interpreter == null) return

        val count = interpreter!!.outputTensorCount
        val size3Indices = mutableListOf<Int>()

        for (i in 0 until count) {
            val tensor = interpreter!!.getOutputTensor(i)
            val shape = tensor.shape() // e.g. [1, 3]

            // Log shapes to be sure
            Log.d(TAG, "Tensor $i: Shape ${shape.contentToString()} Name: ${tensor.name()}")

            if (shape[1] == 10) {
                idxFrn = i // Friend Action (Unique Size 10)
            }
            else if (shape[1] == 4) {
                idxRtn = i // Routine (Unique Size 4)
            }
            else if (shape[1] == 1) {
                idxRhy = i // Rhythm (Unique Size 1)
            }
            else if (shape[1] == 3) {
                size3Indices.add(i) // Emo, Seq, Mot are all size 3
            }
        }

        // CRITICAL FIX FOR TENSORFLOW LITE METADATA LOSS
        // When names are stripped (e.g. "StatefulPartitionedCall..."),
        // we must rely on the standard export order of Keras if alphabetical fails.
        // Based on your logs, the indices are [2, 4, 5].

        if (size3Indices.size >= 3) {
            size3Indices.sort() // Sort by Index Number (2, 4, 5)

            // In many TFLite conversions without metadata, the order matches
            // the alphanumeric sort of the ORIGINAL Python names:
            // 1. out_emo (Emotion)
            // 2. out_mot (Motivation)
            // 3. out_seq (Sequence)

            // However, looking at your specific log dump:
            // "Resolving Size-3 Outputs: [4, 5, 2]"

            // We will map them assuming standard Keras alphanumeric output:
            idxEmo = size3Indices[0] // Emotion
            idxMot = size3Indices[1] // Motivation (Middle one)
            idxSeq = size3Indices[2] // Sequence

            Log.d(TAG, "Mapped Size-3 Indices: Emo=$idxEmo, Mot=$idxMot, Seq=$idxSeq")
        }
    }

    fun predict(inputFeatures: FloatArray): ModelDecision {
        if (interpreter == null) return ModelDecision(0, 0, 0, 0, 0.5f)

        // 1. Prepare Inputs
        val inputs = Array(1) { inputFeatures }

        // 2. Prepare Outputs
        val outEmo = Array(1) { FloatArray(3) }
        val outFrn = Array(1) { FloatArray(10) }
        val outSeq = Array(1) { FloatArray(3) }
        val outRhy = Array(1) { FloatArray(1) }
        val outRtn = Array(1) { FloatArray(4) }
        val outMot = Array(1) { FloatArray(3) }

        // 3. Map Outputs using Discovered Indices
        val outputs = mutableMapOf<Int, Any>()
        outputs[idxEmo] = outEmo
        outputs[idxFrn] = outFrn
        outputs[idxSeq] = outSeq
        outputs[idxRhy] = outRhy
        outputs[idxRtn] = outRtn
        outputs[idxMot] = outMot

        // 4. Run Inference
        try {
            interpreter?.runForMultipleInputsOutputs(arrayOf(inputs), outputs)
            // Log.d(TAG, "Inference success.")
        } catch (e: Exception) {
            Log.e(TAG, "Inference FAILED: ${e.message}")
            return ModelDecision(0, 0, 0, 0, 0.5f)
        }

        // 5. Parse Results
        val emotionLevel = argMax(outEmo[0])
        val friendAction = argMax(outFrn[0])
        val sequenceAction = argMax(outSeq[0])
        val motivationId = argMax(outMot[0])
        val rhythmComplexity = outRhy[0][0]

        return ModelDecision(emotionLevel, friendAction, sequenceAction, motivationId, rhythmComplexity)
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