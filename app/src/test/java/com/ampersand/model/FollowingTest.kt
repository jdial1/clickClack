package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowingTest {

    @Test
    fun `sports stay out until someone follows something or asks for more`() {
        assertTrue(Following.hideSports(emptySet(), emptySet()))
        assertFalse(Following.hideSports(setOf(League.NHL), emptySet()))
        assertFalse(Following.hideSports(emptySet(), setOf(Topic.SPORTS)))
    }

    @Test
    fun `only followed leagues that are local and in season`() {
        val active = setOf(League.NFL, League.NHL, League.COLLEGE)
        assertEquals(setOf(League.NHL), Following.leagues(active, setOf(League.NHL, League.MLB)))
        assertEquals(emptySet<League>(), Following.leagues(active, emptySet()))
    }

    @Test
    fun `every league has a label for the checkboxes`() {
        assertEquals(League.entries.size, League.entries.map { it.label }.toSet().size)
    }
}
