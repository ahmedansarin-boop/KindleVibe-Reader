package com.kindlevibe.reader.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.navigation.NavController
import com.kindlevibe.reader.data.db.AppDb
import com.kindlevibe.reader.data.db.entities.BookEntity
import com.kindlevibe.reader.reader.SmartReflowEngine
import com.kindlevibe.reader.reader.TextPaginator
import com.kindlevibe.reader.ui.utils.ImmersiveMode
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PdfViewMode { RENDER, BOOK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    book: BookEntity,
    navController: NavController
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf<PdfViewMode?>(null) }

    if (selectedMode == null) {
        PdfModeDialog(
            bookTitle = book.title ?: "PDF",
            onRender = { selectedMode = PdfViewMode.RENDER },
            onBookMode = { selectedMode = PdfViewMode.BOOK },
            onDismiss = { navController.popBackStack() }
        )
        return
    }

    when (selectedMode) {
        PdfViewMode.RENDER -> PdfRenderView(
            book = book,
            context = context,
            navController = navController
        )
        PdfViewMode.BOOK -> PdfBookView(
            book = book,
            context = context,
            navController = navController
        )
        null -> {}
    }
}

@Composable
fun PdfModeDialog(
    bookTitle: String,
    onRender: () -> Unit,
    onBookMode: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open PDF") },
        text = {
            Column {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text("How would you like to read this PDF?")
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBookMode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Book Mode  (reflowed text)")
                }
                OutlinedButton(
                    onClick = onRender,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PDF Mode  (original layout)")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        },
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfRenderView(
    book: BookEntity,
    context: Context,
    navController: NavController
) {
    var htmlContent by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(book.uri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(book.uri)
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext
                val renderer = PdfRenderer(pfd)
                val sb = StringBuilder()
                sb.append("""
                    <!DOCTYPE html><html><head>
                    <meta name="viewport"
                      content="width=device-width, initial-scale=1.0,
                               maximum-scale=5.0, user-scalable=yes"/>
                    <style>
                      body { margin:0; background:#666; }
                      img  { width:100%; display:block;
                             margin-bottom:8px; }
                    </style></head><body>
                """.trimIndent())

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )
                    page.close()

                    val stream = java.io.ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val b64 = android.util.Base64.encodeToString(
                        stream.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    bitmap.recycle()
                    sb.append("<img src='data:image/jpeg;base64,$b64'/>")
                }
                sb.append("</body></html>")
                renderer.close()
                pfd.close()
                htmlContent = sb.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book.title ?: "PDF", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Rendering pages...")
                }
            }
        } else if (htmlContent != null) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.apply {
                            javaScriptEnabled = false
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        loadDataWithBaseURL(
                            null,
                            htmlContent!!,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfBookView(
    book: BookEntity,
    context: Context,
    navController: NavController
) {
    var rawBlocks    by remember { mutableStateOf<List<SmartReflowEngine.TextBlock>>(emptyList()) }
    var pages        by remember { mutableStateOf<List<TextPaginator.PageContent>>(emptyList()) }
    var loading      by remember { mutableStateOf(true) }
    var paginating   by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOverlay  by remember { mutableStateOf(true) }
    var currentTheme  by remember { mutableStateOf("day") }
    var fontSize      by remember { mutableStateOf(16f) }
    var smartReflow   by remember { mutableStateOf(true) }
    var savedPage     by remember { mutableStateOf(0) }
    var isStateLoaded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount   = { pages.size.coerceAtLeast(1) }
    )
    val scope    = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()
    val density  = LocalDensity.current
    val bookDao  = remember { AppDb.getInstance(context).bookDao() }

    ImmersiveMode(active = !showOverlay)

    LaunchedEffect(book.id) {
        val state = withContext(Dispatchers.IO) {
            bookDao.getReadingState(book.id)
        }
        if (state != null) {
            if (state.lastFontSize > 0f) {
                fontSize = state.lastFontSize
            }
            if (state.lastTheme.isNotEmpty()) {
                currentTheme = state.lastTheme
            }
            if (state.lastReadPage > 0) {
                savedPage = state.lastReadPage
            }
        }
        isStateLoaded = true
    }

    LaunchedEffect(pagerState.currentPage, fontSize, currentTheme) {
        if (pages.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            bookDao.updateReadingState(
                bookId = book.id,
                page   = pagerState.currentPage,
                fontSize = fontSize,
                theme  = currentTheme
            )
        }
    }

    LaunchedEffect(book.uri) {
        withContext(Dispatchers.IO) {
            try {
                val uri    = Uri.parse(book.uri)
                val stream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Could not open file."
                            loading     = false
                        }
                        return@withContext
                    }
                val doc      = PDDocument.load(stream)
                val total    = doc.numberOfPages
                val stripper = PDFTextStripper()
                val sb       = StringBuilder()

                var start = 1
                while (start <= total) {
                    val end = minOf(start + 19, total)
                    stripper.startPage = start
                    stripper.endPage   = end
                    sb.append(stripper.getText(doc).trim()).append("\n\n")
                    start = end + 1
                }
                doc.close()
                stream.close()

                val extracted = sb.toString()
                val blocks    = if (smartReflow)
                    SmartReflowEngine.process(extracted)
                else
                    listOf(SmartReflowEngine.TextBlock.Paragraph(extracted))

                withContext(Dispatchers.Main) {
                    rawBlocks = blocks
                    loading   = false
                }
            } catch (e: OutOfMemoryError) {
                withContext(Dispatchers.Main) {
                    errorMessage = "PDF too large. Try PDF Mode."
                    loading      = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Could not read PDF: ${e.message}"
                    loading      = false
                }
            }
        }
    }

    LaunchedEffect(smartReflow) {
        if (rawBlocks.isEmpty()) return@LaunchedEffect
        paginating = true
        withContext(Dispatchers.IO) {
            val uri    = Uri.parse(book.uri)
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext
            val doc      = PDDocument.load(stream)
            val total    = doc.numberOfPages
            val stripper = PDFTextStripper()
            val sb       = StringBuilder()
            var start    = 1
            while (start <= total) {
                val end = minOf(start + 19, total)
                stripper.startPage = start
                stripper.endPage   = end
                sb.append(stripper.getText(doc).trim()).append("\n\n")
                start = end + 1
            }
            doc.close()
            stream.close()

            val extracted = sb.toString()
            val blocks    = if (smartReflow)
                SmartReflowEngine.process(extracted)
            else
                listOf(SmartReflowEngine.TextBlock.Paragraph(extracted))

            withContext(Dispatchers.Main) {
                rawBlocks  = blocks
                paginating = false
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageWidthPx = with(density) {
            ((maxWidth - 44.dp).toPx()).roundToInt()
        }
        val pageHeightPx = with(density) {
            ((maxHeight - 68.dp).toPx()).roundToInt()
        }

        LaunchedEffect(rawBlocks, fontSize, isStateLoaded) {
            if (!isStateLoaded) return@LaunchedEffect
            if (rawBlocks.isEmpty()) return@LaunchedEffect
            paginating = true
            val result = withContext(Dispatchers.Default) {
                TextPaginator.paginate(
                    blocks     = rawBlocks,
                    measurer   = measurer,
                    pageWidth  = pageWidthPx,
                    pageHeight = pageHeightPx,
                    fontSize   = fontSize
                )
            }
            pages      = result
            paginating = false
            if (savedPage > 0 && pagerState.currentPage == 0) {
                val targetPage = savedPage.coerceAtMost(result.size - 1)
                pagerState.scrollToPage(targetPage)
                if (targetPage > 0) {
                    android.widget.Toast.makeText(
                        context,
                        "Resumed · Page ${targetPage + 1} · ${currentTheme.replaceFirstChar { it.uppercase() }}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                savedPage = 0
            }
        }

        val currentBg = when (currentTheme) {
            "night" -> Color(0xFF111418)
            "sepia" -> Color(0xFFF5E6C8)
            "paper" -> Color(0xFFEDE0CC)
            else    -> Color(0xFFFAF8F5)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentBg)
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Extracting text...")
                        }
                    }
                }

                paginating -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Paginating...")
                        }
                    }
                }

                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text  = errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navController.popBackStack() }) {
                                Text("Go Back")
                            }
                        }
                    }
                }

                pages.isNotEmpty() -> {
                    HorizontalPager(
                        state    = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.30f -> {
                                            val prev = pagerState.currentPage - 1
                                            if (prev >= 0) scope.launch {
                                                pagerState.animateScrollToPage(prev)
                                            }
                                        }
                                        offset.x > width * 0.70f -> {
                                            val next = pagerState.currentPage + 1
                                            if (next < pages.size) scope.launch {
                                                pagerState.animateScrollToPage(next)
                                            }
                                        }
                                        else -> showOverlay = !showOverlay
                                    }
                                }
                            }
                    ) { pageIndex ->
                        BookPage(
                            page     = pages[pageIndex],
                            fontSize = fontSize,
                            theme    = currentTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible  = !showOverlay,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .zIndex(2f)
                            .statusBarsPadding()
                            .padding(end = 12.dp, top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text     = "${pagerState.currentPage + 1} / ${pages.size}",
                                style    = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(
                                    horizontal = 6.dp, vertical = 2.dp
                                )
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showOverlay,
                        enter   = fadeIn() + slideInVertically { -it },
                        exit    = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(2f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text     = book.title ?: "PDF",
                                    style    = MaterialTheme.typography.titleMedium,
                                    color    = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${pages.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp, vertical = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showOverlay,
                        enter   = fadeIn() + slideInVertically { it },
                        exit    = fadeOut() + slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                // ── ROW 1: Progress Bar ───────────────────────────
                                Column {
                                    val progress = if (pages.isEmpty()) 0f
                                        else (pagerState.currentPage + 1).toFloat() / pages.size
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFFD4A843), Color(0xFFEDD07A))
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text  = "${(progress * 100).toInt()}% complete",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text  = "${(pages.size - pagerState.currentPage - 1).coerceAtLeast(0)} pages left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // ── ROW 2: Font Size + Smart ─────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .clickable { if (fontSize > 12f) fontSize -= 1f },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("A", color = Color.White,
                                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text  = "${fontSize.toInt()}",
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.width(22.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .clickable { if (fontSize < 28f) fontSize += 1f },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("A", color = Color.White,
                                                fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        onClick = { smartReflow = !smartReflow },
                                        shape   = RoundedCornerShape(20.dp),
                                        color   = if (smartReflow) Color(0xFF1B2B4B)
                                                  else Color.White.copy(alpha = 0.15f),
                                        border  = BorderStroke(
                                            1.dp,
                                            if (smartReflow) Color(0xFFD4A843)
                                            else Color.White.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment     = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (smartReflow)
                                                    Icons.Rounded.AutoAwesome
                                                else
                                                    Icons.Rounded.TextFields,
                                                contentDescription = null,
                                                tint     = if (smartReflow) Color(0xFFD4A843)
                                                           else Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text       = if (smartReflow) "Smart" else "Raw",
                                                style      = MaterialTheme.typography.labelSmall,
                                                color      = if (smartReflow) Color(0xFFD4A843)
                                                             else Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines   = 1,
                                                softWrap   = false
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // ── ROW 3: Theme selector (4 options with label) ────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        Triple("day", Color(0xFFFAF8F5), "Day"),
                                        Triple("night", Color(0xFF111418), "Night"),
                                        Triple("sepia", Color(0xFFF5E6C8), "Sepia"),
                                        Triple("paper", Color(0xFFEDE0CC), "Paper")
                                    ).forEach { (themeId, bgColor, label) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val isSelected = currentTheme == themeId
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isSelected) 30.dp else 24.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFFD4A843)
                                                                else Color.White.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                                    .background(bgColor)
                                                    .clickable { currentTheme = themeId }
                                            )
                                            Text(
                                                text  = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color(0xFFD4A843)
                                                        else Color.White.copy(alpha = 0.6f),
                                                fontWeight = if (isSelected) FontWeight.Bold
                                                             else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeDot(
    color: Color,
    border: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (selected) 26.dp else 22.dp)
            .clip(CircleShape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFFD4A843) else border,
                shape = CircleShape
            )
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
fun BookPage(
    page     : TextPaginator.PageContent,
    fontSize : Float,
    theme    : String,
    modifier : Modifier = Modifier
) {
    val bgColor = when (theme) {
        "night" -> Color(0xFF111418)
        "sepia" -> Color(0xFFF5E6C8)
        "paper" -> Color(0xFFEDE0CC)
        else    -> Color(0xFFFAF8F5)
    }
    val textColor = when (theme) {
        "night" -> Color(0xFFD4CDBE)
        "sepia" -> Color(0xFF3B2E1A)
        "paper" -> Color(0xFF2C1F0E)
        else    -> Color(0xFF1A1208)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        val statusBarHeight = WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()
        val topPadding = statusBarHeight + 18.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top    = topPadding,
                    bottom = 24.dp,
                    start  = 22.dp,
                    end    = 22.dp
                )
        ) {
            var isFirstBlock = true
            for (block in page.blocks) {
                when (block) {
                    is SmartReflowEngine.TextBlock.Heading -> {
                        Spacer(Modifier.height((fontSize * 1.4f).dp))
                        Text(
                            text  = block.text,
                            style = TextStyle(
                                fontFamily    = FontFamily.Serif,
                                fontSize      = (fontSize * 1.2f).sp,
                                fontWeight    = FontWeight.Bold,
                                lineHeight    = (fontSize * 2.0f).sp,
                                textAlign     = TextAlign.Center,
                                color         = textColor,
                                letterSpacing = 0.08.em
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height((fontSize * 1.0f).dp))
                        isFirstBlock = true
                    }
                    is SmartReflowEngine.TextBlock.Paragraph -> {
                        val annotated = buildAnnotatedString {
                            if (!isFirstBlock) append("\u2003\u2003")
                            append(block.text)
                        }
                        SelectionContainer {
                            Text(
                                text  = annotated,
                                style = TextStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontSize   = fontSize.sp,
                                    lineHeight = (fontSize * 1.95f).sp,
                                    textAlign  = TextAlign.Justify,
                                    color      = textColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        isFirstBlock = false
                    }
                }
            }
        }
    }
}
