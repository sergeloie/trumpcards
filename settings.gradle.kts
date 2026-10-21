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
include("core")
include("engine")
include("console")
include("desktop-libgdx")
include("android")