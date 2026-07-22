package ru.anseranser.trumpcards.domain

import ru.anseranser.model.Card
import ru.anseranser.model.DeckSize

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val deckSize: DeckSize? = null,
    val trump: Card.Suit? = null,
    val scoreboard: Map<Card.Suit, List<Card>> = emptyMap(),
    val pot: List<Card> = emptyList(),
    val humanHand: List<Card> = emptyList(),
    val opponents: List<OpponentState> = emptyList(),
    val isWaitingForInput: Boolean = false,
    val inputRequest: InputRequestInfo? = null,
    val winner: Card.Suit? = null,
    val statusMessage: String = "",
    val lastEvent: GameEventSnapshot? = null,
)

enum class GamePhase { MENU, PLAYING, GAME_OVER }

data class OpponentState(
    val suit: Card.Suit,
    val cardCount: Int,
    val isEliminated: Boolean = false,
)

data class InputRequestInfo(
    val kind: InputRequestKind,
    val validCards: List<Card>,
)

enum class InputRequestKind { LEAD, DEFEND }

data class GameEventSnapshot(
    val type: String,
    val message: String,
)
