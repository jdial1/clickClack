package com.ampersand.model

/**
 * The four player colors. Values match the body fill of the pack's brick art,
 * since the bricks only exist in these colorways.
 */
enum class BrickColor(val argb: Long, val slug: String, val label: String) {
    RED(0xFFF54E4F, "red", "Red"),
    YELLOW(0xFFFFB733, "yellow", "Yellow"),
    GREEN(0xFF4EB162, "green", "Green"),
    BLUE(0xFF108BE2, "blue", "Blue");

    /** The pack asset for [shape] in this color, e.g. `red_brick_medium_2`. */
    fun asset(shape: BrickShape): String = "${slug}_${shape.asset}"

    companion object {
        fun of(argb: Long): BrickColor = entries.firstOrNull { it.argb == argb } ?: RED

        /** Cycles the palette, for per-option coloring in radio groups. */
        fun at(index: Int): BrickColor = entries[index.mod(entries.size)]

        /**
         * Display colors for two members, never the same one.
         *
         * On a clash the lower uid keeps its color and the other gets the first
         * free one. Computed from uids only so both phones agree; nothing is written.
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
