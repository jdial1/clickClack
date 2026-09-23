package com.ampersand.model

import java.time.LocalDate

/**
 * Bricks, build plates and the rules for earning them.
 *
 * Grid cells are 64 x 28 to match the brick art (stud 64px, shortest body 28px).
 * Heights are 1, 2 or 4 rows; widths 1, 2, 4 or 6 studs.
 */

/** Brick body heights, in grid rows. */
enum class BrickHeight(val rows: Int, val asset: String) {
    LOW(1, "low"),
    MEDIUM(2, "medium"),
    HIGH(4, "high"),
}

/** Slope pieces exist only at MEDIUM and HIGH, and only 2 or 4 studs wide. */
enum class Slope(val asset: String?) {
    NONE(null),
    LEFT("slope_left"),
    RIGHT("slope_right"),
    INVERTED_LEFT("slope_inverted_left"),
    INVERTED_RIGHT("slope_inverted_right"),
}

data class BrickShape(
    val studs: Int,
    val height: BrickHeight = BrickHeight.LOW,
    val slope: Slope = Slope.NONE,
) {
    /** Footprint, in grid cells. */
    val w: Int get() = studs
    val h: Int get() = height.rows

    /** The drawable name, e.g. `brick_medium_slope_left_4`. Slopes collide as their full bounding box. */
    val asset: String
        get() = buildString {
            append("brick_")
            append(height.asset)
            append('_')
            slope.asset?.let { append(it); append('_') }
            append(studs)
        }
}

/** The character bricks. Awarded at milestones, never for an ordinary day. */
enum class BlockFace(val asset: String) {
    A("extra_character_a"),
    B("extra_character_b"),
    C("extra_character_c"),
    D("extra_character_d"),
    E("extra_character_e"),
}

/** Characters are drawn from their own art, so they ignore the owner's color. */
val CharacterShape = BrickShape(2, BrickHeight.HIGH)

data class Block(
    val id: String,
    val shape: BrickShape,
    /** 0xAARRGGBB. The owner's color. */
    val colorArgb: Long,
    val ownerUid: String,
    val earnedOn: LocalDate,
    val face: BlockFace? = null,
    /** Set on a day's mood brick: which face it wears. See [MoodEntry.block]. */
    val moodGlyph: String? = null,
) {
    /** Faces and mood bricks have their own art; every other brick exists in the owner's color. */
    val asset: String
        get() = face?.asset
            ?: moodGlyph?.let { "mood_${it}_${BrickColor.of(colorArgb).slug}" }
            ?: BrickColor.of(colorArgb).asset(shape)
}

/** Bottom-left corner of a block, in grid cells. y = 0 is the baseplate. */
data class Placement(val blockId: String, val x: Int, val y: Int)

