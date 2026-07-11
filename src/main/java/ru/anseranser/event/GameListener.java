package ru.anseranser.event;

/**
 * Receiver of {@link GameEvent}s produced by the game engine.
 *
 * Implementations live in the presentation layer (console, UI) and are
 * responsible for all rendering and translation. The core never depends on
 * any concrete listener.
 */
public interface GameListener {
    void onEvent(GameEvent event);
}
