package com.postmates.android.sparkles.helpers

import android.graphics.PointF

import com.postmates.android.sparkles.model.SparklesDataPoint

/**
 * General Util object for holding common util methods
 */
object SparklesUtil {

    fun getGraphPoint(dataPoint: SparklesDataPoint?) = dataPoint?.graphValue

    fun getGraphPointX(graphPoint: PointF?) = graphPoint?.x ?: -1f

    fun getGraphPointY(graphPoint: PointF?) = graphPoint?.y ?: -1f

    fun calculatePercent(value: Float, maxValue: Float) = when (maxValue == 0f) {
        true -> 0f
        else -> (value / maxValue) * 100f
    }
}