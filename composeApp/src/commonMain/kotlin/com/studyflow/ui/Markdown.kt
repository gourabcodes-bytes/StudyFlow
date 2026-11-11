package com.studyflow.ai

/**
 * A tiny Markdown reader for the assistant's answers: headings, bullet and numbered lists,
 * code blocks, dividers, and **bold** / *italic* / `code` inside text. Pure Kotlin, no UI.
 */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String, val indent: Int) : MdBlock
    data class Numbered(val number: String, val text: String, val indent: Int) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Divider : MdBlock
}

data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
)

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^(\\s*)[-*\u2022]\\s+(.*)$")
private val NUMBERED = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val DIVIDER = Regex("^(-{3,}|\\*{3,}|_{3,})$")

fun parseMarkdown(source: String): List<MdBlock> {
    val blocks = ArrayList<MdBlock>()
    val paragraph = ArrayList<String>()
    val code = ArrayList<String>()
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            blocks += MdBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }

    for (rawLine in source.replace("\r\n", "\n").split("\n")) {
        val trimmed = rawLine.trim()

        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks += MdBlock.Code(code.joinToString("\n"))
                code.clear()
                inCode = false
            } else {
                flushParagraph()
                inCode = true
            }
            continue
        }
        if (inCode) {
            code += rawLine
            continue
        }
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        val heading = HEADING.matchEntire(trimmed)
        if (heading != null) {
            flushParagraph()
            blocks += MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2].trim())
            continue
        }
        if (DIVIDER.matches(trimmed)) {
            flushParagraph()
            blocks += MdBlock.Divider
            continue
        }
        val bullet = BULLET.matchEntire(rawLine)
        if (bullet != null) {
            flushParagraph()
            blocks += MdBlock.Bullet(bullet.groupValues[2].trim(), bullet.groupValues[1].length / 2)
            continue
        }
        val numbered = NUMBERED.matchEntire(rawLine)
        if (numbered != null) {
            flushParagraph()
            blocks += MdBlock.Numbered(numbered.groupValues[2], numbered.groupValues[3].trim(), numbered.groupValues[1].length / 2)
            continue
        }
        paragraph += trimmed
    }

    if (inCode) blocks += MdBlock.Code(code.joinToString("\n"))
    flushParagraph()
    return blocks
}

/**
 * Splits one line of text into styled pieces. Underscores are left alone on purpose, because
 * names such as V_th must not turn into italics, and "2 * 3 * 4" is not emphasis either.
 */
fun parseInline(text: String): List<MdSpan> {
    val out = ArrayList<MdSpan>()
    val plain = StringBuilder()

    fun flush() {
        if (plain.isNotEmpty()) {
            out += MdSpan(plain.toString())
            plain.clear()
        }
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                flush()
                out += MdSpan(text.substring(i + 1, end), code = true)
                i = end + 1
                continue
            }
        }
        if (c == '*' && text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i + 2) {
                flush()
                out += MdSpan(text.substring(i + 2, end), bold = true)
                i = end + 2
                continue
            }
        }
        if (c == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() && text[i + 1] != '*') {
            val end = findItalicEnd(text, i + 1)
            if (end > 0) {
                flush()
                out += MdSpan(text.substring(i + 1, end), italic = true)
                i = end + 1
                continue
            }
        }
        plain.append(c)
        i++
    }
    flush()
    return out
}

private fun findItalicEnd(text: String, from: Int): Int {
    var j = from
    while (j < text.length) {
        if (text[j] == '*' && !text[j - 1].isWhitespace() && (j + 1 >= text.length || text[j + 1] != '*')) return j
        j++
    }
    return -1
}
