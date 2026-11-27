package com.ampersand.data

import com.ampersand.model.Answer
import com.ampersand.model.Block
import com.ampersand.model.BlockFace
import com.ampersand.model.BrickColor
import com.ampersand.model.BrickHeight
import com.ampersand.model.BrickShape
import com.ampersand.model.BucketGroup
import com.ampersand.model.BucketItem
import com.ampersand.model.BuildPlate
import com.ampersand.model.DayEntry
import com.ampersand.model.DayWeather
import com.ampersand.model.KnowingMonth
import com.ampersand.model.League
import com.ampersand.model.Member
import com.ampersand.model.Mood
import com.ampersand.model.MoodEntry
import com.ampersand.model.NameWords
import com.ampersand.model.Placement
import com.ampersand.model.Question
import com.ampersand.model.QuestionType
import com.ampersand.model.QuizRun
import com.ampersand.model.Reaction
import com.ampersand.model.Slope
import com.ampersand.model.Topic
import com.ampersand.model.Wildcard
import com.google.firebase.firestore.DocumentSnapshot
import java.time.LocalDate
import java.time.YearMonth

/*
 * Firestore documents to model types and back: the one codec for the layout in
 * README.md, "Firestore model". Reads are forgiving (a missing or malformed field
 * falls back or drops the item); writes only ever produce what the rules accept.
 */

/** A stored `yyyy-mm-dd`, or null when absent or malformed. Every stored date is read through this. */
internal fun String?.toDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun DocumentSnapshot?.toMember(uid: String, fallback: Member): Member = Member(
    uid = uid,
    name = this?.getString("displayName")?.takeIf { it.isNotBlank() } ?: fallback.name,
    colorArgb = this?.getLong("colorArgb") ?: fallback.colorArgb,
    gender = this?.getString("gender"),
    birthday = this?.getString("birthday").toDateOrNull(),
    mood = (this?.get("mood") as? Map<*, *>)?.let { m ->
        val mood = Mood.entries.firstOrNull { it.name == m["id"] } ?: return@let null
        val on = (m["on"] as? String).toDateOrNull() ?: return@let null
        MoodEntry(
            mood, on, BrickColor.entries.firstOrNull { it.slug == m["color"] },
            atMillis = (m["at"] as? Number)?.toLong() ?: 0,
            spans = (m["spans"] as? Map<*, *>).orEmpty().entries.mapNotNull { (k, v) ->
                val key = Mood.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
                (v as? Number)?.let { key to it.toLong() }
            }.toMap(),
        )
    },
)

internal fun DayWeather.toMap(): Map<String, Any?> =
    mapOf("tempF" to tempF, "summary" to summary, "city" to city, "precipChance" to precipChance, "daytime" to daytime)

internal fun DocumentSnapshot?.toWeather(): DayWeather? {
    val w = this?.get("weather") as? Map<*, *> ?: return null
    val temp = (w["tempF"] as? Number)?.toInt() ?: return null
    return DayWeather(
        tempF = temp,
        summary = w["summary"] as? String ?: "",
        city = w["city"] as? String,
        precipChance = (w["precipChance"] as? Number)?.toInt(),
        daytime = w["daytime"] as? Boolean,
    )
}

internal fun DocumentSnapshot?.leaguesByUid(): Map<String, Set<League>> =
    (this?.get("following") as? Map<*, *>).orEmpty().entries.mapNotNull { (uid, names) ->
        val key = uid as? String ?: return@mapNotNull null
        key to (names as? List<*>).orEmpty().mapNotNull { n -> League.entries.firstOrNull { it.name == n } }.toSet()
    }.toMap()

internal fun DocumentSnapshot?.topicsByUid(field: String): Map<String, Set<Topic>> =
    (this?.get(field) as? Map<*, *>).orEmpty().entries.mapNotNull { (uid, names) ->
        val key = uid as? String ?: return@mapNotNull null
        key to (names as? List<*>).orEmpty().mapNotNull { n -> Topic.entries.firstOrNull { it.name == n } }.toSet()
    }.toMap()

