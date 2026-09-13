package com.ampersand.model

/** Whose move it is on one thing. */
enum class Turn { YOURS, THEIRS }

enum class InboxKind(val label: String) { DECK("Deck"), SITTING("Test") }

data class InboxItem(
    val kind: InboxKind,
    val id: String,
    val title: String,
    val turn: Turn,
    val youDone: Boolean,
    val themDone: Boolean,
)

/**
 * What is waiting, and on whom: every deck or test one of you has finished and
 * the other has not.
 *
 * Today's question is not in here. Its state is on its own card, which is the
 * first thing on the screen; listing it twice would make the list look busier
 * than the day is.
 *
 * Yours first, then theirs, each in the order the decks and tests are listed,
 * so the list does not reshuffle every time something lands.
 */
object Inbox {

    fun of(state: CoupleState, decks: List<Quiz>, sittings: List<Instrument>): List<InboxItem> {
        val deckItems = decks.mapNotNull { quiz ->
            val you = state.myRun(quiz.id) != null
            val them = state.theirRun(quiz.id) != null
            if (you == them) return@mapNotNull null
            InboxItem(InboxKind.DECK, quiz.id, quiz.title, if (you) Turn.THEIRS else Turn.YOURS, you, them)
        }
        val sittingItems = sittings.mapNotNull { kit ->
            val run = state.instrumentRuns.lastOrNull { it.instrumentId == kit.id } ?: return@mapNotNull null
            // Their run stays unreadable until the seal lifts, so a finished
            // partner shows up as a full progress count rather than as answers.
            val you = run.answeredBy(state.you.uid, kit.length) >= kit.length
            val them = run.answeredBy(state.them.uid, kit.length) >= kit.length
            if (you == them) return@mapNotNull null
            InboxItem(InboxKind.SITTING, kit.id, kit.title, if (you) Turn.THEIRS else Turn.YOURS, you, them)
        }
        return (deckItems + sittingItems).sortedBy { it.turn.ordinal }
    }
}
