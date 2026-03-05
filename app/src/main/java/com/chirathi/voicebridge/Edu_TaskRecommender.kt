


package com.chirathi.voicebridge

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Edu_TaskRecommender(context: Context) {

    private val interpreter: Interpreter
    private val inputFeatureCount = 4  // age, disorderType, severity, subject
    private val outputClassCount = 36  // Based on your trained model

    init {
        val modelBuffer = loadModelFile(context, "task_recommender.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Predict the best lesson recommendation
     * @param age Child's age (6-10)
     * @param disorderType Encoded disorder type (0-4)
     * @param severity Encoded severity (0-2)
     * @param subject Encoded subject (0-2, defaults to 0 for Math)
     * @return Pair of (predicted class index, confidence score 0.0-1.0)
     */
    fun predict(age: Int, disorderType: Int, severity: Int, subject: Int = 0): Pair<Int, Float> {
        // Normalize age to 0-1 range
        val normalizedAge = (age - 6).toFloat() / 4.0f

        // Prepare input buffer (4 features to match model's expected 16 bytes)
        val inputBuffer = ByteBuffer.allocateDirect(4 * inputFeatureCount)
            .order(ByteOrder.nativeOrder())

        inputBuffer.putFloat(normalizedAge)
        inputBuffer.putFloat(disorderType.toFloat())
        inputBuffer.putFloat(severity.toFloat())
        inputBuffer.putFloat(subject.toFloat())

        // Allocate output buffer
        val outputBuffer = ByteBuffer.allocateDirect(4 * outputClassCount)
            .order(ByteOrder.nativeOrder())

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Extract probabilities
        outputBuffer.rewind()
        val probabilities = FloatArray(outputClassCount)
        outputBuffer.asFloatBuffer().get(probabilities)

        // Find the class with highest probability
        var maxIndex = 0
        var maxProb = probabilities[0]

        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIndex = i
            }
        }

        return Pair(maxIndex, maxProb)
    }


//     * Close the interpreter to free resources
    fun close() {
        interpreter.close()
    }
}