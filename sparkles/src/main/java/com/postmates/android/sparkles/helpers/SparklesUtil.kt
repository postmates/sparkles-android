package com.postmates.android.sparkles.helpers

import android.graphics.PointF

import com.postmates.android.sparkles.model.SparklesDataPoint

/**
 * General Util object for holding common util methods
 */
object SparklesUtil {

    fun getGraphPoint(dataPoint: SparklesDataPoint?): PointF? {
        return dataPoint?.graphValue
    }

    fun getGraphPointX(graphPoint: PointF?): Float {
        return graphPoint?.x ?: -1f
    }

    fun getGraphPointY(graphPoint: PointF?): Float {
        return graphPoint?.y ?: -1f
    }

    fun calculatePercent(value: Float, maxValue: Float): Float {
        return if (maxValue != 0f) {
            value / maxValue * 100f
        } else 0f
    }
}