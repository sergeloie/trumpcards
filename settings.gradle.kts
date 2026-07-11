rootProject.name = "trumpcards"

// Core holds the game engine (model + events + i18n) and the console launcher.
// Platform modules (desktop-libgdx, later android / html) depend on :core and
// only provide rendering + input, reusing the engine's GameDriver and events.
include("core")
include("desktop-libgdx")
