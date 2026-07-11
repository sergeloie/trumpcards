package ru.anseranser.event;

/**
 * No-op listener used when no presentation layer is attached (e.g. headless
 * tests). Introduced in refactor Stage 1 so the core never has to special-case
 * the absence of a listener.
 */
public enum NopListener implements GameListener {
    INSTANCE;

    @Override
    public void onEvent(GameEvent event) {
        // intentionally does nothing
    }
}
