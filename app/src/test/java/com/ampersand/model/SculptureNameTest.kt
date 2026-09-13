package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SculptureNameTest {

    private val plate = BuildPlate("plate_2026-09-06", LocalDate.of(2026, 9, 6), 7, 12, 28)

    @Test
    fun `the name waits for both, then reads the same on both phones`() {
        assertNull(plate.copy(words = mapOf("alice" to NameWords("Wobbly", "Castle"))).name("alice", "bob"))
        val both = plate.copy(words = mapOf("alice" to NameWords("Wobbly", "Castle"), "bob" to NameWords("Sleepy", "Waffle")))
        val name = both.name("alice", "bob")!!
        assertEquals(name, both.name("bob", "alice"))
        listOf("Wobbly", "Castle", "Sleepy", "Waffle").forEach { assertTrue(name, it in name) }
    }

    @Test
    fun `each of you is offered six of each, and not the same six`() {
        val a = SculptureNames.offered(plate.id, "alice", SculptureNames.Things)
        assertEquals(6, a.size)
        assertEquals(a, SculptureNames.offered(plate.id, "alice", SculptureNames.Things))
        assertNotEquals(a, SculptureNames.offered(plate.id, "bob", SculptureNames.Things))
    }
}
