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
 * Decks and tests one of you has finished and the other has not.
 * Excludes today's question (it has its own card). Yours first, then theirs, in list order.
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
            // Partner's answers are sealed, so use their progress count.
            val you = run.answeredBy(state.you.uid, kit.length) >= kit.length
            val them = run.answeredBy(state.them.uid, kit.length) >= kit.length
            if (you == them) return@mapNotNull null
            InboxItem(InboxKind.SITTING, kit.id, kit.title, if (you) Turn.THEIRS else Turn.YOURS, you, them)
        }
        return (deckItems + sittingItems).sortedBy { it.turn.ordinal }
    }
}
