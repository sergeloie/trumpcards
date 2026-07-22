package ru.anseranser.trumpcards.domain

import ru.anseranser.model.DeckSize

fun gameReducer(state: GameState, intent: GameIntent): GameState {
    return when (intent) {
        is GameIntent.StartGame -> state.copy(
            phase = GamePhase.PLAYING,
            deckSize = intent.deckSize,
        )
        is GameIntent.CardChosen -> state.copy(
            isWaitingForInput = false,
            inputRequest = null,
        )
        is GameIntent.NewGame -> GameState()
        is GameIntent.GoToMenu -> GameState()
    }
}
