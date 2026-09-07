# R8 / ProGuard rules for Net Speed.
#
# Most libraries here ship their own consumer rules, so this file only covers what R8 cannot see
# on its own: code reached by reflection, by the framework via the manifest, or by name.

# ---------------------------------------------------------------------------------------------
# Crash reporting
# ---------------------------------------------------------------------------------------------
# Without these a release stack trace is unreadable: no file, no line, and synthetic frames from
# coroutines collapsed into nothing. Costs a little size and leaks no logic.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotations drive Room's generated code and the serialization plugin; stripping them breaks
# both in ways that only appear at runtime.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# ---------------------------------------------------------------------------------------------
# Android components declared in the manifest
# ---------------------------------------------------------------------------------------------
# The framework instantiates these by name. R8 sees no call site and would otherwise be free to
# rename or remove them, which surfaces as a silent no-op rather than a crash: a widget that
# never updates, a tile that does nothing, a service that never restarts after reboot.
-keep class com.sultonuzdev.netspeed.data.services.SpeedMonitorService { *; }
-keep class com.sultonuzdev.netspeed.data.receivers.BootReceiver { *; }
-keep class com.sultonuzdev.netspeed.data.widget.SpeedWidgetProvider { *; }
-keep class com.sultonuzdev.netspeed.data.widget.UsageWidgetProvider { *; }
-keep class com.sultonuzdev.netspeed.data.tile.SpeedTileService { *; }
-keep class com.sultonuzdev.netspeed.NetSpeedApplication { *; }

# ---------------------------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------------------------
# room-runtime ships consumer rules covering the generated implementations. Entities are kept
# explicitly because their field names are the column names: renaming a field renames the column
# and the generated SQL stops matching the database on disk.
-keep class com.sultonuzdev.netspeed.data.database.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------------------------
# Koin
# ---------------------------------------------------------------------------------------------
# Koin 4 resolves through explicit constructor lambdas rather than reflection, so no keep rules
# are needed for the modules themselves. It does look types up by class, so the classes named in
# a module must survive with their identity intact -- which the rules above and below cover.
-dontwarn org.koin.**

# ---------------------------------------------------------------------------------------------
# kotlinx.serialization
# ---------------------------------------------------------------------------------------------
# @Serializable is declared on three domain models. Nothing serializes them today, but the
# generated serializers are found by name, so a future Json call would fail in release only.
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers class com.sultonuzdev.netspeed.domain.models.** {
    *** Companion;
}
-keepclasseswithmembers class com.sultonuzdev.netspeed.domain.models.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------------------------
# Domain and UI state
# ---------------------------------------------------------------------------------------------
# Data classes crossing the Compose state boundary. Kept whole so that equals/hashCode and the
# generated componentN functions survive, which recomposition depends on.
-keep class com.sultonuzdev.netspeed.domain.models.** { *; }

# ---------------------------------------------------------------------------------------------
# Coroutines
# ---------------------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------------------------
# The compiler plugin emits what it needs; these only silence warnings about optional desktop and
# tooling classes that are absent on Android.
-dontwarn androidx.compose.**
