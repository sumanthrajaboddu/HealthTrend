package com.healthtrend.app.ui.analytics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.healthtrend.app.data.model.Severity
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import java.time.format.DateTimeFormatter

/**
 * ExtraStore key for passing date labels from model to axis formatter.
 */
private val DateLabelsKey = ExtraStore.Key<List<String>>()

/**
 * Short date formatter for X-axis labels (e.g., "2/8").
 */
private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d")

/**
 * Y-axis value formatter: maps 0→"No Pain", 1→"Mild", 2→"Moderate", 3→"Severe".
 * Uses Severity.displayName — NEVER hardcoded labels.
 */
private val SeverityAxisFormatter = CartesianValueFormatter { _, value, _ ->
    val intValue = value.toInt()
    Severity.entries.find { it.numericValue == intValue }?.displayName
        ?: intValue.toString()
}

/**
 * Trend line chart using Vico compose-m3 module.
 * Displays severity data points connected by a line.
 * Y-axis: 0=No Pain, 1=Mild, 2=Moderate, 3=Severe.
 * X-axis: dates within the selected range.
 *
 * @param chartData list of daily aggregated severity data points, sorted by date ascending.
 */
@Composable
fun TrendChart(
    chartData: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val severityPointProvider = remember {
        val pointsBySeverity = Severity.entries.associate { severity ->
            severity.numericValue to LineCartesianLayer.Point(
                component = ShapeComponent(
                    fill = Fill(severity.color.toArgb()),
                    shape = CorneredShape.Pill
                ),
                sizeDp = 8f
            )
        }

        object : LineCartesianLayer.PointProvider {
            override fun getPoint(
                entry: com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel.Entry,
                seriesIndex: Int,
                extraStore: ExtraStore
            ): LineCartesianLayer.Point? {
                val severityValue = entry.y.toInt().coerceIn(
                    Severity.NO_PAIN.numericValue,
                    Severity.SEVERE.numericValue
                )
                return pointsBySeverity[severityValue]
            }

            override fun getLargestPoint(extraStore: ExtraStore): LineCartesianLayer.Point? {
                return pointsBySeverity[Severity.SEVERE.numericValue]
            }
        }
    }

    // Format date labels for X-axis
    val dateLabels = remember(chartData) {
        chartData.map { it.date.format(SHORT_DATE_FORMATTER) }
    }

    // Update chart data when chartData changes
    LaunchedEffect(chartData) {
        modelProducer.runTransaction {
            lineSeries {
                series(chartData.map { it.severityValue.toDouble() })
            }
            extras { extraStore ->
                extraStore[DateLabelsKey] = dateLabels
            }
        }
    }

    // X-axis date formatter — reads date labels from ExtraStore
    val bottomAxisFormatter = remember {
        CartesianValueFormatter { context, value, _ ->
            val labels = context.model.extraStore.getOrNull(DateLabelsKey)
            val index = value.toInt()
            if (labels != null && index in labels.indices) labels[index] else value.toInt().toString()
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        // Keep the connecting line neutral while each data point uses severity color.
                        fill = LineCartesianLayer.LineFill.single(fill(Severity.MODERATE.color)),
                        pointProvider = severityPointProvider
                    )
                ),
                rangeProvider = remember {
                    CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = 3.0)
                }
            ),
            startAxis = VerticalAxis.rememberStart(
                valueFormatter = SeverityAxisFormatter,
                itemPlacer = remember { VerticalAxis.ItemPlacer.step({ 1.0 }) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisFormatter
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    )
}
