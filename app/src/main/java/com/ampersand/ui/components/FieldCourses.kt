package com.ampersand.ui.components

/**
 * How many four-stud bricks each line of a text box uses. No Compose, so it is unit-testable.
 * Finished lines are always two bricks; the last line is one until its text passes the first brick.
 */
object FieldCourses {

    /** The width of every brick in the box. */
    const val Studs = 4

    /** Bricks in a full line. A line of text is eight studs across. */
    const val PerLine = 2

    /**
     * Bricks per course, top course first.
     *
     * @param lineCount lines the visible text currently lays out to
     * @param lastLineRight how far right the last line's text reaches, in px
     *   measured from where the text starts
     * @param oneBrick how far that text may reach and still sit on the first
     *   brick of its line, in the same units
     */
    fun of(lineCount: Int, lastLineRight: Float, oneBrick: Float): List<Int> {
        val finished = (lineCount - 1).coerceAtLeast(0)
        val last = if (lastLineRight > oneBrick) PerLine else 1
        return List(finished) { PerLine } + last
    }
}
