package com.geeksville.mesh.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.recyclerview.widget.RecyclerView

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean,
    private val borderWidth: Int = 5,
    private val borderColor: Int = Color.White.toArgb()
) : RecyclerView.ItemDecoration() {

    private val borderPaint: Paint = Paint()

    init {
        borderPaint.color = borderColor
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth.toFloat() // Convert to float for Paint.setStrokeWidth
    }

    private var winPositions: Array<IntArray> = emptyArray()

    fun setWinPositions(positions: Array<IntArray>) {
        winPositions = positions
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val itemCount = parent.adapter?.itemCount ?: 0
        val childCount = parent.childCount
        val lastRowStart = itemCount - spanCount

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val layoutParams = child.layoutParams as RecyclerView.LayoutParams
            val position = layoutParams.viewAdapterPosition
            val column = position % spanCount

            val left = child.left - layoutParams.leftMargin - (spacing / 2)
            val right = child.right + layoutParams.rightMargin + (spacing / 2)
            val top = child.top - layoutParams.topMargin - (spacing / 2)
            val bottom = child.bottom + layoutParams.bottomMargin + (spacing / 2)

            // Draw vertical lines between items
            if (column < spanCount - 1) {
                val startX = right.toFloat()
                val startY = top.toFloat()
                val endX = right.toFloat()
                val endY = bottom.toFloat()
                canvas.drawLine(startX, startY, endX, endY, borderPaint)
            }

            // Draw horizontal lines between items
            if (position < lastRowStart) {
                val startX = left.toFloat()
                val startY = bottom.toFloat()
                val endX = right.toFloat()
                val endY = bottom.toFloat()
                canvas.drawLine(startX, startY, endX, endY, borderPaint)
            }

        }


    }
}
