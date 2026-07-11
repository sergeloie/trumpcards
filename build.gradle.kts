// Root project: a container for the :core engine and platform modules.
// It declares no sources of its own and no application plugin — each module
// configures its own plugins (core = console app, desktop-libgdx = libgdx run).
subprojects {
    repositories {
        // Use Maven Central for resolving dependencies.
        mavenCentral()
    }
}
