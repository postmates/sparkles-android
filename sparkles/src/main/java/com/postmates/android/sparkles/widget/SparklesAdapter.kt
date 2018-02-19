package com.postmates.android.sparkles.widget

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.postmates.android.sparkles.helpers.Constants
import com.postmates.android.sparkles.helpers.SparklesUtil
import com.postmates.android.sparkles.model.SparklesDataPoint

/**
 * Adapter to hold and process the user input data.
 *
 * Notifies the view about any updates in the data set.
 */
class SparklesAdapter {

    companion object {
        // Some extra top/bottom view space
        private const val VERTICAL_BOUND_OFFSET = 10f
    }

    // User Inputs
    private var inputDataPoints: List<SparklesDataPoint>? = null
    private var inputBaseline: SparklesDataPoint? = null

    // Graph Update Listener
    private var onDataChangedListener: OnDataChangedListener? = null

    val count: Int
        get() {
            return if (inputDataPoints == null) 0 else inputDataPoints!!.size
        }

    /**
     * @return the graph representation of the Y value of the desired baseLine.
     */
    val graphBaseline: Float
        get() {
            return SparklesUtil.getGraphPointY(SparklesUtil.getGraphPoint(inputBaseline))
        }

    /**
     * Gets the float representation of the boundaries of the entire dataset. By default, this will
     * be the min and max of the actual data points in the adapter. This can be overridden for
     * custom behavior. When overriding, make sure to set RectF's values such that:
     *
     *  * left = the minimum X value
     *  * top = the minimum Y value
     *  * right = the maximum X value
     *  * bottom = the maximum Y value
     *
     * @return a RectF of the bounds desired around this adapter's data.
     */
    // reference: rectF (left, top, right, bottom)
    val dataBounds: RectF
        get() {
            var minY = if (hasBaseLine()) graphBaseline else Float.MAX_VALUE
            var maxY = if (hasBaseLine()) minY else -Float.MAX_VALUE
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE

            for (i in 0 until count) {
                val x = getGraphX(i)
                minX = Math.min(minX, x)
                maxX = Math.max(maxX, x)

                val y = getGraphY(i)
                minY = Math.min(minY, y)
                maxY = Math.max(maxY, y)
            }

            Log.d(Constants.LIB_TAG, "Rect Dimens:\nminX: $minX, minY: $minY" +
                    "\nmaxX: $maxX, maxY: $maxY")

            return RectF(minX, minY.minus(VERTICAL_BOUND_OFFSET),
                    maxX, maxY.plus(VERTICAL_BOUND_OFFSET))
        }

    fun setInput(dataPoints: List<SparklesDataPoint>, baseline: SparklesDataPoint?) {
        inputDataPoints = dataPoints
        inputBaseline = baseline ?: SparklesDataPoint(null)
        processInput()
    }

    private fun processInput() {
        // Find the max value in the input
        val maxValue = (0 until inputDataPoints!!.size)
                .mapNotNull { inputDataPoints!![it].inputValue }
                .map { it.toFloat() }
                .max() ?: -Float.MAX_VALUE

        Log.d(Constants.LIB_TAG, "Final Max Value: $maxValue")

        // Compute the input points relative to the max value to plot on graph
        var lastGoodValue = 0f

        for (i in 0 until inputDataPoints!!.size) {
            val point = inputDataPoints!![i]
            if (!point.isEmptyValue) {
                lastGoodValue = SparklesUtil.calculatePercent(point.inputValue!!.toFloat(), maxValue)
            }
            Log.d(Constants.LIB_TAG, "Point at $i : $lastGoodValue" +
                    ", isMissing: ${point.isEmptyValue}")
            point.graphValue = PointF(i.toFloat(), lastGoodValue)
        }

        // Calculate the relative baseline to plot on the graph
        val inputBaseline = inputBaseline!!.inputValue
        this.inputBaseline!!.graphValue = if (inputBaseline == null) null else {
            PointF(0f, SparklesUtil.calculatePercent(inputBaseline.toFloat(), maxValue))
        }

        Log.d(Constants.LIB_TAG, "Calculated Graph Baseline: $graphBaseline")
        notifyDataSetChanged()
    }

    /**
     * @return whether the value at a given index is null or empty.
     */
    fun isEmptyValue(index: Int): Boolean {
        return inputDataPoints!![index].isEmptyValue
    }

    /**
     * @return the float representation of the X value of the point at the given index.
     */
    fun getGraphX(index: Int): Float {
        return SparklesUtil.getGraphPointX(SparklesUtil.getGraphPoint(inputDataPoints!![index]))
    }

    /**
     * @return the float representation of the Y value of the point at the given index.
     */
    fun getGraphY(index: Int): Float {
        return SparklesUtil.getGraphPointY(SparklesUtil.getGraphPoint(inputDataPoints!![index]))
    }

    fun hasBaseLine(): Boolean {
        return graphBaseline >= 0
    }

    fun setListener(listener: OnDataChangedListener) {
        onDataChangedListener = listener
    }

    fun notifyDataSetChanged() {
        onDataChangedListener?.onDataChanged()
    }

    fun notifyDataSetInvalidated() {
        onDataChangedListener?.onDataInvalidated()
    }

    interface OnDataChangedListener {
        fun onDataChanged()
        fun onDataInvalidated()
    }
}