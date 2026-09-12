package com.ampersand.ui.components

/**
 * How many four-stud bricks each line of a text box is built from.
 *
 * Kept free of Compose so the rule can be tested as arithmetic: the box starts
 * as one brick, gains a second once the line's text runs past the first, and
 * when the text wraps that line is finished -- two bricks -- and the next line
 * starts again at one.
 *
 * Every finished line is exactly two bricks even when it was ended early by
 * pressing return. A finished course is a finished course; a wall whose rows
 * stop at random widths above the line you are typing on stops reading as a
 * wall and starts reading as a bar chart of your line breaks.
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
