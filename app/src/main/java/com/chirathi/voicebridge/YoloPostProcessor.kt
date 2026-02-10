package com.chirathi.voicebridge

import android.graphics.Rect

data class BestDetection(
    val className: String,
    val confidence: Float,
    val box: Rect

)

object YoloPostProcessor {

    /**
     * Find detection with MAX confidence from YOLO output
     * output: Array[1][25200][85]
     * labels: list of class names
     */
    fun getBestDetectionFromImage(
        output: Array<Array<FloatArray>>,
        labels: List<String>,
        threshold: Float = 0.25f
    ): BestDetection? {

        var bestConfidence = 0f
        var bestClassIndex = -1

        for (i in output[0].indices) {
            val detection = output[0][i]

            val objectness = detection[4]
            if (objectness < threshold) continue

            // Find best class for this detection
            var maxClassScore = 0f
            var maxClassIndex = -1

            for (c in labels.indices) {
                val classScore = detection[5 + c]
                if (classScore > maxClassScore) {
                    maxClassScore = classScore
                    maxClassIndex = c
                }
            }

            val finalConfidence = objectness * maxClassScore
            if (finalConfidence > bestConfidence) {
                bestConfidence = finalConfidence
                bestClassIndex = maxClassIndex
            }
        }

        return if (bestClassIndex != -1) {
            BestDetection(
                className = labels[bestClassIndex],
                confidence = bestConfidence,
                box = TODO()
            )
        } else {
            null
        }
    }

    /**
     * Build readable sentence for detected object
     */
    fun buildBestSentence(result: BestDetection?): String {
        return result?.let {
            val percent = String.format("%.2f", it.confidence * 100)
            "Detected: ${it.className} ($percent%)"
        } ?: "No object detected"
    }
}