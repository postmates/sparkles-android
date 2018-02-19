package com.postmates.android.sparkles.widget.line

import android.graphics.RectF
import android.support.annotation.Dimension
import com.postmates.android.sparkles.widget.SparklesAdapter

/**
 * Helps scale the graph coordinates to fit in the view.
 */
class LineGraphScale(adapter: SparklesAdapter,
                     contentRect: RectF,
                     @Dimension lineWidth: Float,
                     shouldFill: Boolean) {

    // Dimensions of the graph view
    private val width: Float
    private val height: Float

    // Scale factor for coordinate values
    private val xScale: Float
    private val yScale: Float

    // Translation for x and y after being scaled
    private val xTranslation: Float
    private val yTranslation: Float

    init {
        // Make sure the line width is accommodated in the view
        val lineWidthOffset: Float = if (shouldFill) 0f else lineWidth
        val bounds = adapter.dataBounds

        width = contentRect.width().minus(lineWidthOffset)
        height = contentRect.height().minus(lineWidthOffset)

        // Calculate scale based on the view dimens
        xScale = width.div((bounds.right.minus(bounds.left)))
        yScale = height.div(bounds.bottom.minus(bounds.top))

        // Calculate x & y translation
        xTranslation = contentRect.left.plus(lineWidthOffset).minus(bounds.left.times(xScale))
        yTranslation = contentRect.top.plus(lineWidthOffset).plus(bounds.top.times(yScale))

        // Adjust the rectangle size based on the bounds
        bounds.inset(if (bounds.width() == 0f) -1f else 0f, if (bounds.height() == 0f) -1f else 0f)
    }

    /**
     * Scaled 'x' value per view dimensions and raw position
     */
    fun getX(rawX: Float): Float {
        return rawX.times(xScale).plus(xTranslation)
    }

    /**
     * Scaled 'y' value per view dimensions and raw position
     */
    fun getY(rawY: Float): Float {
        return height.plus(yTranslation).minus(rawY.times(yScale))
    }

    /**
     * Scaled Bottom coordinate for the graph
     */
    fun getBottom(): Float {
        return height.plus(yTranslation)
    }
}