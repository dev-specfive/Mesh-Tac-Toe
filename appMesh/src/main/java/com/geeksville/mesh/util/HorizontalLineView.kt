package com.geeksville.mesh.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class HorizontalLineView : View {
    private val paint = Paint()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    val lineWidth = 20f

    private fun init() {

        paint.color =// resources.getColor(android.R.color.holo_red_dark)
            Color.parseColor("#FFFFFF")
        paint.isAntiAlias = true
        paint.strokeWidth = lineWidth // Adjust thickness as needed
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lineWidthHalf = (lineWidth) / 2f
        canvas.let {
            alpha = 0.8f
            val margin = 0.1f // Adjust margin ratio as needed (10% margin in this example)

            // Calculate the drawing area after applying the margins
            val startXM = margin * width.toFloat()
            val startYM = margin * height.toFloat()
            val endXM = (1 - margin) * width.toFloat()
            val endYM = (1 - margin) * height.toFloat()

            val startX = 0f//width.toFloat()
            val startY = 0f// height.toFloat()
            val endX = width.toFloat()
            val endY = height.toFloat()

            when (linePosition) {
                0, 1, 2 -> {
                    val splitPart = (endY - startY) / 3
                    val y = when (linePosition) {
                        0 -> startY + (splitPart / 2) + lineWidthHalf
                        1 -> startY + (splitPart / 2) + splitPart
                        2 -> startY + (splitPart / 2) + (2 * splitPart) - lineWidthHalf
                        else -> 0f
                    }
                    canvas.drawLine(startXM, y, endXM, y, paint)
                }

                3, 4, 5 -> {
                    val splitPart = (endX - startX) / 3

                    val x = when (linePosition) {
                        3 -> startX + (splitPart / 2) + lineWidthHalf
                        4 -> startX + (splitPart / 2) + splitPart
                        5 -> startX + (splitPart / 2) + (2 * splitPart) - (1.5f * lineWidthHalf)
                        else -> 0f
                    }
                    canvas.drawLine(x, startYM, x, endYM, paint)
                }

                6, 7 -> {
                    if (linePosition == 6) {
                        val diagonalMargin = (margin) * kotlin.math.sqrt(2f) / 2
                        val rotationAngleDegrees = 0f
                        canvas.save()
                        canvas.rotate(
                            rotationAngleDegrees,
                            (startXM + endXM) / 2,
                            (startYM + endYM) / 2
                        )

                        canvas.drawLine(
                            startXM + diagonalMargin,
                            startYM + diagonalMargin,
                            endXM - diagonalMargin,
                            endYM - diagonalMargin,
                            paint
                        )
                        canvas.restore()
                    } else {
                        val diagonalMargin = (margin) * kotlin.math.sqrt(2f) / 2
                        val rotationAngleDegrees = 0f
                        canvas.save()
                        canvas.rotate(
                            rotationAngleDegrees,
                            (endXM + endXM) / 2,
                            (endYM + endYM) / 2
                        )

                        canvas.drawLine(
                            endXM - diagonalMargin,
                            startYM + diagonalMargin,
                            startXM + diagonalMargin,
                            endYM - diagonalMargin,
                            paint
                        )
                        canvas.restore()
                    }
                }
            }
        }
    }


    private var linePosition = 1
    fun setPosition(linePosition: Int) {
        this.linePosition = linePosition
        invalidate()
    }
}