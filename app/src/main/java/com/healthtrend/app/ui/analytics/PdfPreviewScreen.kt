package com.healthtrend.app.ui.analytics

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * PDF preview screen (Story 6.1 Task 4).
 * Displays rendered PDF pages in a scrollable column.
 * Shows Share button to launch Android share sheet (Task 5).
 * Back navigation returns to Analytics (subtask 4.4).
 *
 * @param pdfFile the generated PDF file
 * @param onBack callback to return to Analytics (resets export state)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    pdfFile: File,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Handle system back button (subtask 4.4)
    BackHandler(onBack = onBack)

    val pageCount = remember(pdfFile) { getPdfPageCount(pdfFile) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top app bar with back and title
        TopAppBar(
            title = { Text("Report Preview") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Analytics"
                    )
                }
            }
        )

        // Scrollable PDF pages (subtask 4.3)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items((0 until pageCount).toList()) { pageIndex ->
                PdfPageImage(
                    pdfFile = pdfFile,
                    pageIndex = pageIndex,
                    pageCount = pageCount
                )
            }
        }

        // Share button (subtask 4.2, Task 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Button(
                onClick = { sharePdf(context, pdfFile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Share PDF report"
                        onClick(label = "share") { false }
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
        }
    }
}

/**
 * Render all PDF pages to bitmaps for preview display.
 * Renders at 2x scale for sharp display on high-DPI screens.
 */
private fun getPdfPageCount(pdfFile: File): Int {
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)

    return try {
        renderer.pageCount
    } finally {
        renderer.close()
        pfd.close()
    }
}

@Composable
private fun PdfPageImage(
    pdfFile: File,
    pageIndex: Int,
    pageCount: Int
) {
    val bitmap = remember(pdfFile, pageIndex) {
        renderPdfPage(pdfFile, pageIndex)
    }

    DisposableEffect(bitmap) {
        onDispose { bitmap.recycle() }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Report page ${pageIndex + 1} of $pageCount",
        modifier = Modifier.fillMaxWidth(),
        contentScale = ContentScale.FillWidth
    )
}

private fun renderPdfPage(pdfFile: File, pageIndex: Int): Bitmap {
    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(pfd)

    try {
        val page = renderer.openPage(pageIndex)
        // Render at 1.5x to reduce peak memory while keeping text crisp.
        val bitmap = Bitmap.createBitmap(
            (page.width * 1.5f).toInt(),
            (page.height * 1.5f).toInt(),
            Bitmap.Config.ARGB_8888
        )
        bitmap.eraseColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    } finally {
        renderer.close()
        pfd.close()
    }
}

/**
 * Launch Android share sheet with the PDF file (Story 6.1 Task 5).
 * Uses FileProvider for secure URI sharing (subtask 5.2).
 * MIME type: application/pdf (subtask 5.3).
 * Targets: WhatsApp, email, print, any installed share-capable app (subtask 5.5).
 */
private fun sharePdf(context: Context, pdfFile: File) {
    // Get content URI via FileProvider (subtask 5.2)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    // Create share intent (subtask 5.1)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf" // subtask 5.3
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Launch share sheet (subtask 5.4)
    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
}
