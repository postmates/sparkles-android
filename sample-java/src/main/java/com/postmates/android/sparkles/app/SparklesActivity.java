package com.postmates.android.sparkles.app;

import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntegerRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.postmates.android.sparkles.model.SparklesDataPoint;
import com.postmates.android.sparkles.widget.SparklesAdapter;
import com.postmates.android.sparkles.widget.line.SparklesLineView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sample Activity to show usage of Sparkles Line Graph.
 * @author - prerak-trivedi
 */
public class SparklesActivity extends AppCompatActivity {

    private SparklesLineView mTranslateUpGraph;
    private SparklesLineView mLinePathGraph;

    private SparklesAdapter mTranslateUpGraphAdapter;
    private SparklesAdapter mLinePathGraphAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sparkles);
        initViews();
        randomizeInput();
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.toolbar));
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v ->
                fab.animate().setDuration(250).scaleX(1.15f).scaleY(1.15f).withEndAction(() -> {
                    fab.animate().setDuration(250).scaleX(1f).scaleY(1f);
                    randomizeInput();
                }));

        mTranslateUpGraph = findViewById(R.id.translateUpGraph);
        mLinePathGraph = findViewById(R.id.linePathGraph);
        setGraphAdapters();
    }

    private void setGraphAdapters() {
        mTranslateUpGraphAdapter = new SparklesAdapter();
        mTranslateUpGraph.setAdapter(mTranslateUpGraphAdapter);

        mLinePathGraphAdapter = new SparklesAdapter();
        mLinePathGraph.setAdapter(mLinePathGraphAdapter);
    }

    private void randomizeInput() {
        // Randomize fill opacity
        @IntegerRes int fillOpacityRes = ThreadLocalRandom.current().nextBoolean() ?
                R.integer.light_fill : R.integer.dark_fill;
        mTranslateUpGraph.setFillOpacityPercent(getResources().getInteger(fillOpacityRes));

        mTranslateUpGraphAdapter.setInput(randomDataPoints(), randomDataPoint());

        // Randomize Line Width between thick and thin
        @DimenRes int lineWidthRes = ThreadLocalRandom.current().nextBoolean() ?
                R.dimen.line_thin : R.dimen.line_thick;
        mLinePathGraph.setLineWidth(getResources().getDimension(lineWidthRes));

        // Randomize Line Color between blue and red
        @ColorRes int colorRes = ThreadLocalRandom.current().nextBoolean() ?
            R.color.colorPrimaryDark : R.color.graph_red;
        mLinePathGraph.setLineColor(ContextCompat.getColor(this, colorRes));
        mLinePathGraphAdapter.setInput(randomDataPoints(), randomDataPoint());
    }

    private SparklesDataPoint randomDataPoint() {
        return new SparklesDataPoint(getRandomInRange());
    }

    private List<SparklesDataPoint> randomDataPoints() {
        List<SparklesDataPoint> dataPointsList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            dataPointsList.add(randomDataPoint());
        }
        return dataPointsList;
    }

    private BigDecimal getRandomInRange() {
        return ThreadLocalRandom.current().nextBoolean() ? null :
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1, 100));
    }
}
