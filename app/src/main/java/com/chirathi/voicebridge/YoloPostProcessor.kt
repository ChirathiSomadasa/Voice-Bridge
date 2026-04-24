package com.chirathi.voicebridge

import android.graphics.Rect
import android.util.Log
import kotlin.math.pow

data class BestDetection(
    val className: String,
    val confidence: Float,
    val box: Rect,
    val score: Float
)

object YoloPostProcessor {
    fun getBestDetectionFromImage(
        output: Array<Array<FloatArray>>,
        labels: List<String>,
        imgWidth: Int,
        imgHeight: Int,
        threshold: Float = 0.1f
    ): BestDetection? {

        // Novel Formula Constants
        val alpha = 0.6f
        val beta = 0.2f
        val gamma = 0.2f

        var bestDetection: BestDetection? = null
        var maxFinalScore = 0f

        val imageArea = (imgWidth * imgHeight).toFloat()
        val maxDistance = Math.sqrt((imgWidth * imgWidth + imgHeight * imgHeight).toDouble())

        // YOLOv5 output shape: [1][25200][5 + classes]
        val detections = output[0]

        for (i in detections.indices) {
            val detection = detections[i]

            // 1. Objectness score (Is there an object?)
            val objectness = detection[4]

            if (i % 1000 == 0) {
                Log.d("VoiceBridgeDebug", "Row $i Objectness: $objectness")
            }
            if (objectness < threshold) continue

            // 2. Get the best class score
            var maxClassScore = 0f
            var classIndex = -1

            // 5 වෙනි index එකේ සිට ඉදිරියට class scores පවතී
            for (c in labels.indices) {
                val classScore = detection[5 + c]
                if (classScore > maxClassScore) {
                    maxClassScore = classScore
                    classIndex = c
                }
            }

            // 3. Calculate actual confidence
            val confidence = objectness * maxClassScore
            if (confidence < threshold) continue

            // 4. Coordinates (YOLO format: cx, cy, w, h - Normalized 0 to 1)
            val cx = detection[0] * imgWidth
            val cy = detection[1] * imgHeight
            val w = detection[2] * imgWidth
            val h = detection[3] * imgHeight

            val x1 = (cx - w / 2).toInt().coerceAtLeast(0)
            val y1 = (cy - h / 2).toInt().coerceAtLeast(0)
            val x2 = (cx + w / 2).toInt().coerceAtMost(imgWidth)
            val y2 = (cy + h / 2).toInt().coerceAtMost(imgHeight)

            val box = Rect(x1, y1, x2, y2)

            // 5. Area Factor
            val area = w * h
            val areaFactor = area / imageArea

            // 6. Center Factor
            val centerDistance = Math.sqrt(
                ((cx - imgWidth / 2).toDouble().pow(2.0)) +
                        ((cy - imgHeight / 2).toDouble().pow(2.0))
            )
            val centerFactor = 1.0 - (centerDistance / maxDistance)

            // 7. NOVEL SCORING FORMULA
            val finalScore = (alpha * confidence) +
                    (beta * areaFactor) +
                    (gamma * centerFactor.toFloat())

            Log.d("VoiceBridge", "Detected: ${labels[classIndex]} | Conf: $confidence | Final: $finalScore")

            if (finalScore > maxFinalScore) {
                maxFinalScore = finalScore
                bestDetection = BestDetection(
                    className = labels[classIndex],
                    confidence = confidence,
                    box = box,
                    score = finalScore
                )
            }
        }

        return bestDetection
    }
}

//package com.chirathi.voicebridge
//
//import android.graphics.Rect
//import kotlin.math.pow
//
//data class BestDetection(
//    val className: String,
//    val confidence: Float,
//    val box: Rect,
//    val score: Float
//)
//
//object YoloPostProcessor {
//    fun getBestDetectionFromImage(
//        output: Array<Array<FloatArray>>,
//        labels: List<String>,
//        imgWidth: Int,
//        imgHeight: Int,
//        threshold: Float = 0.1f
//    ): BestDetection? {
//
//        val alpha = 0.5f
//        val beta = 0.3f
//        val gamma = 0.2f
//
//        var bestDetection: BestDetection? = null
//        var bestScore = 0f
//
//        val imageArea = imgWidth * imgHeight
//        val maxDistance = Math.sqrt((imgWidth * imgWidth + imgHeight * imgHeight).toDouble())
//
//        for (i in output[0].indices) {
//            val detection = output[0][i]
//
//            val objectness = detection[4]
//            if (objectness < threshold) continue
//
//            // Get best class
//            var maxClassScore = 0f
//            var classIndex = -1
//
//            for (c in labels.indices) {
//                val classScore = detection[5 + c]
//                if (classScore > maxClassScore) {
//                    maxClassScore = classScore
//                    classIndex = c
//                }
//            }
//
//            val confidence = objectness * maxClassScore
//
//            // YOLO format (center x,y, width, height)
//            val cx = detection[0] * imgWidth
//            val cy = detection[1] * imgHeight
//            val w = detection[2] * imgWidth
//            val h = detection[3] * imgHeight
//
//            val x1 = (cx - w / 2).toInt()
//            val y1 = (cy - h / 2).toInt()
//            val x2 = (cx + w / 2).toInt()
//            val y2 = (cy + h / 2).toInt()
//
//            val box = Rect(x1, y1, x2, y2)
//
//            //  AREA FACTOR
//            val area = w * h
//            val areaFactor = area / imageArea
//
//            // CENTER FACTOR
//            val centerDistance = Math.sqrt(
//                ((cx - imgWidth / 2).toDouble().pow(2.0)) +
//                        ((cy - imgHeight / 2).toDouble().pow(2.0))
//            )
//            val centerFactor = 1 - (centerDistance / maxDistance)
//
//            //  NOVEL FORMULA
//            val score = alpha * confidence +
//                    beta * areaFactor.toFloat() +
//                    gamma * centerFactor.toFloat()
//
//            if (score > bestScore) {
//                bestScore = score
//
//                bestDetection = BestDetection(
//                    className = labels[classIndex],
//                    confidence = confidence,
//                    box = box,
//                    score = score
//                )
//            }
//        }
//
//        return bestDetection
//    }
//
//}