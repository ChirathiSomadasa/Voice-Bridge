package com.chirathi.voicebridge

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BoundingBoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val boundingBoxes = mutableListOf<DetectionBox>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        style = Paint.Style.FILL
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    data class DetectionBox(
        val rect: RectF,
        val label: String,
        val confidence: Float,
        val color: Int
    )

    fun updateBoxes(newBoxes: List<DetectionBox>) {
        boundingBoxes.clear()
        boundingBoxes.addAll(newBoxes)
        invalidate()
    }

    fun clearBoxes() {
        boundingBoxes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (box in boundingBoxes) {
            // Draw bounding box
            paint.color = box.color
            canvas.drawRect(box.rect, paint)

            // Draw label background
            val text = "${box.label} ${(box.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(text)
            val textHeight = textPaint.textSize

            labelBackgroundPaint.color = box.color
            val labelRect = RectF(
                box.rect.left,
                box.rect.top - textHeight - 8,
                box.rect.left + textWidth + 16,
                box.rect.top
            )
            canvas.drawRect(labelRect, labelBackgroundPaint)

            // Draw label text
            canvas.drawText(
                text,
                box.rect.left + 8,
                box.rect.top - 8,
                textPaint
            )
        }
    }
}