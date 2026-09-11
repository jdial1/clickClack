package com.ampersand.model

/**
 * The four colours you can be.
 *
 * Values are lifted from the pack's own artwork — each is the *body* fill of a
 * real brick, so UI painted in these sits flush against the brick art instead of
 * near it. A free colour picker was the earlier design and it was wrong: the
 * bricks only exist in these colourways, and tinting greyscale art cannot reach
 * a saturated red without clipping, so the pieces never quite matched the chrome.
 */
enum class BrickColor(val argb: Long, val slug: String, val label: String) {
    RED(0xFFF54E4F, "red", "Red"),
    YELLOW(0xFFFFB733, "yellow", "Yellow"),
    GREEN(0xFF4EB162, "green", "Green"),
    BLUE(0xFF108BE2, "blue", "Blue");

    /** The pack asset for [shape] in this colour, e.g. `red_brick_medium_2`. */
    fun asset(shape: BrickShape): String = "${slug}_${shape.asset}"

    companion object {
        fun of(argb: Long): BrickColor = entries.firstOrNull { it.argb == argb } ?: RED

        /** Cycles the palette, for per-option colouring in radio groups. */
        fun at(index: Int): BrickColor = entries[index.mod(entries.size)]
    }
}
