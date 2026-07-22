package ru.anseranser.trumpcards.presentation.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.anseranser.trumpcards.domain.GameIntent
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.domain.gameReducer
import ru.anseranser.trumpcards.engine.GameEngineBridge

class GameViewModel(
    private val engineBridge: GameEngineBridge,
) : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    init {
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
                engineBridge.startGame(intent.deckSize)
            }
            is GameIntent.CardChosen -> {
                _state.value = gameReducer(_state.value, intent)
                engineBridge.chooseCard(intent.card)
            }
            is GameIntent.NewGame -> {
                engineBridge.stop()
                _state.value = gameReducer(_state.value, intent)
            }
            is GameIntent.GoToMenu -> {
                engineBridge.stop()
                _state.value = gameReducer(_state.value, intent)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        engineBridge.destroy()
    }
}
