# Readium
-keep class org.readium.** { *; }
-dontwarn org.readium.**

# Room
-keep class com.kindlevibe.reader.data.db.** { *; }

# Kotlin serialization / coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
