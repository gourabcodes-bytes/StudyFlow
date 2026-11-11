package com.studyflow.ai

/** The assistant is told to put this exact line between practice questions and their answers. */
const val ANSWERS_MARKER = "---ANSWERS---"

/** Quick modes shown as chips above the message box. */
enum class AssistMode(val label: String) {
    EXPLAIN("Step by step"),
    NOTES("Simple notes"),
    EXAMPLES("Examples"),
    PRACTICE("Practice questions"),
    QUIZ("Quiz me"),
}

/** The standing instructions that make the AI behave like a patient tutor. */
fun buildSystemPrompt(studyProfile: String): String {
    val profile = studyProfile.trim()
    val who = if (profile.isEmpty()) "" else "The student studies: $profile. Pitch examples and difficulty at that level.\n"
    return """
You are StudyFlow's study assistant: a patient, friendly tutor for university students.
$who
How to answer:
- Teach, do not just state facts. Start with the basic idea in one or two plain sentences, then explain step by step with numbered steps, then give one small worked example, then finish with a one-line summary.
- Use simple language and short sentences. Define every new term the first time you use it. Prefer concrete examples with real numbers (for electronics, small circuits with actual values).
- Write maths in plain text with Unicode symbols (Ω, √, ×, ², V_th, R_th). Never use LaTeX and never use dollar signs around formulas. Do not use tables; use bullet lists instead.
- Use short headings that start with ## and bullet lists for notes. Keep answers focused, with no long introduction.
- If a PDF is attached, base your answer on it and say where in the document the information comes from when you can (section or page). If the PDF does not cover the question, say so first, then answer from general knowledge and mark that part clearly with "Not in your PDF:".
- For practice questions or quizzes: write only the questions first. Then add a line containing exactly $ANSWERS_MARKER and after it write the answers with a short explanation each.
- If you are not sure about something, say so honestly. Never invent facts, formulas or page numbers.
""".trimIndent()
}

/**
 * Turns what the student typed plus the chosen mode into the message sent to the AI.
 * With no mode the question is sent as it is.
 */
fun buildUserPrompt(mode: AssistMode?, text: String, hasPdf: Boolean): String {
    val typed = text.trim()
    val topic = if (typed.isNotEmpty()) typed else if (hasPdf) "the attached PDF" else ""
    if (topic.isEmpty()) return ""
    return when (mode) {
        null -> if (typed.isNotEmpty()) typed else "Explain the attached PDF simply, step by step."
        AssistMode.EXPLAIN ->
            "Explain this step by step in simple language: begin with the basic idea in one or two sentences, " +
                "then numbered steps, then one small worked example, then a one-line summary.\n\nTopic: $topic"
        AssistMode.NOTES ->
            "Write short, simple study notes on this: the key idea, the important formulas or definitions, " +
                "when to use it, and common mistakes. Use headings and bullet points.\n\nTopic: $topic"
        AssistMode.EXAMPLES ->
            "Give 3 solved examples on this, from easy to harder, showing every step and the final answer.\n\nTopic: $topic"
        AssistMode.PRACTICE ->
            "Create 5 practice questions on this, from easy to hard. Write only the questions first. " +
                "Then add a line containing exactly $ANSWERS_MARKER and after it give the answer to each question " +
                "with a short explanation.\n\nTopic: $topic"
        AssistMode.QUIZ ->
            "Quiz me on this with 5 multiple-choice questions (options A to D). Write only the questions and options first. " +
                "Then add a line containing exactly $ANSWERS_MARKER and after it give the correct answers with a one-line reason each." +
                "\n\nTopic: $topic"
    }
}

/** Splits a reply into the part to show now and the answers to reveal on demand (or null). */
fun splitAnswers(reply: String): Pair<String, String?> {
    val index = reply.indexOf(ANSWERS_MARKER)
    if (index < 0) return reply.trim() to null
    val before = reply.substring(0, index).trim()
    val after = reply.substring(index + ANSWERS_MARKER.length).trim()
    return before to (if (after.isEmpty()) null else after)
}