internal fun DocumentSnapshot?.toSubmitted(): Set<String> =
    (this?.get("submitted") as? List<*>).orEmpty().filterIsInstance<String>().toSet()

internal fun DocumentSnapshot?.toReactions(): Map<String, Reaction> {
    val raw = this?.get("reactions") as? Map<*, *> ?: return emptyMap()
    return raw.entries.mapNotNull { (k, v) ->
        val uid = k as? String ?: return@mapNotNull null
        val name = v as? String ?: return@mapNotNull null
        Reaction.entries.firstOrNull { it.name == name }?.let { uid to it }
    }.toMap()
}

internal fun DocumentSnapshot?.stringMap(field: String): Map<String, String> =
    (this?.get(field) as? Map<*, *>).orEmpty().entries
        .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }
        .toMap()

internal fun DocumentSnapshot?.toUidSet(field: String): Set<String> =
    (this?.get(field) as? List<*>).orEmpty().filterIsInstance<String>().toSet()

internal fun DocumentSnapshot?.toTalkedAbout(): Set<String> {
    @Suppress("UNCHECKED_CAST")
    val raw = this?.get("talkedAbout") as? List<String> ?: return emptySet()
    return raw.toSet()
}

internal fun DocumentSnapshot.toWildcard(): Wildcard? {
    val text = getString("text")?.takeIf { it.isNotBlank() } ?: return null
    val type = QuestionType.entries.firstOrNull { it.name == getString("type") } ?: return null
    @Suppress("UNCHECKED_CAST")
    val options = (get("options") as? List<String>).orEmpty()
    return Wildcard(
        id = id,
        text = text,
        type = type,
        options = options,
        byUid = getString("byUid").orEmpty(),
        byName = getString("byName").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        usedOn = getString("usedOn").toDateOrNull(),
    )
}

internal fun DocumentSnapshot.toBucketItem(): BucketItem? {
    val text = getString("text")?.takeIf { it.isNotBlank() } ?: return null
    return BucketItem(
        id = id,
        text = text,
        group = BucketGroup.entries.firstOrNull { it.name == getString("group") } ?: BucketGroup.GOALS,
        byUid = getString("byUid").orEmpty(),
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        doneOn = getString("doneOn").toDateOrNull(),
    )
}

internal fun DocumentSnapshot.toKnowingMonth(myUid: String, partnerUid: String): KnowingMonth? {
    val month = runCatching { YearMonth.parse(id) }.getOrNull() ?: return null
    var mine = 0
    var theirs = 0
    var scored = 0
    data.orEmpty().forEach { (_, value) ->
        val day = value as? Map<*, *> ?: return@forEach
        scored++
        if (day[myUid] == true) mine++
        if (day[partnerUid] == true) theirs++
    }
    return KnowingMonth(month, youHits = mine, theyHits = theirs, scored = scored)
}

internal fun DocumentSnapshot.toAnswer(): Answer? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val values = get("values") as? List<String> ?: return null
    @Suppress("UNCHECKED_CAST")
    val guess = get("guess") as? List<String>
    return Answer(uid = id, values = values, guess = guess)
}

internal fun DocumentSnapshot.toDayEntry(pool: QuestionPool, resolve: (String?, LocalDate) -> Question?): DayEntry? {
    val date = id.toDateOrNull() ?: return null
    // Skip day docs that only hold weather.
    if (getString("questionId") == null && getBoolean("revealed") != true && toSubmitted().isEmpty()) return null
    val question = resolve(getString("questionId"), date) ?: pool.daily(date)
    // No answers here (subcollection read per day); the Reveal screen loads them.
    return DayEntry(
        date = date,
        question = question,
        reactions = toReactions(),
        notes = stringMap("notes"),
        moodReplies = stringMap("moodReplies"),
        talkedAbout = toTalkedAbout(),
        askAgain = toUidSet("askAgain"),
        opened = getBoolean("revealed") == true,
        submitted = toSubmitted(),
        nextTheme = getString("nextTheme"),
        nextThemeBy = getString("nextThemeBy"),
        recordedRun = getLong("run")?.toInt() ?: 0,
    )
}

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toQuizRun(quizId: String): QuizRun? {
    if (!exists()) return null
    val answers = (get("answers") as? Map<String, List<String>>).orEmpty()
    if (answers.isEmpty()) return null
    return QuizRun(
        quizId = quizId,
        uid = id,
        answers = answers,
        guesses = (get("guesses") as? Map<String, List<String>>).orEmpty(),
        completedAtMillis = getTimestamp("completedAt")?.toDate()?.time,
    )
}

