package com.chirathi.voicebridge

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.io.IOException

class YoloV5Detector(context: Context, modelPath: String = "ml/object-detection-model.tflite") {

    private val interpreter: Interpreter
    private val inputSize = 640
    private val IMAGE_MEAN = 0f
    private val IMAGE_STD = 255f

    init {
        // Load from ml directory
        val model: MappedByteBuffer = loadModelFileFromMlDir(context, modelPath)
        interpreter = Interpreter(model)
    }

    /**
     * Load TFLite model from ml directory
     */
    private fun loadModelFileFromMlDir(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("object-detection-model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun detect(bitmap: Bitmap): Array<Array<FloatArray>> {
        // Preprocess bitmap
        val tensorImage = preprocess(bitmap)
        val output = Array(1) { Array(25200) { FloatArray(85) } }
        interpreter.run(tensorImage.buffer, output)
        return output
    }

    private fun preprocess(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(org.tensorflow.lite.DataType.FLOAT32)
        tensorImage.load(bitmap)

        val imageProcessor = org.tensorflow.lite.support.image.ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
            .build()

        return imageProcessor.process(tensorImage)
    }
}