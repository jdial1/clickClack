package com.ampersand.model

import java.time.LocalDate

/** One message in a conversation: a line of text or an emote. */
data class ChatMessage(
    val id: String,
    val byUid: String,
    val text: String? = null,
    val emote: Reaction? = null,
    val atMillis: Long = 0,
)

/** The latest state of a conversation, for previews without loading every message. */
data class ChatPreview(val lastText: String, val lastByUid: String, val count: Int)

/** A photo kept from a special day. [data] is a base64 JPEG. */
data class DayPhoto(val id: String, val date: LocalDate, val label: String, val byUid: String, val data: String)

/** Conversation ids: one for each opened day, one for each day's moods. */
object Chats {
    fun day(date: LocalDate) = "day_$date"
    fun mood(date: LocalDate) = "mood_$date"

    fun title(id: String, them: String): String {
        val date = runCatching { LocalDate.parse(id.substringAfter('_')) }.getOrNull()
        val day = date?.let { "${it.dayOfMonth} ${it.month.name.lowercase().replaceFirstChar(Char::uppercase)}" }.orEmpty()
        return when {
            id.startsWith("mood_") -> "Moods, $day"
            else -> "The question, $day"
        }.let { if (them.isBlank()) it else it }
    }
}
