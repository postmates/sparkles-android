package com.postmates.android.sparkles.model

import android.graphics.PointF

import java.math.BigDecimal

/**
 * Model to hold the input data from the user and relative point plotted on the graph
 */
class SparklesDataPoint {

    var inputValue: BigDecimal? = null
    var graphValue: PointF? = null
    var isEmptyValue: Boolean = true

    constructor(value: BigDecimal?) {
        this.inputValue = value
        this.isEmptyValue = value == null
    }

    // Hide the default constructor to make sure that the one with value is always used!
    private constructor()
}