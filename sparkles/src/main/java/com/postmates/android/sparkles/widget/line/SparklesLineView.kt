package com.postmates.android.sparkles.widget.line

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Dimension
import androidx.annotation.IntegerRes
import com.postmates.android.sparkles.R
import com.postmates.android.sparkles.helpers.AnimationHelper
import com.postmates.android.sparkles.helpers.Constants
import com.postmates.android.sparkles.helpers.Constants.LIB_TAG
import com.postmates.android.sparkles.model.AnimationType
import com.postmates.android.sparkles.model.AnimationType.LINE_PATH
import com.postmates.android.sparkles.model.AnimationType.NONE
import com.postmates.android.sparkles.model.AnimationType.TRANSLATE_UP
import com.postmates.android.sparkles.model.PathDataHolder
import com.postmates.android.sparkles.widget.SparklesAdapter

/**
 * A line graph with no axis and an optional base line represented by user input
 * provided as a list of [com.postmates.android.sparkles.model.SparklesDataPoint] via
 * [SparklesAdapter]
 */
class SparklesLineView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.pm_sparkles_DefaultSparkLinesView,
        defStyleRes: Int = R.style.pm_sparkles_DefaultSparkLinesViewStyle)
    : View(context, attrs, defStyleAttr, defStyleRes), SparklesAdapter.OnDataChangedListener {

    // For drawing the lines & shapes on canvas
    private val contentRect = RectF()
    private val solidLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dottedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val baseLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Path holders
    private var fillPathHolder: PathDataHolder? = null
    private var baseLinePathHolder: PathDataHolder? = null
    private val pathDataHolderList = ArrayList<PathDataHolder>()

    // Animations!
    private var animationSet: AnimatorSet? = null

    // Helpers
    private var lineGraphScale: LineGraphScale? = null

    @ColorInt
    var lineColor: Int = 0
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    @ColorInt
    var baseLineColor: Int = 0
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    @Dimension
    var lineWidth: Float = 0f
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    @Dimension
    var baseLineWidth: Float = 0f
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    @IntegerRes
    var fillOpacityPercent: Int = 0
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    var animationType: AnimationType = NONE
        set(value) {
            field = value
            setValues()
            invalidate()
        }

    var shouldFill: Boolean = false
        set(value) {
            field = value
            makeGraph()
        }

    var adapter: SparklesAdapter? = null
        set(value) {
            value?.let {
                field = value
                it.setListener(this)
                makeGraph()
            }
        }

    private val graphBottom: Float
        get() {
            return height.toFloat().minus(paddingBottom)
        }

    init {
        attrs?.let {
            val a: TypedArray? = context.obtainStyledAttributes(attrs,
                    R.styleable.pm_sparkles_SparkLinesView, defStyleAttr, defStyleRes)
            try {
                a?.let {
                    initValues(a)
                }
            } catch (ex: Exception) {
                Log.e(LIB_TAG, "Problem Initializing Styleable\n ", ex)
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
        fillPaint.apply {
            style = Paint.Style.FILL
            color = lineColor
            alpha = fillOpacityPercent.times(255).div(100)
        }

        if (shouldAnimate()) {
            animationSet = AnimatorSet()
        }
    }

    private fun setStrokeStyle(paint: Paint, @ColorInt colorRes: Int,
                               @Dimension width: Float, effect: PathEffect?) {
        with (paint) {
            style = Paint.Style.STROKE
            color = colorRes
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = effect
        }
    }

    private fun makeGraph() {
        if (width == 0 || height == 0 || adapter == null) {
            Log.e(LIB_TAG, "Invalid value for adapter or measurement")
            return
        }

        if (adapter!!.count < 2) {
            Log.e(LIB_TAG, "Invalid or insufficient data points")
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

        for (i in 0 until adapter!!.count) {
            currentX = lineGraphScale!!.getX(this.adapter!!.getGraphX(i))
            currentY = lineGraphScale!!.getY(this.adapter!!.getGraphY(i))

            when (i) {
                0 -> {
                    prevX = currentX
                    prevY = currentY
                    fillPath.moveTo(currentX, currentY)
                }
                else -> fillPath.lineTo(currentX, currentY)
            }

            val startPoint = PointF(prevX, prevY)
            val endPoint = PointF(currentX, currentY)

            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                lineTo(endPoint.x, endPoint.y)
            }

            val paintStyle = when (adapter!!.isEmptyValue(i)) {
                true -> dottedLinePaint
                false -> solidLinePaint
            }

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
        holder?.let {
            canvas.drawPath(it.path, it.paint)
        }
    }

    private fun performAnimations() {

        if (!shouldAnimate() || animationSet == null || pathDataHolderList.isEmpty()) {
            Log.i(LIB_TAG, "Skipping animation due to invalid input")
            return
        }

        with (animationSet!!) {
            if (isRunning) {
                // Cancel any existing animations
                Log.i(LIB_TAG, "Cancelling Existing Animation")
                cancel()
            }
        }

        when (animationType) {
            LINE_PATH -> playLinePathAnimations()
            TRANSLATE_UP -> playTranslateUpAnimations()
            else -> Log.i(LIB_TAG, "Animation type undefined or unsupported")
        }
    }

    private fun playLinePathAnimations() {
        Log.d(LIB_TAG, "Building Line Path Animations")
        val animationsList = ArrayList<Animator>()

        baseLinePathHolder?.let {
            AnimationHelper.getLinePathAnimator(this, it.path,
                    Constants.ANIM_DURATION_BASE_LINE_DRAW_MS)?.apply {
                animationsList.add(this)
            }
        }

        if (!pathDataHolderList.isEmpty()) {
            // Equally distribute the line animations so it adds to the total time required
            val perLineAnimDuration = Constants.ANIM_DURATION_LINE_DRAW_MS / pathDataHolderList.size

            pathDataHolderList.forEach {
                AnimationHelper.getLinePathAnimator(this, it.path,
                        perLineAnimDuration)?.apply {
                    animationsList.add(this)
                }
            }
        }

        if (shouldFill) {
            fillPathHolder?.let {
                AnimationHelper.getLinePathAnimator(this, it.path,
                        Constants.ANIM_DURATION_FILL_DRAW_MS)?.apply {
                    animationsList.add(this)
                }
            }
        }

        with(animationSet!!) {
            playSequentially(animationsList)
            start()
        }

        Log.i(LIB_TAG, "Playing Line Path Animation")
    }

    private fun playTranslateUpAnimations() {
        Log.d(LIB_TAG, "Building Translate Up Animations")

        baseLinePathHolder?.let {
            AnimationHelper.getLinePathAnimator(this, it.path,
                    Constants.ANIM_DURATION_BASE_LINE_DRAW_MS)?.start()
        }

        val animatorList = ArrayList<Animator>()

        if (!pathDataHolderList.isEmpty()) {
            pathDataHolderList.forEach {
                AnimationHelper.getTranslateUpAnimator(this, it, graphBottom,
                        Constants.ANIM_DURATION_LINE_DRAW_MS)?.apply {
                    animatorList.add(this)
                }
            }
        }

        if (shouldFill) {
            fillPathHolder?.let {
                AnimationHelper.getFillTranslationAnimator(this, it.path,
                        graphBottom, Constants.ANIM_DURATION_LINE_DRAW_MS)?.apply {
                    animatorList.add(this)
                }
            }
        }

        with(animationSet!!) {
            startDelay = Constants.ANIM_DURATION_BASE_LINE_DRAW_MS
            playTogether(animatorList)
            start()
        }

        Log.i(LIB_TAG, "Playing Translate Up Animation")
    }

    private fun resetData() {
        resetPathList()
        lineGraphScale = null
        resetPaths()
        invalidate()
    }

    private fun resetPaths() {
        resetPathList()
        resetHolderPath(fillPathHolder)
        resetHolderPath(baseLinePathHolder)
    }

    private fun resetPathList() {
        for (holder in pathDataHolderList) {
            resetHolderPath(holder)
        }
        pathDataHolderList.clear()
    }

    private fun resetHolderPath(holder: PathDataHolder?) = holder?.path?.reset()

    private fun shouldAnimate() = animationType != NONE

    /**
     * Gets the rect representing the 'content area' of the view. This is essentially the bounding
     * rect minus any padding.
     */
    private fun calculateViewPort() = contentRect.set(paddingStart.toFloat(), paddingTop.toFloat(),
            width.minus(paddingEnd).toFloat(), graphBottom)

    /**
     * This updates the original path with a partial path
     * that is used for animations by the ValueAnimator
     * @param originalPath - The original path as drawn on canvas
     * @param currentPath - The partial path as computed by the ValueAnimator
     */
    internal fun updateAnimationPath(originalPath: Path, currentPath: Path) {
        originalPath.reset()
        originalPath.addPath(currentPath)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        calculateViewPort()
        makeGraph()
    }

    override fun onDataChanged() {
        makeGraph()
        if (shouldAnimate()) {
            performAnimations()
        }
    }

    override fun onDataInvalidated() = resetData()
}