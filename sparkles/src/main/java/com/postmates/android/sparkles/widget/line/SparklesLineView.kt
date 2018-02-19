package com.postmates.android.sparkles.widget.line

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.support.annotation.ColorInt
import android.support.annotation.Dimension
import android.support.annotation.IntegerRes
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.postmates.android.sparkles.R
import com.postmates.android.sparkles.helpers.AnimationHelper
import com.postmates.android.sparkles.helpers.Constants
import com.postmates.android.sparkles.model.AnimationType
import com.postmates.android.sparkles.model.PathDataHolder
import com.postmates.android.sparkles.widget.SparklesAdapter
import java.util.*

/**
 * A line graph with no axis and an optional base line represented by user input
 * provided as a list of [com.postmates.android.sparkles.model.SparklesDataPoint] via
 * [SparklesAdapter]
 */
class SparklesLineView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.pm_sparkles_DefaultSparkLinesView,
        defStyleRes: Int = R.style.pm_sparkles_DefaultSparkLinesViewStyle) :
        View(context, attrs, defStyleAttr, defStyleRes), SparklesAdapter.OnDataChangedListener {

    // For determining the styles
    @ColorInt private var lineColor: Int = 0
    @ColorInt private var baseLineColor: Int = 0
    @Dimension private var lineWidth: Float = 0.toFloat()
    @Dimension private var baseLineWidth: Float = 0.toFloat()
    @IntegerRes private var fillOpacityPercent: Int = 0

    private var animationType = AnimationType.NONE
    private var shouldAnimate: Boolean = false
    private var shouldFill: Boolean = false

    // For drawing the lines on canvas
    private val contentRect = RectF()
    private val solidLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dottedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val baseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var fillPathHolder: PathDataHolder? = null
    private var baseLinePathHolder: PathDataHolder? = null
    private val pathDataHolderList = ArrayList<PathDataHolder>()

    // Animations!
    private var animationSet: AnimatorSet? = null

    // Helpers
    private var lineGraphScale: LineGraphScale? = null

    /**
     * Sets the backing [SparklesAdapter] to generate the points to be graphed
     */
    var adapter: SparklesAdapter? = null
        set(value) {
            value?.let {
                field = value
                this.adapter!!.setListener(this)
                makeGraph()
            }
        }
    private val graphBottom: Float
        get() = height.toFloat() - paddingBottom

    init {
        attrs?.let {
            val a: TypedArray? = context.obtainStyledAttributes(attrs,
                    R.styleable.pm_sparkles_SparkLinesView, defStyleAttr, defStyleRes)
            try {
                a?.let {
                    initValues(a)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Problem Initializing Styleable\n ", ex)
            } finally {
                a?.recycle()
            }
        }

        setValues()
    }

    private fun initValues(a: TypedArray) {
        // Init Main Line
        lineColor = a.getColor(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_lineColor, 0)
        lineWidth = a.getDimension(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_lineWidth, 0f)

        // Init Base Line
        baseLineColor = a.getColor(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_baseLineColor, 0)
        baseLineWidth = a.getDimension(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_baseLineWidth, 0f)

        // Init graph properties
        shouldFill = a.getBoolean(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_shouldFill, true)
        fillOpacityPercent = a.getInteger(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_fillOpacityPercent, 0)

        val declaredAnim = a.getInt(R.styleable.pm_sparkles_SparkLinesView_pm_sparkles_animationType, 0)
        animationType = AnimationType.values()[declaredAnim]
        shouldAnimate = animationType !== AnimationType.NONE
    }

    private fun setValues() {
        // set the solid line path style
        setStrokeStyle(solidLinePaint, lineColor, lineWidth, null)

        // set the dashed line path style
        setStrokeStyle(dottedLinePaint, lineColor, lineWidth,
                DashPathEffect(floatArrayOf(1f, 4f), 0f))

        // set the baseline path style
        setStrokeStyle(baseLinePaint, baseLineColor, baseLineWidth, null)

        // set the fill style
        fillPaint.style = Paint.Style.FILL
        fillPaint.color = lineColor
        fillPaint.alpha = fillOpacityPercent.times(255).div(100)

        setShouldFill(shouldFill)

        if (shouldAnimate) {
            animationSet = AnimatorSet()
        }
    }

    private fun setStrokeStyle(paint: Paint,
                               @ColorInt color: Int,
                               @Dimension width: Float,
                               pathEffect: PathEffect?) {

        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.pathEffect = pathEffect
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        calculateViewPort()
        makeGraph()
    }

    private fun makeGraph() {
        if (width == 0 || height == 0) {
            Log.e(TAG, "Invalid value for adapter or measurement")
            return
        }

        if (this.adapter == null || this.adapter!!.count < 2) {
            Log.e(TAG, "Invalid adapter values")
            resetData()
            return
        }

        lineGraphScale = LineGraphScale(adapter!!, contentRect, lineWidth, shouldFill)

        // Make sure to reset all paths to prevent any overlaps
        resetPaths()

        createPathAndFill()
        createBaseline()

        // force redraw the graph
        invalidate()
    }

    private fun createPathAndFill() {

        var currentX: Float
        var currentY: Float
        var prevX = 0f
        var prevY = 0f

        val fillPath = Path()

        for (i in 0 until this.adapter!!.count) {
            currentX = lineGraphScale!!.getX(this.adapter!!.getGraphX(i))
            currentY = lineGraphScale!!.getY(this.adapter!!.getGraphY(i))

            if (i == 0) {
                prevX = currentX
                prevY = currentY
                fillPath.moveTo(currentX, currentY)
            } else {
                fillPath.lineTo(currentX, currentY)
            }

            val startPoint = PointF(prevX, prevY)
            val endPoint = PointF(currentX, currentY)

            val path = Path()
            path.moveTo(startPoint.x, startPoint.y)
            path.lineTo(endPoint.x, endPoint.y)

            val paintStyle = if (this.adapter!!.isEmptyValue(i)) dottedLinePaint else solidLinePaint
            pathDataHolderList.add(PathDataHolder(path, paintStyle, startPoint, endPoint))

            prevX = currentX
            prevY = currentY
        }

        // Close fill path and then paint should fill appropriately
        if (shouldFill) {
            val fillEdge = graphBottom
            val lastX = lineGraphScale!!.getX((this.adapter!!.count - 1).toFloat())
            fillPath.lineTo(lastX, fillEdge)
            fillPath.lineTo(paddingStart.toFloat(), fillEdge)
            fillPath.close()
            fillPathHolder = PathDataHolder(fillPath, fillPaint)
        }
    }

    private fun createBaseline() {
        if (adapter!!.hasBaseLine()) {
            val baseLinePath = Path()
            val scaledBaseLine = lineGraphScale!!.getY(this.adapter!!.graphBaseline)
            baseLinePath.moveTo(0f, scaledBaseLine)
            baseLinePath.lineTo(width.toFloat(), scaledBaseLine)
            baseLinePathHolder = PathDataHolder(baseLinePath, baseLinePaint)
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        calculateViewPort()
        makeGraph()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPath(canvas, baseLinePathHolder)
        for (holder in pathDataHolderList) {
            drawPath(canvas, holder)
        }
        drawPath(canvas, fillPathHolder)
    }

    private fun drawPath(canvas: Canvas, holder: PathDataHolder?) {
        if (holder != null) {
            canvas.drawPath(holder.path, holder.paint)
        }
    }

    private fun performAnimations() {

        if (!shouldAnimate || pathDataHolderList == null || animationSet == null) {
            Log.i(TAG, "Skipping animation due to invalid input")
            return
        }

        if (animationSet!!.isRunning) {
            // Cancel any existing animations
            Log.i(TAG, "Cancelling Existing Animation")
            animationSet!!.cancel()
        }

        when (animationType) {
            AnimationType.LINE_PATH -> playLinePathAnimations()
            AnimationType.TRANSLATE_UP -> playTranslateUpAnimations()
            AnimationType.NONE -> Log.i(TAG, "Animation type undefined")
            else -> Log.i(TAG, "Animation type undefined")
        }
    }

    private fun playLinePathAnimations() {
        Log.d(TAG, ">Building Line Path Animations")
        val animationsList = ArrayList<Animator>()

        if (baseLinePathHolder != null) {
            val baseLineAnimator = AnimationHelper.getLinePathAnimator(this,
                    baseLinePathHolder!!.path, Constants.ANIM_DURATION_BASE_LINE_DRAW_MS)
            if (baseLineAnimator != null) {
                animationsList.add(baseLineAnimator)
            }
        }

        if (!pathDataHolderList.isEmpty()) {
            // Equally distribute the line animations so it adds to the total time required
            val perLineAnimDuration = Constants.ANIM_DURATION_LINE_DRAW_MS / pathDataHolderList.size
            for ((path) in pathDataHolderList) {
                val lineAnimator = AnimationHelper.getLinePathAnimator(this,
                        path, perLineAnimDuration)
                if (lineAnimator != null) {
                    animationsList.add(lineAnimator)
                }
            }
        }

        if (shouldFill && fillPathHolder != null) {
            val fillAnimator = AnimationHelper.getLinePathAnimator(this,
                    fillPathHolder!!.path, Constants.ANIM_DURATION_FILL_DRAW_MS)
            if (fillAnimator != null) {
                animationsList.add(fillAnimator)
            }
        }

        animationSet!!.playSequentially(animationsList)
        Log.i(TAG, ">Starting Line Path Animation")
        animationSet!!.start()
    }

    private fun playTranslateUpAnimations() {
        Log.d(TAG, ">Building Translate Up Animations")

        if (baseLinePathHolder != null) {
            val baseLineAnimator = AnimationHelper.getLinePathAnimator(this,
                    baseLinePathHolder!!.path, Constants.ANIM_DURATION_BASE_LINE_DRAW_MS)
            baseLineAnimator?.start()
        }

        val animatorList = ArrayList<Animator>()

        if (!pathDataHolderList.isEmpty()) {
            // Equally distribute the line animations so it adds to the total time required
            for (i in pathDataHolderList.indices) {
                val holder = pathDataHolderList[i]
                val lineAnimator = AnimationHelper.getTranslateUpAnimator(this,
                        holder, graphBottom, Constants.ANIM_DURATION_LINE_DRAW_MS)
                if (lineAnimator != null) {
                    animatorList.add(lineAnimator)
                }
            }
        }

        if (shouldFill && fillPathHolder != null) {
            val fillAnimator = AnimationHelper.getFillTranslationAnimator(this,
                    fillPathHolder!!.path, graphBottom, Constants.ANIM_DURATION_LINE_DRAW_MS)
            if (fillAnimator != null) {
                animatorList.add(fillAnimator)
            }
        }

        animationSet!!.startDelay = Constants.ANIM_DURATION_BASE_LINE_DRAW_MS
        animationSet!!.playTogether(animatorList)
        Log.i(TAG, ">Starting Translate Up Animation")
        animationSet!!.start()
    }

    /**
     * GETTERS & SETTERS
     */
    fun setLineColor(@ColorInt lineColor: Int) {
        this.lineColor = lineColor
        setValues()
        invalidate()
    }

    fun setLineWidth(@Dimension lineWidth: Float) {
        this.lineWidth = lineWidth
        setValues()
        invalidate()
    }

    fun setBaselineColor(@ColorInt baseLineColor: Int) {
        this.baseLineColor = baseLineColor
        setValues()
        invalidate()
    }

    fun setBaselineWidth(@Dimension baseLineWidth: Float) {
        this.baseLineWidth = baseLineWidth
        setValues()
        invalidate()
    }

    fun setShouldFill(fill: Boolean) {
        if (this.shouldFill != fill) {
            this.shouldFill = fill
            makeGraph()
        }
    }

    private fun resetData() {
        resetPathList()
        lineGraphScale = null
        resetPaths()
        invalidate()
    }

    private fun resetPaths() {
        if (pathDataHolderList != null) {
            resetPathList()
        }
        resetHolderPath(fillPathHolder)
        resetHolderPath(baseLinePathHolder)
    }

    private fun resetPathList() {
        for (holder in pathDataHolderList) {
            resetHolderPath(holder)
        }
        pathDataHolderList.clear()
    }

    private fun resetHolderPath(holder: PathDataHolder?) {
        holder?.path?.reset()
    }

    /**
     * This updates the original path with a partial path
     * that is used for animations by the ValueAnimator
     * @param originalPath - The original path as drawn on canvas
     * @param currentPath - The partial path as computed by the ValueAnimator
     */
    fun updateAnimationPath(originalPath: Path, currentPath: Path) {
        originalPath.reset()
        originalPath.addPath(currentPath)
        invalidate()
    }

    /**
     * Gets the rect representing the 'content area' of the view. This is essentially the bounding
     * rect minus any padding.
     */
    private fun calculateViewPort() {
        contentRect?.set(paddingStart.toFloat(), paddingTop.toFloat(),
                (width - paddingEnd).toFloat(),
                graphBottom
        )
    }

    override fun onDataChanged() {
        makeGraph()
        if (shouldAnimate) {
            performAnimations()
        }
    }

    override fun onDataInvalidated() {
        resetData()
    }

    companion object {

        private val TAG = SparklesLineView::class.java.simpleName
    }
}