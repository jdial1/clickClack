package com.ampersand.model

import java.time.LocalDate

/**
 * The build: what you get for showing up, and what you make out of it.
 *
 * Geometry follows the brick art rather than the other way round. In the pack a
 * stud is 64px and the shortest brick body is 28px, so the grid cell is 64 x 28
 * -- wider than it is tall, exactly like a real brick. Heights come in 1, 2 and
 * 4 rows; widths in 1, 2, 4 and 6 studs. Inventing a square grid and stretching
 * the art onto it would have been the obvious move and it would look wrong.
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

    /**
     * The drawable this maps to, e.g. `brick_medium_slope_left_4`.
     *
     * Slopes are collided as their full bounding box. Honouring the diagonal
     * would be more correct and, on a plate this size, invisible.
     */
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

/** Characters are drawn from their own art, so they ignore the owner's colour. */
val CharacterShape = BrickShape(2, BrickHeight.HIGH)

data class Block(
    val id: String,
    val shape: BrickShape,
    /** 0xAARRGGBB. The owner's colour, so the finished piece shows who built what. */
    val colorArgb: Long,
    val ownerUid: String,
    val earnedOn: LocalDate,
    val face: BlockFace? = null,
) {
    /** Faces have their own art; every other brick exists in the owner's colour. */
    val asset: String get() = face?.asset ?: BrickColor.of(colorArgb).asset(shape)
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
    /**
     * The day this sculpture was sealed, or null while it is still being built.
     *
     * The date belongs to the **finished thing**, not to the bricks in it. A
     * brick that remembered its own day would turn the sculpture into a
     * scrollable diary and the plate into an index; what you actually want to
     * be able to say is "that is the one we made the week of the ninth". So
     * individual blocks stay anonymous and the plate carries the date.
     */
    val completedOn: LocalDate? = null,
) {
    companion object {
        /** Width in studs. A brick is up to 6 wide, so this fits two side by side. */
        const val GRID_W = 12

        /**
         * Height in rows, at 4 rows to a tall brick.
         *
         * Deliberately far more than a fortnight of bricks can fill. The plate
         * is the room the sculpture gets to grow into, and a board sized to the
         * expected number of bricks makes every week end the same shape --
         * spread wide along the floor, because there was nowhere else to go.
         * Headroom is what lets a tower be a choice.
         */
        const val GRID_H = 28
    }

    private val byId: Map<String, Block> get() = blocks.associateBy { it.id }

    fun block(id: String): Block? = byId[id]

    /** Sealed sculptures are read-only: no tray, no placing, no awarding. */
    val isSealed: Boolean get() = completedOn != null

    /** The last row with anything standing on it. Sizes a thumbnail honestly. */
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
     * Whether [shape] may sit with its bottom-left corner at ([x], [y]).
     *
     * Three rules, and the third is the one that makes it feel like bricks
     * rather than stickers: a block must be on the baseplate or resting on at
     * least one stud of something already placed. Without it you can hang
     * bricks in mid-air and the sculpture stops reading as built.
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

/** What turning up is worth, and what knowing them is worth. */
object Rewards {

    /**
     * One brick for each thing you personally finished, sized by how well you
     * did on it.
     *
     * Both halves of that are deliberate, and both are changes from the first
     * version, which handed each person one identical brick per completed day.
     *
     * **Volume is participation.** If you answer the daily question and play two
     * decks and your partner answers the daily question, you earn three bricks
     * and they earn one, and the week's sculpture is visibly three quarters your
     * colour. Real couples are not symmetric -- one person nearly always turns
     * up more -- and an app that quietly evens that out is telling a comfortable
     * lie about the week. The sculpture is a record, so it should record it.
     *
     * **Size is how well you knew them.** A landed guess earns a six-stud
     * brick, a miss earns a two. So a good week builds wide and solid and a
     * rough one builds out of stubs, and the two are different silhouettes
     * across the room. It is not a score anybody has to read.
     *
     * **Height is depth, plus what you put in.** A deep question is taller
     * than a light one, and a real answer to a text question is one step
     * taller than a word or two -- see [effortOf]. It is your own words that
     * count, so a partner who writes an essay does not grow your brick and one
     * who writes "yeah" does not shrink it.
     *
     * Nothing here pays more for *winning* an argument about who is better --
     * both people earn on their own knowledge of the other, independently.
     */
    fun forDay(day: DayEntry, member: Member, partner: Member, streakAfter: Int): List<Block> {
        if (!day.isRevealed) return emptyList()
        // You earn for turning up, so no answer means no brick -- even when
        // your partner answered and the day is technically revealed.
        val mine = day.answerFor(member.uid) ?: return emptyList()
        val theirs = day.answerFor(partner.uid) ?: return emptyList()

        val effort = effortOf(mine, day.question)
        val milestone = streakAfter > 0 && streakAfter % 7 == 0
        val called = day.question.isScored && day.question.guessHit(mine.guess, theirs.values)
        val shape = when {
            milestone -> CharacterShape
            // The one day whose question came from inside the relationship
            // earns the piece that is not a box. A day that carried on away
            // from the phone earns one too, but not here: that is only known
            // the morning after, long after this brick is laid -- see [forTalk].
            Wildcard.isWildcard(day.question.id) -> roof(member, partner)
            // The rarest good outcome in the app: you called the answer to a
            // question that was actually hard. It earns the only piece that is
            // not a box, which is the only way a week ever stops being one.
            called && day.question.depth.level >= 3 -> roof(member, partner)
            !day.question.isScored -> unscored(day.question.depth, effort)
            called -> landed(day.question.depth, effort)
            else -> missed(day.question.depth, effort)
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
     * A small sloped piece for a day that came up again, laid the morning after.
     *
     * An extra piece rather than an upgrade to the day's own brick, for two
     * reasons. That brick is already built into the plate by then, and turning
     * a two-stud stub into anything wider could collide with whatever was laid
     * beside it. And the week it belonged to may have sealed overnight -- a new
     * piece simply lands on the week you are in, dated the morning you said so.
     *
     * Both of you earn one, whichever of you tapped: the conversation was had by
     * two people. Two studs wide so it adds to a week without dominating it, and
     * sloped so it still reads from across the room.
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

    /**
     * A brick for finishing a deck, sized by the share of your guesses that
     * landed.
     *
     * A deck is five or six questions rather than one, so it pays a taller
     * brick than a day: the height says how much you did, the width says how
     * well.
     */
    fun forQuiz(
        result: QuizResult,
        member: Member,
        partner: Member,
        hits: Int,
        earnedOn: LocalDate,
        /** From [deckEffort]: 1 if you wrote properly, 0 otherwise. */
        effort: Int = 0,
    ): List<Block> {
        if (result.possible == 0) {
            return listOf(
                block(result, member, BrickShape(4, deckHeight(4, effort)), earnedOn),
            )
        }
        // Every scorable question in the deck, called. Worth a roof for the
        // same reason a deep day is: it is the outcome that does not happen by
        // turning up.
        if (hits == result.possible) {
            return listOf(block(result, member, roof(member, partner), earnedOn))
        }

        val share = hits.toFloat() / result.possible
        val width = when {
            share >= 0.7f -> 6
            share >= 0.35f -> 4
            else -> 2
        }
        return listOf(
            block(result, member, BrickShape(width, deckHeight(width, effort)), earnedOn),
        )
    }

    /**
     * Whether you wrote properly across a deck: 0 or 1, the same cap as a day.
     *
     * One step if at least half of the deck's text questions got a real answer
     * from you, by the same fifteen-word line [effortOf] uses. Half rather than
     * all, because a deck is five or six questions played in one go and one
     * short answer in the middle of it should not undo the rest. A deck with no
     * text questions has nothing to measure and earns nothing extra.
     *
     * [yours] picks which side of each cross-answer is the member's own writing,
     * since a result holds both of you and only your words count toward yours.
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

    /**
     * A deck pays a step taller than a day, and what you wrote adds one more --
     * but only where the result still fits under the roof.
     *
     * The six-stud brick for guessing most of a deck right is the one that
     * cannot grow: at four rows it would be twenty-four cells, half again the
     * rarest piece in the app. It is already the widest thing a deck can earn,
     * so effort shows on everything narrower instead.
     */
    private fun deckHeight(width: Int, effort: Int): BrickHeight =
        if (effort > 0 && width * BrickHeight.HIGH.rows <= RoofArea) BrickHeight.HIGH
        else BrickHeight.MEDIUM

    /** The slope's footprint, and the ceiling nothing ordinary may exceed. */
    const val RoofArea = 4 * 4

    private fun block(
        result: QuizResult,
        member: Member,
        shape: BrickShape,
        earnedOn: LocalDate,
    ) = Block(
        // Idempotent the same way a day is: one brick per deck per person, no
        // matter how many listeners report the reveal.
        id = "quiz_${result.quiz.id}_${member.uid}",
        shape = shape,
        colorArgb = member.colorArgb,
        ownerUid = member.uid,
        earnedOn = earnedOn,
    )

    /**
     * The piece that gives a sculpture a skyline.
     *
     * Flats can only ever make a rectangle: every week the two of you built
     * came out the same shape, wider or narrower, and the silhouette said
     * nothing the caption did not. A slope is the one piece that changes the
     * outline, and it is deliberately the hardest to get.
     *
     * The pair face opposite ways, decided by whose uid sorts first so both
     * phones agree without talking. When you both call the same deep question
     * the two slopes meet in a peak, which is the sculpture saying something
     * about the two of you rather than about each of you.
     */
    private fun roof(member: Member, partner: Member): BrickShape = BrickShape(
        studs = 4,
        height = BrickHeight.HIGH,
        slope = if (member.uid < partner.uid) Slope.LEFT else Slope.RIGHT,
    )

    /**
     * Whether you actually wrote something: 0 or 1, and never more.
     *
     * Only the two question types that are a text box. Every other type is a
     * choice between options somebody else wrote, so its "length" is the length
     * of the option label and measures nothing about you.
     *
     * One step, not a scale, and that is the cap. Fifteen words and fifteen
     * hundred earn the same brick. Letting length keep climbing would turn a
     * six-stud "you called it" into a four-row slab twenty-four cells big --
     * half again the size of the rarest piece in the app -- and one long night
     * would fill a third of the week's plate by itself. Worse, it would pay for
     * an essay over a sentence that was true, which is the wrong thing to pay
     * for in a couples app.
     *
     * Counts your answer and your guess together, since both are things you
     * wrote. Words rather than characters, so "aaaaaaaaaaaaaaaa" is one.
     */
    fun effortOf(answer: Answer, question: Question): Int {
        if (question.type != QuestionType.REFLECT && question.type != QuestionType.GUESS_ME) return 0
        val words = (answer.values + answer.guess.orEmpty())
            .sumOf { text -> text.split(Whitespace).count { it.isNotBlank() } }
        return if (words >= SubstantialWords) 1 else 0
    }

    /** A couple of real sentences, typed on a phone. */
    const val SubstantialWords = 15

    private val Whitespace = Regex("\\s+")

    /**
     * Depth and effort, one step each, capped at the tallest brick the pack has.
     *
     * Adding rather than taking the higher is what lets both show: a light
     * question answered properly stands as tall as a deep one answered in two
     * words, and only a deep question answered properly reaches HIGH. Because
     * the roof already takes every called deep day, the widest brick that can
     * reach HIGH is the four-stud reflection -- sixteen cells, exactly the
     * roof's own size -- so nothing ordinary ever outgrows the best piece.
     */
    private fun heightFor(depth: Depth, effort: Int): BrickHeight =
        when ((if (depth.level >= 3) 1 else 0) + effort) {
            0 -> BrickHeight.LOW
            1 -> BrickHeight.MEDIUM
            else -> BrickHeight.HIGH
        }

    /** Wide and solid: you called it. */
    private fun landed(depth: Depth, effort: Int) = BrickShape(6, heightFor(depth, effort))

    /** A stub. You turned up, which is the part that earns anything at all. */
    private fun missed(depth: Depth, effort: Int) = BrickShape(2, heightFor(depth, effort))

    /** REFLECT days cannot be got right or wrong, so they are not judged. */
    private fun unscored(depth: Depth, effort: Int) = BrickShape(4, heightFor(depth, effort))

    private fun faceFor(streak: Int): BlockFace =
        BlockFace.entries[(streak / 7 - 1).coerceAtLeast(0) % BlockFace.entries.size]
}
