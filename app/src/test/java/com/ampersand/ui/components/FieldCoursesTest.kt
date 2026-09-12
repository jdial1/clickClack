package com.ampersand.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a text box's bricks follow what is typed into it.
 *
 * The box starts as one four-stud brick, gains a second once the line runs past
 * the first, and when the text wraps that line is finished at two and the next
 * starts again at one.
 */
class FieldCoursesTest {

    // How far text may reach and still sit on one brick. Any number will do;
    // the rule only cares which side of it the line ends on.
    private val one = 100f

    private fun of(lines: Int, right: Float) = FieldCourses.of(lines, right, one)

    @Test
    fun `an empty box is one brick`() {
        assertEquals(listOf(1), of(lines = 1, right = 0f))
    }

    @Test
    fun `a short line stays one brick`() {
        assertEquals(listOf(1), of(lines = 1, right = 60f))
    }

    /** Right up to the edge still fits; the second brick comes when it would not. */
    @Test
    fun `exactly reaching the first brick does not add a second`() {
        assertEquals(listOf(1), of(lines = 1, right = one))
    }

    @Test
    fun `running past the first brick lays a second beside it`() {
        assertEquals(listOf(2), of(lines = 1, right = one + 1f))
    }

    @Test
    fun `wrapping finishes the line at two and starts the next at one`() {
        assertEquals(listOf(2, 1), of(lines = 2, right = 10f))
    }

    @Test
    fun `the new line grows to two the same way`() {
        assertEquals(listOf(2, 2), of(lines = 2, right = one + 1f))
    }

    @Test
    fun `every finished line is two bricks however many there are`() {
        assertEquals(listOf(2, 2, 2, 2, 1), of(lines = 5, right = 0f))
    }

    /**
     * A line ended by return is finished too.
     *
     * The layout reports its right edge only for the last line, so a short
     * line above can only ever be counted as finished -- and a wall with rows
     * stopping at random widths reads as a chart of your line breaks.
     */
    @Test
    fun `a line ended early by return is still a finished course`() {
        // "hi\n" lays out as two lines, the second empty.
        assertEquals(listOf(2, 1), of(lines = 2, right = 0f))
    }

    @Test
    fun `a zero line count still gives one brick`() {
        assertEquals(listOf(1), of(lines = 0, right = 0f))
    }

    @Test
    fun `there is one course per line and no line is wider than two bricks`() {
        for (lines in 1..12) for (right in listOf(0f, one, one + 1f, 999f)) {
            val courses = of(lines, right)
            assertEquals("courses for $lines lines", lines, courses.size)
            assertTrue(courses.all { it in 1..FieldCourses.PerLine })
            assertTrue("only the last can be short", courses.dropLast(1).all { it == FieldCourses.PerLine })
        }
    }

    @Test
    fun `a full line is eight studs`() {
        assertEquals(8, FieldCourses.Studs * FieldCourses.PerLine)
    }
}
