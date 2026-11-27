package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * [Rewards.due] is the one rule for which bricks are owed. Both repositories lay
 * exactly what it returns, so these cases are the contract for both.
 */
class RewardsDueTest {

    private val today = LocalDate.of(2026, 9, 14)
    private val yesterday = today.minusDays(1)
    private val q = Question("q", "?", QuestionType.THIS_OR_THAT, "Everyday", Depth(1))

    private fun plate(id: String, started: LocalDate, sealed: LocalDate? = null, blocks: List<Block> = emptyList()) =
        BuildPlate(id, started, 7, 12, 28, blocks = blocks, completedOn = sealed)

    private fun state(
        you: Member = Member("u1", "You", 0xFFF54E4F),
        them: Member = Member("u2", "Maya", 0xFF108BE2),
        today: DayEntry = DayEntry(this.today, q),
        plate: BuildPlate = plate("this-week", this.today),
        history: List<DayEntry> = emptyList(),
        bucket: List<BucketItem> = emptyList(),
    ) = CoupleState(you = you, them = them, today = today, plate = plate, history = history, bucket = bucket)

    private val revealedToday = DayEntry(today, q, answers = mapOf("u1" to Answer.of("u1", "a"), "u2" to Answer.of("u2", "b")))

    private fun due(s: CoupleState, plates: List<BuildPlate> = emptyList()) = Rewards.due(s, today, emptyList(), plates)

    @Test
    fun `nothing is owed on a quiet day`() {
        assertTrue(due(state()).isEmpty())
    }

    @Test
    fun `a revealed day owes one brick each`() {
        val owed = due(state(today = revealedToday))
        assertEquals(setOf("${today}_u1", "${today}_u2"), owed.map { it.id }.toSet())
    }

    @Test
    fun `a brick already on any plate is not owed again`() {
        val first = due(state(today = revealedToday))
        val sealedWithMine = plate("last-week", today.minusDays(7), sealed = today, blocks = first.take(1))
        val livePlate = plate("this-week", today, blocks = first.drop(1))
        assertTrue(due(state(today = revealedToday, plate = livePlate), listOf(sealedWithMine)).isEmpty())
    }

    @Test
    fun `nothing is laid on a sealed plate`() {
        val sealed = plate("this-week", today, sealed = today)
        assertTrue(due(state(today = revealedToday, plate = sealed)).isEmpty())
    }

    @Test
    fun `a talk counts only for a past day`() {
        val talkedToday = revealedToday.copy(talkedAbout = setOf("u1"))
        assertEquals(2, due(state(today = talkedToday)).size)
        val talkedYesterday = DayEntry(yesterday, q, opened = true, talkedAbout = setOf("u1"))
        val owed = due(state(history = listOf(talkedYesterday))).map { it.id }.toSet()
        assertEquals(setOf("talk_${yesterday}_u1", "talk_${yesterday}_u2"), owed)
    }

    @Test
    fun `a ticked bucket item owes one each`() {
        val item = BucketItem("b1", "Go north", BucketGroup.GOALS, "u1", doneOn = today)
        assertEquals(2, due(state(bucket = listOf(item))).size)
    }

    @Test
    fun `a mood earns its brick once its day is over`() {
        val you = Member("u1", "You", 0xFFF54E4F)
        val moodToday = you.copy(mood = MoodEntry(Mood.CLICKED, today))
        val moodYesterday = you.copy(mood = MoodEntry(Mood.CLICKED, yesterday))
        assertTrue(due(state(you = moodToday)).isEmpty())
        assertEquals(listOf("mood_${yesterday}_u1"), due(state(you = moodYesterday)).map { it.id })
    }
}
