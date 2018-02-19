package com.postmates.android.sparkles.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.postmates.android.sparkles.model.SparklesDataPoint
import com.postmates.android.sparkles.widget.SparklesAdapter
import kotlinx.android.synthetic.main.app_bar_sparkles.*
import kotlinx.android.synthetic.main.content_sparkles.*
import java.math.BigDecimal
import java.util.concurrent.ThreadLocalRandom

class SparklesActivity : AppCompatActivity() {

    private lateinit var linePathGraphAdapter: SparklesAdapter
    private lateinit var translateUpGraphAdapter: SparklesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sparkles)
        setSupportActionBar(toolbar)
        setGraphAdapters()
        fab.setOnClickListener {
            randomizeInput()
        }
        randomizeInput()
    }

    private fun setGraphAdapters() {
        linePathGraphAdapter = SparklesAdapter()
        translateUpGraphAdapter = SparklesAdapter()
        linePathGraph?.adapter = linePathGraphAdapter
        translateUpGraph?.adapter = translateUpGraphAdapter
    }

    private fun randomizeInput() {
        linePathGraphAdapter.setInput(randomDataPoints(), randomBaseline())
        translateUpGraphAdapter.setInput(randomDataPoints(), randomBaseline())
    }

    private fun randomBaseline(): SparklesDataPoint? {
        return SparklesDataPoint(getRandomInRange())
    }

    private fun randomDataPoints(): MutableList<SparklesDataPoint> {
        val dataPointsList: MutableList<SparklesDataPoint> = mutableListOf()
        for (i in 1..20) {
            dataPointsList.add(SparklesDataPoint(getRandomInRange()))
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