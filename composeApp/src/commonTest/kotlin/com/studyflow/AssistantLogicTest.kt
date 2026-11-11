package com.studyflow

import com.studyflow.ai.ANSWERS_MARKER
import com.studyflow.ai.AssistMode
import com.studyflow.ai.MdBlock
import com.studyflow.ai.MdSpan
import com.studyflow.ai.buildSystemPrompt
import com.studyflow.ai.buildUserPrompt
import com.studyflow.ai.parseInline
import com.studyflow.ai.parseMarkdown
import com.studyflow.ai.splitAnswers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssistantLogicTest {

    @Test
    fun parsesHeadingsListsAndParagraphs() {
        val blocks = parseMarkdown(
            """
            ## Idea
            Any circuit can be replaced by a source and a resistor.

            1. Remove the load
            2. Find V_th
              - sub point
            * plain bullet
            ---
            """.trimIndent(),
        )
        assertEquals(MdBlock.Heading(2, "Idea"), blocks[0])
        assertEquals(MdBlock.Paragraph("Any circuit can be replaced by a source and a resistor."), blocks[1])
        assertEquals(MdBlock.Numbered("1", "Remove the load", 0), blocks[2])
        assertEquals(MdBlock.Numbered("2", "Find V_th", 0), blocks[3])
        assertEquals(MdBlock.Bullet("sub point", 1), blocks[4])
        assertEquals(MdBlock.Bullet("plain bullet", 0), blocks[5])
        assertEquals(MdBlock.Divider, blocks[6])
    }

    @Test
    fun keepsCodeBlocksTogether() {
        val blocks = parseMarkdown("Before\n```\nR = V / I\n  I = 2 A\n```\nAfter")
        assertEquals(MdBlock.Paragraph("Before"), blocks[0])
        assertEquals(MdBlock.Code("R = V / I\n  I = 2 A"), blocks[1])
        assertEquals(MdBlock.Paragraph("After"), blocks[2])
    }

    @Test
    fun unfinishedCodeBlockStillShows() {
        val blocks = parseMarkdown("```\nx = 1")
        assertEquals(listOf<MdBlock>(MdBlock.Code("x = 1")), blocks)
    }

    @Test
    fun consecutiveLinesStayInOneParagraph() {
        val blocks = parseMarkdown("Given: V = 10 V\nFind: I")
        assertEquals(listOf<MdBlock>(MdBlock.Paragraph("Given: V = 10 V\nFind: I")), blocks)
    }

    @Test
    fun inlineStyles() {
        assertEquals(
            listOf(MdSpan("Use "), MdSpan("Ohm's law", bold = true), MdSpan(" and "), MdSpan("V = IR", code = true), MdSpan(".")),
            parseInline("Use **Ohm's law** and `V = IR`."),
        )
        assertEquals(listOf(MdSpan("a "), MdSpan("slanted", italic = true), MdSpan(" word")), parseInline("a *slanted* word"))
    }

    @Test
    fun underscoresAndMultiplicationAreNotEmphasis() {
        assertEquals(listOf(MdSpan("V_th = I_n * R_n")), parseInline("V_th = I_n * R_n"))
        assertEquals(listOf(MdSpan("2 * 3 * 4")), parseInline("2 * 3 * 4"))
        assertEquals(listOf(MdSpan("an **unfinished bold")), parseInline("an **unfinished bold"))
    }

    @Test
    fun splitsAnswersFromQuestions() {
        val (questions, answers) = splitAnswers("1. Q one\n2. Q two\n$ANSWERS_MARKER\n1. A one\n2. A two")
        assertEquals("1. Q one\n2. Q two", questions)
        assertEquals("1. A one\n2. A two", answers)
        assertNull(splitAnswers("Just an explanation").second)
        assertNull(splitAnswers("Questions\n$ANSWERS_MARKER\n   ").second)
    }

    @Test
    fun userPromptDependsOnMode() {
        assertEquals("What is Thevenin's theorem?", buildUserPrompt(null, "  What is Thevenin's theorem?  ", hasPdf = false))
        assertEquals("", buildUserPrompt(null, "   ", hasPdf = false))
        assertEquals("", buildUserPrompt(AssistMode.NOTES, "", hasPdf = false))

        val steps = buildUserPrompt(AssistMode.EXPLAIN, "Thevenin's theorem", hasPdf = false)
        assertTrue(steps.contains("step by step"))
        assertTrue(steps.endsWith("Topic: Thevenin's theorem"))

        val practice = buildUserPrompt(AssistMode.PRACTICE, "Kirchhoff's laws", hasPdf = false)
        assertTrue(practice.contains(ANSWERS_MARKER))

        // a PDF alone is enough to ask something
        assertTrue(buildUserPrompt(AssistMode.NOTES, "", hasPdf = true).endsWith("Topic: the attached PDF"))
        assertTrue(buildUserPrompt(null, "", hasPdf = true).contains("attached PDF"))
    }

    @Test
    fun systemPromptMentionsTheStudentWhenKnown() {
        assertTrue(buildSystemPrompt("1st year ECE").contains("1st year ECE"))
        assertTrue(!buildSystemPrompt("   ").contains("The student studies"))
        assertTrue(buildSystemPrompt("").contains(ANSWERS_MARKER))
    }
}
