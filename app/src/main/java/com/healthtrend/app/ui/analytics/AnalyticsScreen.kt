package com.healthtrend.app.ui.analytics

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Analytics screen — replaces AnalyticsPlaceholderScreen.
 * Displays severity trend chart with date range selection (Story 5.1)
 * and time-of-day breakdown cards below the chart (Story 5.2).
 * When ExportState is Preview, shows PdfPreviewScreen instead (Story 6.1).
 * NO loading spinners — data from local Room completes < 500ms.
 *
 * Uses collectAsStateWithLifecycle() — NEVER collectAsState().
 */
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Date range selector — always visible (AC #1)
        Spacer(modifier = Modifier.height(16.dp))
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Export PDF button — visible in Success and Empty states (Story 6.1 AC #1, #7)
        if (uiState !is AnalyticsUiState.Loading) {
            Spacer(modifier = Modifier.height(16.dp))
            ExportPdfButton(
                onClick = viewModel::onExportPdf,
                isExporting = currentExportState is ExportState.Generating,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Analytics content showing trend chart (5.1) and time-of-day breakdown cards (5.2).
 */
@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Success,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val talkBackSummary = "Severity trend for ${summary.periodLabel}. " +
        "Average: ${summary.averageSeverity.displayName}. " +
        "Trend: ${summary.trendDirection.displayName}."

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics {
                contentDescription = talkBackSummary
            }
    ) {
        // Trend chart (Story 5.1 AC #2)
        TrendChart(
            chartData = state.chartData,
            modifier = Modifier.fillMaxWidth()
        )

        // Time-of-day breakdown cards (Story 5.2 AC #1)
        Spacer(modifier = Modifier.height(24.dp))
        TimeOfDayBreakdownSection(
            slotAverages = state.slotAverages,
            periodLabel = summary.periodLabel
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Empty state — calm, neutral, no motivational text (AC #4).
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
        Text(
            text = "No data for this period",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                contentDescription = "Export PDF report"
                onClick(label = "generate") { false }
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
            Text("Generating…")
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
