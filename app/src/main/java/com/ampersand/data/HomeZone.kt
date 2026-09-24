package com.ampersand.data

import android.content.Context
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * The couple's home time zone, which decides what day it is on both phones.
 *
 * DESIGN.md: day boundaries follow the couple's home timezone, never device
 * time. The day is the seal's key, so two phones that disagree about the date
 * answer two different days and neither ever opens.
 *
 * The zone lives on the couple document; this caches the last one seen per
 * couple, so the first frame after launch is already on the right day.
 */
object HomeZone {

    private const val PREFS = "home_zone"

    /** [id] as a zone, or the device's own when there is none yet or it doesn't parse. */
    fun of(id: String?): ZoneId =
        id?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    /** Today at home. */
    fun today(id: String?, clock: Clock = Clock.systemUTC()): LocalDate =
        LocalDate.now(clock.withZone(of(id)))

    fun read(context: Context, coupleId: String): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(coupleId, null)

    fun write(context: Context, coupleId: String, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(coupleId, id).apply()
    }
}
