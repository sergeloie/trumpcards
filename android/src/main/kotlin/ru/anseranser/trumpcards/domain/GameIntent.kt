package ru.anseranser.trumpcards.domain

import ru.anseranser.engine.Card
import ru.anseranser.engine.RankSet

sealed interface GameIntent {
    data class StartGame(val rankSet: RankSet) : GameIntent
    data class CardChosen(val card: Card) : GameIntent
    data object TakePotChosen : GameIntent
    data object NewGame : GameIntent
    data object GoToMenu : GameIntent
}
