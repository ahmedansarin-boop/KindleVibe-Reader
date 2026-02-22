package com.kindlevibe.reader.reader

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

object PdfTextExtractor {

    data class PdfPage(
        val pageNumber: Int,
        val text: String
    )

    /**
     * Extracts text from all pages of a clean digital PDF.
     * Returns list of PdfPage — one entry per PDF page.
     */
    fun extractPages(context: Context, uri: Uri): List<PdfPage> {
        val pages = mutableListOf<PdfPage>()
        var stream: InputStream? = null
        var doc: PDDocument? = null
        try {
            stream = context.contentResolver.openInputStream(uri)
                ?: return emptyList()
            doc = PDDocument.load(stream)
            val stripper = PDFTextStripper()

            for (i in 1..doc.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i
                val text = stripper.getText(doc)
                    .trim()
                    .replace(Regex("\\n{3,}"), "\n\n") // collapse excess blank lines
                if (text.isNotBlank()) {
                    pages.add(PdfPage(pageNumber = i, text = text))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            doc?.close()
            stream?.close()
        }
        return pages
    }

    /**
     * Converts extracted pages into a single HTML string
     * that can be rendered in a WebView in book style.
     * Applies chapter breaks between pages.
     */
    fun pagesToHtml(
        pages: List<PdfPage>,
        title: String
    ): String {
        val bodyContent = buildString {
            pages.forEach { page ->
                // Clean up common PDF artifacts
                val cleaned = page.text
                    .replace(Regex("(?m)^\\d+$"), "")         // standalone page numbers
                    .replace(Regex("(?m)^.{1,60}\\|.{1,60}$"), "") // header/footer lines
                    .trim()

                if (cleaned.isNotBlank()) {
                    // Split into paragraphs
                    cleaned.split("\n\n").forEach { para ->
                        val p = para.trim()
                        if (p.isNotBlank()) {
                            append("<p>")
                            append(p.replace("\n", " "))
                            append("</p>\n")
                        }
                    }
                    append("<hr class='page-break'/>")
                }
            }
        }

        return """<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8"/>
<meta name="viewport"
      content="width=device-width,
               initial-scale=1.0,
               maximum-scale=1.0,
               user-scalable=no"/>
<style>
* {
    box-sizing: border-box;
    -webkit-text-size-adjust: none;
    text-size-adjust: none;
}
html, body {
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
    margin: 0;
    padding: 0;
    background-color: #FAFAFA;
    color: #1A1A1A;
}
body {
    font-family: Georgia, 'Times New Roman', serif;
    font-size: 17px;
    line-height: 1.85;
    padding: 20px 18px;
    word-wrap: break-word;
    overflow-wrap: break-word;
    word-break: normal;
    hyphens: auto;
}
h2 {
    font-size: 1.2em;
    text-align: center;
    margin: 0 0 1.5em 0;
    font-weight: bold;
    line-height: 1.4;
}
p {
    margin: 0 0 1.1em 0;
    text-align: justify;
    width: 100%;
    max-width: 100%;
}
hr.page-break {
    border: none;
    border-top: 1px solid #DDD;
    margin: 1.5em 0;
}
</style>
</head>
<body>
<h2>$title</h2>
$bodyContent
</body>
</html>""".trimIndent()
    }
}
