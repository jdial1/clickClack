package com.ampersand.model

/** Today's mood, shown to your partner and cleared tomorrow. Art is in MoodArt. */
enum class Mood(val label: String, val kind: Kind, val glyph: String) {
    SNAPPED_IN("Happy", Kind.FACE, "smile"),
    CLICKED("Content", Kind.FACE, "smile"),
    PLAYING_IT_COOL("Fine", Kind.FACE, "flat"),
    NEW_SET("Excited", Kind.FACE, "smile"),
    FLAT_PLATE("Meh", Kind.FACE, "flat"),
    SOMETHING_TO_TELL("Got news", Kind.SPECIAL, "news"),
    RARE_PIECE("Grateful", Kind.SPECIAL, "smile"),
    SOLID_GROUND("Steady", Kind.SPECIAL, "flat"),
    STILL_IN_THE_BOX("Tired", Kind.SPECIAL, "down"),
    HEAVY("Down", Kind.SPECIAL, "down"),
    HANDLE_WITH_CARE("Stressed", Kind.SPECIAL, "spiky"),
    MISSING_A_PIECE("Missing you", Kind.BUILT, "heart"),
    KNOCKED_OVER("Overwhelmed", Kind.BUILT, "spiky"),
    STEPPED_ON_ONE("Annoyed", Kind.BUILT, "spiky");

    enum class Kind {
        /** A character brick, in a color you choose. */
        FACE,

        /** One of the pack's special bricks. */
        SPECIAL,

        /** Plain bricks arranged, in your own color. */
        BUILT,
    }
}

/**
 * A saved mood, the day it was set, and a face's color (null = pack color). Only today's counts.
 * [atMillis] is when it was set; [spans] is how long each earlier mood that day lasted, in ms.
 */
data class MoodEntry(
    val mood: Mood,
    val on: java.time.LocalDate,
    val color: BrickColor? = null,
    val atMillis: Long = 0,
    val spans: Map<Mood, Long> = emptyMap(),
) {
    fun current(today: java.time.LocalDate): Mood? = mood.takeIf { on == today }

    /** The mood held longest on [on], counting the last one until midnight. */
    fun most(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Mood {
        val midnight = on.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val totals = spans.toMutableMap()
        if (atMillis > 0) totals[mood] = (totals[mood] ?: 0) + (midnight - atMillis).coerceAtLeast(0)
        return totals.maxByOrNull { it.value }?.key ?: mood
    }

    /** The one-stud sculpture brick for this day's mood, in [owner]'s colour. */
    fun block(owner: Member): Block = Block(
        id = "mood_${on}_${owner.uid}",
        shape = BrickShape(1, BrickHeight.LOW),
        colorArgb = owner.colorArgb,
        ownerUid = owner.uid,
        earnedOn = on,
        moodGlyph = most().glyph,
    )
}
