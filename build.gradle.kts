// Root project: a container for the :core engine and platform modules.
// It declares no sources of its own and no application plugin — each module
// configures its own plugins (core = console app, desktop-libgdx = libgdx run).
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
}

subprojects {
    repositories {
        // Use Maven Central for resolving dependencies.
        mavenCentral()
        google()
    }
}
