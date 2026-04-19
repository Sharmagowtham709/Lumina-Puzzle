# Keep DataStore preference keys and Kotlin metadata used in serialization-safe contexts.
-keep class kotlin.Metadata { *; }

# AdMob requirements
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep Compose internal names for optimization
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}
