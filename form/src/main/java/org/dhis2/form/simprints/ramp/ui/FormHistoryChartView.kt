package org.dhis2.form.simprints.ramp.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import dhis2.org.analytics.charts.data.ChartType
import dhis2.org.analytics.charts.data.Graph
import dhis2.org.analytics.charts.data.GraphFieldValue
import dhis2.org.analytics.charts.data.GraphPoint
import dhis2.org.analytics.charts.data.SerieData
import dhis2.org.analytics.charts.data.toChartBuilder
import dhis2.org.analytics.charts.formatters.CategoryFormatter
import dhis2.org.analytics.charts.mappers.DEFAULT_VALUE_TEXT_SIZE
import dhis2.org.analytics.charts.mappers.GraphToLineData
import org.dhis2.form.simprints.ramp.model.FormHistoryChart
import org.hisp.dhis.android.core.common.RelativePeriod
import org.hisp.dhis.android.core.period.PeriodType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val HISTORY_CHART_HEIGHT_DP = 180
private const val HISTORY_CHART_PERIOD_STEP = 1L
private const val HISTORY_CHART_X_AXIS_LABEL_ROTATION = 30f
private const val HISTORY_CHART_TEXT_SIZE_FACTOR = 1.5f
private const val HISTORY_CHART_TEXT_SIZE = DEFAULT_VALUE_TEXT_SIZE * HISTORY_CHART_TEXT_SIZE_FACTOR
private const val HISTORY_CHART_Y_AXIS_LABEL_COUNT = 5
private const val HISTORY_CHART_FLAT_RANGE_EPSILON = 0.0001f
private const val HISTORY_CHART_INTEGER_GRANULARITY_MIN_SPAN = 1f
private const val HISTORY_CHART_INTEGER_GRANULARITY = 1f
private const val HISTORY_CHART_DEFAULT_MAX_DECIMAL_PLACES = 2

@Composable
fun FormHistoryChartView(historyChart: FormHistoryChart) {
    val graph = remember(historyChart) { historyChart.toAnalyticsGraph() }

    AndroidView(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(HISTORY_CHART_HEIGHT_DP.dp)
                .padding(top = 8.dp, bottom = 12.dp),
        factory = { context ->
            FrameLayout(context).apply {
                addView(graph.toChartView(context, historyChart))
            }
        },
        update = { chartContainer ->
            (chartContainer.getChildAt(0) as? LineChart)
                ?.configureHistoryChart(historyChart, graph)
        },
    )
}

private fun Graph.toChartView(
    context: Context,
    historyChart: FormHistoryChart,
) = toChartBuilder()
    .withType(ChartType.LINE_CHART)
    .withGraphData(this)
    .build()
    .getChartView(context)
    .apply {
        layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        (this as? LineChart)?.configureHistoryChart(historyChart, this@toChartView)
    }

private fun LineChart.configureHistoryChart(
    historyChart: FormHistoryChart,
    graph: Graph,
) {
    val axisValueFormatter = IntegerAwareValueFormatter()
    val pointValueFormatter = IntegerAwareValueFormatter(historyChart.displayMaxDecimalPlaces)

    setHighlightPerTapEnabled(false)
    setHighlightPerDragEnabled(false)
    data = graph.toHistoryLineData(pointValueFormatter)
    xAxis.apply {
        axisMinimum = -1f
        axisMaximum = historyChart.labels.size.toFloat()
        granularity = HISTORY_CHART_INTEGER_GRANULARITY
        setLabelCount(historyChart.labels.size + 2, true)
        this.valueFormatter = CategoryFormatter(historyChart.labels)
        labelRotationAngle = HISTORY_CHART_X_AXIS_LABEL_ROTATION
        textSize = HISTORY_CHART_TEXT_SIZE
    }
    axisLeft.apply {
        this.valueFormatter = axisValueFormatter
        setLabelCount(HISTORY_CHART_Y_AXIS_LABEL_COUNT, false)
        textSize = HISTORY_CHART_TEXT_SIZE
    }
    configureHistoryYAxis(historyChart)
    legend.textSize = HISTORY_CHART_TEXT_SIZE
    notifyDataSetChanged()
    invalidate()
}

