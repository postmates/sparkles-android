package com.postmates.android.sparkles.model

import android.graphics.PointF

import java.math.BigDecimal

/**
 * Model to hold the input data from the user and relative point plotted on the graph
 */
data class SparklesDataPoint(private val dataPointValue: BigDecimal?) {

    var inputValue: BigDecimal? = null
    var graphValue: PointF? = null
    var isEmptyValue: Boolean = true

    init {
        this.inputValue = dataPointValue
        this.isEmptyValue = dataPointValue == null
    }

    // Hide the default constructor to make sure that the one with dataPointValue is always used!
    private constructor(): this(null)
}