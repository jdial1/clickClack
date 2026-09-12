package com.ampersand.data

import com.ampersand.model.Instrument
import com.ampersand.model.InstrumentItem
import com.ampersand.model.PageSize
import com.ampersand.model.ProgressFloor
import com.ampersand.model.Cadence
import com.ampersand.model.Subscale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Paging a sitting, checked by arithmetic rather than by scrolling one.
 *
 * Page boundaries are exactly the kind of thing that looks right on the two
 * pages somebody happens to open and is wrong on the third: a 28-item
 * instrument at ten a page ends on a short page, a 20-item one across two
 * passes must not let a page straddle them, and the flat response slots have to
 * keep lining up with the items after every split.
 */
class PaginationTest {

    // ---- the real instruments ---------------------------------------------

    @Test
    fun `a thirty-two item sitting is four pages, the last one short`() {
        val pages = Instruments.JUNGIAN.pages()
        assertEquals(4, pages.size)
        assertEquals(listOf(10, 10, 10, 2), pages.map { it.size })
        assertEquals(0..9, pages[0].items)
        assertEquals(30..31, pages[3].items)
        // One pass, so slots and items are the same numbers.
        assertTrue(pages.all { it.items == it.slots })
        assertTrue(pages.all { it.pass == 0 })
    }

    @Test
    fun `a twenty-eight item sitting is three pages`() {
        val pages = Instruments.LOVE_ATTITUDES.pages()
        assertEquals(3, pages.size)
        assertEquals(listOf(10, 10, 8), pages.map { it.size })
        assertEquals(20..27, pages[2].items)
    }

    /**
     * The one that would break first.
     *
     * Twenty items asked twice is forty responses, and the naive
     * implementation -- page the flat list -- gives four pages of ten that
     * happen to land correctly. Twenty-two items twice would not: page two
     * would end mid-pass and page three would open with the tail of the first
     * stem under the second one's heading.
     */
    @Test
    fun `a page never straddles two passes`() {
        val pages = Instruments.LOVE_LANGUAGES.pages()
        assertEquals(4, pages.size)
        assertEquals(listOf(0, 0, 1, 1), pages.map { it.pass })
        assertEquals(0..9, pages[0].slots)
        assertEquals(10..19, pages[1].slots)
        // Second pass restarts at item 0 but continues at slot 20.
        assertEquals(0..9, pages[2].items)
        assertEquals(20..29, pages[2].slots)
        assertEquals(30..39, pages[3].slots)
    }

    @Test
    fun `an awkward split still keeps passes whole`() {
        val kit = fake(items = 22, passes = 2)
        val pages = kit.pages()
        assertEquals(listOf(0, 0, 0, 1, 1, 1), pages.map { it.pass })
        assertEquals(listOf(10, 10, 2, 10, 10, 2), pages.map { it.size })
        // Pass one ends at slot 21, pass two opens at 22 -- no page spans them.
        assertEquals(20..21, pages[2].slots)
        assertEquals(22..31, pages[3].slots)
    }

    // ---- invariants that must hold for anything ---------------------------

    @Test
    fun `every response slot is on exactly one page`() {
        listOf(
            Instruments.JUNGIAN,
            Instruments.LOVE_ATTITUDES,
            Instruments.LOVE_LANGUAGES,
            fake(items = 1, passes = 1),
            fake(items = 9, passes = 3),
            fake(items = 10, passes = 1),
            fake(items = 11, passes = 2),
        ).forEach { kit ->
            val slots = kit.pages().flatMap { it.slots.toList() }
            assertEquals("${kit.id}: no slot twice", slots.size, slots.toSet().size)
            assertEquals("${kit.id}: every slot covered", (0 until kit.length).toSet(), slots.toSet())
            assertEquals("${kit.id}: in order", slots.sorted(), slots)
        }
    }

    @Test
    fun `page indices are contiguous from zero`() {
        val pages = Instruments.LOVE_LANGUAGES.pages()
        assertEquals(pages.indices.toList(), pages.map { it.index })
    }

    @Test
    fun `no page is ever empty and none exceeds the size asked for`() {
        listOf(1, 2, 3, 7, 10, 40, 100).forEach { per ->
            val pages = Instruments.JUNGIAN.pages(per)
            assertTrue("size $per: none empty", pages.all { it.size > 0 })
            assertTrue("size $per: none over", pages.all { it.size <= per })
        }
    }

    @Test
    fun `a page bigger than the pass is one page per pass`() {
        val pages = Instruments.LOVE_LANGUAGES.pages(perPage = 100)
        assertEquals(2, pages.size)
        assertEquals(listOf(20, 20), pages.map { it.size })
    }

    @Test
    fun `a page size of zero is refused rather than looping forever`() {
        assertThrows(IllegalArgumentException::class.java) {
            Instruments.JUNGIAN.pages(perPage = 0)
        }
    }

    // ---- the progress header gate -----------------------------------------

    @Test
    fun `progress shows only past five questions`() {
        assertEquals(5, ProgressFloor)
        assertEquals(10, PageSize)

        assertFalse("five is a form you can see all of", fake(items = 5, passes = 1).needsProgress)
        assertTrue("six has somewhere to be", fake(items = 6, passes = 1).needsProgress)
        // Counted across passes: three asked twice is six answers to give.
        assertTrue("three twice is six", fake(items = 3, passes = 2).needsProgress)

        assertTrue(Instruments.JUNGIAN.needsProgress)
        assertTrue(Instruments.LOVE_ATTITUDES.needsProgress)
        assertTrue(Instruments.LOVE_LANGUAGES.needsProgress)
    }

    private fun fake(items: Int, passes: Int) = Instrument(
        id = "fake-$items-$passes",
        sourceId = "none",
        title = "Fake",
        blurb = "",
        items = List(items) { InstrumentItem("low", "high") },
        subscales = listOf(Subscale("all", "All", terms = (0 until items).associateWith { 1 })),
        cadence = Cadence.ONCE,
        minutes = 1,
        passes = List(passes) { "pass $it" },
    )
}
