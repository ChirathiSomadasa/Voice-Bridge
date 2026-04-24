package com.chirathi.voicebridge

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YoloTfliteDetector(context: Context, modelName: String) {
    private var interpreter: Interpreter

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(loadModelFile(context, modelName), options)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun detect(bitmap: Bitmap): Array<Array<FloatArray>> {
        // YOLOv5 standard input size = 640x640
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
        val input = convertBitmapToByteBuffer(resizedBitmap)

        // Output shape: [1, 25200, 53]
        // 25200 boxes, 53 values (cx, cy, w, h, conf + 48 classes)
        val output = Array(1) { Array(25200) { FloatArray(53) } }
        interpreter.run(input, output)
        return output
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * 640 * 640 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(640 * 640)
        bitmap.getPixels(intValues, 0, 640, 0, 0, 640, 640)
        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16 and 0xFF) / 255f))
            byteBuffer.putFloat(((pixelValue shr 8 and 0xFF) / 255f))
            byteBuffer.putFloat(((pixelValue and 0xFF) / 255f))
        }
        return byteBuffer
    }
}