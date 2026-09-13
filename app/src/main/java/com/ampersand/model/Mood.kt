package com.ampersand.model

/**
 * How you are today, said in bricks.
 *
 * One tap, shown to the other person, and gone tomorrow. Named the way a
 * builder would say it, with a plain line underneath so nobody has to decode
 * "stepped on one" at seven in the morning. No faces and no emoji: each mood is
 * drawn from the same pack bricks as everything else (see MoodArt).
 *
 * Not a chat and not a log. It says where you are so the other person can go
 * gently, or not, and that is all it does.
 */
enum class Mood(val label: String, val plain: String) {
    SNAPPED_IN("Snapped in", "Good, and close to you"),
    BUILDING("Building something", "Motivated"),
    NEW_SET("New set energy", "Excited"),
    SORTING("Sorting pieces", "Busy in my head"),
    STILL_IN_THE_BOX("Still in the box", "Low on energy"),
    LOOSE("A little loose", "A bit off"),
    MISSING_A_PIECE("Missing a piece", "Missing something, or you"),
    KNOCKED_OVER("Knocked over", "Overwhelmed"),
    STEPPED_ON_ONE("Stepped on one", "Sore or annoyed"),
    HANDLE_WITH_CARE("Handle with care", "Stressed, go gently"),
}

/** A mood as saved: which, and the day it was set. Only today's counts. */
data class MoodEntry(val mood: Mood, val on: java.time.LocalDate) {
    fun current(today: java.time.LocalDate): Mood? = mood.takeIf { on == today }
}
