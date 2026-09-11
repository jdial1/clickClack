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

    companion object {
        val FLAT_WIDTHS = listOf(1, 2, 4, 6)
        val SLOPE_WIDTHS = listOf(2, 4)
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
    private val byId: Map<String, Block> get() = blocks.associateBy { it.id }

    fun block(id: String): Block? = byId[id]

    /** Sealed sculptures are read-only: no tray, no placing, no awarding. */
    val isSealed: Boolean get() = completedOn != null

    /** The last row with anything standing on it. Sizes a thumbnail honestly. */
    fun builtHeight(): Int = placed.maxOfOrNull { p ->
        p.y + (byId[p.blockId]?.shape?.h ?: 0)
    } ?: 0

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

/**
 * What a finished day is worth.
 *
 * Deliberately modest: one brick each per day, sized by how much the day asked
 * of you. Paying out more for a better score would make the plate a scoreboard,
 * and the point is that it records turning up together, not winning.
 */
object Rewards {

    fun forDay(day: DayEntry, member: Member, streakAfter: Int): List<Block> {
        if (!day.isRevealed) return emptyList()

        val milestone = streakAfter > 0 && streakAfter % 7 == 0
        val shape = when {
            milestone -> CharacterShape
            day.question.depth.level >= 3 -> BrickShape(4, BrickHeight.MEDIUM)
            day.question.depth.level == 2 -> BrickShape(4, BrickHeight.LOW)
            else -> BrickShape(2, BrickHeight.LOW)
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

    private fun faceFor(streak: Int): BlockFace =
        BlockFace.entries[(streak / 7 - 1).coerceAtLeast(0) % BlockFace.entries.size]
}
