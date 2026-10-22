package ru.anseranser.trumpcards.domain

fun gameReducer(state: GameState, intent: GameIntent): GameState {
    return when (intent) {
        is GameIntent.StartGame -> state.copy(
            phase = GamePhase.PLAYING,
            rankSet = intent.rankSet,
            choice = null,
        )
        is GameIntent.CardChosen -> state.copy(choice = null)
        GameIntent.TakePotChosen -> state.copy(choice = null)
        GameIntent.NewGame -> GameState()
        GameIntent.GoToMenu -> GameState()
    }
}
