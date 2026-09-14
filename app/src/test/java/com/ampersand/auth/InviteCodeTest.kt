package com.ampersand.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class InviteCodeTest {

    @Test
    fun `a code is two words and three digits, typed any way`() {
        val code = InviteCode.next(Random(7))
        assertTrue(code, Regex("[A-Z]+[0-9]{3}").matches(code))
        val shown = InviteCode.display(code)
        assertTrue(shown, Regex("[A-Z][a-z]+ [A-Z][a-z]+ [0-9]{3}").matches(shown))
        assertEquals(code, InviteCode.normalise(shown.lowercase()))
        assertEquals(code, InviteCode.normalise(shown.replace(' ', '-')))
    }

    @Test
    fun `a shared link carries the code`() {
        assertEquals("WOBBLYCASTLE427", InviteCode.fromLink(InviteCode.link("WOBBLYCASTLE427")))
        assertNull(InviteCode.fromLink("https://example.com/join/X"))
    }
}
