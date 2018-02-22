package com.postmates.android.sparkles.app

import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DimenRes
import android.support.annotation.IntegerRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.postmates.android.sparkles.model.SparklesDataPoint
import com.postmates.android.sparkles.widget.SparklesAdapter
import kotlinx.android.synthetic.main.app_bar_sparkles.*
import kotlinx.android.synthetic.main.content_sparkles.*
import java.math.BigDecimal
import java.util.concurrent.ThreadLocalRandom

/**
 * Sample Activity to show usage of Sparkles Line Graph.
 * @author - prerak-trivedi
 */
class SparklesActivity : AppCompatActivity() {

    private lateinit var linePathGraphAdapter: SparklesAdapter
    private lateinit var translateUpGraphAdapter: SparklesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sparkles)
        setSupportActionBar(toolbar)
        setGraphAdapters()
        fab.setOnClickListener {
            fab.animate().setDuration(250).scaleX(1.15f).scaleY(1.15f).withEndAction {
                fab.animate().setDuration(250).scaleX(1f).scaleY(1f)
                randomizeInput()
            }
        }
        randomizeInput()
    }

    private fun setGraphAdapters() {
        translateUpGraphAdapter = SparklesAdapter()
        translateUpGraph?.adapter = translateUpGraphAdapter

        linePathGraphAdapter = SparklesAdapter()
        linePathGraph?.adapter = linePathGraphAdapter
    }

    private fun randomizeInput() {
        // Randomize fill opacity
        @IntegerRes val fillOpacityRes = if (ThreadLocalRandom.current().nextBoolean())
            R.integer.light_fill else R.integer.dark_fill
        translateUpGraph?.fillOpacityPercent = resources?.getInteger(fillOpacityRes) as Int

        translateUpGraphAdapter.setInput(randomDataPoints(), randomDataPoint())

        // Randomize Line Width between thick and thin
        @DimenRes val lineWidthRes = if (ThreadLocalRandom.current().nextBoolean())
            R.dimen.line_thin else R.dimen.line_thick
        linePathGraph?.lineWidth = resources?.getDimension(lineWidthRes) as Float

        // Randomize Line Color between blue and red
        @ColorRes val colorRes = if (ThreadLocalRandom.current().nextBoolean())
            R.color.colorPrimaryDark else R.color.graph_red
        linePathGraph?.lineColor = ContextCompat.getColor(this, colorRes)
        linePathGraphAdapter.setInput(randomDataPoints(), randomDataPoint())
    }

    private fun randomDataPoint(): SparklesDataPoint {
        return SparklesDataPoint(getRandomInRange())
    }

    private fun randomDataPoints(): MutableList<SparklesDataPoint> {
        val dataPointsList: MutableList<SparklesDataPoint> = mutableListOf()
        for (i in 1..20) {
            dataPointsList.add(randomDataPoint())
        }
        return dataPointsList
    }

    private fun getRandomInRange(): BigDecimal? {
        return if (ThreadLocalRandom.current().nextBoolean()) null else {
            val random = ThreadLocalRandom.current().nextInt(1, 100)
            BigDecimal.valueOf(random.toDouble())
        }
    }
}