package com.kindlevibe.reader.reader

import java.text.BreakIterator
import java.util.Locale

object SmartReflowEngine {

    sealed class TextBlock {
        data class Heading(val text: String)   : TextBlock()
        data class Paragraph(val text: String) : TextBlock()
    }

    /**
     * Main entry point.
     * Takes raw extracted PDF/EPUB text.
     * Returns List<TextBlock> — structured novel content.
     */
    fun process(rawText: String): List<TextBlock> {
        val lines    = rawText.lines()
        val cleaned  = cleanLines(lines)
        val rejoined = joinBrokenLines(cleaned)
        return buildBlocks(rejoined)
    }

    // ── STAGE 1: CLEAN ──────────────────────────
    private fun cleanLines(lines: List<String>): List<String> {
        val freq = lines.map { it.trim() }
            .groupingBy { it }.eachCount()

        val result = mutableListOf<String>()
        for (line in lines) {
            val t = line.trim()
            if (t.isBlank())                          { result.add(""); continue }
            if (t.matches(Regex("^\\d{1,4}$")))       continue  // page numbers
            if (t.length < 60 && (freq[t] ?: 0) >= 3) continue  // headers/footers
            result.add(if (t.endsWith("-")) t.dropLast(1) else t)
        }
        return result
    }

    // ── STAGE 2: JOIN BROKEN LINES ──────────────
    private fun joinBrokenLines(lines: List<String>): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()

        for (line in lines) {
            if (line.isBlank()) {
                if (current.isNotBlank()) {
                    result.add(current.toString().trim())
                    current.clear()
                }
                continue
            }
            val prev = current.toString().trimEnd()
            val prevEndedWithHyphen = prev.endsWith("-")

            when {
                prevEndedWithHyphen -> {
                    current.deleteCharAt(current.length - 1)
                    current.append(line)
                }
                current.isNotBlank() -> current.append(" $line")
                else                 -> current.append(line)
            }
        }
        if (current.isNotBlank()) result.add(current.toString().trim())
        return result
    }

    // ── STAGE 3: BUILD BLOCKS ───────────────────
    private fun buildBlocks(lines: List<String>): List<TextBlock> {
        val blocks   = mutableListOf<TextBlock>()
        val iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH)

        for (line in lines) {
            if (line.isBlank()) continue

            if (isHeading(line)) {
                blocks.add(TextBlock.Heading(line.trim()))
                continue
            }

            val sentences = mutableListOf<String>()
            iterator.setText(line)
            var start = iterator.first()
            var end   = iterator.next()
            while (end != BreakIterator.DONE) {
                val s = line.substring(start, end).trim()
                if (s.isNotBlank()) sentences.add(s)
                start = end
                end   = iterator.next()
            }

            sentences.chunked(4).forEach { group ->
                val text = group.joinToString(" ")
                if (text.isNotBlank())
                    blocks.add(TextBlock.Paragraph(text))
            }
        }
        return blocks
    }

    private fun isHeading(line: String): Boolean {
        if (line.length > 65) return false
        return line.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() }
            || line.matches(Regex("^(Chapter|CHAPTER|Part|PART|Preface|PREFACE|Prologue|Epilogue).*"))
            || (line.length < 40 && !line.contains(".")
                && line.first().isUpperCase()
                && line.none { it == ',' })
    }
}