data class BuildPlate(
    val id: String,
    val startedOn: LocalDate,
    val periodDays: Int,
    /** Width in studs. */
    val gridW: Int,
    /** Height in rows. */
    val gridH: Int,
    val blocks: List<Block> = emptyList(),
    val placed: List<Placement> = emptyList(),
    /** The day this sculpture was sealed, or null while still being built. Blocks carry no date of their own. */
    val completedOn: LocalDate? = null,
    /** One line each about the week, keyed by uid. Written after the plate seals. */
    val notes: Map<String, String> = emptyMap(),
    /** The words each of you picked to name the week, keyed by uid. See [SculptureNames]. */
    val words: Map<String, NameWords> = emptyMap(),
) {
    companion object {
        /** Width in studs. A brick is up to 6 wide, so this fits two side by side. */
        const val GRID_W = 12

        /** Height in rows. Intentionally far more than two weeks of bricks can fill, so towers are possible. */
        const val GRID_H = 28
    }

    private val byId: Map<String, Block> get() = blocks.associateBy { it.id }

    fun block(id: String): Block? = byId[id]

    /** Sealed sculptures are read-only: no tray, no placing, no awarding. */
    val isSealed: Boolean get() = completedOn != null

    /** The last row with anything standing on it. Used to size thumbnails. */
    fun builtHeight(): Int = placed.maxOfOrNull { p ->
        p.y + (byId[p.blockId]?.shape?.h ?: 0)
    } ?: 0

    /** How many bricks [uid] earned into this plate, placed or not. */
    fun earnedBy(uid: String): Int = blocks.count { it.ownerUid == uid }

    /** Bricks earned but not yet on the plate, for [uid]. */
    fun tray(uid: String): List<Block> {
        val used = placed.mapTo(mutableSetOf()) { it.blockId }
        return blocks.filter { it.ownerUid == uid && it.id !in used }
    }

    fun endsOn(): LocalDate = startedOn.plusDays(periodDays.toLong() - 1)

    fun daysLeft(today: LocalDate): Int =
        (endsOn().toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)

    /** Every cell currently covered, for collision and support tests. */
    fun occupied(): Set<Pair<Int, Int>> {
        val cells = mutableSetOf<Pair<Int, Int>>()
        placed.forEach { p ->
            val b = byId[p.blockId] ?: return@forEach
            for (dx in 0 until b.shape.w) {
                for (dy in 0 until b.shape.h) {
                    cells += (p.x + dx) to (p.y + dy)
                }
            }
        }
        return cells
    }

    /**
     * Whether [shape] may sit with its bottom-left corner at ([x], [y]): in bounds,
     * not overlapping, and on the baseplate or resting on at least one occupied cell.
     */
    fun canPlace(
        shape: BrickShape,
        x: Int,
        y: Int,
        occupied: Set<Pair<Int, Int>> = occupied(),
    ): Boolean {
        if (x < 0 || y < 0 || x + shape.w > gridW || y + shape.h > gridH) return false

        for (dx in 0 until shape.w) {
            for (dy in 0 until shape.h) {
                if ((x + dx to y + dy) in occupied) return false
            }
        }

        if (y == 0) return true
        return (0 until shape.w).any { dx -> (x + dx to y - 1) in occupied }
    }

    /** Where a block dropped in column [x] would come to rest, or null if it cannot. */
    fun dropRow(
        shape: BrickShape,
        x: Int,
        occupied: Set<Pair<Int, Int>> = occupied(),
    ): Int? = (0 until gridH).firstOrNull { canPlace(shape, x, it, occupied) }
}

/** Brick rewards for days, talks and decks. */
object Rewards {

    /**
     * One brick for [member] for a revealed day they answered.
     *
     * Flat: the same for both of you whether or not a guess landed, so the plate
     * records turning up together and never who knew whom better. Width is the
     * question type ([widthFor]); height is depth plus [effortOf]. Every 7-day
     * streak earns a character instead, and a wildcard earns a roof.
     */
    fun forDay(day: DayEntry, member: Member, partner: Member, streakAfter: Int): List<Block> {
        if (!day.isRevealed) return emptyList()
        // No answer from you means no brick, even if the day is revealed.
        val mine = day.answerFor(member.uid) ?: return emptyList()
        day.answerFor(partner.uid) ?: return emptyList()

        val milestone = streakAfter > 0 && streakAfter % 7 == 0
        val shape = when {
            milestone -> CharacterShape
            // Talked-about days earn their piece later, in [forTalk].
            Wildcard.isWildcard(day.question.id) -> roof(member, partner)
            else -> BrickShape(widthFor(day.question.type), heightFor(day.question.depth, effortOf(mine, day.question)))
        }

        return listOf(
            Block(
                id = "${day.date}_${member.uid}",
                shape = shape,
                colorArgb = member.colorArgb,
                ownerUid = member.uid,
                earnedOn = day.date,
                face = if (milestone) faceFor(streakAfter) else null,
            ),
        )
    }

    /**
     * A small extra slope for a day you talked about, dated [on] (the morning after).
     *
     * A separate piece because the day's brick may already be placed or its plate
     * sealed. Both partners earn one.
     */
    fun forTalk(day: DayEntry, member: Member, partner: Member, on: LocalDate): List<Block> {
        if (!day.isRevealed || !day.wasTalkedAbout) return emptyList()
        return listOf(
            Block(
                id = "talk_${day.date}_${member.uid}",
                shape = BrickShape(
                    studs = 2,
                    height = BrickHeight.MEDIUM,
                    slope = if (member.uid < partner.uid) Slope.LEFT else Slope.RIGHT,
                ),
                colorArgb = member.colorArgb,
                ownerUid = member.uid,
                earnedOn = on,
            ),
        )
    }

