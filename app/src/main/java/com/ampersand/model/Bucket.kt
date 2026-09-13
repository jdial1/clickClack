package com.ampersand.model

import java.time.LocalDate

/** The three kinds of thing worth doing together someday. */
enum class BucketGroup(val label: String) {
    GOALS("Goals"),
    MOMENTS("Moments"),
    GETAWAYS("Getaways"),
}

/**
 * One thing the two of you want to do, and whether you have.
 *
 * Either of you adds one; either of you ticks it off. Ticking is the part that
 * matters: it is the first thing in the app that starts as words and ends as
 * something the two of you actually did, so it earns a brick for both.
 */
data class BucketItem(
    val id: String,
    val text: String,
    val group: BucketGroup,
    val byUid: String,
    val createdAtMillis: Long = 0L,
    val doneOn: LocalDate? = null,
) {
    val isDone: Boolean get() = doneOn != null
}

object BucketList {

    /** To-do first, oldest first; then done, most recent first. */
    fun ordered(items: List<BucketItem>): Pair<List<BucketItem>, List<BucketItem>> {
        val (done, todo) = items.partition { it.isDone }
        return todo.sortedBy { it.createdAtMillis } to done.sortedByDescending { it.doneOn }
    }

    /**
     * The brick a ticked-off item earns [member]: a 4-stud flat, laid the day
     * it was ticked. One per person per item, by id, so ticking twice or on
     * two phones never lays it again.
     */
    fun reward(item: BucketItem, member: Member): Block? {
        val on = item.doneOn ?: return null
        return Block(
            id = "bucket_${item.id}_${member.uid}",
            shape = BrickShape(studs = 4, height = BrickHeight.MEDIUM),
            colorArgb = member.colorArgb,
            ownerUid = member.uid,
            earnedOn = on,
        )
    }
}
