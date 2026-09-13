package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DistinctColorTest {

    private val red = BrickColor.RED.argb
    private val blue = BrickColor.BLUE.argb

    @Test
    fun `different colours are left alone`() {
        assertEquals(red to blue, BrickColor.distinct("a", red, "b", blue))
    }

    @Test
    fun `a clash always ends in two colours`() {
        val (x, y) = BrickColor.distinct("a", red, "b", red)
        assertNotEquals(x, y)
    }

    @Test
    fun `both phones settle a clash the same way, the lower uid keeps it`() {
        val mine = BrickColor.distinct("a", red, "b", red)
        val theirs = BrickColor.distinct("b", red, "a", red)
        assertEquals(red, mine.first)
        assertEquals(mine, theirs.second to theirs.first)
    }
}
