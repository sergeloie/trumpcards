# ProGuard rules for trumpcards Android module

# Keep core game model classes (used via Gson serialization)
-keep class ru.anseranser.model.** { *; }
-keep class ru.anseranser.event.** { *; }

# Keep Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
