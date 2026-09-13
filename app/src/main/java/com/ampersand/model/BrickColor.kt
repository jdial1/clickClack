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

        /**
         * The colours two members are drawn in, never the same one.
         *
         * Two people can land on one colour without anyone choosing it: both
         * picked red before pairing, or neither has picked yet. Both phones
         * settle it the same way, from the uids alone: the lower uid keeps the
         * colour and the other is shown in the first one left. Nothing is
         * written, so picking again settles it for real.
         */
        fun distinct(aUid: String, aArgb: Long, bUid: String, bArgb: Long): Pair<Long, Long> {
            val a = of(aArgb)
            val b = of(bArgb)
            if (a != b) return a.argb to b.argb
            val free = entries.first { it != a }.argb
            return if (aUid <= bUid) a.argb to free else free to b.argb
        }
    }
}