internal fun DocumentSnapshot.toPlate(): BuildPlate? {
    val started = getString("startedOn").toDateOrNull() ?: return null
    @Suppress("UNCHECKED_CAST")
    val blocks = (get("blocks") as? List<Map<String, Any?>>).orEmpty().mapNotNull { it.toBlock() }
    @Suppress("UNCHECKED_CAST")
    // Placements are appended, so two phones placing the same brick at once leave two; the first stands.
    val placed = (get("placed") as? List<Map<String, Any?>>).orEmpty().mapNotNull { it.toPlacement() }.distinctBy { it.blockId }
    return BuildPlate(
        id = id,
        startedOn = started,
        periodDays = getLong("periodDays")?.toInt() ?: BuildPlate.PERIOD_DAYS,
        gridW = (getLong("gridW") ?: BuildPlate.GRID_W.toLong()).toInt(),
        gridH = (getLong("gridH") ?: BuildPlate.GRID_H.toLong()).toInt(),
        blocks = blocks,
        placed = placed,
        completedOn = getString("completedOn").toDateOrNull(),
        notes = (get("notes") as? Map<*, *>).orEmpty().entries
            .mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }
            .toMap(),
        words = (get("words") as? Map<*, *>).orEmpty().entries.mapNotNull { (k, v) ->
            val m = v as? Map<*, *> ?: return@mapNotNull null
            val describing = m["describing"] as? String ?: return@mapNotNull null
            val thing = m["thing"] as? String ?: return@mapNotNull null
            (k as? String)?.let { it to NameWords(describing, thing) }
        }.toMap(),
    )
}

internal fun Map<String, Any?>.toBlock(): Block? {
    val id = this["id"] as? String ?: return null
    val owner = this["ownerUid"] as? String ?: return null
    val shape = BrickShape(
        studs = (this["studs"] as? Number)?.toInt() ?: return null,
        height = BrickHeight.entries.firstOrNull { it.name == this["height"] } ?: BrickHeight.LOW,
        slope = Slope.entries.firstOrNull { it.name == this["slope"] } ?: Slope.NONE,
    )
    return Block(
        id = id,
        shape = shape,
        colorArgb = (this["colorArgb"] as? Number)?.toLong() ?: 0xFFF54E4F,
        ownerUid = owner,
        earnedOn = (this["earnedOn"] as? String).toDateOrNull()
            ?: LocalDate.now(),
        face = BlockFace.entries.firstOrNull { it.name == this["face"] },
        moodGlyph = this["moodGlyph"] as? String,
    )
}

internal fun Map<String, Any?>.toPlacement(): Placement? {
    val id = this["blockId"] as? String ?: return null
    return Placement(
        blockId = id,
        x = (this["x"] as? Number)?.toInt() ?: return null,
        y = (this["y"] as? Number)?.toInt() ?: return null,
    )
}

internal fun Block.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "studs" to shape.studs,
    "height" to shape.height.name,
    "slope" to shape.slope.name,
    "colorArgb" to colorArgb,
    "ownerUid" to ownerUid,
    "earnedOn" to earnedOn.toString(),
    "face" to face?.name,
    "moodGlyph" to moodGlyph,
)

internal fun Placement.toMap(): Map<String, Any?> = mapOf("blockId" to blockId, "x" to x, "y" to y)

internal fun BuildPlate.toMap(): Map<String, Any?> = mapOf(
    "startedOn" to startedOn.toString(),
    "periodDays" to periodDays,
    "gridW" to gridW,
    "gridH" to gridH,
) + (
    // Only when set: merging completedOn = null from a stale plate would un-seal it.
    completedOn?.let { mapOf("completedOn" to it.toString()) } ?: emptyMap()
)

