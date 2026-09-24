package com.ampersand.data

import com.ampersand.model.DayEntry
import com.ampersand.model.Depth
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** After a quiet spell, coming back costs nothing: the next question is a light one. */
class GentleReturnTest {

    private val today = LocalDate.of(2026, 9, 24)
    private val q = Question("q", "?", QuestionType.REFLECT, "Everyday", Depth(1))
    private fun answered(daysAgo: Long) = DayEntry(today.minusDays(daysAgo), q, submitted = setOf("u1"))

    @Test
    fun `three quiet days bring a gentle question`() {
        assertTrue(gentleReturn(today, listOf(answered(4))))
    }

    @Test
    fun `either of you answering in the last three days keeps things as usual`() {
        assertFalse(gentleReturn(today, listOf(answered(4), answered(2))))
    }

    /** A new couple has had no quiet spell; they just haven't started. */
    @Test
    fun `a couple who never answered is not returning`() {
        assertFalse(gentleReturn(today, emptyList()))
    }
}
