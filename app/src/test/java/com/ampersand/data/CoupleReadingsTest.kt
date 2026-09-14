package com.ampersand.data

import com.ampersand.model.InstrumentRun
import com.ampersand.model.Member
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoupleReadingsTest {
    private val you = Member("a", "Sam", 0xFF0000FF)
    private val them = Member("b", "Tasia", 0xFFFF0000)

    private fun answers(i: com.ampersand.model.Instrument, pick: (Int) -> Int) =
        List(i.items.size * i.passes.size) { pick(it) }

    @Test
    fun everyLongerTestReadsTogetherOnceBothFinish() {
        Instruments.all.forEach { i ->
            val low = answers(i) { 1 }
            val mixed = answers(i) { if (it % 2 == 0) 5 else 1 }
            listOf(low to low, low to mixed).forEach { (x, y) ->
                val text = CoupleReadings.of(i.id, InstrumentRun(i.id, mapOf("a" to x, "b" to y)), you, them)
                assertNotNull(i.id, text)
                assertTrue(i.id, text!!.length > 40 && "null" !in text)
            }
            assertNull(CoupleReadings.of(i.id, InstrumentRun(i.id, mapOf("a" to low)), you, them))
        }
    }
}
