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
include("bots")
include("ml")
include("console")
include("desktop-libgdx")
include("android")
include("tools:exporter")
include("tools:verify")
include("tools:eval")