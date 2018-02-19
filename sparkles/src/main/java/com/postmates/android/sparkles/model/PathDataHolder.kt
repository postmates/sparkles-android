package com.postmates.android.sparkles.model

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF

/**
 * Convenience class to hold information related to a Line.
 *
 * Required:
 * @param path - Information related to the Path
 * @param paint - Information related to the Paint
 *
 * Optional:
 * @param startPoint - starting point for the Path
 * @param endPoint - ending point for the Path
 *
 */
data class PathDataHolder constructor(val path: Path,
                                      val paint: Paint,
                                      val startPoint: PointF? = null,
                                      val endPoint: PointF? = null) {

    constructor(path: Path, paint: Paint) : this(path, paint, null, null)

    fun hasValidPointInfo(): Boolean {
        return startPoint != null && endPoint != null
    }
}