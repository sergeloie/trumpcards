package ru.anseranser.android.engine

import ru.anseranser.model.Card
import ru.anseranser.model.GameState

/**
 * Immutable snapshot of the game state for Compose UI rendering.
 * Analogous to GameSnapshot in desktop-libgdx, but designed for Compose's
 * immutable state model.
 */
data class ComposeSnapshot(
    val trump: Card.Suit,
    val scoreboard: Map<Card.Suit, List<Card>>,
    val pot: List<Card>,
    val humanHand: List<Card>,
    val humanValidChoices: List<Card>,
    val opponents: List<OpponentView>,
    val statusText: String,
    val logLines: List<String>,
    val isGameOver: Boolean,
    val winnerSuit: Card.Suit?,
    val isHumanTurn: Boolean
) {
    data class OpponentView(
        val trump: Card.Suit,
        val cardCount: Int,
        val isActive: Boolean
    )

    companion object {
        /** Human always plays SPADES. */
        private val HUMAN_SUIT = Card.Suit.SPADES

        fun capture(
            state: GameState,
            validChoices: List<Card>,
            log: List<String>,
            isHumanTurn: Boolean,
            isGameOver: Boolean,
            winnerSuit: Card.Suit?
        ): ComposeSnapshot {
            val opponents = state.players()
                .filter { it.gamer() && it.trump() != HUMAN_SUIT }
                .map { p ->
                    OpponentView(
                        trump = p.trump(),
                        cardCount = p.hand().size,
                        isActive = true
                    )
                }

            val humanHand = state.players()
                .firstOrNull { it.trump() == HUMAN_SUIT && it.gamer() }
                ?.hand() ?: emptyList()

            val gamersAlive = state.players().count { it.gamer() }
            val status = buildString {
                append("Trump: ${state.trump()}")
                append("  |  Players: $gamersAlive")
            }

            return ComposeSnapshot(
                trump = state.trump(),
                scoreboard = state.scoreboard(),
                pot = state.pot(),
                humanHand = humanHand,
                humanValidChoices = validChoices,
                opponents = opponents,
                statusText = status,
                logLines = log.takeLast(50),
                isGameOver = isGameOver,
                winnerSuit = winnerSuit,
                isHumanTurn = isHumanTurn
            )
        }

        fun empty(): ComposeSnapshot = ComposeSnapshot(
            trump = Card.Suit.SPADES,
            scoreboard = emptyMap(),
            pot = emptyList(),
            humanHand = emptyList(),
            humanValidChoices = emptyList(),
            opponents = emptyList(),
            statusText = "",
            logLines = emptyList(),
            isGameOver = false,
            winnerSuit = null,
            isHumanTurn = false
        )
    }
}
