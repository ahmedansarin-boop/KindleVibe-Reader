package com.kindlevibe.reader.reader

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

object TextPaginator {

    data class PageContent(
        val blocks: List<SmartReflowEngine.TextBlock>
    )

    /**
     * Splits List<TextBlock> into pages that fit
     * exactly on screen at the given fontSize.
     *
     * Called on Dispatchers.Default — NOT on UI thread.
     * Re-called automatically when fontSize changes.
     */
    fun paginate(
        blocks: List<SmartReflowEngine.TextBlock>,
        measurer: TextMeasurer,
        pageWidth: Int,
        pageHeight: Int,
        fontSize: Float
    ): List<PageContent> {
        val pages       = mutableListOf<PageContent>()
        var currentPage = mutableListOf<SmartReflowEngine.TextBlock>()
        var usedHeight  = 0

        val paraStyle = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize   = fontSize.sp,
            lineHeight = (fontSize * 1.85f).sp,
            textAlign  = TextAlign.Justify
        )
        val headingStyle = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize   = (fontSize * 1.2f).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (fontSize * 2.2f).sp,
            textAlign  = TextAlign.Center
        )

        for (block in blocks) {
            val style = when (block) {
                is SmartReflowEngine.TextBlock.Heading   -> headingStyle
                is SmartReflowEngine.TextBlock.Paragraph -> paraStyle
            }
            val text = when (block) {
                is SmartReflowEngine.TextBlock.Heading   -> block.text
                is SmartReflowEngine.TextBlock.Paragraph -> block.text
            }

            val spacingBefore = when (block) {
                is SmartReflowEngine.TextBlock.Heading   -> (fontSize * 2.5f).toInt()
                is SmartReflowEngine.TextBlock.Paragraph -> (fontSize * 0.3f).toInt()
            }

            val measured = measurer.measure(
                text        = text,
                style       = style,
                constraints = Constraints(maxWidth = pageWidth)
            )
            val blockHeight = measured.size.height + spacingBefore

            when {
                usedHeight + blockHeight <= pageHeight -> {
                    currentPage.add(block)
                    usedHeight += blockHeight
                }

                currentPage.isEmpty() -> {
                    currentPage.add(block)
                    pages.add(PageContent(currentPage.toList()))
                    currentPage = mutableListOf()
                    usedHeight  = 0
                }

                else -> {
                    pages.add(PageContent(currentPage.toList()))
                    currentPage = mutableListOf(block)
                    usedHeight  = blockHeight
                }
            }
        }

        if (currentPage.isNotEmpty()) {
            pages.add(PageContent(currentPage.toList()))
        }

        return pages.ifEmpty {
            listOf(PageContent(listOf(
                SmartReflowEngine.TextBlock.Paragraph("No content found.")
            )))
        }
    }
}
