package ru.anseranser.trumpcards.domain

sealed interface GameEffect {
    data class ShowError(val message: String) : GameEffect
    data object NavigateToGame : GameEffect
    data object NavigateToMenu : GameEffect
}
