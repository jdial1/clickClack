package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** The placement and rollover rules both repositories share. */
class PlateRulesTest {

    private val start = LocalDate.of(2026, 9, 14)
    private val brick = Block("b1", BrickShape(2), 0xFFF54E4F, "u1", start)
    private val other = Block("b2", BrickShape(2), 0xFF108BE2, "u2", start)
    private val plate = BuildPlate.startingOn(start, listOf(brick, other))

    @Test
    fun `a new week is dated, seven days long and keeps what it carries`() {
        assertEquals("plate_$start", plate.id)
        assertEquals(start.plusDays(6), plate.endsOn())
        assertEquals(listOf(brick, other), plate.unplaced())
    }

    @Test
    fun `a free spot in an open week is a legal move`() {
        assertNotNull(plate.placement("b1", 0, 0, start))
    }

    @Test
    fun `nothing is placed once the week is over or sealed`() {
        assertNull(plate.placement("b1", 0, 0, start.plusDays(7)))
        assertNull(plate.copy(completedOn = start).placement("b1", 0, 0, start))
    }

    @Test
    fun `a brick goes down once, onto a free spot, and only if it is this plate's`() {
        val placed = plate.copy(placed = listOf(Placement("b1", 0, 0)))
        assertNull(placed.placement("b1", 4, 0, start))
        assertNull(placed.placement("b2", 0, 0, start))
        assertNull(plate.placement("nope", 0, 0, start))
        assertEquals(listOf(other), placed.unplaced())
    }
}
