package com.ampersand.model

import java.time.LocalDate

/** Bucket list categories. */
enum class BucketGroup(val label: String) {
    GOALS("Goals"),
    MOMENTS("Moments"),
    GETAWAYS("Getaways"),
}

/** One bucket list item. Either partner can add or tick it; ticking earns both a brick. */
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
     * The brick a ticked item earns [member]: a 4-stud flat dated the day it was
     * ticked. The id is per person per item, so ticking twice never lays it again.
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
