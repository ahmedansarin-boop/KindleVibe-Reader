package com.kindlevibe.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTestTag
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kindlevibe.reader.core.KindleVibeApp
import com.kindlevibe.reader.data.db.entities.BookEntity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Instrumented tests for PDF flow (P1–P6).
 * Requires: emulator/device running, app installed.
 * Seeds one PDF book then runs UI assertions.
 */
@RunWith(AndroidJUnit4::class)
class PdfFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var pdfUriString: String

    @Before
    fun setUp() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as KindleVibeApp
        val cacheFile = File(context.cacheDir, "test_sample.pdf").apply {
            parentFile?.mkdirs()
            writeBytes(MINIMAL_PDF_BYTES)
        }
        pdfUriString = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        ).toString()
        val pdfBookId = UUID.randomUUID().toString()
        val book = BookEntity(
            id = pdfBookId,
            uri = pdfUriString,
            title = "Test PDF",
            author = "Test Author",
            lastReadAt = 0L,
            addedAt = System.currentTimeMillis(),
            progress = 0.0,
            readingTimeSeconds = 0L,
            lastOpenedAt = null,
            lastLocatorJson = null,
            lastProgress = null,
            fileType = "pdf"
        )
        app.db.bookDao().insert(book)
    }

    @Test
    fun p1_pdfAppearsInLibraryWithBadge() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PDF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test PDF").assertIsDisplayed()
    }

    @Test
    fun p2_tapPdfShowsModeDialog() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test PDF").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Open PDF").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book Mode  (reflowed text)").assertIsDisplayed()
        composeTestRule.onNodeWithText("PDF Mode  (original layout)").assertIsDisplayed()
    }

    @Test
    fun p3_pdfModeRendersPages() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test PDF").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PDF Mode  (original layout)").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(3000)
        composeTestRule.onNodeWithText("Test PDF", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun p5_bookModeTapShowsTopBar() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test PDF").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Book Mode  (reflowed text)").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(5000)
        composeTestRule.onNodeWithTestTag("pdf_book_content").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    companion object {
        private val MINIMAL_PDF_BYTES = buildMinimalPdf()
    }
}

private fun buildMinimalPdf(): ByteArray {
    val part1 = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
    val o0 = 0
    val o1 = part1.indexOf("1 0 obj")
    val o2 = part1.indexOf("2 0 obj")
    val o3 = part1.indexOf("3 0 obj")
    val xrefStart = part1.length
    val xref = "xref\n0 4\n${o0.toString().padStart(10, '0')} 65535 f \n${o1.toString().padStart(10, '0')} 00000 n \n${o2.toString().padStart(10, '0')} 00000 n \n${o3.toString().padStart(10, '0')} 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n$xrefStart\n%%EOF\n"
    return (part1 + xref).toByteArray(Charsets.US_ASCII)
}