private fun LineChart.configureHistoryYAxis(historyChart: FormHistoryChart) {
    val plottedValues = historyChart.values.filterNotNull()
    if (plottedValues.isEmpty()) {
        axisLeft.apply {
            isGranularityEnabled = false
            resetAxisMinimum()
            resetAxisMaximum()
        }
        return
    }

    val minValue = plottedValues.min()
    val maxValue = plottedValues.max()

    axisLeft.apply {
        isGranularityEnabled = maxValue - minValue >= HISTORY_CHART_INTEGER_GRANULARITY_MIN_SPAN
        if (isGranularityEnabled) {
            granularity = HISTORY_CHART_INTEGER_GRANULARITY
        }
    }

    applyHistoryYAxisRange(minValue, maxValue)
}

private fun LineChart.applyHistoryYAxisRange(
    minValue: Float,
    maxValue: Float,
) {
    // Fix for: MPAndroidChart renders no Y labels/gridlines when the Y axis range collapses
    axisLeft.apply {
        when {
            abs(maxValue - minValue) <= HISTORY_CHART_FLAT_RANGE_EPSILON && maxValue > 0f -> {
                axisMinimum = 0f
                axisMaximum = maxValue + historyChartYAxisPadding(0f, maxValue)
            }

            abs(maxValue - minValue) <= HISTORY_CHART_FLAT_RANGE_EPSILON && minValue < 0f -> {
                axisMinimum = minValue - historyChartYAxisPadding(minValue, 0f)
                axisMaximum = 0f
            }

            abs(maxValue - minValue) <= HISTORY_CHART_FLAT_RANGE_EPSILON -> {
                axisMinimum = -1f
                axisMaximum = 1f
            }

            else -> {
                val padding = historyChartYAxisPadding(minValue, maxValue)
                axisMinimum = minValue - padding
                axisMaximum = maxValue + padding
            }
        }
    }
}

private fun historyChartYAxisPadding(
    minValue: Float,
    maxValue: Float,
): Float = ceil((maxValue - minValue) * 0.05f)

private fun Graph.toHistoryLineData(valueFormatter: ValueFormatter) =
    GraphToLineData()
        .map(this)
        .apply {
            setValueFormatter(valueFormatter)
            setValueTextSize(HISTORY_CHART_TEXT_SIZE)
        }

internal class IntegerAwareValueFormatter(
    maxDecimalPlaces: Int? = null,
) : ValueFormatter() {
    private val decimalFormat =
        DecimalFormat("0", DecimalFormatSymbols(Locale.US)).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxDecimalPlaces.validMaxDecimalPlaces()
        }

    override fun getFormattedValue(value: Float): String =
        if (abs(value - value.roundToInt()) < INTEGER_VALUE_EPSILON) {
            value.roundToInt().toString()
        } else {
            decimalFormat.format(value.toDouble())
        }

    override fun getAxisLabel(
        value: Float,
        axis: AxisBase?,
    ): String = getFormattedValue(value)

    private companion object {
        const val INTEGER_VALUE_EPSILON = 0.0001f
    }
}

private fun Int?.validMaxDecimalPlaces(): Int =
    this?.takeIf { it >= 0 } ?: HISTORY_CHART_DEFAULT_MAX_DECIMAL_PLACES

private fun FormHistoryChart.toAnalyticsGraph(): Graph =
    Graph(
        title = title,
        series =
            listOf(
                SerieData(
                    fieldName = title,
                    coordinates =
                        values.mapIndexedNotNull { index, value ->
                            value?.let {
                                GraphPoint(
                                    eventDate = Date(index.toLong()),
                                    position = index.toFloat(),
                                    fieldValue = GraphFieldValue.Decimal(value),
                                )
                            }
                        },
                ),
            ),
        periodToDisplayDefault = RelativePeriod.TODAY,
        eventPeriodType = PeriodType.Daily,
        periodStep = HISTORY_CHART_PERIOD_STEP,
        chartType = ChartType.LINE_CHART,
        categories = labels,
    )
