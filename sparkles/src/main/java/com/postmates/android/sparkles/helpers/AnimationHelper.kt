package com.postmates.android.sparkles.helpers

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import com.postmates.android.sparkles.helpers.Constants.LIB_TAG
import com.postmates.android.sparkles.model.PathDataHolder
import com.postmates.android.sparkles.widget.line.SparklesLineView

/**
 * Animation helper for line & block animations
 */
object AnimationHelper {

    /**
     * Returns an animator which helps animating a path from an origin to its final point
     * @param sparklesLineView - The original sparkles graph view for invoking redraws
     * @param holder - The path holder with relevant information about the original path
     * @param yOrigin - Origin on graph from where the translation should start
     * @param durationMs - Duration for which the path animation should run
     * @return - Value Animator which performs translate up animation.
     *
     * Note: call start() when ready.
     */
    fun getTranslateUpAnimator(sparklesLineView: SparklesLineView,
                               holder: PathDataHolder,
                               yOrigin: Float,
                               durationMs: Long): ValueAnimator? {

        if (!holder.hasValidPointInfo()) {
            Log.w(LIB_TAG, "Input path start & end points information was invalid")
            return null
        }

        // Since animator will redraw the path, reset for now
        holder.path.reset()
        val animatedPath = Path()

        return getValueAnimator().apply {
            duration = durationMs
            addUpdateListener { animation ->
                animatedPath.reset()

                val animatedValue = animation.animatedValue as Float
                val startY = animatedValue * (holder.startPoint!!.y - yOrigin) + yOrigin
                val endY = animatedValue * (holder.endPoint!!.y - yOrigin) + yOrigin

                animatedPath.moveTo(holder.startPoint.x, startY)
                animatedPath.lineTo(holder.endPoint.x, endY)

                sparklesLineView.updateAnimationPath(holder.path, animatedPath)
            }
        }
    }

    /**
     * Returns an animator which helps animating a path along its length
     * @param sparklesLineView - The original sparkles graph view for invoking redraws
     * @param originalPath - The path to be animated
     * @param durationMs - Duration for which the path animation should run
     * @return - Value Animator which can animate path drawings.
     *
     * Note: call start() when ready.
     */
    fun getLinePathAnimator(sparklesLineView: SparklesLineView,
                            originalPath: Path,
                            durationMs: Long): ValueAnimator? {

        // get path length
        val pathMeasure = PathMeasure(originalPath, false)
        val endLength = pathMeasure.length
        val animatedPath = Path()

        // Since animator will redraw the path, reset for now
        originalPath.reset()

        return if (endLength <= 0) null else {
            getValueAnimator().apply {
                duration = durationMs
                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    val animatedPathLength = animatedValue * endLength
                    animatedPath.reset()
                    pathMeasure.getSegment(0f, animatedPathLength, animatedPath, true)
                    sparklesLineView.updateAnimationPath(originalPath, animatedPath)
                }
            }
        }
    }

    /**
     * Returns an animator which helps translate a fill path from an origin to its final point
     * @param sparklesLineView - The original sparkles graph view for invoking redraws
     * @param originalFillPath - The fill path to be animated
     * @param yOrigin - Origin on graph from where the translation should start
     * @param durationMs - Duration for which the path animation should run
     * @return - Value Animator which performs the fill translation animation.
     *
     * Note: call start() when ready.
     */
    fun getFillTranslationAnimator(sparklesLineView: SparklesLineView,
                                   originalFillPath: Path,
                                   yOrigin: Float,
                                   durationMs: Long): ValueAnimator? {

        val pathMeasure = PathMeasure(originalFillPath, true)
        val endLength = pathMeasure.length
        val animatedPath = Path()

        // Since animator will redraw the path, reset for now
        originalFillPath.reset()

        return if (endLength <= 0) null else {
            getValueAnimator().apply {
                duration = durationMs
                addUpdateListener { animation ->
                    val animatedValue = animation.animatedValue as Float
                    animatedPath.reset()

                    pathMeasure.getSegment(0f, endLength, animatedPath, true)

                    val matrix = Matrix()
                    matrix.postTranslate(0f, -yOrigin * animatedValue + yOrigin)
                    animatedPath.transform(matrix)

                    sparklesLineView.updateAnimationPath(originalFillPath, animatedPath)
                }
            }
        }
    }

    private fun getValueAnimator(): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 1f)
    }
}