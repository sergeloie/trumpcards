pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "trumpcards"

// :core is the pure game engine (model + events + i18n). It has no main entry
// point and no platform dependencies. Each platform (console, desktop-libgdx,
// and later android / html) is a separate module that depends on :core and only
// provides rendering + input, reusing the engine's GameDriver and events.
include("core")
include("console")
include("desktop-libgdx")
