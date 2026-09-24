package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Bricks left a week on the tray can be placed by either of you, and stay their owner's. */
class HandOverTest {

    private val today = LocalDate.of(2026, 9, 24)
    private fun brick(id: String, owner: String, earned: LocalDate) =
        Block(id, BrickShape(2, BrickHeight.LOW), 0xFFF54E4F, owner, earned)

    private val plate = BuildPlate(
        "p", today.minusDays(10), 14, 12, 28,
        blocks = listOf(
            brick("mine", "u1", today),
            brick("theirs-new", "u2", today.minusDays(3)),
            brick("theirs-old", "u2", today.minusDays(HandOverDays)),
            brick("theirs-placed", "u2", today.minusDays(9)),
        ),
        placed = listOf(Placement("theirs-placed", 0, 0)),
    )

    @Test
    fun `your tray plus their bricks left a week, never their placed or recent ones`() {
        assertEquals(listOf("mine", "theirs-old"), plate.placeable("u1", today).map { it.id })
    }

    @Test
    fun `a handed-over brick keeps its owner`() {
        assertEquals("u2", plate.placeable("u1", today).single { it.id == "theirs-old" }.ownerUid)
    }

    @Test
    fun `the plain tray is unchanged, so carry-over still sees only your own`() {
        assertEquals(listOf("mine"), plate.tray("u1").map { it.id })
    }
}