    /** A brick for finishing a deck. Flat, like a day: how many guesses landed never changes it. */
    fun forQuiz(
        result: QuizResult,
        member: Member,
        earnedOn: LocalDate,
        /** From [deckEffort]: 1 if you wrote properly, 0 otherwise. */
        effort: Int = 0,
    ): List<Block> =
        listOf(block(result, member, BrickShape(4, deckHeight(4, effort)), earnedOn))

    /**
     * 1 if at least half of the deck's text questions got a real answer (per [effortOf]),
     * else 0. [yours] picks which side of each [CrossAnswer] is the member's own writing.
     */
    fun deckEffort(rows: List<CrossAnswer>, yours: Boolean): Int {
        val text = rows.filter {
            it.question.type == QuestionType.REFLECT || it.question.type == QuestionType.GUESS_ME
        }
        if (text.isEmpty()) return 0
        val substantial = text.count { row ->
            val written = Answer(
                uid = "",
                values = if (yours) row.yours else row.theirs,
                guess = if (yours) row.yourGuess else row.theirGuess,
            )
            effortOf(written, row.question) > 0
        }
        return if (substantial * 2 >= text.size) 1 else 0
    }

    /** Decks are MEDIUM, or HIGH with effort as long as the area stays within [RoofArea]. */
    private fun deckHeight(width: Int, effort: Int): BrickHeight =
        if (effort > 0 && width * BrickHeight.HIGH.rows <= RoofArea) BrickHeight.HIGH
        else BrickHeight.MEDIUM

    /** The roof's footprint; no ordinary brick may exceed it. */
    const val RoofArea = 4 * 4

    private fun block(
        result: QuizResult,
        member: Member,
        shape: BrickShape,
        earnedOn: LocalDate,
    ) = Block(
        // One brick per deck per person, however many listeners report the reveal.
        id = "quiz_${result.quiz.id}_${member.uid}",
        shape = shape,
        colorArgb = member.colorArgb,
        ownerUid = member.uid,
        earnedOn = earnedOn,
    )

    /**
     * The 4x4 slope, the rarest piece. The lower uid faces left so both phones
     * agree, and a shared win makes a peak.
     */
    private fun roof(member: Member, partner: Member): BrickShape = BrickShape(
        studs = 4,
        height = BrickHeight.HIGH,
        slope = if (member.uid < partner.uid) Slope.LEFT else Slope.RIGHT,
    )

    /**
     * 1 if your answer plus guess on a text question (REFLECT or GUESS_ME) reaches
     * [SubstantialWords] words, else 0. Capped at 1 so long answers cannot outgrow the roof.
     */
    fun effortOf(answer: Answer, question: Question): Int {
        if (question.type != QuestionType.REFLECT && question.type != QuestionType.GUESS_ME) return 0
        val words = (answer.values + answer.guess.orEmpty())
            .sumOf { text -> text.split(Whitespace).count { it.isNotBlank() } }
        return if (words >= SubstantialWords) 1 else 0
    }

    const val SubstantialWords = 15

    private val Whitespace = Regex("\\s+")

    /** Deep question (level 3+) and effort add one row step each, capped at HIGH. */
    private fun heightFor(depth: Depth, effort: Int): BrickHeight =
        when ((if (depth.level >= 3) 1 else 0) + effort) {
            0 -> BrickHeight.LOW
            1 -> BrickHeight.MEDIUM
            else -> BrickHeight.HIGH
        }

    /**
     * Width comes from how the question is answered, so a week's plate varies
     * without anyone being graded: a pick is 2 studs, a written answer 4, a list 6.
     * Only written answers can earn height from effort, which keeps 4 x HIGH the
     * tallest ordinary brick and inside [RoofArea].
     */
    fun widthFor(type: QuestionType): Int = when (type) {
        QuestionType.REFLECT, QuestionType.GUESS_ME -> 4
        QuestionType.MULTI_SELECT, QuestionType.RANK -> 6
        QuestionType.THIS_OR_THAT, QuestionType.PICK_ONE, QuestionType.SCALE, QuestionType.DROPDOWN -> 2
    }

    private fun faceFor(streak: Int): BlockFace =
        BlockFace.entries[(streak / 7 - 1).coerceAtLeast(0) % BlockFace.entries.size]
}
