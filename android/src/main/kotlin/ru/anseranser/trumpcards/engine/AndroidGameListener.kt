package ru.anseranser.trumpcards.engine

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import ru.anseranser.event.GameEvent
import ru.anseranser.event.GameListener

class AndroidGameListener : GameListener {

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events

    override fun onEvent(event: GameEvent) {
        _events.tryEmit(event)
    }
}
