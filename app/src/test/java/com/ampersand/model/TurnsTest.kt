package com.ampersand.model

import com.ampersand.data.QuestionPool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** One of you picks what tomorrow is about, and both phones agree who. */
class TurnsTest {

    private val day = LocalDate.of(2026, 3, 10) // a Tuesday, no occasion

    private fun q(id: String, category: String, depth: Int = 1) =
        Question(id, id, QuestionType.REFLECT, category, Depth(depth))

    private fun opened(date: LocalDate, theme: String? = null, by: String? = null) =
        DayEntry(date, q("x", "Everyday"), opened = true, nextTheme = theme, nextThemeBy = by)

    @Test
    fun `both phones name the same chooser, whichever order they pass the uids`() {
        (0L..30L).forEach { n ->
            val d = day.plusDays(n)
            assertEquals(Turns.chooser(d, "alice", "bob"), Turns.chooser(d, "bob", "alice"))
        }
    }

    @Test
    fun `the turn alternates every day`() {
        (0L..30L).forEach { n ->
            val d = day.plusDays(n)
            assertNotEquals(Turns.chooser(d, "alice", "bob"), Turns.chooser(d.plusDays(1), "alice", "bob"))
        }
    }

    @Test
    fun `only the chooser can pick, only once the day is open, and only once`() {
        val chooser = Turns.chooser(day, "alice", "bob")
        val other = if (chooser == "alice") "bob" else "alice"
        val open = opened(day)
        assertTrue(Turns.canPick(open, chooser, other))
        assertFalse(Turns.canPick(open, other, chooser))
        assertFalse(Turns.canPick(open.copy(opened = false), chooser, other))
        assertFalse(Turns.canPick(open.copy(nextTheme = "Deep", nextThemeBy = chooser), chooser, other))
    }

    @Test
    fun `yesterday's pick is today's theme`() {
        val chooser = Turns.chooser(day, "alice", "bob")
        val history = listOf(opened(day, "Deep", chooser))
        assertEquals("Deep", Turns.themeFor(day.plusDays(1), history, "alice", "bob"))
    }

    @Test
    fun `a pick by the wrong person is ignored`() {
        val chooser = Turns.chooser(day, "alice", "bob")
        val other = if (chooser == "alice") "bob" else "alice"
        val history = listOf(opened(day, "Deep", other))
        assertNull(Turns.themeFor(day.plusDays(1), history, "alice", "bob"))
    }

    @Test
    fun `a pick from two days ago does not linger`() {
        val chooser = Turns.chooser(day, "alice", "bob")
        val history = listOf(opened(day, "Deep", chooser))
        assertNull(Turns.themeFor(day.plusDays(2), history, "alice", "bob"))
    }

    // ---- the draw ----------------------------------------------------------

    private val pool = QuestionPool(
        decks = listOf(
            Quiz("e", "e", "", QuizTheme.EVERYDAY, (1..20).map { q("e$it", "Everyday") }),
            Quiz(
                "d", "d", "", QuizTheme.DEEP,
                (1..3).map { q("d$it", "Deep", depth = 1) } + (4..6).map { q("d$it", "Deep", depth = 3) },
            ),
        ),
    )

    @Test
    fun `a picked theme decides the category`() {
        (0L..20L).forEach { n ->
            assertEquals("Deep", pool.daily(day.plusDays(n), theme = "Deep").category)
        }
    }

    @Test
    fun `a pick does not put a deep question on a weekday`() {
        (0L..20L).map { day.plusDays(it) }
            .filter { it.dayOfWeek.value <= 4 }
            .forEach { assertTrue(pool.daily(it, theme = "Deep").depth.level <= 2) }
    }

    @Test
    fun `when everything in the theme was asked lately, the pick gives way`() {
        val recent = (1..6).map { "d$it" }.toSet()
        assertEquals("Everyday", pool.daily(day, theme = "Deep", recent = recent).category)
    }

    @Test
    fun `an unknown theme is no theme`() {
        assertEquals(pool.daily(day), pool.daily(day, theme = "Nonsense"))
    }

    @Test
    fun `themes lists every category once`() {
        assertEquals(listOf("Everyday", "Deep"), pool.themes)
    }
}
