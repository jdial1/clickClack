package com.ampersand.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BucketListTest {

    private val you = Member("a", "Justin", BrickColor.RED.argb)

    @Test
    fun `to-do oldest first, done most recent first`() {
        val items = listOf(
            BucketItem("1", "Northern lights", BucketGroup.GETAWAYS, "a", createdAtMillis = 30),
            BucketItem("2", "Learn to dance", BucketGroup.GOALS, "b", createdAtMillis = 10),
            BucketItem("3", "Picnic", BucketGroup.MOMENTS, "a", doneOn = LocalDate.of(2026, 5, 1)),
            BucketItem("4", "Hot air balloon", BucketGroup.MOMENTS, "b", doneOn = LocalDate.of(2026, 8, 1)),
        )
        val (todo, done) = BucketList.ordered(items)
        assertEquals(listOf("2", "1"), todo.map { it.id })
        assertEquals(listOf("4", "3"), done.map { it.id })
    }

    @Test
    fun `only a ticked item earns, and the brick id is stable per person`() {
        val open = BucketItem("x", "Picnic", BucketGroup.MOMENTS, "a")
        assertNull(BucketList.reward(open, you))
        val ticked = open.copy(doneOn = LocalDate.of(2026, 9, 13))
        val brick = BucketList.reward(ticked, you)!!
        assertEquals("bucket_x_a", brick.id)
        assertEquals(LocalDate.of(2026, 9, 13), brick.earnedOn)
    }
}
