package ru.anseranser.trumpcards.domain

import ru.anseranser.model.Card
import ru.anseranser.model.DeckSize

sealed interface GameIntent {
    data class StartGame(val deckSize: DeckSize) : GameIntent
    data class CardChosen(val card: Card) : GameIntent
    data object NewGame : GameIntent
    data object GoToMenu : GameIntent
}
