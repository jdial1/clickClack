package com.ampersand.model

/** Today's mood, shown to your partner and cleared tomorrow. Art is in MoodArt. */
enum class Mood(val label: String, val kind: Kind) {
    SNAPPED_IN("Happy", Kind.FACE),
    CLICKED("Content", Kind.FACE),
    PLAYING_IT_COOL("Fine", Kind.FACE),
    NEW_SET("Excited", Kind.FACE),
    FLAT_PLATE("Meh", Kind.FACE),
    SOMETHING_TO_TELL("Got news", Kind.SPECIAL),
    RARE_PIECE("Grateful", Kind.SPECIAL),
    SOLID_GROUND("Steady", Kind.SPECIAL),
    STILL_IN_THE_BOX("Tired", Kind.SPECIAL),
    HEAVY("Down", Kind.SPECIAL),
    HANDLE_WITH_CARE("Stressed", Kind.SPECIAL),
    MISSING_A_PIECE("Missing you", Kind.BUILT),
    KNOCKED_OVER("Overwhelmed", Kind.BUILT),
    STEPPED_ON_ONE("Annoyed", Kind.BUILT);

    enum class Kind {
        /** A character brick, in a color you choose. */
        FACE,

        /** One of the pack's special bricks. */
        SPECIAL,

        /** Plain bricks arranged, in your own color. */
        BUILT,
    }
}

/** A saved mood, the day it was set, and a face's color (null = pack color). Only today's counts. */
data class MoodEntry(val mood: Mood, val on: java.time.LocalDate, val color: BrickColor? = null) {
    fun current(today: java.time.LocalDate): Mood? = mood.takeIf { on == today }
}
