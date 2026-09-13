package com.ampersand.model

/**
 * How you are today, said in bricks.
 *
 * One tap, shown to the other person, and gone tomorrow. Named the way a
 * builder would say it, with a plain line underneath so nobody has to decode
 * "stepped on one" at seven in the morning. No emoji: each mood is a brick from
 * the pack (see MoodArt).
 *
 * The five character bricks come first and carry the everyday moods; each can
 * be worn in its own pack colour or any of the four brick colours. After them,
 * the pack's other special bricks, then three built from plain bricks for the
 * ones a face cannot say.
 *
 * Not a chat and not a log. It says where you are so the other person can go
 * gently, or not, and that is all it does.
 */
enum class Mood(val label: String, val plain: String, val kind: Kind) {
    SNAPPED_IN("Snapped in", "Good, and close to you", Kind.FACE),
    CLICKED("Clicked into place", "Content and easy", Kind.FACE),
    PLAYING_IT_COOL("Playing it cool", "Fine, really", Kind.FACE),
    NEW_SET("New set energy", "Excited", Kind.FACE),
    FLAT_PLATE("Flat plate", "Neither up nor down", Kind.FACE),
    SOMETHING_TO_TELL("Something to tell you", "I have news", Kind.SPECIAL),
    RARE_PIECE("Found a rare piece", "Lucky, grateful", Kind.SPECIAL),
    SOLID_GROUND("On solid ground", "Steady", Kind.SPECIAL),
    STILL_IN_THE_BOX("Still in the box", "Low on energy", Kind.SPECIAL),
    HEAVY("Heavy as a stone", "Weighed down", Kind.SPECIAL),
    HANDLE_WITH_CARE("Handle with care", "Stressed, go gently", Kind.SPECIAL),
    MISSING_A_PIECE("Missing a piece", "Missing something, or you", Kind.BUILT),
    KNOCKED_OVER("Knocked over", "Overwhelmed", Kind.BUILT),
    STEPPED_ON_ONE("Stepped on one", "Sore or annoyed", Kind.BUILT);

    enum class Kind {
        /** A character brick, in a colour of your choosing. */
        FACE,

        /** One of the pack's special bricks, as the pack draws it. */
        SPECIAL,

        /** Plain bricks arranged, in your own colour. */
        BUILT,
    }
}

/**
 * A mood as saved: which, the day it was set, and for a face the colour it was
 * worn in (null is the face's own pack colour). Only today's counts.
 */
data class MoodEntry(val mood: Mood, val on: java.time.LocalDate, val color: BrickColor? = null) {
    fun current(today: java.time.LocalDate): Mood? = mood.takeIf { on == today }
}
