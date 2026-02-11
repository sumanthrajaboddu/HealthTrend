package com.healthtrend.app.ui.analytics

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.drawToBitmap

/**
 * Analytics screen — replaces AnalyticsPlaceholderScreen.
 * Displays severity trend chart with date range selection (Story 5.1)
 * and time-of-day breakdown cards below the chart (Story 5.2).
 * When ExportState is Preview, shows PdfPreviewScreen instead (Story 6.1).
 * NO loading spinners — data from local Room completes < 500ms.
 *
 * Uses collectAsStateWithLifecycle() — NEVER collectAsState().
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val rootView = LocalView.current
    var chartBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

    // PDF Preview mode — replaces analytics content (Story 6.1 Task 4)
    val currentExportState = exportState
    if (currentExportState is ExportState.Preview) {
        PdfPreviewScreen(
            pdfFile = currentExportState.pdfFile,
            onBack = viewModel::resetExportState,
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Date range selector — always visible (AC #1)
            Spacer(modifier = Modifier.height(8.dp))
            DateRangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = viewModel::selectRange
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    // NO loading spinner per UX constraints.
                    // Data loads from local Room in < 500ms, so Loading state is transient.
                }

                is AnalyticsUiState.Empty -> {
                    EmptyAnalyticsState(
                        selectedRange = state.selectedRange,
                        modifier = Modifier.weight(1f)
                    )
                }

                is AnalyticsUiState.Success -> {
                    AnalyticsContent(
                        state = state,
                        onChartBoundsChanged = { chartBoundsInRoot = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (currentExportState is ExportState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentExportState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Export PDF button — visible in Success and Empty states (Story 6.1 AC #1, #7)
            if (uiState !is AnalyticsUiState.Loading) {
                Spacer(modifier = Modifier.height(16.dp))
                ExportPdfButton(
                    onClick = {
                        val chartBitmap = captureChartBitmap(rootView, chartBoundsInRoot)
                        viewModel.onExportPdf(chartBitmap = chartBitmap)
                    },
                    isExporting = currentExportState is ExportState.Generating,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Analytics content showing trend chart (5.1) and time-of-day breakdown cards (5.2).
 */
@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    onChartBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val talkBackSummary = "Severity trend for ${summary.periodLabel}. " +
        "Average: ${summary.averageSeverity.displayName}. " +
        "Trend: ${summary.trendDirection.displayName}."

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
    ) {
        // Trend chart (Story 5.1 AC #2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onChartBoundsChanged(coordinates.boundsInRoot())
                }
        ) {
            TrendChart(
                chartData = state.chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = talkBackSummary
                    }
            )
        }

        // Time-of-day breakdown cards (Story 5.2 AC #1)
        Spacer(modifier = Modifier.height(24.dp))
        TimeOfDayBreakdownSection(
            slotAverages = state.slotAverages,
            periodDays = state.selectedRange.days
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Empty state — calm, neutral icon + guidance text (AC #4).
 * Announces "No data for selected period" via TalkBack.
 */
@Composable
private fun EmptyAnalyticsState(
    selectedRange: DateRange,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics {
                contentDescription = "No data for selected period."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No data for this period",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Log severity entries on the Today tab to see trends here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Export PDF button (Story 6.1 Task 1).
 * TalkBack: "Export PDF report. Double tap to generate." (AC #7).
 * 48dp+ touch target (subtask 1.4).
 * Shows CircularProgressIndicator when isExporting = true (Task 3).
 *
 * @param onClick triggers PDF generation — wired to ViewModel.onExportPdf()
 * @param isExporting true while PDF is generating — disables button and shows spinner
 */
@Composable
private fun ExportPdfButton(
    onClick: () -> Unit,
    isExporting: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isExporting,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Export PDF report. Double tap to generate."
            }
    ) {
        if (isExporting) {
            // The ONLY loading spinner in the entire app (Story 6.1 AC #1, UX constraint)
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generating\u2026")
        } else {
            Icon(
                imageVector = Icons.Filled.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export PDF")
        }
    }
}

private fun captureChartBitmap(view: View, chartBoundsInRoot: Rect?): Bitmap? {
    val bounds = chartBoundsInRoot ?: return null
    if (bounds.width <= 0f || bounds.height <= 0f) return null

    val rootBitmap = view.drawToBitmap()
    val left = bounds.left.toInt().coerceIn(0, rootBitmap.width - 1)
    val top = bounds.top.toInt().coerceIn(0, rootBitmap.height - 1)
    val width = bounds.width.toInt().coerceAtLeast(1).coerceAtMost(rootBitmap.width - left)
    val height = bounds.height.toInt().coerceAtLeast(1).coerceAtMost(rootBitmap.height - top)
    return Bitmap.createBitmap(rootBitmap, left, top, width, height)
}
