package com.chirathi.voicebridge

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class PronunciationTFLiteHelper(context: Context) {

    private val interpreter: Interpreter

    private val MAX_LEN = 32   // MUST match Python
    private val N_MFCC = 13

    init {
        val modelBuffer = loadModelFile(context)
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val fileDescriptor = context.assets.openFd("pronunciation_scorer.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(input: Array<Array<Float>>): Float {
        val inputBuffer =
            ByteBuffer.allocateDirect(4 * MAX_LEN * N_MFCC).order(ByteOrder.nativeOrder())

        for (i in 0 until MAX_LEN) {
            for (j in 0 until N_MFCC) {
                inputBuffer.putFloat(input[i][j])
            }
        }

        val outputBuffer =
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return outputBuffer.float   // probability (0–1)
    }
}
