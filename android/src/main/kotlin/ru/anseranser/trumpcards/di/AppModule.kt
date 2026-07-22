package ru.anseranser.trumpcards.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ru.anseranser.trumpcards.data.SettingsDataStore
import ru.anseranser.trumpcards.engine.GameEngineBridge
import ru.anseranser.trumpcards.presentation.screens.game.GameViewModel

val appModule = module {
    single { SettingsDataStore(androidContext()) }
    single { GameEngineBridge() }
    viewModel { GameViewModel(get()) }
}
