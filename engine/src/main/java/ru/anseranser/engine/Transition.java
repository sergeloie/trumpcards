package ru.anseranser.engine;

import java.util.List;

/**
 * The result of a step: the new state and the things that happened on the way.
 */
public record Transition(State state, List<Event> events) {

    public Transition {
        events = List.copyOf(events);
    }
}
