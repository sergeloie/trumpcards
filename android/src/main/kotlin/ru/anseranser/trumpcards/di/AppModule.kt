package ru.anseranser.trumpcards.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.anseranser.engine.Card
import ru.anseranser.trumpcards.data.SettingsDataStore
import ru.anseranser.trumpcards.engine.GameEngineBridge
import ru.anseranser.trumpcards.presentation.screens.game.GameViewModel

/** The seat the person at the table plays. Decided once, in the composition root. */
private val HUMAN_SEAT = Card.Suit.SPADES

val appModule = module {
    single { SettingsDataStore(androidContext()) }
    single { GameEngineBridge(HUMAN_SEAT) }
    viewModel { GameViewModel(get(), get()) }
}
