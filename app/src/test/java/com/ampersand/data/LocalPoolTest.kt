package com.ampersand.data

import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.Quiz
import com.ampersand.model.QuizTheme
import com.ampersand.model.Topic
import com.ampersand.model.UsRegion
import com.ampersand.model.WeatherKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Weather days, regional questions, and sports as something you asked for. */
class LocalPoolTest {

    private fun q(id: String, vararg tags: String) =
        Question(id, id, QuestionType.REFLECT, "Everyday", Depth(1), tags = tags.toSet())

    private val tuesday = LocalDate.of(2026, 3, 10)

    private val pool = QuestionPool(
        decks = listOf(Quiz("d", "d", "", QuizTheme.EVERYDAY, (1..10).map { q("q$it") })),
        weather = mapOf(WeatherKind.RAIN to listOf(q("rain1"), q("rain2"))),
        regional = mapOf(UsRegion.MIDWEST to listOf(q("mw1", "sports"))),
    )

    @Test
    fun `a rainy day asks about rain every other day, the same on both phones`() {
        val days = (0L..9L).map { tuesday.plusDays(it) }
        val rainy = days.map { pool.daily(it, weatherKind = WeatherKind.RAIN).id.startsWith("rain") }
        assertEquals(days.map { it.toEpochDay() % 2 == 0L }, rainy)
    }

    @Test
    fun `no weather, no weather question`() {
        (0L..9L).forEach { assertTrue(!pool.daily(tuesday.plusDays(it)).id.startsWith("rain")) }
    }

    @Test
    fun `regional questions reach only couples in that region`() {
        val midwest = (0L..40L).map { pool.daily(tuesday.plusDays(it), region = UsRegion.MIDWEST).id }.toSet()
        val south = (0L..40L).map { pool.daily(tuesday.plusDays(it), region = UsRegion.SOUTH).id }.toSet()
        assertTrue("mw1" in midwest)
        assertTrue("mw1" !in south)
        assertEquals("mw1", pool.question("mw1")?.id)
    }

    @Test
    fun `hiding sports hides the regional sports question too`() {
        val ids = (0L..40L).map { pool.daily(tuesday.plusDays(it), region = UsRegion.MIDWEST, hidden = setOf(Topic.SPORTS)).id }
        assertTrue("mw1" !in ids)
    }

    @Test
    fun `asking for sports makes it come up more`() {
        val base = QuestionPool(decks = listOf(Quiz("d", "d", "", QuizTheme.EVERYDAY, (1..10).map { q("q$it") } + q("s1", "sports"))))
        val state = com.ampersand.data.MockCouple.state().let { it.copy(interests = setOf(Topic.SPORTS), instrumentRuns = emptyList(), history = emptyList()) }
        val taste = Profiles.of(state)
        val days = (0L..120L).map { tuesday.plusDays(it) }
        val plain = days.count { base.daily(it).id == "s1" }
        val leaned = days.count { base.daily(it, taste = taste).id == "s1" }
        assertTrue("leaned $leaned vs plain $plain", leaned > plain)
        assertNotEquals(emptyList<String>(), taste.reasons)
    }
}
