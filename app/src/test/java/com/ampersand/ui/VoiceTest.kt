package com.ampersand.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * DESIGN.md's voice, pinned: no exclamation marks, no emoji, and never "your
 * partner" once the app knows their name. Every string is a test target.
 */
class VoiceTest {

    private val root = listOf(File("src/main"), File("app/src/main")).first { it.exists() }

    /** String literals in the UI, with `${...}` templates removed so `!!` and `!=` inside them don't count. */
    private fun uiStrings(): List<Pair<String, String>> =
        File(root, "java/com/ampersand/ui").walk()
            // Pairing happens before a name is known, so "your partner" is honest there.
            .filter { it.extension == "kt" && it.parentFile.name != "auth" }
            .flatMap { file ->
                Regex(""""((?:[^"\\\n]|\\.)*)"""").findAll(file.readText())
                    .map { file.name to it.groupValues[1].replace(Regex("""\$\{[^}]*\}"""), "") }
            }
            .toList()

    private val emoji = Regex("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]")

    @Test
    fun `no exclamation marks in the UI`() {
        val strings = uiStrings()
        assertTrue("found only ${strings.size} strings; is the scan reading the UI?", strings.size > 200)
        val bad = strings.filter { (_, s) -> '!' in s }
        assertTrue("exclamation marks: $bad", bad.isEmpty())
    }

    @Test
    fun `no emoji in the UI`() {
        val bad = uiStrings().filter { (_, s) -> emoji.containsMatchIn(s) }
        assertTrue("emoji: $bad", bad.isEmpty())
    }

    @Test
    fun `the partner is always named`() {
        val bad = uiStrings().filter { (_, s) -> s.contains("your partner", ignoreCase = true) }
        assertTrue("\"your partner\": $bad", bad.isEmpty())
    }

    @Test
    fun `no exclamation marks or emoji in the questions`() {
        val bad = File(root, "assets").walk().filter { it.extension == "json" }
            .flatMap { file ->
                Regex(""""((?:[^"\\]|\\.)*)"""").findAll(file.readText())
                    .map { file.name to it.groupValues[1] }
                    .filter { (_, s) -> '!' in s || emoji.containsMatchIn(s) }
            }
            .toList()
        assertTrue("in question files: $bad", bad.isEmpty())
    }
}
