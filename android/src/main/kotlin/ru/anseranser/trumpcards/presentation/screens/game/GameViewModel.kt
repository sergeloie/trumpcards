package ru.anseranser.trumpcards.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.anseranser.engine.RankSet
import ru.anseranser.trumpcards.data.SettingsDataStore
import ru.anseranser.trumpcards.domain.GameIntent
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.domain.gameReducer
import ru.anseranser.trumpcards.engine.GameEngineBridge
import ru.anseranser.trumpcards.engine.MoveChoice

class GameViewModel(
    private val engineBridge: GameEngineBridge,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _rankSet = MutableStateFlow(RankSet.SIX_TO_ACE)
    val rankSet: StateFlow<RankSet> = _rankSet.asStateFlow()

    init {
        viewModelScope.launch {
            settings.rankSet.collect { remembered ->
                _rankSet.value = remembered
            }
        }
        viewModelScope.launch {
            engineBridge.state.collect { engineState ->
                _state.value = engineState
            }
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.StartGame -> {
                _state.value = gameReducer(_state.value, intent)
                _rankSet.value = intent.rankSet
                viewModelScope.launch { settings.setRankSet(intent.rankSet) }
                engineBridge.startGame(intent.rankSet)
            }
            is GameIntent.CardChosen -> {
                _state.value = gameReducer(_state.value, intent)
                engineBridge.choose(MoveChoice.PlayCard(intent.card))
            }
            GameIntent.TakePotChosen -> {
                _state.value = gameReducer(_state.value, intent)
                engineBridge.choose(MoveChoice.TakePot)
            }
            GameIntent.NewGame -> {
                engineBridge.stop()
                _state.value = gameReducer(_state.value, intent)
            }
            GameIntent.GoToMenu -> {
                engineBridge.stop()
                _state.value = gameReducer(_state.value, intent)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineBridge.stop()
    }
}
