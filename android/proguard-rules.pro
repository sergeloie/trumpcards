-keepattributes *Annotation*
-keep class ru.anseranser.trumpcards.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }

# Lombok is a compile-time-only processor (see :core/build.gradle.kts). The
# accessors it generates carry a @lombok.Generated annotation that survives into
# the bytecode, but the annotation class itself is never shipped, so R8 full mode
# needs to be told the missing reference is harmless.
-dontwarn lombok.Generated
