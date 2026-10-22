package ru.anseranser.trumpcards.domain

import ru.anseranser.engine.Card
import ru.anseranser.engine.RankSet

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val rankSet: RankSet? = null,
    val trump: Card.Suit? = null,
    val scoreboard: Map<Card.Suit, List<Card>> = emptyMap(),
    val pot: List<Card> = emptyList(),
    val humanHand: List<Card> = emptyList(),
    val opponents: List<OpponentState> = emptyList(),
    val choice: PlayerChoice? = null,
    val winner: Card.Suit? = null,
    val humanSeat: Card.Suit? = null,
)

enum class GamePhase { MENU, PLAYING, GAME_OVER }

data class OpponentState(
    val suit: Card.Suit,
    val cardCount: Int,
    val isEliminated: Boolean = false,
)

/**
 * The move the person at the table may choose from: the cards that may be
 * played, and whether the pot may be taken.
 */
data class PlayerChoice(
    val playableCards: List<Card>,
    val canTakePot: Boolean,
)
