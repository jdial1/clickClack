package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InboxTest {

    private val today = LocalDate.of(2026, 9, 13)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))
    private val you = Member("a", "Justin", BrickColor.RED.argb)
    private val them = Member("b", "Tasia", BrickColor.BLUE.argb)
    private fun deck(id: String) = Quiz(id, id.uppercase(), "", QuizTheme.EVERYDAY, listOf(q))
    private val kit = Instrument("k", "src", "The test", "", items = List(4) { InstrumentItem(low = "no", high = "yes", text = "i$it") }, subscales = emptyList(), minutes = 1)

    private fun state(runs: List<QuizRun> = emptyList(), sittings: List<InstrumentRun> = emptyList()) = CoupleState(
        you = you, them = them, today = DayEntry(today, q),
        plate = BuildPlate(id = "p", startedOn = today, periodDays = 7, gridW = 12, gridH = 28),
        quizRuns = runs, instrumentRuns = sittings,
    )

    @Test
    fun `a deck only one of you finished is waiting on the other`() {
        val items = Inbox.of(
            state(listOf(QuizRun("d1", "b", emptyMap()), QuizRun("d2", "a", emptyMap()),
                QuizRun("d3", "a", emptyMap()), QuizRun("d3", "b", emptyMap()))),
            listOf(deck("d1"), deck("d2"), deck("d3"), deck("d4")),
            emptyList(),
        )
        assertEquals(listOf("d1" to Turn.YOURS, "d2" to Turn.THEIRS), items.map { it.id to it.turn })
    }

    @Test
    fun `a sealed partner sitting counts as done from their progress`() {
        val run = InstrumentRun("k", answers = emptyMap(), progress = mapOf("b" to 4))
        val items = Inbox.of(state(sittings = listOf(run)), emptyList(), listOf(kit))
        assertEquals(listOf(Turn.YOURS), items.map { it.turn })
        assertEquals(true, items.single().themDone)
    }
}
