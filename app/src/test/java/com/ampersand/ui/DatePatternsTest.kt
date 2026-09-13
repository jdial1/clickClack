package com.ampersand.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Every DateTimeFormatter.ofPattern literal in the app must parse. They sit in
 * top-level vals, so a bad one crashes the whole screen's file on first use.
 */
class DatePatternsTest {

    @Test
    fun `every date pattern in the source parses and formats`() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }
        val patterns = root.walk().filter { it.extension == "kt" }
            .flatMap { Regex("""ofPattern\("([^"]*)"\)""").findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toSet()
        assertTrue("no patterns found", patterns.isNotEmpty())
        patterns.forEach { DateTimeFormatter.ofPattern(it).format(LocalDate.of(2026, 9, 13).atTime(9, 30)) }
    }
}
