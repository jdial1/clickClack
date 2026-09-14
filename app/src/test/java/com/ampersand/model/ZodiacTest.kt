package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZodiacTest {

    @Test
    fun `signs change on their first day, and Capricorn wraps the year`() {
        assertEquals(Zodiac.ARIES, Zodiac.of(LocalDate.of(1989, 4, 15)))
        assertEquals(Zodiac.TAURUS, Zodiac.of(LocalDate.of(1990, 4, 20)))
        assertEquals(Zodiac.CAPRICORN, Zodiac.of(LocalDate.of(1990, 12, 25)))
        assertEquals(Zodiac.CAPRICORN, Zodiac.of(LocalDate.of(1990, 1, 5)))
        assertEquals(Zodiac.AQUARIUS, Zodiac.of(LocalDate.of(1990, 1, 20)))
        assertEquals(Zodiac.SAGITTARIUS, Zodiac.of(LocalDate.of(1990, 12, 21)))
    }

    @Test
    fun `a sign question is filled from each birthday`() {
        val q = Question("hz", "A {mysign} and a {theirsign}", QuestionType.REFLECT, "Playful", Depth(1))
        assertEquals("A Aries and a Pisces", q.forSigns(LocalDate.of(1989, 4, 15), LocalDate.of(1991, 3, 1)).text)
        assertEquals(q, q.forSigns(null, LocalDate.of(1991, 3, 1)))
    }

    @Test
    fun `Chinese zodiac by year, turning in early February`() {
        assertEquals(ChineseZodiac.SNAKE, ChineseZodiac.of(LocalDate.of(1989, 4, 15)))
        assertEquals(ChineseZodiac.DRAGON, ChineseZodiac.of(LocalDate.of(1989, 1, 20)))
        assertEquals(ChineseZodiac.RAT, ChineseZodiac.of(LocalDate.of(2020, 6, 1)))
        assertEquals(1980, decadeOf(LocalDate.of(1989, 12, 31)))
    }

    @Test
    fun `a half-birthday is a date to celebrate`() {
        val today = LocalDate.of(2026, 10, 15)
        assertEquals(listOf(DateKind.HALF_BIRTHDAY), Dates.kindsOn(today, emptyList(), listOf(LocalDate.of(1989, 4, 15)), null))
    }
}
